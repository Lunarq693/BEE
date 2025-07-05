package com.lunar.raid.Raid;

import com.lunar.raid.EarthGUI.EarthMenuCommand;
import com.lunar.raid.EarthGUI.TownyGUI.PlotManagement;
import com.lunar.raid.EarthGUI.TownyGUI.TownManagement;
import com.lunar.raid.EarthGUI.TownyGUI.TownyGUIListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class Raid extends JavaPlugin {

    @Override
    public void onEnable() {
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

        getServer().getPluginManager().registerEvents(townMgmt, this);
        getServer().getPluginManager().registerEvents(plotMgmt, this);
        getServer().getPluginManager().registerEvents(new RaidListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradeGUI(this), this);

        // Register commands, ensuring they are properly defined in plugin.yml
        registerCommand("earth", new EarthMenuCommand(this, townyGUI));
        registerCommand("raid", new RaidCommand(this));
        this.getCommand("stamp").setExecutor(new StampCommand(this));
        getCommand("townupgrade").setExecutor(new UpgradeCommand(this)); // Ensure this command works
        getCommand("townupgrade").setExecutor(new TownUpgradeSystem(this));  // Or your appropriate command executor

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
}
