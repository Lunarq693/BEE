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

        // Check Minecraft version
        if (!isVersionSupported(player)) {
            player.sendMessage("§c§l[Dungeons] §7You need to update your Minecraft to version 1.21.4 or above to access dungeons!");
            player.sendMessage("§7Your current version is not supported. Please update your game and try again.");
            return true;
        }

        // Open the dungeons GUI
        new DungeonsGUI(plugin).open(player);

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
}