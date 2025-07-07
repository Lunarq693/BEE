package com.lunar.raid.EarthGUI;

import com.lunar.raid.EarthGUI.TownyGUI.TownyGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthMenuCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final TownyGUIListener townyGUIListener;

    public EarthMenuCommand(JavaPlugin plugin, TownyGUIListener townyGUIListener) {
        this.plugin = plugin;
        this.townyGUIListener = townyGUIListener;

        // Register listener with access to the proper TownyGUI
        Bukkit.getPluginManager().registerEvents(new EarthMenuListener(plugin, townyGUIListener), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        Inventory gui = Bukkit.createInventory(null, 27, "§2🌍 Earth Menu");

        for (int i = 0; i < 27; i++) {
            boolean isEdge = i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    paneMaterial = (i == 0 || i == 8) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            : (i % 2 == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
                } else if (i >= 18) {
                    int pos = i - 18;
                    paneMaterial = (pos == 0 || pos == 8) ? Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            : (pos % 2 == 0 ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
                } else {
                    paneMaterial = Material.LIME_STAINED_GLASS_PANE;
                }

                gui.setItem(i, createPane(paneMaterial));
            }
        }

        // Add the buttons for the Earth menu
        gui.setItem(11, createGlowingMenuItem(Material.PAPER, "§fTown Management", "§7Manage your town"));
        gui.setItem(13, createGlowingMenuItem(Material.NETHERITE_SWORD, "§cRaid", "§7Start/view raids"));
        gui.setItem(15, createGlowingMenuItem(Material.BUNDLE, "§eServer Features", "§7Warps, shops, jobs"));

        // Add hub button in bottom right corner (slot 26)
        gui.setItem(26, createGlowingMenuItem(Material.COMPASS, "§6Hub", "§7Return to the hub"));

        player.openInventory(gui);
        return true;
    }

    private ItemStack createPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack createGlowingMenuItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.List.of(lore));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}
