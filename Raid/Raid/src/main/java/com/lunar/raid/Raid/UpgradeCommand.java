package com.lunar.raid.Raid;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpgradeCommand implements CommandExecutor {
    private final Raid plugin;

    public UpgradeCommand(Raid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this!");
            return true;
        }
        // Open the Upgrade GUI
        new UpgradeGUI(plugin).open(player);  // This should open the GUI when the command is used
        return true;
    }
}
