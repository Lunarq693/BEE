package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

import java.util.Iterator;

public class EarthItemManager implements Listener {

    private static final String EARTH_WORLD_NAME = "Earth";
    private static final int EARTH_ITEM_MODEL_DATA = 12345;

    private final JavaPlugin plugin;

    public EarthItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Give item after 1 tick to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getWorld().getName().equals(EARTH_WORLD_NAME)) {
                giveEarthItem(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String newWorldName = player.getWorld().getName();

        if (newWorldName.equals(EARTH_WORLD_NAME)) {
            // Player entered Earth world - give item
            giveEarthItem(player);
        } else {
            // Player left Earth world - remove item
            removeEarthItem(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Check after respawn if player is in Earth world
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getWorld().getName().equals(EARTH_WORLD_NAME)) {
                giveEarthItem(player);
            }
        }, 3L); // 3 ticks delay to ensure respawn is complete
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && isEarthItem(item)) {
            if (event.getAction().toString().contains("RIGHT_CLICK")) {
                event.setCancelled(true);

                // Execute earth command on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.performCommand("earth");
                });
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (isEarthItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§l[Earth] §7You cannot drop the Earth Menu item!");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Remove Earth item from death drops
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isEarthItem(item)) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Check if Earth item is involved
        boolean clickedIsEarth = clickedItem != null && isEarthItem(clickedItem);
        boolean cursorIsEarth = cursorItem != null && isEarthItem(cursorItem);

        if (!clickedIsEarth && !cursorIsEarth) return;

        // Allow movement within player's own inventory
        if (event.getInventory().getType() == InventoryType.CRAFTING ||
                event.getInventory().equals(player.getInventory())) {

            // Check if the clicked slot is in the player's inventory section
            if (event.getClickedInventory() != null &&
                    event.getClickedInventory().equals(player.getInventory())) {

                // Allow movement but prevent stacking
                if (clickedIsEarth && cursorIsEarth) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l[Earth] §7You can only have one Earth Menu item!");
                }
                return; // Allow the move
            }
        }

        // Block all other inventory interactions
        if (clickedIsEarth || cursorIsEarth) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Earth] §7The Earth Menu item cannot be stored in containers!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || !isEarthItem(draggedItem)) return;

        // Allow dragging within player inventory only
        boolean allowDrag = true;
        for (int slot : event.getRawSlots()) {
            // Check if slot is outside player inventory area
            if (slot < event.getInventory().getSize()) {
                allowDrag = false;
                break;
            }
        }

        if (!allowDrag) {
            event.setCancelled(true);
            player.sendMessage("§c§l[Earth] §7The Earth Menu item cannot be stored in containers!");
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        for (ItemStack item : matrix) {
            if (item != null && isEarthItem(item)) {
                event.getInventory().setResult(null);
                break;
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack[] items = event.getInventory().getContents();

        for (ItemStack item : items) {
            if (item != null && isEarthItem(item)) {
                event.setResult(null);
                break;
            }
        }
    }

    private void giveEarthItem(Player player) {
        // Check if player already has the item
        if (hasEarthItem(player)) {
            return;
        }

        ItemStack earthItem = createEarthItem();
        player.getInventory().addItem(earthItem);
    }

    private void removeEarthItem(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isEarthItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private boolean hasEarthItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isEarthItem(item)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createEarthItem() {
        ItemStack item = new ItemStack(Material.BLUE_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§2🌍 Earth Menu");
            meta.setLore(java.util.List.of("§7Right-click to open Earth Menu"));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.setCustomModelData(EARTH_ITEM_MODEL_DATA);
            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean isEarthItem(ItemStack item) {
        if (item == null || item.getType() != Material.BLUE_DYE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.hasCustomModelData() &&
                meta.getCustomModelData() == EARTH_ITEM_MODEL_DATA &&
                meta.hasDisplayName() &&
                meta.getDisplayName().equals("§2🌍 Earth Menu");
    }
}
