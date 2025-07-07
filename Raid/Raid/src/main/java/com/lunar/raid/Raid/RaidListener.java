package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public class RaidListener implements Listener {

    private final JavaPlugin plugin;

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        // Check if this entity is a raid mob
        if (!entity.hasMetadata("raid_target_town")) {
            return;
        }

        String targetTownName = entity.getMetadata("raid_target_town").get(0).asString();
        String attackerName = entity.getMetadata("raid_attacker").get(0).asString();
        String attackerTownName = entity.getMetadata("raid_attacker_town").get(0).asString();

        Town targetTown = TownyAPI.getInstance().getTown(targetTownName);
        Town attackerTown = TownyAPI.getInstance().getTown(attackerTownName);

        if (targetTown == null || attackerTown == null) {
            plugin.getLogger().warning("Could not find towns for raid resolution!");
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            plugin.getServer().broadcastMessage("§7The raid on " + targetTownName + " ended inconclusively.");
            return;
        }

        Resident killerResident = TownyAPI.getInstance().getResident(killer);
        if (killerResident == null) {
            return;
        }

        // Check if the killer is from the attacking town (raid success)
        if (killerResident.hasTown() && killerResident.getTownOrNull().equals(attackerTown)) {
            handleRaidSuccess(killer, targetTown, attackerTown, attackerName);
        }
        // Check if the killer is from the defending town (raid defense)
        else if (killerResident.hasTown() && killerResident.getTownOrNull().equals(targetTown)) {
            handleRaidDefense(killer, targetTown, attackerTown, attackerName);
        }
        // Someone else killed the mob
        else {
            plugin.getServer().broadcastMessage("§7The raid on " + targetTownName + " was interrupted by " + killer.getName() + "!");
        }
    }

    private void handleRaidSuccess(Player raider, Town targetTown, Town attackerTown, String originalAttacker) {
        try {
            plugin.getLogger().info("Raid successful! " + raider.getName() + " conquered " + targetTown.getName());

            // Broadcast the victory
            plugin.getServer().broadcastMessage("§c⚔ " + raider.getName() + " has successfully raided " + targetTown.getName() + "!");
            plugin.getServer().broadcastMessage("§c" + targetTown.getName() + " has been conquered by " + attackerTown.getName() + "!");

            // Notify the raider
            raider.sendMessage("§a§l✓ RAID SUCCESSFUL!");
            raider.sendMessage("§aYou have conquered " + targetTown.getName() + "!");
            raider.sendMessage("§aAll town blocks have been transferred to " + attackerTown.getName() + "!");

            // Transfer all town blocks to the attacking town
            transferTownBlocks(targetTown, attackerTown);

            // Clear the conquered town's upgrades
            TownUpgradeSystem.clearTownUpgrades(targetTown);

            // Notify target town residents
            for (Resident resident : targetTown.getResidents()) {
                Player townPlayer = resident.getPlayer();
                if (townPlayer != null && townPlayer.isOnline()) {
                    townPlayer.sendMessage("§c§l✗ YOUR TOWN HAS BEEN CONQUERED!");
                    townPlayer.sendMessage("§cYour town " + targetTown.getName() + " has been conquered by " + raider.getName() + "!");
                    townPlayer.sendMessage("§cAll town blocks have been transferred to " + attackerTown.getName() + ".");
                }
            }

            // Delete the conquered town
            TownyAPI.getInstance().getDataSource().removeTown(targetTown);

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling raid success: " + e.getMessage());
            e.printStackTrace();
            raider.sendMessage("§cError processing raid victory. Contact an administrator.");
        }
    }

    private void handleRaidDefense(Player defender, Town targetTown, Town attackerTown, String originalAttacker) {
        try {
            plugin.getLogger().info("Raid defended! " + defender.getName() + " defended " + targetTown.getName());

            // Broadcast the successful defense
            plugin.getServer().broadcastMessage("§a⚔ " + defender.getName() + " has successfully defended " + targetTown.getName() + "!");
            plugin.getServer().broadcastMessage("§a" + targetTown.getName() + " has repelled the raid from " + attackerTown.getName() + "!");

            // Notify the defender
            defender.sendMessage("§a§l✓ DEFENSE SUCCESSFUL!");
            defender.sendMessage("§aYou have successfully defended " + targetTown.getName() + " from " + originalAttacker + "!");
            defender.sendMessage("§aYour town is safe from conquest!");

            // Notify other town residents
            for (Resident resident : targetTown.getResidents()) {
                Player townPlayer = resident.getPlayer();
                if (townPlayer != null && townPlayer.isOnline() && !townPlayer.equals(defender)) {
                    townPlayer.sendMessage("§a§l✓ TOWN DEFENDED!");
                    townPlayer.sendMessage("§a" + defender.getName() + " has successfully defended our town from " + originalAttacker + "!");
                }
            }

            // Notify the attacking town
            for (Resident resident : attackerTown.getResidents()) {
                Player townPlayer = resident.getPlayer();
                if (townPlayer != null && townPlayer.isOnline()) {
                    townPlayer.sendMessage("§c§l✗ RAID FAILED!");
                    townPlayer.sendMessage("§cYour raid on " + targetTown.getName() + " has been repelled by " + defender.getName() + "!");
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error handling raid defense: " + e.getMessage());
            e.printStackTrace();
            defender.sendMessage("§cError processing defense. Contact an administrator.");
        }
    }

    private void transferTownBlocks(Town fromTown, Town toTown) {
        try {
            plugin.getLogger().info("Transferring " + fromTown.getTownBlocks().size() + " blocks from " + fromTown.getName() + " to " + toTown.getName());

            // Get all town blocks from the conquered town
            for (TownBlock townBlock : fromTown.getTownBlocks()) {
                try {
                    // Remove the block from the old town
                    fromTown.removeTownBlock(townBlock);

                    // Add the block to the new town
                    toTown.addTownBlock(townBlock);
                    townBlock.setTown(toTown);

                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to transfer town block: " + e.getMessage());
                }
            }

            // Save both towns
            fromTown.save();
            toTown.save();

            plugin.getLogger().info("Successfully transferred town blocks from " + fromTown.getName() + " to " + toTown.getName());

        } catch (Exception e) {
            plugin.getLogger().severe("Error transferring town blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
