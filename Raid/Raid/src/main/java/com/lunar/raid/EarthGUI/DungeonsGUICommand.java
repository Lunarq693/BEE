package com.lunar.raid.EarthGUI;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonsGUICommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public DungeonsGUICommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§l[Dungeons] §7This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Open the dungeons GUI
        new DungeonsGUI(plugin).open(player);

        return true;
    }
}
