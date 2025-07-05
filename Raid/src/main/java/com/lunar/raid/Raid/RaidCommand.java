package com.lunar.raid.Raid;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class RaidCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;

    public RaidCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Location location = player.getLocation();
        Town locationTown = TownyAPI.getInstance().getTown(location);

        if (locationTown == null || TownyAPI.getInstance().isWilderness(location)) {
            player.sendMessage("§cYou cannot start a raid in the wilderness!");
            return true;
        }

        Resident resident = TownyAPI.getInstance().getResident(player);
        if (resident == null || !resident.hasTown()) {
            player.sendMessage("§cYou must be in a town to start a raid.");
            return true;
        }

        if (!resident.isMayor()) {
            player.sendMessage("§cOnly the mayor can initiate raids!");
            return true;
        }

        Town playerTown = resident.getTownOrNull();
        if (playerTown != null && playerTown.equals(locationTown)) {
            player.sendMessage("§cYou cannot raid your own town!");
            return true;
        }

        String level = "1";
        if (locationTown.hasMeta("raid_defense_level")) {
            CustomDataField<?> meta = locationTown.getMetadata("raid_defense_level");
            if (meta instanceof StringDataField stringField) {
                String val = stringField.getValue();
                if (val != null && val.matches("\\d+")) {
                    level = val;
                }
            }
        }

        FileConfiguration config = plugin.getConfig();
        String tierKey = "raids.level-" + level;

        if (!config.contains(tierKey)) {
            player.sendMessage("§cRaid configuration missing for level: " + level);
            return true;
        }

        String mobType = config.getString(tierKey + ".mob", "WITHER_SKELETON");
        int delay = config.getInt(tierKey + ".spawn-delay", 20);
        int radius = config.getInt(tierKey + ".broadcast-radius", 50);

        // Spawn 10 blocks above the player
        Location spawnLoc = location.clone().add(0, 10, 0);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitAPIHelper api = MythicBukkit.inst().getAPIHelper();
            try {
                Entity spawned = api.spawnMythicMob(mobType, spawnLoc, 1);
                if (!(spawned instanceof LivingEntity mob)) {
                    player.sendMessage("§cFailed to spawn a living mob.");
                    return;
                }

                mob.setMetadata("raided_town", new FixedMetadataValue(plugin, locationTown.getName()));
                mob.setMetadata("raider_name", new FixedMetadataValue(plugin, player.getName()));

                RaidListener.startRaid(locationTown.getName());

                player.sendMessage("§a" + mobType + " spawned!");
                player.getWorld().getPlayers().stream()
                        .filter(p -> !p.equals(player))
                        .filter(p -> p.getLocation().distance(spawnLoc) <= radius)
                        .forEach(p -> p.sendMessage("§c⚠ A raid has started nearby! ⚠"));

                final Location[] lastLocation = {spawnLoc.clone()};
                final boolean[] isProtected = {false};

                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (mob == null || !mob.isValid() || !mob.hasMetadata("raided_town")) {
                        task.cancel();
                        return;
                    }

                    Location current = mob.getLocation();
                    boolean notMoved = lastLocation[0].getBlockX() == current.getBlockX()
                            && lastLocation[0].getBlockY() == current.getBlockY()
                            && lastLocation[0].getBlockZ() == current.getBlockZ();

                    boolean headBlocked = current.clone().add(0, 1, 0).getBlock().getType().isSolid();
                    boolean insideBlock = current.getBlock().getType().isSolid();

                    boolean hasTarget = false;
                    boolean farFromTarget = false;

                    if (mob instanceof org.bukkit.entity.Mob mm && mm.getTarget() != null) {
                        hasTarget = true;
                        double dist = mm.getTarget().getLocation().distanceSquared(current);
                        farFromTarget = dist > 16;
                    }

                    boolean pathStuck = notMoved && hasTarget && farFromTarget;
                    boolean blockTrapped = headBlocked || insideBlock;

                    boolean trulyStuck = pathStuck || blockTrapped;

                    if (trulyStuck && !isProtected[0]) {
                        mob.setMetadata("stuck_protected", new FixedMetadataValue(plugin, true));
                        Location above = current.clone().add(0, 10, 0);
                        mob.teleport(above);
                        mob.getWorld().spawnParticle(Particle.PORTAL, above, 30);
                        player.sendMessage("§eThe raid boss was stuck and has been lifted upward!");
                        isProtected[0] = true;
                    }

                    if (!trulyStuck && isProtected[0]) {
                        mob.removeMetadata("stuck_protected", plugin);
                        isProtected[0] = false;
                    }

                    lastLocation[0] = current;

                }, 40L, 40L); // every 2 seconds

            } catch (Exception ex) {
                player.sendMessage("§cSpawn error: " + ex.getMessage());
                plugin.getLogger().warning("Spawn error: " + ex.getMessage());
            }
        }, delay);

        player.sendMessage("§6⚔ RAID STARTED! ⚔ Boss arriving above…");
        return true;
    }

    @EventHandler
    public void onBossMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (entity.hasMetadata("raided_town")) {
            String targetTownName = entity.getMetadata("raided_town").get(0).asString();
            Town fromTown = TownyAPI.getInstance().getTown(event.getFrom());
            Town toTown = TownyAPI.getInstance().getTown(event.getTo());

            boolean isLeavingTown = (toTown == null || !toTown.getName().equals(targetTownName));
            boolean wasInsideTown = (fromTown != null && fromTown.getName().equals(targetTownName));

            if (wasInsideTown && isLeavingTown) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRaidBossDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!entity.hasMetadata("raided_town")) return;

        if (entity.hasMetadata("stuck_protected")) {
            event.setCancelled(true);
            return;
        }

        if (event instanceof EntityDamageByEntityEvent ede) {
            if (!(ede.getDamager() instanceof Player)) {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }
}
