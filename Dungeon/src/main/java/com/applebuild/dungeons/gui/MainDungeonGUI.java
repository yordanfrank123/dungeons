package com.applebuild.dungeons.gui;

import com.applebuild.dungeons.CustomDungeons;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainDungeonGUI implements Listener {

    private final Inventory inventory;
    private final Player player;
    private final CustomDungeons plugin;
    private boolean closingForSubGUI = false; // Flag to indicate if closing for sub-GUI

    public MainDungeonGUI(Player player) {
        this.player = player;
        this.plugin = CustomDungeons.getPlugin(CustomDungeons.class);
        this.inventory = Bukkit.createInventory(null, 27, Component.text("Menú Principal de Dungeons", NamedTextColor.DARK_PURPLE));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] New GUI instance created and listener registered for " + player.getName());
        }
    }

    private void initializeItems() {
        inventory.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, "§7", ""));
        }

        inventory.setItem(11, createGuiItem(Material.NETHER_STAR, "§bGestionar Dungeons",
                "§7Crea, edita o elimina", "§7tus configuraciones de dungeons."));

        inventory.setItem(13, createGuiItem(Material.ENDER_PEARL, "§bIniciar Dungeon",
                "§7Inicia una nueva instancia", "§7de dungeon."));

        inventory.setItem(15, createGuiItem(Material.COMPASS, "§bObjetivos Globales",
                "§7Gestiona los objetivos reutilizables", "§7que se pueden asignar a las dungeons."));

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] Items initialized for " + player.getName());
        }
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name));
        meta.lore(Arrays.stream(lore)
                .map(Component::text)
                .collect(Collectors.toList()));

        item.setItemMeta(meta);
        return item;
    }

    public void open() {
        initializeItems();
        player.openInventory(inventory);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] GUI opened for " + player.getName());
        }
    }

    public void returnFromSubGUI() {
        // This method is called by sub-GUIs when they close, to reopen this one.
        Bukkit.getScheduler().runTask(plugin, () -> {
            initializeItems();
            player.openInventory(inventory);
        });
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] Returning from sub-GUI for " + player.getName());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player clickedPlayer)) return;
        if (!clickedPlayer.getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true); 

        final ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        Material type = clickedItem.getType();
        int slot = event.getSlot();

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] Player " + player.getName() + " clicked on " + type.name() + " at slot " + slot);
        }

        if (slot == 11 && type == Material.NETHER_STAR) {
            closingForSubGUI = true; // Set flag
            player.closeInventory();
            new DungeonManagementGUI(player).open();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[MainDungeonGUI] Player " + player.getName() + " opened DungeonManagementGUI.");
            }
        } else if (slot == 13 && type == Material.ENDER_PEARL) {
            // Lógica para iniciar una dungeon
            player.sendMessage(Component.text("§aFuncionalidad 'Iniciar Dungeon' en desarrollo.", NamedTextColor.GREEN));
        } else if (slot == 15 && type == Material.COMPASS) {
            closingForSubGUI = true; // Set flag
            player.closeInventory();
            new ObjectiveListGUI(player).open(); // Abre la GUI de objetivos globales
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[MainDungeonGUI] Player " + player.getName() + " opened ObjectiveListGUI (Global).");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        if (closingForSubGUI) {
            // If we are closing because a sub-GUI is being opened, do not unregister.
            // The sub-GUI will handle its own listener lifecycle.
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[MainDungeonGUI] Inventory closed for " + player.getName() + " (intentional for sub-GUI). Listener remains registered.");
            }
            closingForSubGUI = false; // Reset flag
            return;
        }

        // If not closing for a sub-GUI, then the player just closed the main GUI. Unregister.
        unregisterListener();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] Inventory closed for " + player.getName() + ". Listener unregistered.");
        }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[MainDungeonGUI] Listener for " + player.getName() + " fully unregistered.");
        }
    }
}
