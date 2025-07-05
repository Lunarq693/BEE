package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RaidListener implements Listener {
    private static final Map<String, Boolean> raidActive = new HashMap<>();
    private final JavaPlugin plugin;

    public RaidListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isRaidEndedForTown(String townName) {
        return !raidActive.getOrDefault(townName, false);
    }

    public static void failRaid(String townName) {
        raidActive.put(townName, false);
    }

    public static void startRaid(String townName) {
        raidActive.put(townName, true);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent evt) {
        LivingEntity entity = evt.getEntity();
        if (!entity.hasMetadata("raided_town")) return;

        String townName = entity.getMetadata("raided_town").get(0).asString();
        Town targetTown = TownyAPI.getInstance().getTown(townName);
        if (targetTown == null) return;

        if (!raidActive.getOrDefault(townName, false)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;

        Resident res = TownyAPI.getInstance().getResident(killer);
        if (res == null || !res.hasTown()) {
            killer.sendMessage("§cYou must belong to a town to benefit from this raid.");
            return;
        }

        Town attackerTown = res.getTownOrNull();
        if (attackerTown == null) {
            killer.sendMessage("§cCould not determine your town.");
            return;
        }

        // If the killer is from the town being raided, consider the raid failed (repelled)
        if (attackerTown.equals(targetTown)) {
            killer.sendMessage("§aYour town has successfully repelled the raid!");
            Bukkit.broadcastMessage("§a" + targetTown.getName() + " has defended itself against the raid!");
            raidActive.put(townName, false);
            return;
        }

        // RAID SUCCESSFUL: Remove town and transfer blocks
        Set<?> blocks = Set.copyOf(targetTown.getTownBlocks());
        TownyUniverse.getInstance().getDataSource().removeTown(targetTown);

        for (Object block : blocks) {
            try {
                com.palmergames.bukkit.towny.object.TownBlock tb = (com.palmergames.bukkit.towny.object.TownBlock) block;
                tb.setTown(attackerTown);
                attackerTown.addTownBlock(tb);
            } catch (Exception ex) {
                plugin.getLogger().warning("Block transfer failed: " + ex.getMessage());
            }
        }

        attackerTown.save();
        raidActive.put(townName, false);

        Bukkit.broadcastMessage("§6⚔ " + killer.getName() + " has conquered and annexed " + townName + "!");
    }
}
