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

        // If the required item is null, something went wrong in the config
        if (requiredItem == null) {
            player.sendMessage("§cNo required item defined for this upgrade.");
            return true;
        }

        // Check if the player has the specific required item anywhere in their inventory
        if (!hasRequiredItem(player, requiredItem)) {
            player.sendMessage("§cYou need a " + requiredItem + " to perform this upgrade!");
            return true;
        }

        consumeRequiredItem(player, requiredItem);
        unlockUpgrade(town, upgradeKey);

        String mobName = cfg.getString(path + ".mob", "SkeletonKing");
        town.addMetaData(new StringDataField("raid_mob", mobName));
        town.save();

        player.sendMessage("§aUpgrade " + upgradeKey + " unlocked! You can now start a raid with the selected mob.");

        return true;
    }

    private boolean hasRequiredItem(Player player, String requiredItem) {
        plugin.getLogger().info("Checking for item with name: " + requiredItem);

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;  // Skip empty slots

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // Check if the item's name matches the required name (and item is a BARRIER)
            if (item.getType() == Material.BARRIER && meta.getDisplayName().equals("§a" + requiredItem)) {
                plugin.getLogger().info("Player has the required item: " + requiredItem);
                return true;  // Player has the correct item
            }
        }

        return false;  // Return false if no matching item is found
    }

    private void consumeRequiredItem(Player player, String requiredItemId) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.BARRIER && item.getItemMeta().getDisplayName().equals("§a" + requiredItemId)) {
                item.setAmount(item.getAmount() - 1);
                player.updateInventory();
                break;
            }
        }
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
