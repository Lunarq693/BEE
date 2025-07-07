package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class RaidGUI implements Listener {
    private final JavaPlugin plugin;
    private static RaidGUI instance;
    private static final Map<UUID, Long> processingPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastCommandTime = new ConcurrentHashMap<>();
    private static final long PROCESSING_TIMEOUT = 3000; // 3 seconds

    public RaidGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        // Only register events once using singleton pattern
        if (instance == null) {
            instance = this;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§c⚔ Raid Management");

        // Create the glass pane pattern (same as Town Management)
        for (int i = 0; i < 27; i++) {
            boolean isEdge = i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    // Top row pattern
                    paneMaterial = (i == 0 || i == 8) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            : (i % 2 == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
                } else if (i >= 18) {
                    // Bottom row pattern
                    int pos = i - 18;
                    paneMaterial = (pos == 0 || pos == 8) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            : (pos % 2 == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
                } else {
                    // Side columns
                    paneMaterial = Material.LIME_STAINED_GLASS_PANE;
                }

                gui.setItem(i, createPane(paneMaterial));
            }
        }

        // Add the 3 main items in the middle row (slots 11, 13, 15)
        gui.setItem(11, createGlowingMenuItem(Material.SPAWNER, "§5Dungeons", "§7Explore dangerous dungeons", "§7for rare loot and rewards"));
        gui.setItem(13, createGlowingMenuItem(Material.NETHERITE_SWORD, "§cStart a Raid", "§7Attack another town", "§7Stand in their territory and click!"));
        gui.setItem(15, createGlowingMenuItem(Material.ZOMBIE_HEAD, "§6Boss Info", "§7View information about", "§7raid bosses and upgrades"));

        player.openInventory(gui);
    }

    private ItemStack createPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createGlowingMenuItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of(lore));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean isPlayerProcessing(UUID playerId) {
        Long processingTime = processingPlayers.get(playerId);
        if (processingTime == null) {
            return false;
        }

        // Check if processing has timed out
        if (System.currentTimeMillis() - processingTime > PROCESSING_TIMEOUT) {
            processingPlayers.remove(playerId);
            return false;
        }

        return true;
    }

    private static void setPlayerProcessing(UUID playerId) {
        processingPlayers.put(playerId, System.currentTimeMillis());

        // Auto-cleanup after timeout
        Bukkit.getScheduler().runTaskLater(instance.plugin, () -> {
            processingPlayers.remove(playerId);
        }, PROCESSING_TIMEOUT / 50); // Convert ms to ticks
    }

    private void executeRaidCommand(Player player) {
        UUID playerId = player.getUniqueId();

        if (isPlayerProcessing(playerId)) {
            return;
        }

        setPlayerProcessing(playerId);
        lastCommandTime.put(playerId, System.currentTimeMillis());

        player.closeInventory();

        // Execute command on main thread
        if (Bukkit.isPrimaryThread()) {
            player.performCommand("raid");
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("raid"));
        }

        // Schedule auto-reopen check
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getSize() == 0) {
                Long commandTime = lastCommandTime.get(playerId);
                if (commandTime != null && System.currentTimeMillis() - commandTime < 4000) {
                    // Check if player is not in a dungeon/raid world
                    String worldName = player.getWorld().getName().toLowerCase();
                    if (!worldName.contains("dungeon") && !worldName.contains("instance") &&
                            !worldName.contains("raid") && !worldName.contains("mythic")) {
                        // Reopen the GUI
                        new RaidGUI(plugin).open(player);
                    }
                }
            }
        }, 60L); // 3 seconds
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§c⚔ Raid Management")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().toString().contains("GLASS_PANE")) return;

        UUID playerId = player.getUniqueId();
        if (isPlayerProcessing(playerId)) {
            return;
        }

        switch (clicked.getType()) {
            case SPAWNER:
                // Open Dungeons GUI
                setPlayerProcessing(playerId);
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new DungeonsGUI(plugin).open(player);
                    processingPlayers.remove(playerId);
                });
                break;

            case NETHERITE_SWORD:
                // Execute /raid command
                executeRaidCommand(player);
                break;

            case ZOMBIE_HEAD:
                // Open Boss Info GUI
                setPlayerProcessing(playerId);
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new BossInfoGUI(plugin).open(player);
                    processingPlayers.remove(playerId);
                });
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§c⚔ Raid Management")) {
            UUID playerId = event.getPlayer().getUniqueId();
            // Don't remove processing state immediately, let it timeout naturally
            // This allows for auto-reopen functionality
        }
    }

    public static void cleanup() {
        processingPlayers.clear();
        lastCommandTime.clear();
    }
}
