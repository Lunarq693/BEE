package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UpgradeGUI implements Listener {
    private final JavaPlugin plugin;

    public UpgradeGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);  // Register the event listener
    }

    public void open(Player player) {
        plugin.getLogger().info("Opening upgrade GUI for player: " + player.getName());

        FileConfiguration cfg = plugin.getConfig();
        Set<String> upgrades = cfg.getConfigurationSection("upgrades").getKeys(false);

        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 45, "Town Upgrades");

        Resident res = TownyAPI.getInstance().getResident(player);
        if (res == null || !res.hasTown()) {
            player.sendMessage("§cYou must be in a town.");
            return;
        }
        Town town = res.getTownOrNull();
        if (town == null) return;

        int slot = 0;

        for (String upgradeKey : upgrades) {
            String path = "upgrades." + upgradeKey;
            String desc = cfg.getString(path + ".description", "No info");

            // Fetch the required-item from the config
            String requiredItem = cfg.getString(path + ".required-item", null);

            if (requiredItem == null) {
                player.sendMessage("§cNo required item found in the config for " + upgradeKey);
                continue; // Skip this upgrade if no required item is found
            }

            boolean isUnlocked = TownUpgradeSystem.isUpgradeUnlocked(town, upgradeKey);
            boolean isCurrent = TownUpgradeSystem.isCurrentUpgrade(town, upgradeKey);
            Material mat = isCurrent ? Material.DIAMOND_BLOCK : (isUnlocked ? Material.EMERALD_BLOCK : Material.GRAY_STAINED_GLASS_PANE);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + upgradeKey);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + desc);
            lore.add("§7Required Item: §e" + requiredItem);
            if (isUnlocked)
                lore.add("§7Click to upgrade!");
            if (isCurrent)
                lore.add("§7Currently active upgrade");
            else
                lore.add("§7Not unlocked yet.");
            meta.setLore(lore);

            // Set the item name to match the required item from config (e.g., "Dark Knight Boss")
            meta.setDisplayName("§a" + requiredItem);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("Town Upgrades")) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String requiredItem = meta.getDisplayName().replace("§a", "");  // Remove the formatting (e.g., §a) from the name

        plugin.getLogger().info("Detected upgrade required item: " + requiredItem);

        Resident res = TownyAPI.getInstance().getResident(player);
        if (res == null || !res.hasTown()) return;

        Town town = res.getTownOrNull();
        if (town == null) return;

        FileConfiguration cfg = plugin.getConfig();
        String path = "upgrades." + requiredItem;  // Update path to use upgradeKey
        String configRequiredItem = cfg.getString(path + ".required-item", null);  // Get the required item from config

        if (configRequiredItem == null) {
            player.sendMessage("§cError: Required item is missing in the config.");
            return;
        }

        // Check if the player has the required item
        if (!hasRequiredItem(player, configRequiredItem)) {
            player.sendMessage("§cYou need a " + configRequiredItem + " to perform this upgrade!");
            return;
        }

        performUpgrade(player, town, requiredItem, configRequiredItem);
    }

    // Check if the player has the required item based on item name
    private boolean hasRequiredItem(Player player, String requiredItem) {
        plugin.getLogger().info("Checking for item with name: " + requiredItem);

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;  // Skip empty slots

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // Check if the item's name matches the required name
            if (meta.getDisplayName().equals(requiredItem)) {
                plugin.getLogger().info("Player has the required item: " + requiredItem);
                return true;  // Player has the correct item
            }
        }

        return false;  // Return false if no matching item is found
    }

    private void performUpgrade(Player player, Town town, String upgradeKey, String requiredItem) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.BARRIER && item.getItemMeta().getDisplayName().equals(requiredItem)) {
                item.setAmount(item.getAmount() - 1);
                player.updateInventory();
                break;
            }
        }

        TownUpgradeSystem.unlockUpgrade(town, upgradeKey);
        TownUpgradeSystem.setCurrentUpgrade(town, upgradeKey);

        String mobName = getMobForUpgrade(upgradeKey);
        spawnMythicMob(player, town, mobName);

        player.sendMessage("§aUpgrade successful! Your town's defense has been upgraded.");
    }

    private String getMobForUpgrade(String upgradeKey) {
        switch (upgradeKey) {
            case "upgrade-1": return "SkeletonKing";
            case "upgrade-2": return "SkeletalMinion";
            case "upgrade-3": return "StaticallyChargedSheep";
            case "upgrade-4": return "AngrySludge";
            case "upgrade-5": return "GIANT";
            default: return "SkeletonKing";
        }
    }

    private void spawnMythicMob(Player player, Town town, String mobName) {
        Location location = player.getLocation().add(0, 5, 0);
        try {
            BukkitAPIHelper api = MythicBukkit.inst().getAPIHelper();
            api.spawnMythicMob(mobName, location, 1);
            player.sendMessage("§6A " + mobName + " has been summoned to defend the town!");
        } catch (Exception e) {
            player.sendMessage("§cFailed to spawn mob: " + e.getMessage());
        }
    }
}
