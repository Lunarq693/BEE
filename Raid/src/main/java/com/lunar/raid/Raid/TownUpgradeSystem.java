package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TownUpgradeSystem implements CommandExecutor {

    private final JavaPlugin plugin;

    public TownUpgradeSystem(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /townupgrade <upgrade>");
            return true;
        }

        String upgradeKey = args[0];
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident == null || !resident.hasTown()) {
            player.sendMessage("§cYou must be in a town to upgrade it.");
            return true;
        }
        Town town = resident.getTownOrNull();
        if (town == null) {
            player.sendMessage("§cCould not fetch town.");
            return true;
        }

        FileConfiguration cfg = plugin.getConfig();
        String path = "upgrades." + upgradeKey;
        if (!cfg.contains(path)) {
            player.sendMessage("§cNo upgrade defined for " + upgradeKey);
            return true;
        }

        String requiredItem = cfg.getString(path + ".required-item", null);

        if (requiredItem == null) {
            player.sendMessage("§cNo required item defined for this upgrade.");
            return true;
        }

        // Check if the player has the specific required item by lore
        if (!hasRequiredItemByLore(player, requiredItem)) {
            player.sendMessage("§cYou need an item with '" + requiredItem + "' in its lore to perform this upgrade!");
            return true;
        }

        consumeRequiredItemByLore(player, requiredItem);
        unlockUpgrade(town, upgradeKey);

        String mobName = cfg.getString(path + ".mob", "SkeletonKing");
        town.addMetaData(new StringDataField("raid_mob", mobName));
        town.save();

        player.sendMessage("§aUpgrade " + upgradeKey + " unlocked! You can now start a raid with the selected mob.");

        return true;
    }

    private boolean hasRequiredItemByLore(Player player, String requiredLoreText) {
        plugin.getLogger().info("Checking for item with lore containing: " + requiredLoreText);

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || meta.getLore() == null) continue;

            // Check each line of lore for the required text
            for (String loreLine : meta.getLore()) {
                // Strip color codes from lore line for comparison
                String cleanLoreLine = stripColorCodes(loreLine);
                String cleanRequiredText = stripColorCodes(requiredLoreText);
                
                if (cleanLoreLine.contains(cleanRequiredText)) {
                    plugin.getLogger().info("Found item with required lore: " + requiredLoreText);
                    return true;
                }
            }
        }

        return false;
    }

    private void consumeRequiredItemByLore(Player player, String requiredLoreText) {
        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || meta.getLore() == null) continue;

            // Check if this item has the required lore
            boolean hasRequiredLore = false;
            for (String loreLine : meta.getLore()) {
                String cleanLoreLine = stripColorCodes(loreLine);
                String cleanRequiredText = stripColorCodes(requiredLoreText);
                
                if (cleanLoreLine.contains(cleanRequiredText)) {
                    hasRequiredLore = true;
                    break;
                }
            }

            if (hasRequiredLore) {
                item.setAmount(item.getAmount() - 1);
                player.updateInventory();
                break;
            }
        }
    }

    // Helper method to strip color codes
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public static void unlockUpgrade(Town town, String upgradeKey) {
        town.addMetaData(new StringDataField(upgradeKey, "true"));
        town.save();
    }

    public static boolean isUpgradeUnlocked(Town town, String upgradeKey) {
        StringDataField df = (StringDataField) town.getMetadata(upgradeKey);
        return df != null && Boolean.parseBoolean(df.getValue());
    }

    public static boolean isCurrentUpgrade(Town town, String upgradeKey) {
        StringDataField df = (StringDataField) town.getMetadata("current_upgrade");
        return df != null && upgradeKey.equals(df.getValue());
    }

    public static void setCurrentUpgrade(Town town, String upgradeKey) {
        town.addMetaData(new StringDataField("current_upgrade", upgradeKey));
        town.save();
    }
}