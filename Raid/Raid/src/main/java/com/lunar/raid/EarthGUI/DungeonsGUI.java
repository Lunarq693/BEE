package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonsGUI implements Listener {
    private static final Map<UUID, String> awaitingInput = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> processingPlayers = new ConcurrentHashMap<>();
    private static final long PROCESSING_TIMEOUT = 2000; // 2 seconds
    private static DungeonsGUI instance;
    private static JavaPlugin pluginInstance;

    private final JavaPlugin plugin;

    public DungeonsGUI(JavaPlugin plugin) {
        this.plugin = plugin;

        // Only register events once
        if (instance == null) {
            instance = this;
            pluginInstance = plugin;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    public void open(Player player) {
        // Clear any existing processing state for this player
        UUID playerId = player.getUniqueId();
        processingPlayers.remove(playerId);
        awaitingInput.remove(playerId);

        Inventory gui = Bukkit.createInventory(null, 54, "§5🏰 Dungeons");

        // Create the glass pane pattern for larger GUI
        for (int i = 0; i < 54; i++) {
            boolean isEdge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    paneMaterial = (i == 0 || i == 8) ? Material.PURPLE_STAINED_GLASS_PANE
                            : (i % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE);
                } else if (i >= 45) {
                    int pos = i - 45;
                    paneMaterial = (pos == 0 || pos == 8) ? Material.PURPLE_STAINED_GLASS_PANE
                            : (pos % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE);
                } else {
                    paneMaterial = Material.MAGENTA_STAINED_GLASS_PANE;
                }

                gui.setItem(i, createPane(paneMaterial));
            }
        }

        // Add dungeons in a 3x3 grid pattern with new names, lore, and prices
        // Row 2
        gui.setItem(11, createDungeonItem(Material.BLACKSTONE, "§8Tier I", "The Dark Knight Boss", "FREE", null, player));
        gui.setItem(13, createDungeonItem(Material.OAK_LEAVES, "§2Tier II", "Forest Guardian Boss", "$15,000", "dungeon.tier2", player));
        gui.setItem(15, createDungeonItem(Material.BONE_BLOCK, "§fTier III", "Nightarrow Wendigo Boss", "$50,000", "dungeon.tier3", player));

        // Row 3
        gui.setItem(20, createDungeonItem(Material.OBSERVER, "§6Tier IV", "The Deceptor Boss", "$100,000", "dungeon.tier4", player));
        gui.setItem(22, createDungeonItem(Material.AMETHYST_BLOCK, "§dTier V", "The Illusionist Boss", "$200,000", "dungeon.tier5", player));
        gui.setItem(24, createDungeonItem(Material.PACKED_ICE, "§bTier VI", "Krampus Boss", "$350,000", "dungeon.tier6", player));

        // Row 4
        gui.setItem(29, createDungeonItem(Material.COBBLESTONE, "§7Tier VII", "Tower Skeleton Boss", "$707,707", "dungeon.tier7", player));
        gui.setItem(31, createDungeonItem(Material.SLIME_BLOCK, "§aTier VIII", "Queen of Thorns Boss", "$1,000,000", "dungeon.tier8", player));

        // Back button
        gui.setItem(49, createMenuItem(Material.ARROW, "§fBack to Raid Menu", "§7Return to the main raid menu"));

        // Party invite button (bottom right corner)
        gui.setItem(53, createGlowingMenuItem(Material.GOLD_BLOCK, "§6Party Invite", "§7Click to invite a player to your party"));

        player.openInventory(gui);
    }

    private ItemStack createDungeonItem(Material material, String name, String boss, String price, String permission, Player player) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            // Create lore with boss name and price
            if (permission == null || player.hasPermission(permission)) {
                // Player has access
                meta.setLore(Arrays.asList(
                        "§7" + boss,
                        "§6Price: §a" + price,
                        "",
                        "§aClick to enter dungeon!"
                ));
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                // Player doesn't have permission
                meta.setLore(Arrays.asList(
                        "§7" + boss,
                        "§6Price: §c" + price,
                        "",
                        "§c§lLOCKED",
                        "§7You need permission to access this dungeon!"
                ));
                // No enchantment for locked dungeons
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean isProcessing(UUID playerId) {
        if (processingPlayers.containsKey(playerId)) {
            long processingTime = processingPlayers.get(playerId);
            if (System.currentTimeMillis() - processingTime < PROCESSING_TIMEOUT) {
                return true;
            } else {
                processingPlayers.remove(playerId);
            }
        }
        return false;
    }

    private static void setProcessing(UUID playerId) {
        processingPlayers.put(playerId, System.currentTimeMillis());

        // Auto-remove after timeout
        if (pluginInstance != null) {
            Bukkit.getScheduler().runTaskLater(pluginInstance, () -> {
                processingPlayers.remove(playerId);
            }, PROCESSING_TIMEOUT / 50); // Convert to ticks
        }
    }

    private void executeDungeonCommand(Player player, String dungeonName, String permission) {
        UUID playerId = player.getUniqueId();

        if (isProcessing(playerId)) {
            return;
        }

        // Check permission if required
        if (permission != null && !player.hasPermission(permission)) {
            player.sendMessage("§c§l[Dungeons] §7You don't have permission to access this dungeon!");
            return;
        }

        setProcessing(playerId);
        player.closeInventory();

        // Execute command immediately on main thread
        if (Bukkit.isPrimaryThread()) {
            player.performCommand("md play " + dungeonName);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("md play " + dungeonName);
            });
        }
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
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMenuItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§5🏰 Dungeons")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().toString().contains("GLASS_PANE")) return;

        UUID playerId = player.getUniqueId();

        // Double check processing state
        if (isProcessing(playerId)) {
            return;
        }

        switch (clicked.getType()) {
            case BLACKSTONE:
                executeDungeonCommand(player, "TheDarkKnight", null);
                break;
            case OAK_LEAVES:
                executeDungeonCommand(player, "ForestGuardian", "dungeon.tier2");
                break;
            case BONE_BLOCK:
                executeDungeonCommand(player, "nightarrow", "dungeon.tier3");
                break;
            case OBSERVER:
                executeDungeonCommand(player, "deceptor", "dungeon.tier4");
                break;
            case AMETHYST_BLOCK:
                executeDungeonCommand(player, "Illusionist", "dungeon.tier5");
                break;
            case PACKED_ICE:
                executeDungeonCommand(player, "krampus", "dungeon.tier6");
                break;
            case COBBLESTONE:
                executeDungeonCommand(player, "towerskeleton", "dungeon.tier7");
                break;
            case SLIME_BLOCK:
                executeDungeonCommand(player, "VirusQueen", "dungeon.tier8");
                break;
            case GOLD_BLOCK:
                // Check if player is already awaiting input to prevent spam
                if (awaitingInput.containsKey(playerId) || isProcessing(playerId)) {
                    return;
                }

                setProcessing(playerId);
                player.closeInventory();
                awaitingInput.put(playerId, "party_invite");

                // Send message immediately
                player.sendMessage("§6§l[Party] §7Please type the name of the player you want to invite:");
                player.sendMessage("§7Type §c'cancel' §7to cancel the invitation.");
                break;
            case ARROW:
                if (isProcessing(playerId)) {
                    return;
                }
                setProcessing(playerId);
                player.closeInventory();

                // Open raid GUI immediately
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new RaidGUI(plugin).open(player);
                });
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!awaitingInput.containsKey(playerId)) {
            return;
        }

        String inputType = awaitingInput.get(playerId);

        if (inputType.equals("party_invite")) {
            event.setCancelled(true);
            awaitingInput.remove(playerId);
            processingPlayers.remove(playerId);

            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§6§l[Party] §7Party invitation cancelled.");
                });
                return;
            }

            // Schedule the command to run on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("party invite " + message);
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§5🏰 Dungeons")) {
            UUID playerId = event.getPlayer().getUniqueId();
            // Clean up processing state when GUI is closed
            processingPlayers.remove(playerId);
        }
    }

    public static void cleanup() {
        if (instance != null) {
            HandlerList.unregisterAll(instance);
            instance = null;
            pluginInstance = null;
            awaitingInput.clear();
            processingPlayers.clear();
        }
    }
}
