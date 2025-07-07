package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerGUI {
    private final JavaPlugin plugin;

    public ServerGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§eServer Commands");

        gui.setItem(11, createItem(Material.ENDER_PEARL, "§bTeleport Menu", "§7Teleport to spawn, warps, etc."));
        gui.setItem(13, createItem(Material.EMERALD, "§aShop", "§7Buy & sell items"));
        gui.setItem(15, createItem(Material.WRITABLE_BOOK, "§dHelp & Info", "§7See server guide & support"));

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
