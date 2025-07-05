package com.lunar.raid.EarthGUI.TownyGUI;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TownyGUI {
    private final JavaPlugin plugin;

    public TownyGUI(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§aTowny Management");

        Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident != null && resident.hasTown()) {
            gui.setItem(11, createItem(Material.PAPER, "§fMy Town", "§7Manage your town"));
        } else {
            gui.setItem(11, createItem(Material.CHEST, "§aCreate Town", "§7Click to start a new town"));
        }

        gui.setItem(13, createItem(Material.EMERALD_BLOCK, "§eUpgrade Defense", "§7Upgrade your raid level"));
        gui.setItem(15, createItem(Material.BOOK, "§bPlots", "§7Manage your plots"));

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
