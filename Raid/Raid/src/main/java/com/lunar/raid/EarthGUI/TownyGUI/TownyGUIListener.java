package com.lunar.raid.EarthGUI.TownyGUI;

import com.lunar.raid.Raid.UpgradeGUI;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TownyGUIListener implements Listener {

    private final JavaPlugin plugin;
    private final TownManagement townMgmt;
    private PlotManagement plotMgmt; // Set later

    public TownyGUIListener(JavaPlugin plugin, TownManagement townMgmt) {
        this.plugin = plugin;
        this.townMgmt = townMgmt;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setPlotManagement(PlotManagement plotMgmt) {
        this.plotMgmt = plotMgmt;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§aTowny Management");

        // Fixed border pattern with proper Light Blue/Lime alternating
        for (int i = 0; i < 27; i++) {
            boolean isEdge = i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                // Back button in slot 18 (bottom-left corner)
                if (i == 18) {
                    inv.setItem(i, createGlowingMenuItem(Material.ARROW, "§cGo Back", "§7Return to Earth Menu"));
                    continue;
                }

                Material paneMaterial;

                if (i < 9) {
                    // Top row: corners are light blue, alternating pattern for middle
                    if (i == 0 || i == 8) {
                        paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    } else {
                        paneMaterial = (i % 2 == 1) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
                    }
                } else if (i >= 18) {
                    // Bottom row: corners are light blue, alternating pattern for middle
                    int pos = i - 18;
                    if (pos == 0 || pos == 8) {
                        paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                    } else {
                        paneMaterial = (pos % 2 == 1) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE;
                    }
                } else {
                    // Side columns: lime
                    paneMaterial = Material.LIME_STAINED_GLASS_PANE;
                }

                inv.setItem(i, createPane(paneMaterial));
            }
        }

        boolean hasTown = TownyAPI.getInstance().getResident(player).hasTown();

        if (hasTown) {
            inv.setItem(11, createGlowingMenuItem(Material.PAPER, "§fMy Town", "Manage your town"));
        } else {
            inv.setItem(11, createGlowingMenuItem(Material.GRASS_BLOCK, "§aCreate Town", "Create a new town"));
        }

        // Change "Coming soon!" message to open the actual upgrade GUI
        inv.setItem(13, createGlowingMenuItem(Material.EMERALD_BLOCK, "§eUpgrade Defense", "§7Upgrade your town's raid defense"));
        inv.setItem(15, createGlowingMenuItem(Material.BOOK, "§bPlots", "Manage your plots"));

        player.openInventory(inv);
    }

    private ItemStack createPane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlowingMenuItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("§aTowny Management")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();

        switch (name) {
            case "§fMy Town" -> townMgmt.open(player);
            case "§aCreate Town" -> {
                player.closeInventory();
                townMgmt.promptInput(player, "§eEnter your desired town name:", "town new");
            }
            case "§eUpgrade Defense" -> {
                // Open the existing Upgrade GUI when clicked
                new UpgradeGUI(plugin).open(player);  // Opens the /upgrades GUI
            }
            case "§bPlots" -> {
                if (plotMgmt != null) {
                    plotMgmt.open(player);
                } else {
                    player.sendMessage("§eFeature coming soon!");
                }
            }
            case "§cGo Back" -> {
                player.closeInventory();
                player.performCommand("earth");
            }
            default -> player.sendMessage("§cUnknown selection.");
        }
    }
}