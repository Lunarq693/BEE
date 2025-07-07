package com.lunar.raid.Raid;

import com.lunar.raid.EarthGUI.DungeonsGUICommand;
import com.lunar.raid.EarthGUI.EarthItemManager;
import com.lunar.raid.EarthGUI.EarthMenuCommand;
import com.lunar.raid.EarthGUI.TownyGUI.PlotManagement;
import com.lunar.raid.EarthGUI.TownyGUI.TownManagement;
import com.lunar.raid.EarthGUI.TownyGUI.TownyGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Raid extends JavaPlugin {

    private static Raid instance;
    private Map<UUID, RaidData> activeRaids = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Check if Towny is installed
        if (getServer().getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Create instances of the managers and listeners
        TownManagement townMgmt = new TownManagement(this);
        TownyGUIListener townyGUI = new TownyGUIListener(this, townMgmt);
        PlotManagement plotMgmt = new PlotManagement(this);

        // Setting up the listeners
        townyGUI.setPlotManagement(plotMgmt);
        plotMgmt.setTownyGUIListener(townyGUI);
        townMgmt.setTownyGUIListener(townyGUI);

        // Register all event listeners
        getServer().getPluginManager().registerEvents(townMgmt, this);
        getServer().getPluginManager().registerEvents(plotMgmt, this);
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradeGUI(this), this);
        getServer().getPluginManager().registerEvents(new EarthItemManager(this), this);

        // Register commands, ensuring they are properly defined in plugin.yml
        registerCommand("earth", new EarthMenuCommand(this, townyGUI));
        registerCommand("raid", new RaidCommand(this));
        registerCommand("dungeonsGUI", new DungeonsGUICommand(this));
        this.getCommand("stamp").setExecutor(new StampCommand(this));
        registerCommand("upgrades", new UpgradeCommand(this));

        getLogger().info("Raid plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Raid plugin has been disabled!");
    }

    private void registerCommand(String commandName, CommandExecutor commandExecutor) {
        if (this.getCommand(commandName) != null) {
            if (commandExecutor != null) {
                this.getCommand(commandName).setExecutor(commandExecutor);
            }
        } else {
            getLogger().warning("Command /" + commandName + " not found in plugin.yml");
        }
    }

    public static Raid getInstance() {
        return instance;
    }

    public void startRaid(UUID playerId, String raidType) {
        RaidData raidData = new RaidData(raidType, System.currentTimeMillis());
        activeRaids.put(playerId, raidData);

        // Schedule raid end after 30 minutes (1800 seconds)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            endRaid(playerId);
        }, 36000L); // 30 minutes in ticks
    }

    public void endRaid(UUID playerId) {
        activeRaids.remove(playerId);
    }

    public boolean isInRaid(UUID playerId) {
        return activeRaids.containsKey(playerId);
    }

    public RaidData getRaidData(UUID playerId) {
        return activeRaids.get(playerId);
    }

    public static class RaidData {
        private final String raidType;
        private final long startTime;

        public RaidData(String raidType, long startTime) {
            this.raidType = raidType;
            this.startTime = startTime;
        }

        public String getRaidType() {
            return raidType;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
