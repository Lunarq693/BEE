package com.lunar.raid.EarthGUI;

import com.lunar.raid.Raid.TownUpgradeSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;

public class BossInfoGUI implements Listener {
    private final JavaPlugin plugin;

    public BossInfoGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6👑 Boss Information");

        // Create the glass pane pattern for larger GUI
        for (int i = 0; i < 54; i++) {
            boolean isEdge = i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8;

            if (isEdge) {
                Material paneMaterial;

                if (i < 9) {
                    paneMaterial = (i == 0 || i == 8) ? Material.ORANGE_STAINED_GLASS_PANE
                            : (i % 2 == 0 ? Material.ORANGE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
                } else if (i >= 45) {
                    int pos = i - 45;
                    paneMaterial = (pos == 0 || pos == 8) ? Material.ORANGE_STAINED_GLASS_PANE
                            : (pos % 2 == 0 ? Material.ORANGE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
                } else {
                    paneMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                }

                gui.setItem(i, createPane(paneMaterial));
            }
        }

        // Add boss information items
        gui.setItem(10, createBossItem(Material.WITHER_SKELETON_SKULL, "§8Dark Knight Boss", "§7Mob: §fbl_dark_knight", "§7Health: §c500 HP", "§7Abilities: §eSword Strike, Dark Magic"));
        gui.setItem(12, createBossItem(Material.OAK_LEAVES, "§2Forest Guardian Boss", "§7Mob: §fForestGuardian", "§7Health: §c750 HP", "§7Abilities: §eNature's Wrath, Healing"));
        gui.setItem(14, createBossItem(Material.BLACK_CONCRETE, "§0Backrooms Boss", "§7Mob: §fdimmer_sister", "§7Health: §c600 HP", "§7Abilities: §eTeleport, Confusion"));
        gui.setItem(16, createBossItem(Material.ENDER_EYE, "§5Deceptor Boss", "§7Mob: §fmf_deceptor", "§7Health: §c800 HP", "§7Abilities: §eIllusions, Mind Control"));

        gui.setItem(19, createBossItem(Material.AMETHYST_SHARD, "§dIllusionist Boss", "§7Mob: §fbl_illusionist", "§7Health: §c700 HP", "§7Abilities: §eMirror Images, Vanish"));
        gui.setItem(21, createBossItem(Material.BONE, "§fNightarrow Wendigo Boss", "§7Mob: §fNightharrow_Wendigo", "§7Health: §c900 HP", "§7Abilities: §eFreeze, Howl"));
        gui.setItem(23, createBossItem(Material.SKELETON_SKULL, "§7Tower Skeleton Boss", "§7Mob: §fTOWER_SKELETON", "§7Health: §c650 HP", "§7Abilities: §eArrow Rain, Bone Shield"));
        gui.setItem(25, createBossItem(Material.COAL_BLOCK, "§4Krampus Boss", "§7Mob: §fLRD_KRAMPUS", "§7Health: §c1000 HP", "§7Abilities: §eChain Attack, Fear"));

        gui.setItem(31, createBossItem(Material.SLIME_BALL, "§aVirus Queen Boss", "§7Mob: §fgm_virusg_queen", "§7Health: §c850 HP", "§7Abilities: §eInfection, Spawn Minions"));

        // Back button
        gui.setItem(49, createMenuItem(Material.ARROW, "§fBack to Raid Menu", "§7Return to the main raid menu"));

        player.openInventory(gui);
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

    private ItemStack createBossItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of(lore));
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
            meta.setLore(java.util.List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("§6👑 Boss Information")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().toString().contains("GLASS_PANE")) return;

        if (clicked.getType() == Material.ARROW) {
            player.closeInventory();
            new RaidGUI(plugin).open(player);
        }
    }
}
