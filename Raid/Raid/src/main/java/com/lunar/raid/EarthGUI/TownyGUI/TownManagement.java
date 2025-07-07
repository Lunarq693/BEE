package com.lunar.raid.EarthGUI.TownyGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TownManagement implements Listener {

    private final JavaPlugin plugin;
    private TownyGUIListener townyGUIListener;
    private final String title = "§2🏘 Town Management";
    private final Map<Player, String> awaitingInput = new HashMap<>();
    private final Set<String> skipReopen = Set.of("town new", "town delete");

    public TownManagement(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setTownyGUIListener(TownyGUIListener listener) {
        this.townyGUIListener = listener;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, title);

        // First, fill the entire inventory with a base pattern to ensure visibility
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, createGlassPane(Material.BLACK_STAINED_GLASS_PANE));
        }

        // Now create the proper Light Blue/Lime pattern on edges
        for (int i = 0; i < 45; i++) {
            boolean isEdge = i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    // Top row: Light Blue at corners (0,8), alternating Lime/Light Blue in middle
                    if (i == 0 || i == 8) {
                        paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    } else {
                        // Positions 1,2,3,4,5,6,7 - alternate starting with Lime
                        paneMaterial = (i % 2 == 1) ? Material.LIME_STAINED_GLASS_PANE : Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    }
                } else if (i >= 36) {
                    // Bottom row: Light Blue at corners, alternating pattern
                    int pos = i - 36;
                    if (pos == 0 || pos == 8) {
                        paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    } else {
                        // Positions 1,2,3,4,5,6,7 - alternate starting with Lime
                        paneMaterial = (pos % 2 == 1) ? Material.LIME_STAINED_GLASS_PANE : Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    }
                } else {
                    // Left and right sides - all Lime
                    paneMaterial = Material.LIME_STAINED_GLASS_PANE;
                }

                inv.setItem(i, createGlassPane(paneMaterial));
            }
        }

        // Clear the center area (remove black glass from non-edge slots)
        for (int i = 0; i < 45; i++) {
            boolean isEdge = i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8;
            if (!isEdge) {
                inv.setItem(i, null); // Clear center slots
            }
        }

        // Action items
        inv.setItem(12, createMenuItem(Material.PLAYER_HEAD, "§eAdd Resident", "Add a resident to your town", true));
        inv.setItem(13, createMenuItem(Material.IRON_SWORD, "§eKick Resident", "Kick a resident from your town", true));
        inv.setItem(14, createMenuItem(Material.DIAMOND_HELMET, "§dSet Mayor", "Assign a new mayor", true));

        inv.setItem(21, createMenuItem(Material.NAME_TAG, "§dSet Town Name", "Rename your town", true));
        inv.setItem(22, createMenuItem(Material.OAK_SIGN, "§bSet Homeblock", "Set town homeblock", false));
        inv.setItem(23, createMenuItem(Material.RED_BED, "§bSet Spawn", "Set spawn (in homeblock)", false));

        inv.setItem(30, createMenuItem(Material.GOLD_INGOT, "§6Withdraw Money", "Withdraw from town bank", true));
        inv.setItem(31, createMenuItem(Material.GOLD_BLOCK, "§6Deposit Money", "Deposit to town bank", true));

        // Back button (this will override the glass pane at slot 36)
        inv.setItem(36, createMenuItem(Material.ARROW, "§cGo Back", "Return to Towny Management menu", false));
        
        // Delete Town button (this will override the glass pane at slot 44)
        inv.setItem(44, createMenuItem(Material.BARRIER, "§4Delete Town", "Disband your town", false));

        player.openInventory(inv);
    }

    private ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createMenuItem(Material material, String name, String lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            if (glow) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!Objects.equals(event.getView().getTitle(), title)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        player.closeInventory();

        switch (name) {
            case "§bSet Homeblock" -> runCmd(player, "town set homeblock");
            case "§bSet Spawn" -> runCmd(player, "town set spawn");
            case "§eAdd Resident" -> promptInput(player, "§eEnter player name to add:", "town add");
            case "§eKick Resident" -> promptInput(player, "§eEnter player name to kick:", "town kick");
            case "§6Withdraw Money" -> promptInput(player, "§eEnter amount to withdraw:", "town withdraw");
            case "§6Deposit Money" -> promptInput(player, "§eEnter amount to deposit:", "town deposit");
            case "§dSet Mayor" -> promptInput(player, "§eEnter new mayor's name:", "town set mayor");
            case "§dSet Town Name" -> promptInput(player, "§eEnter new town name:", "town set name");
            case "§4Delete Town" -> runCmd(player, "town delete");
            case "§cGo Back" -> {
                if (townyGUIListener != null) townyGUIListener.open(player);
            }
            default -> player.sendMessage("§cUnknown command.");
        }
    }

    private void runCmd(Player player, String cmd) {
        Bukkit.dispatchCommand(player, cmd);
    }

    public void promptInput(Player player, String prompt, String commandPrefix) {
        if (awaitingInput.containsKey(player)) {
            player.sendMessage("§cYou already have a pending input.");
            return;
        }
        awaitingInput.put(player, commandPrefix);
        player.sendMessage(prompt);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingInput.containsKey(player)) return;

        event.setCancelled(true);
        String input = event.getMessage();
        String command = awaitingInput.remove(player);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(player, command + " " + input);

            boolean shouldSkipReopen = skipReopen.stream()
                    .anyMatch(skip -> command.toLowerCase().startsWith(skip.toLowerCase()));

            if (!shouldSkipReopen) {
                open(player);
            }
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (awaitingInput.containsKey(player)) {
            awaitingInput.remove(player);
            player.sendMessage("§cInput cancelled because you closed the inventory.");
        }
    }
}