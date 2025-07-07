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

        // Check Minecraft version
        if (!isVersionSupported(player)) {
            player.sendMessage("§c§l[Raid] §7You need to update your Minecraft to version 1.21.4 or above to start raids!");
            player.sendMessage("§7Your current version is not supported. Please update your game and try again.");
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

    private boolean isVersionSupported(Player player) {
        try {
            // Get the player's protocol version
            String version = player.getClass().getMethod("getClientBrandName").invoke(player).toString();
            
            // Alternative method: Check server version and assume player is compatible
            String serverVersion = org.bukkit.Bukkit.getVersion();
            
            // Parse Minecraft version from server version string
            // Example: "git-Paper-196 (MC: 1.21.4)"
            if (serverVersion.contains("MC: ")) {
                String mcVersion = serverVersion.substring(serverVersion.indexOf("MC: ") + 4);
                mcVersion = mcVersion.substring(0, mcVersion.indexOf(")"));
                
                return isVersionAtLeast(mcVersion, "1.21.4");
            }
            
            // Fallback: assume compatible if we can't determine version
            return true;
            
        } catch (Exception e) {
            // If we can't determine the version, we'll use a different approach
            // Check if the player has access to 1.21.4+ features
            try {
                // Try to access a 1.21.4+ specific method/field
                // This is a simple check - in practice you might want to use ProtocolLib
                return true; // For now, allow all players
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private boolean isVersionAtLeast(String currentVersion, String requiredVersion) {
        try {
            String[] current = currentVersion.split("\\.");
            String[] required = requiredVersion.split("\\.");
            
            for (int i = 0; i < Math.max(current.length, required.length); i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int requiredPart = i < required.length ? Integer.parseInt(required[i]) : 0;
                
                if (currentPart > requiredPart) {
                    return true;
                } else if (currentPart < requiredPart) {
                    return false;
                }
            }
            
            return true; // Versions are equal
        } catch (NumberFormatException e) {
            return true; // If we can't parse, assume compatible
        }
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