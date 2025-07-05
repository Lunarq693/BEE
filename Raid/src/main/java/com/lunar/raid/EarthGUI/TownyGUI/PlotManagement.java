package com.lunar.raid.EarthGUI.TownyGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PlotManagement implements Listener {

    private final JavaPlugin plugin;
    private TownyGUIListener townyGUIListener;  // for back navigation
    private final String title = "§2📦 Plot Management";

    private final Map<Player, String> awaitingInput = new HashMap<>();

    public PlotManagement(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setTownyGUIListener(TownyGUIListener listener) {
        this.townyGUIListener = listener;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, title);

        Material lime = Material.LIME_STAINED_GLASS_PANE;
        Material lightBlue = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

        // Glass border top and bottom
        for (int i = 0; i < 9; i++) inv.setItem(i, createGlassPane((i % 2 == 0) ? lime : lightBlue));
        for (int i = 36; i < 45; i++) inv.setItem(i, createGlassPane(((i - 36) % 2 == 0) ? lime : lightBlue));

        // Glass border sides
        int[] leftSlots = {0, 9, 18, 27, 36};
        int[] rightSlots = {8, 17, 26, 35, 44};
        for (int i = 0; i < leftSlots.length; i++) {
            inv.setItem(leftSlots[i], createGlassPane((i % 2 == 0) ? lime : lightBlue));
            inv.setItem(rightSlots[i], createGlassPane((i % 2 == 0) ? lime : lightBlue));
        }

        // Shifted core menu items one slot to the right:
        inv.setItem(12, createMenuItem(Material.GRASS_BLOCK, "§aClaim Plot", "Claim the plot you are standing on", false));
        inv.setItem(13, createMenuItem(Material.REDSTONE_BLOCK, "§cUnclaim Plot", "Unclaim the plot you are standing on", false));
        inv.setItem(14, createMenuItem(Material.BARRIER, "§4Unclaim All", "Unclaim all plots of your town", false));

        inv.setItem(21, createMenuItem(Material.EMERALD, "§6Set Plot For Sale", "Set a sale price for this plot", true));
        inv.setItem(22, createMenuItem(Material.RED_DYE, "§cRemove Plot From Sale", "Stop selling this plot", false));
        inv.setItem(23, createMenuItem(Material.NAME_TAG, "§bSet Plot Name", "Rename this plot", true));

        inv.setItem(36, createMenuItem(Material.ARROW, "§cGo Back", "Return to Towny Management menu", false));

        player.openInventory(inv);
    }

    private ItemStack createGlassPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack createMenuItem(Material material, String name, String lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(title)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        player.closeInventory();

        switch (name) {
            case "§aClaim Plot" -> Bukkit.dispatchCommand(player, "town claim");
            case "§cUnclaim Plot" -> Bukkit.dispatchCommand(player, "town unclaim");
            case "§4Unclaim All" -> Bukkit.dispatchCommand(player, "town unclaim all");
            case "§6Set Plot For Sale" -> promptInput(player, "§eEnter sale price:", "plot forsale");
            case "§cRemove Plot From Sale" -> Bukkit.dispatchCommand(player, "plot notforsale");
            case "§bSet Plot Name" -> promptInput(player, "§eEnter new plot name:", "plot set name");
            case "§cGo Back" -> {
                if (townyGUIListener != null) townyGUIListener.open(player);
            }
            default -> player.sendMessage("§cUnknown command.");
        }
    }

    public void promptInput(Player player, String prompt, String commandPrefix) {
        if (awaitingInput.containsKey(player)) {
            player.sendMessage("§cYou already have a pending input.");
            return;
        }
        awaitingInput.put(player, commandPrefix);
        player.sendMessage(prompt);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingInput.containsKey(player)) return;

        event.setCancelled(true);
        String input = event.getMessage();
        String command = awaitingInput.remove(player);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(player, command + " " + input);
            open(player);
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (awaitingInput.containsKey(player)) {
            awaitingInput.remove(player);
            player.sendMessage("§cInput cancelled because you closed the inventory.");
        }
    }
}
