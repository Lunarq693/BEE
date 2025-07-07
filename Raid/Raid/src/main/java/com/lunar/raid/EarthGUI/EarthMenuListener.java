package com.lunar.raid.EarthGUI;

import com.lunar.raid.EarthGUI.TownyGUI.TownyGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthMenuListener implements Listener {

    private final JavaPlugin plugin;
    private final TownyGUIListener townyGUIListener;

    public EarthMenuListener(JavaPlugin plugin, TownyGUIListener townyGUIListener) {
        this.plugin = plugin;
        this.townyGUIListener = townyGUIListener;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§2🌍 Earth Menu")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        switch (event.getSlot()) {
            case 11: // Town Management
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    townyGUIListener.open(player); // Use TownyGUIListener instead of TownyGUI
                });
                break;

            case 13: // Raid
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    new RaidGUI(plugin).open(player);
                });
                break;

            case 15: // Server Features
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    new ServerGUI(plugin).open(player);
                });
                break;

            case 26: // Hub button
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    player.performCommand("hub");
                });
                break;
        }
    }
}