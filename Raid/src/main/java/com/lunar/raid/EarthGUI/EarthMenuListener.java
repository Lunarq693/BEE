package com.lunar.raid.EarthGUI;

import com.lunar.raid.EarthGUI.TownyGUI.TownyGUI;
import com.lunar.raid.EarthGUI.TownyGUI.TownyGUIListener;
import com.lunar.raid.Raid.UpgradeGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthMenuListener implements Listener {

    private final JavaPlugin plugin;
    private final TownyGUIListener townyGUIListener;

    public EarthMenuListener(JavaPlugin plugin, TownyGUIListener townyGUIListener) {
        this.plugin = plugin;
        this.townyGUIListener = townyGUIListener;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().equals("§2🌍 Earth Menu")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        switch (clicked.getType()) {
            case EMERALD_BLOCK -> {
                // Open the Town Defense (Upgrade) GUI when clicked
                new UpgradeGUI(plugin).open(player);  // Opens your existing Upgrade GUI
            }
            case PAPER -> Bukkit.getScheduler().runTask(plugin, () -> townyGUIListener.open(player));
            case BUNDLE -> Bukkit.getScheduler().runTask(plugin, () -> new ServerGUI(plugin).open(player));
            default -> player.sendMessage("§cUnknown selection.");
        }
    }
}
