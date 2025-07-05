package com.lunar.raid.Raid;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GiveUpgradeItemCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public GiveUpgradeItemCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /giveupgradeitem <item-name>");
            player.sendMessage("§eAvailable items:");
            player.sendMessage("§e- Dark Knight Boss");
            player.sendMessage("§e- Forest Guardian Boss");
            player.sendMessage("§e- Backrooms Boss");
            player.sendMessage("§e- Deceptor Boss");
            player.sendMessage("§e- Illusionist Boss");
            return true;
        }

        String itemName = String.join(" ", args);
        
        // Validate the item name against config
        String[] validItems = {
            "Dark Knight Boss",
            "Forest Guardian Boss", 
            "Backrooms Boss",
            "Deceptor Boss",
            "Illusionist Boss"
        };
        
        boolean isValid = false;
        for (String validItem : validItems) {
            if (validItem.equalsIgnoreCase(itemName)) {
                itemName = validItem; // Use the correct casing
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            player.sendMessage("§cInvalid item name! Use /giveupgradeitem without arguments to see valid items.");
            return true;
        }

        // Create the BARRIER item with the correct display name
        ItemStack upgradeItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = upgradeItem.getItemMeta();
        
        // Set the display name with the green color code
        meta.setDisplayName("§a" + itemName);
        
        // Add some lore for clarity
        List<String> lore = new ArrayList<>();
        lore.add("§7Upgrade Item");
        lore.add("§7Use this in the Town Upgrades GUI");
        lore.add("§7to unlock: §e" + itemName);
        meta.setLore(lore);
        
        upgradeItem.setItemMeta(meta);
        
        // Give the item to the player
        player.getInventory().addItem(upgradeItem);
        player.sendMessage("§aGiven you a " + itemName + " upgrade item!");
        
        return true;
    }
}