package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RaidCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public RaidCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        // Get the player's current location
        Location playerLocation = player.getLocation();

        // Get the town block at the player's location
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(playerLocation);
        if (townBlock == null || !townBlock.hasTown()) {
            player.sendMessage("§cYou must be standing in a town to raid it!");
            return true;
        }

        Town targetTown = townBlock.getTownOrNull();
        if (targetTown == null) {
            player.sendMessage("§cCould not determine the town you're trying to raid!");
            return true;
        }

        // Get the raider's town
        Resident raiderResident = TownyAPI.getInstance().getResident(player);
        if (raiderResident == null || !raiderResident.hasTown()) {
            player.sendMessage("§cYou must be in a town to start a raid!");
            return true;
        }

        Town raiderTown = raiderResident.getTownOrNull();
        if (raiderTown == null) {
            player.sendMessage("§cCould not determine your town!");
            return true;
        }

        // Check if trying to raid own town
        if (targetTown.equals(raiderTown)) {
            player.sendMessage("§cYou cannot raid your own town!");
            return true;
        }

        // Get the mob to spawn based on the target town's current upgrade
        String mobType = getMobForTown(targetTown);
        if (mobType == null) {
            player.sendMessage("§cThis town has no defensive mob configured!");
            return true;
        }

        plugin.getLogger().info("Starting raid on town: " + targetTown.getName() + " with mob: " + mobType);

        try {
            // Spawn the MythicMob
            ActiveMob mythicMob = MythicBukkit.inst().getMobManager().spawnMob(mobType, playerLocation);
            if (mythicMob != null) {
                Entity mobEntity = mythicMob.getEntity().getBukkitEntity();

                // Add metadata to track the raid
                mobEntity.setCustomName("§c" + targetTown.getName() + " Defender");
                mobEntity.setCustomNameVisible(true);

                // Store raid information in the mob's metadata
                mobEntity.setMetadata("raid_target_town", new org.bukkit.metadata.FixedMetadataValue(plugin, targetTown.getName()));
                mobEntity.setMetadata("raid_attacker", new org.bukkit.metadata.FixedMetadataValue(plugin, player.getName()));
                mobEntity.setMetadata("raid_attacker_town", new org.bukkit.metadata.FixedMetadataValue(plugin, raiderTown.getName()));

                // Broadcast the raid start
                plugin.getServer().broadcastMessage("§c⚔ " + player.getName() + " from " + raiderTown.getName() + " is raiding " + targetTown.getName() + "!");
                player.sendMessage("§aRaid started! Defeat the " + mobType + " to conquer " + targetTown.getName() + "!");

                // Notify the target town
                for (Resident resident : targetTown.getResidents()) {
                    Player townPlayer = resident.getPlayer();
                    if (townPlayer != null && townPlayer.isOnline()) {
                        townPlayer.sendMessage("§c⚠ Your town is under attack by " + player.getName() + "! Defend the " + mobType + "!");
                    }
                }

            } else {
                player.sendMessage("§cFailed to spawn the defensive mob! Check server logs.");
                plugin.getLogger().severe("Failed to spawn MythicMob: " + mobType);
            }
        } catch (Exception e) {
            player.sendMessage("§cError starting raid: " + e.getMessage());
            plugin.getLogger().severe("Error spawning raid mob: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private String getMobForTown(Town town) {
        // First check if the town has a current upgrade set
        String currentUpgrade = TownUpgradeSystem.getCurrentUpgrade(town);
        if (currentUpgrade != null) {
            FileConfiguration config = plugin.getConfig();
            String mobName = config.getString("upgrades." + currentUpgrade + ".mob");
            if (mobName != null) {
                plugin.getLogger().info("Using mob from current upgrade " + currentUpgrade + ": " + mobName);
                return mobName;
            }
        }

        // Check if town has a raid_mob metadata
        if (town.hasMeta("raid_mob")) {
            StringDataField field = (StringDataField) town.getMetadata("raid_mob");
            String mobName = field.getValue();
            if (mobName != null && !mobName.isEmpty()) {
                plugin.getLogger().info("Using mob from town metadata: " + mobName);
                return mobName;
            }
        }

        // Default fallback
        plugin.getLogger().info("Using default mob: SkeletonKing");
        return "SkeletonKing";
    }
}
