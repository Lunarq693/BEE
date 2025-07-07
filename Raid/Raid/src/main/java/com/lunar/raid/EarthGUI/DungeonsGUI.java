package com.lunar.raid.EarthGUI;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonsGUI implements Listener {
    private static final Map<UUID, String> awaitingInput = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> processingPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, PurchaseData> awaitingPurchase = new ConcurrentHashMap<>();
    private static final long PROCESSING_TIMEOUT = 2000; // 2 seconds
    private static DungeonsGUI instance;
    private static JavaPlugin pluginInstance;
    private static Economy economy = null;

    private final JavaPlugin plugin;

    // Dungeon data structure
    private static class DungeonInfo {
        final String name;
        final String boss;
        final double price;
        final String permission;
        final String command;

        DungeonInfo(String name, String boss, double price, String permission, String command) {
            this.name = name;
            this.boss = boss;
            this.price = price;
            this.permission = permission;
            this.command = command;
        }
    }

    // Purchase data structure
    private static class PurchaseData {
        final String dungeonKey;
        final DungeonInfo dungeonInfo;

        PurchaseData(String dungeonKey, DungeonInfo dungeonInfo) {
            this.dungeonKey = dungeonKey;
            this.dungeonInfo = dungeonInfo;
        }
    }

    // Dungeon configuration
    private static final Map<String, DungeonInfo> DUNGEONS = new HashMap<>();
    static {
        DUNGEONS.put("tier1", new DungeonInfo("§8Tier I", "The Dark Knight Boss", 0, null, "TheDarkKnight"));
        DUNGEONS.put("tier2", new DungeonInfo("§2Tier II", "Forest Guardian Boss", 15000, "dungeon.tier2", "ForestGuardian"));
        DUNGEONS.put("tier3", new DungeonInfo("§fTier III", "Nightarrow Wendigo Boss", 50000, "dungeon.tier3", "nightarrow"));
        DUNGEONS.put("tier4", new DungeonInfo("§6Tier IV", "The Deceptor Boss", 100000, "dungeon.tier4", "deceptor"));
        DUNGEONS.put("tier5", new DungeonInfo("§dTier V", "The Illusionist Boss", 200000, "dungeon.tier5", "Illusionist"));
        DUNGEONS.put("tier6", new DungeonInfo("§bTier VI", "Krampus Boss", 350000, "dungeon.tier6", "krampus"));
        DUNGEONS.put("tier7", new DungeonInfo("§7Tier VII", "Tower Skeleton Boss", 707707, "dungeon.tier7", "towerskeleton"));
        DUNGEONS.put("tier8", new DungeonInfo("§aTier VIII", "Queen of Thorns Boss", 1000000, "dungeon.tier8", "VirusQueen"));
    }

    public DungeonsGUI(JavaPlugin plugin) {
        this.plugin = plugin;

        // Only register events once
        if (instance == null) {
            instance = this;
            pluginInstance = plugin;
            setupEconomy();
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy plugin found! Economy features will be disabled.");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Economy successfully hooked with " + economy.getName());
    }

    public void open(Player player) {
        // Clear any existing processing state for this player
        UUID playerId = player.getUniqueId();
        processingPlayers.remove(playerId);
        awaitingInput.remove(playerId);
        awaitingPurchase.remove(playerId);

        Inventory gui = Bukkit.createInventory(null, 54, "§5🏰 Dungeons");

        // Create the glass pane pattern for larger GUI
        for (int i = 0; i < 54; i++) {
            boolean isEdge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    paneMaterial = (i == 0 || i == 8) ? Material.PURPLE_STAINED_GLASS_PANE
                            : (i % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE);
                } else if (i >= 45) {
                    int pos = i - 45;
                    paneMaterial = (pos == 0 || pos == 8) ? Material.PURPLE_STAINED_GLASS_PANE
                            : (pos % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE);
                } else {
                    paneMaterial = Material.MAGENTA_STAINED_GLASS_PANE;
                }

                gui.setItem(i, createPane(paneMaterial));
            }
        }

        // Add dungeons in a 3x3 grid pattern
        int[] slots = {11, 13, 15, 20, 22, 24, 29, 31};
        String[] dungeonKeys = {"tier1", "tier2", "tier3", "tier4", "tier5", "tier6", "tier7", "tier8"};
        Material[] materials = {Material.BLACKSTONE, Material.OAK_LEAVES, Material.BONE_BLOCK, 
                               Material.OBSERVER, Material.AMETHYST_BLOCK, Material.PACKED_ICE, 
                               Material.COBBLESTONE, Material.SLIME_BLOCK};

        for (int i = 0; i < dungeonKeys.length && i < slots.length; i++) {
            DungeonInfo dungeon = DUNGEONS.get(dungeonKeys[i]);
            if (dungeon != null) {
                gui.setItem(slots[i], createDungeonItem(materials[i], dungeon, player));
            }
        }

        // Back button
        gui.setItem(49, createMenuItem(Material.ARROW, "§fBack to Raid Menu", "§7Return to the main raid menu"));

        // Party invite button (bottom right corner)
        gui.setItem(53, createGlowingMenuItem(Material.GOLD_BLOCK, "§6Party Invite", "§7Click to invite a player to your party"));

        player.openInventory(gui);
    }

    private ItemStack createDungeonItem(Material material, DungeonInfo dungeon, Player player) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(dungeon.name);

            boolean hasAccess = dungeon.permission == null || player.hasPermission(dungeon.permission);
            
            if (hasAccess) {
                // Player has access
                meta.setLore(Arrays.asList(
                        "§7" + dungeon.boss,
                        "§6Price: §a" + formatPrice(dungeon.price),
                        "",
                        "§aClick to enter dungeon!"
                ));
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } else {
                // Player doesn't have permission
                meta.setLore(Arrays.asList(
                        "§7" + dungeon.boss,
                        "§6Price: §c" + formatPrice(dungeon.price),
                        "",
                        "§c§lLOCKED",
                        "§7Click to purchase access!"
                ));
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatPrice(double price) {
        if (price == 0) return "FREE";
        if (price >= 1000000) return String.format("$%.1fM", price / 1000000);
        if (price >= 1000) return String.format("$%.0fK", price / 1000);
        return String.format("$%.0f", price);
    }

    private static boolean isProcessing(UUID playerId) {
        if (processingPlayers.containsKey(playerId)) {
            long processingTime = processingPlayers.get(playerId);
            if (System.currentTimeMillis() - processingTime < PROCESSING_TIMEOUT) {
                return true;
            } else {
                processingPlayers.remove(playerId);
            }
        }
        return false;
    }

    private static void setProcessing(UUID playerId) {
        processingPlayers.put(playerId, System.currentTimeMillis());

        // Auto-remove after timeout
        if (pluginInstance != null) {
            Bukkit.getScheduler().runTaskLater(pluginInstance, () -> {
                processingPlayers.remove(playerId);
            }, PROCESSING_TIMEOUT / 50); // Convert to ticks
        }
    }

    private void executeDungeonCommand(Player player, String dungeonKey) {
        UUID playerId = player.getUniqueId();

        if (isProcessing(playerId)) {
            return;
        }

        DungeonInfo dungeon = DUNGEONS.get(dungeonKey);
        if (dungeon == null) return;

        // Check permission if required
        if (dungeon.permission != null && !player.hasPermission(dungeon.permission)) {
            // Offer to purchase
            offerPurchase(player, dungeonKey, dungeon);
            return;
        }

        setProcessing(playerId);
        player.closeInventory();

        // Execute command through console
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "md play " + dungeon.command + " " + player.getName());
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "md play " + dungeon.command + " " + player.getName());
            });
        }
    }

    private void offerPurchase(Player player, String dungeonKey, DungeonInfo dungeon) {
        UUID playerId = player.getUniqueId();
        
        if (economy == null) {
            player.sendMessage("§c§l[Dungeons] §7Economy system is not available!");
            return;
        }

        if (awaitingPurchase.containsKey(playerId) || awaitingInput.containsKey(playerId)) {
            return; // Already in a purchase process
        }

        player.closeInventory();
        awaitingPurchase.put(playerId, new PurchaseData(dungeonKey, dungeon));
        
        player.sendMessage("§6§l[Dungeons] §7Purchase " + dungeon.name + "?");
        player.sendMessage("§7Boss: §e" + dungeon.boss);
        player.sendMessage("§7Price: §a" + formatPrice(dungeon.price));
        player.sendMessage("§7Your balance: §a$" + String.format("%.2f", economy.getBalance(player)));
        player.sendMessage("");
        player.sendMessage("§aType 'yes' to purchase or 'no' to cancel");
    }

    private void processPurchase(Player player, boolean confirm) {
        UUID playerId = player.getUniqueId();
        PurchaseData purchaseData = awaitingPurchase.remove(playerId);
        
        if (purchaseData == null) return;

        if (!confirm) {
            player.sendMessage("§6§l[Dungeons] §7Purchase cancelled.");
            return;
        }

        if (economy == null) {
            player.sendMessage("§c§l[Dungeons] §7Economy system is not available!");
            return;
        }

        DungeonInfo dungeon = purchaseData.dungeonInfo;
        
        // Check if player has enough money
        if (!economy.has(player, dungeon.price)) {
            double needed = dungeon.price - economy.getBalance(player);
            player.sendMessage("§c§l[Dungeons] §7You need $" + String.format("%.2f", needed) + " more to purchase this dungeon!");
            return;
        }

        // Withdraw money
        if (!economy.withdrawPlayer(player, dungeon.price).transactionSuccess()) {
            player.sendMessage("§c§l[Dungeons] §7Failed to process payment!");
            return;
        }

        // Give permission
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + dungeon.permission + " true");
        
        // Success message
        player.sendMessage("§a§l[Dungeons] §7Successfully purchased " + dungeon.name + "!");
        player.sendMessage("§7You can now access this dungeon anytime.");
        
        // Auto-enter the dungeon
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "md play " + dungeon.command + " " + player.getName());
        }, 20L); // 1 second delay
    }

    private ItemStack createPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createGlowingMenuItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMenuItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§5🏰 Dungeons")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().toString().contains("GLASS_PANE")) return;

        UUID playerId = player.getUniqueId();

        // Double check processing state
        if (isProcessing(playerId)) {
            return;
        }

        // Map slots to dungeon keys
        Map<Integer, String> slotToDungeon = new HashMap<>();
        slotToDungeon.put(11, "tier1");
        slotToDungeon.put(13, "tier2");
        slotToDungeon.put(15, "tier3");
        slotToDungeon.put(20, "tier4");
        slotToDungeon.put(22, "tier5");
        slotToDungeon.put(24, "tier6");
        slotToDungeon.put(29, "tier7");
        slotToDungeon.put(31, "tier8");

        String dungeonKey = slotToDungeon.get(event.getSlot());
        if (dungeonKey != null) {
            executeDungeonCommand(player, dungeonKey);
            return;
        }

        // Handle other buttons
        switch (clicked.getType()) {
            case GOLD_BLOCK:
                // Check if player is already awaiting input to prevent spam
                if (awaitingInput.containsKey(playerId) || isProcessing(playerId)) {
                    return;
                }

                setProcessing(playerId);
                player.closeInventory();
                awaitingInput.put(playerId, "party_invite");

                // Send message immediately
                player.sendMessage("§6§l[Party] §7Please type the name of the player you want to invite:");
                player.sendMessage("§7Type §c'cancel' §7to cancel the invitation.");
                break;
            case ARROW:
                if (isProcessing(playerId)) {
                    return;
                }
                setProcessing(playerId);
                player.closeInventory();

                // Open raid GUI immediately
                Bukkit.getScheduler().runTask(plugin, () -> {
                    new RaidGUI(plugin).open(player);
                });
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Handle purchase confirmation
        if (awaitingPurchase.containsKey(playerId)) {
            event.setCancelled(true);
            String message = event.getMessage().trim().toLowerCase();

            if (message.equals("yes") || message.equals("y")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processPurchase(player, true);
                });
            } else if (message.equals("no") || message.equals("n") || message.equals("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processPurchase(player, false);
                });
            } else {
                player.sendMessage("§c§l[Dungeons] §7Please type 'yes' to purchase or 'no' to cancel.");
            }
            return;
        }

        // Handle party invite
        if (!awaitingInput.containsKey(playerId)) {
            return;
        }

        String inputType = awaitingInput.get(playerId);

        if (inputType.equals("party_invite")) {
            event.setCancelled(true);
            awaitingInput.remove(playerId);
            processingPlayers.remove(playerId);

            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§6§l[Party] §7Party invitation cancelled.");
                });
                return;
            }

            // Schedule the command to run on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("party invite " + message);
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("§5🏰 Dungeons")) {
            UUID playerId = event.getPlayer().getUniqueId();
            // Clean up processing state when GUI is closed
            processingPlayers.remove(playerId);
        }
    }

    public static void cleanup() {
        if (instance != null) {
            HandlerList.unregisterAll(instance);
            instance = null;
            pluginInstance = null;
            awaitingInput.clear();
            processingPlayers.clear();
            awaitingPurchase.clear();
        }
    }
}