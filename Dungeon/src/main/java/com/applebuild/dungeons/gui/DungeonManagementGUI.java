package com.applebuild.dungeons.gui;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.DungeonConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DungeonManagementGUI implements Listener {

    private final Inventory inventory;
    private final Player player;
    private final CustomDungeons plugin;

    private static final Map<UUID, String> dungeonNamePendingDeletion = new HashMap<>(); 
    private static final Map<UUID, Long> deletionConfirmationTime = new HashMap<>(); 

    private boolean closingForSubGUI = false; 

    public DungeonManagementGUI(Player player) {
        this.player = player;
        this.plugin = CustomDungeons.getPlugin(CustomDungeons.class);
        this.inventory = Bukkit.createInventory(null, 54, Component.text("Gestión de Dungeons", NamedTextColor.DARK_AQUA));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManagementGUI] New GUI instance created and listener registered for " + player.getName());
        }
    }

    private void initializeItems() {
        inventory.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", ""));
        }

        List<DungeonConfig> dungeonsToDisplay = plugin.getDungeonManager().getAllDungeonConfigs().values().stream()
                .sorted(Comparator.comparing(DungeonConfig::getName))
                .collect(Collectors.toList());

        int slot = 0;
        for (DungeonConfig dungeon : dungeonsToDisplay) {
            if (slot >= 45) break; 

            List<String> lore = new ArrayList<>();
            lore.add("§7ID: §f" + dungeon.getId());
            lore.add("§7Activa: " + (dungeon.isActive() ? "§aSí" : "§cNo"));
            // lore.add("§7Esquema: §f" + (dungeon.getSchematicName() != null ? dungeon.getSchematicName() : "No definido")); // REMOVED
            lore.add("§7Spawn: " + (dungeon.getSpawnLocation() != null ? "§aSeteado" : "§cNo seteado"));
            lore.add("§7Región: " + (dungeon.getMinLocation() != null && dungeon.getMaxLocation() != null ? "§aSeteada" : "§cNo seteada"));
            lore.add("");
            lore.add("§eClic normal: §bEditar Dungeon");
            lore.add("§eShift + Clic izquierdo: §cEliminar Dungeon (¡PELIGRO!)");
            
            inventory.setItem(slot, createGuiItem(Material.TOTEM_OF_UNDYING, "§b" + dungeon.getName(), lore.toArray(new String[0])));
            slot++;
        }

        inventory.setItem(49, createGuiItem(Material.LIME_CONCRETE, "§aCrear Nueva Dungeon",
                "§7Haz clic para crear una nueva", "§7configuración de dungeon."));

        inventory.setItem(53, createGuiItem(Material.RED_BED, "§cVolver",
                "§7Regresar al menú principal."));

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManagementGUI] Items initialized for " + player.getName() + ". Displaying " + dungeonsToDisplay.size() + " dungeons.");
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
            plugin.getLogger().info("[DungeonManagementGUI] GUI opened for " + player.getName());
        }
    }

    public void returnFromSubGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            initializeItems();
            player.openInventory(inventory);
        });
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManagementGUI] Returning from sub-GUI for " + player.getName());
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
            plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " clicked on " + type.name() + " at slot " + slot + " with ClickType: " + event.getClick());
        }

        if (slot == 49 && type == Material.LIME_CONCRETE) { 
            closingForSubGUI = true; 
            player.closeInventory();
            new DungeonCreationGUI(player, this).open(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " opened DungeonCreationGUI.");
            }
        } else if (slot == 53 && type == Material.RED_BED) { 
            player.closeInventory();
            new MainDungeonGUI(player).open();
            unregisterListener(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " returned to MainDungeonGUI.");
            }
        } else if (type == Material.TOTEM_OF_UNDYING) { 
            List<DungeonConfig> dungeonsToDisplay = plugin.getDungeonManager().getAllDungeonConfigs().values().stream()
                    .sorted(Comparator.comparing(DungeonConfig::getName))
                    .collect(Collectors.toList());

            if (slot < dungeonsToDisplay.size()) {
                DungeonConfig selectedDungeon = dungeonsToDisplay.get(slot);
                String dungeonId = selectedDungeon.getId(); 
                String dungeonName = selectedDungeon.getName();

                if (event.getClick() == ClickType.SHIFT_LEFT) { 
                    if (dungeonNamePendingDeletion.containsKey(player.getUniqueId()) && 
                        dungeonNamePendingDeletion.get(player.getUniqueId()).equals(dungeonName) &&
                        (System.currentTimeMillis() - deletionConfirmationTime.get(player.getUniqueId()) < 5000)) { 
                        
                        plugin.getDungeonManager().deleteDungeonConfig(dungeonId); 
                        player.sendMessage(Component.text("§aDungeon '" + dungeonName + "' eliminada exitosamente.", NamedTextColor.GREEN));
                        dungeonNamePendingDeletion.remove(player.getUniqueId());
                        deletionConfirmationTime.remove(player.getUniqueId());
                        initializeItems(); 
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " confirmed deletion of dungeon " + dungeonId);
                        }
                    } else {
                        dungeonNamePendingDeletion.put(player.getUniqueId(), dungeonName);
                        deletionConfirmationTime.put(player.getUniqueId(), System.currentTimeMillis());
                        player.sendMessage(Component.text("§c¿Estás seguro de que quieres eliminar la dungeon '" + dungeonName + "'? Haz clic de nuevo con SHIFT + CLIC IZQUIERDO en 5 segundos para confirmar.", NamedTextColor.RED));
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " initiated deletion for dungeon " + dungeonId);
                        }
                    }
                } else if (event.getClick() == ClickType.LEFT) { 
                    closingForSubGUI = true;
                    player.closeInventory();
                    // CORRECCIÓN: Pasar 'this' como parentGUI
                    new DungeonEditGUI(player, this, selectedDungeon).open(); 
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " opened edit GUI for dungeon " + dungeonId);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        if (closingForSubGUI) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManagementGUI] Inventory closed for " + player.getName() + " (intentional for sub-GUI). Listener remains registered.");
            }
            closingForSubGUI = false; 
            return;
        }

        if (dungeonNamePendingDeletion.containsKey(player.getUniqueId())) {
            dungeonNamePendingDeletion.remove(player.getUniqueId());
            deletionConfirmationTime.remove(player.getUniqueId());
            player.sendMessage(Component.text("§eEliminación de dungeon cancelada.", NamedTextColor.YELLOW));
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManagementGUI] Player " + player.getName() + " closed GUI, cancelling pending dungeon deletion.");
            }
        }

        unregisterListener();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManagementGUI] Inventory closed for " + player.getName() + ". Listener unregistered.");
        }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManagementGUI] Listener for " + player.getName() + " fully unregistered.");
        }
    }
}
