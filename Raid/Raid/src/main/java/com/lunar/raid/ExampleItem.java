package com.lunar.raid;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ExampleItem implements IItemDrop {

    private Material material;

    public ExampleItem(MythicLineConfig config, String argument) {
        String str = config.getString(new String[] {"type", "t"}, "STONE", argument);

        this.material = Material.valueOf(str.toUpperCase());
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata data, double amount) {
        final ItemStack item = new ItemStack(material, (int) amount);
        final ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lSPECIAL EXAMPLE ITEM");
            item.setItemMeta(meta);
        }

        // Use BukkitAdapter to properly convert ItemStack to AbstractItemStack
        return BukkitAdapter.adapt(item);
    }
}