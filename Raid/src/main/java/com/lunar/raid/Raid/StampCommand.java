package com.lunar.raid.Raid;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class StampCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public StampCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        // Check if the player is holding an item
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage("§cYou must be holding an item to stamp it!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /stamp <upgrade-id>");
            return true;
        }

        String upgradeId = args[0];  // Example: "upgrade-1", "upgrade-2"

        // Get the current item meta (the item's properties)
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            player.sendMessage("§cThis item cannot be stamped!");
            return true;
        }

        // Add the ID to the item's lore
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Add the required ID to the lore (e.g., "ID: upgrade-1")
        lore.add("ID: " + upgradeId);
        meta.setLore(lore);

        // Apply the new meta to the item
        itemInHand.setItemMeta(meta);

        // Notify the player
        player.sendMessage("§aSuccessfully stamped the item with ID: " + upgradeId);

        return true;
    }
}
