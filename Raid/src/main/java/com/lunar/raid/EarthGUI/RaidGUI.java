package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class RaidGUI {
    private final JavaPlugin plugin;

    public RaidGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§cRaid Management");

        gui.setItem(11, createItem(Material.NETHERITE_SWORD, "§6Start Raid", "§7Attempt to raid another town"));
        gui.setItem(13, createItem(Material.BOOK, "§eRaid History", "§7View recent raid results"));
        gui.setItem(15, createItem(Material.SHIELD, "§aCurrent Raid Status", "§7Ongoing raids in your area"));

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}
