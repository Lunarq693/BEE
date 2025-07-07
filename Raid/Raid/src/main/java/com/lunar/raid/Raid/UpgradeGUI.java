package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class UpgradeGUI implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private final Set<UUID> processingPlayers = new HashSet<>();

    public UpgradeGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown()) {
            player.sendMessage("§cYou must be in a town to view upgrades!");
            return;
        }

        Town town = resident.getTownOrNull();
        if (town == null) {
            player.sendMessage("§cCould not fetch your town!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, "§6Town Upgrades - " + town.getName());
        FileConfiguration config = plugin.getConfig();

        // Get unlocked upgrades for this town
        List<String> unlockedUpgrades = TownUpgradeSystem.getUnlockedUpgrades(town);
        String currentUpgrade = TownUpgradeSystem.getCurrentUpgrade(town);

        int slot = 0;
        for (String upgradeKey : config.getConfigurationSection("upgrades").getKeys(false)) {
            if (slot >= 45) break; // Leave space for navigation

            String path = "upgrades." + upgradeKey;
            String description = config.getString(path + ".description", upgradeKey);
            String requiredItem = config.getString(path + ".required-item", "Unknown Item");
            String mobName = config.getString(path + ".mob", "Unknown Mob");

            boolean isUnlocked = unlockedUpgrades.contains(upgradeKey);
            boolean isCurrent = upgradeKey.equals(currentUpgrade);

            ItemStack item;
            List<String> lore = new ArrayList<>();

            if (isCurrent) {
                // Currently active upgrade
                item = new ItemStack(Material.EMERALD_BLOCK);
                lore.add("§a§lCURRENTLY ACTIVE");
                lore.add("§7This upgrade is currently active");
                lore.add("§7Mob: §e" + mobName);
                lore.add("");
                lore.add("§aClick to deactivate");
            } else if (isUnlocked) {
                // Unlocked but not active
                item = new ItemStack(Material.DIAMOND);
                lore.add("§a§lUNLOCKED");
                lore.add("§7This upgrade has been unlocked");
                lore.add("§7Mob: §e" + mobName);
                lore.add("");
                lore.add("§eClick to activate");
            } else {
                // Locked upgrade
                item = new ItemStack(Material.BARRIER);
                lore.add("§c§lLOCKED");
                lore.add("§7Required Item: §c" + requiredItem);
                lore.add("§7Mob: §e" + mobName);
                lore.add("");
                lore.add("§cYou need an item with '" + requiredItem + "'");
                lore.add("§cin its lore to unlock this upgrade");
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6" + description);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            gui.setItem(slot, item);
            slot++;
        }

        // Add close button
        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cClose");
            closeMeta.setLore(Arrays.asList("§7Click to close this menu"));
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(53, closeButton);

        player.openInventory(gui);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("§6Town Upgrades")) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClick() != ClickType.LEFT) return;

        event.setCancelled(true);

        // Check if player is already being processed
        UUID playerId = player.getUniqueId();
        if (processingPlayers.contains(playerId)) {
            return;
        }

        // Check cooldown (2 seconds)
        long currentTime = System.currentTimeMillis();
        if (lastClickTime.containsKey(playerId)) {
            long timeDiff = currentTime - lastClickTime.get(playerId);
            if (timeDiff < 2000) {
                return;
            }
        }

        // Set processing flag
        processingPlayers.add(playerId);
        lastClickTime.put(playerId, currentTime);

        // Schedule the actual processing for next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                processUpgradeClick(player, event.getSlot());
            } finally {
                processingPlayers.remove(playerId);
            }
        });
    }

    private void processUpgradeClick(Player player, int slot) {
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown()) {
            player.sendMessage("§cYou must be in a town to manage upgrades!");
            player.closeInventory();
            return;
        }

        Town town = resident.getTownOrNull();
        if (town == null) {
            player.sendMessage("§cCould not fetch your town!");
            player.closeInventory();
            return;
        }

        FileConfiguration config = plugin.getConfig();
        List<String> upgradeKeys = new ArrayList<>(config.getConfigurationSection("upgrades").getKeys(false));

        if (slot >= upgradeKeys.size()) {
            return; // Invalid slot
        }

        String upgradeKey = upgradeKeys.get(slot);
        String path = "upgrades." + upgradeKey;
        String description = config.getString(path + ".description", upgradeKey);
        String requiredItem = config.getString(path + ".required-item");
        String mobName = config.getString(path + ".mob", "SkeletonKing");

        List<String> unlockedUpgrades = TownUpgradeSystem.getUnlockedUpgrades(town);
        String currentUpgrade = TownUpgradeSystem.getCurrentUpgrade(town);
        boolean isUnlocked = unlockedUpgrades.contains(upgradeKey);
        boolean isCurrent = upgradeKey.equals(currentUpgrade);

        if (isCurrent) {
            // Deactivate current upgrade
            TownUpgradeSystem.setCurrentUpgrade(town, null);
            player.sendMessage("§aDeactivated upgrade: " + description);
            player.closeInventory();
            open(player); // Refresh GUI
        } else if (isUnlocked) {
            // Activate this upgrade
            TownUpgradeSystem.setCurrentUpgrade(town, upgradeKey);
            player.sendMessage("§aActivated upgrade: " + description);
            player.sendMessage("§aYour town will now be defended by: " + mobName);
            player.closeInventory();
        } else {
            // Try to unlock the upgrade
            if (requiredItem == null || requiredItem.isEmpty()) {
                player.sendMessage("§cThis upgrade has no required item configured!");
                return;
            }

            if (!hasRequiredItemByLore(player, requiredItem)) {
                player.sendMessage("§cYou need an item with '" + requiredItem + "' in its lore to perform this upgrade!");
                return;
            }

            // Consume the item and unlock the upgrade
            consumeRequiredItemByLore(player, requiredItem);
            TownUpgradeSystem.unlockUpgrade(town, upgradeKey);
            TownUpgradeSystem.setCurrentUpgrade(town, upgradeKey);

            player.sendMessage("§aUpgrade " + description + " unlocked and activated!");
            player.sendMessage("§aYour town will now be defended by: " + mobName);
            player.closeInventory();
        }
    }

    private boolean hasRequiredItemByLore(Player player, String requiredLoreText) {
        if (requiredLoreText == null || requiredLoreText.isEmpty()) return false;

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || meta.getLore() == null) continue;

            for (String loreLine : meta.getLore()) {
                String cleanLoreLine = stripColorCodes(loreLine);
                String cleanRequiredText = stripColorCodes(requiredLoreText);

                if (cleanLoreLine.toLowerCase().contains(cleanRequiredText.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void consumeRequiredItemByLore(Player player, String requiredLoreText) {
        if (requiredLoreText == null || requiredLoreText.isEmpty()) return;

        for (ItemStack item : player.getInventory()) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null || meta.getLore() == null) continue;

            boolean hasRequiredLore = false;
            for (String loreLine : meta.getLore()) {
                String cleanLoreLine = stripColorCodes(loreLine);
                String cleanRequiredText = stripColorCodes(requiredLoreText);

                if (cleanLoreLine.toLowerCase().contains(cleanRequiredText.toLowerCase())) {
                    hasRequiredLore = true;
                    break;
                }
            }

            if (hasRequiredLore) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().remove(item);
                }
                player.updateInventory();
                break;
            }
        }
    }

    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
