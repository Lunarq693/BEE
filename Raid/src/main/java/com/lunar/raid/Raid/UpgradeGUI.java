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
import java.util.HashMap;
import java.util.Map;

public class UpgradeGUI implements Listener {
    private final JavaPlugin plugin;
    private final Map<String, String> displayNameToUpgradeKey = new HashMap<>();

    public UpgradeGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        displayNameToUpgradeKey.clear(); // Clear previous mappings

        for (String upgradeKey : upgrades) {
            String path = "upgrades." + upgradeKey;
            String desc = cfg.getString(path + ".description", "No info");
            String requiredItem = cfg.getString(path + ".required-item", null);

            if (requiredItem == null) {
                player.sendMessage("§cNo required item found in the config for " + upgradeKey);
                continue;
            }

            // Store the mapping between display name and upgrade key
            displayNameToUpgradeKey.put(requiredItem, upgradeKey);

            boolean isUnlocked = TownUpgradeSystem.isUpgradeUnlocked(town, upgradeKey);
            boolean isCurrent = TownUpgradeSystem.isCurrentUpgrade(town, upgradeKey);
            Material mat = isCurrent ? Material.DIAMOND_BLOCK : (isUnlocked ? Material.EMERALD_BLOCK : Material.GRAY_STAINED_GLASS_PANE);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a" + requiredItem);
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

        String requiredItemName = meta.getDisplayName().replace("§a", "");
        
        // Get the actual upgrade key from our mapping
        String upgradeKey = displayNameToUpgradeKey.get(requiredItemName);
        if (upgradeKey == null) {
            player.sendMessage("§cCould not find upgrade configuration for: " + requiredItemName);
            return;
        }

        plugin.getLogger().info("Processing upgrade: " + upgradeKey + " with required item: " + requiredItemName);

        Resident res = TownyAPI.getInstance().getResident(player);
        if (res == null || !res.hasTown()) return;

        Town town = res.getTownOrNull();
        if (town == null) return;

        // Check if the player has the required item
        if (!hasRequiredItem(player, requiredItemName)) {
            player.sendMessage("§cYou need a " + requiredItemName + " to perform this upgrade!");
            return;
        }

        performUpgrade(player, town, upgradeKey, requiredItemName);
    }

    // Check if the player has the required item based on item name
    private boolean hasRequiredItem(Player player, String requiredItem) {
        plugin.getLogger().info("Checking for item with name: " + requiredItem);

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // Check if the item is a BARRIER with the correct display name
            if (item.getType() == Material.BARRIER && meta.getDisplayName().equals("§a" + requiredItem)) {
                plugin.getLogger().info("Player has the required item: " + requiredItem);
                return true;
            }
        }

        return false;
    }

    private void performUpgrade(Player player, Town town, String upgradeKey, String requiredItem) {
        // Remove the required item from inventory
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == Material.BARRIER && 
                item.getItemMeta() != null && 
                item.getItemMeta().getDisplayName().equals("§a" + requiredItem)) {
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
        player.closeInventory();
    }

    private String getMobForUpgrade(String upgradeKey) {
        FileConfiguration cfg = plugin.getConfig();
        return cfg.getString("upgrades." + upgradeKey + ".mob", "SkeletonKing");
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