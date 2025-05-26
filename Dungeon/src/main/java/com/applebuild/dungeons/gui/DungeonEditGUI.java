package com.applebuild.dungeons.gui;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.DungeonConfig;
import com.applebuild.dungeons.selection.PlayerSelectionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DungeonEditGUI implements Listener {

    private final Inventory inventory;
    private final Player player;
    private final CustomDungeons plugin;
    private final DungeonConfig dungeonConfig;
    private final DungeonManagementGUI parentGUI; 

    private static final Map<UUID, DungeonEditGUI> waitingForChatInput = new HashMap<>();
    private InputType currentInputType = null;
    private boolean closingForChatInput = false;
    private boolean closingForSubGUI = false;

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9 ]+$");

    private enum InputType {
        NAME, SCHEMATIC_NAME
    }

    public DungeonEditGUI(Player player, DungeonManagementGUI parentGUI, DungeonConfig dungeonConfig) {
        this.player = player;
        this.plugin = CustomDungeons.getPlugin(CustomDungeons.class);
        this.dungeonConfig = dungeonConfig;
        this.parentGUI = parentGUI; 

        this.inventory = Bukkit.createInventory(null, 54, Component.text("Editar Dungeon: " + dungeonConfig.getName(), NamedTextColor.DARK_AQUA));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonEditGUI] New GUI instance created and listener registered for " + player.getName() + " editing dungeon: " + dungeonConfig.getName());
        }
    }

    private void initializeItems() {
        inventory.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", ""));
        }

        inventory.setItem(10, createGuiItem(Material.NAME_TAG, "§bNombre: §f" + dungeonConfig.getName(),
                "§7Clic para cambiar el nombre."));

        inventory.setItem(12, createGuiItem(Material.PAPER, "§bEsquema: §f" + (dungeonConfig.getSchematicName() != null ? dungeonConfig.getSchematicName() : "No definido"),
                "§7Clic para cambiar el nombre del esquema.", "§7(Ej: my_dungeon_layout)"));

        inventory.setItem(14, createGuiItem(Material.COMPASS, "§bSpawn: " + (dungeonConfig.getSpawnLocation() != null ? "§aSeteado" : "§cNo seteado"),
                "§7Clic para establecer tu ubicación actual", "§7como punto de spawn de la dungeon."));

        inventory.setItem(16, createGuiItem(Material.STONE_PICKAXE, "§bRegión: " + (dungeonConfig.getMinLocation() != null && dungeonConfig.getMaxLocation() != null ? "§aSeteada" : "§cNo seteada"),
                "§7Clic para establecer la región de la dungeon.", "§7(Necesitas seleccionar 2 puntos con la herramienta de selección)"));

        inventory.setItem(28, createGuiItem(Material.ITEM_FRAME, "§bObjetivos: §f" + dungeonConfig.getObjectiveIds().size(),
                "§7Clic para gestionar los objetivos", "§7asociados a esta dungeon."));

        inventory.setItem(30, createGuiItem(dungeonConfig.isActive() ? Material.LIME_WOOL : Material.RED_WOOL,
                "§bEstado: " + (dungeonConfig.isActive() ? "§aActiva" : "§cInactiva"),
                "§7Clic para alternar el estado de la dungeon.", "§7(Solo las dungeons activas pueden ser jugadas)"));

        inventory.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§aGuardar Cambios",
                "§7Haz clic para guardar las", "§7modificaciones de esta dungeon."));

        inventory.setItem(53, createGuiItem(Material.RED_BED, "§cVolver",
                "§7Regresar al menú anterior."));
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonEditGUI] Items initialized for " + player.getName() + " editing dungeon: " + dungeonConfig.getName());
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
            plugin.getLogger().info("[DungeonEditGUI] GUI opened for " + player.getName() + " editing dungeon: " + dungeonConfig.getName());
        }
    }

    public void returnFromSubGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            initializeItems();
            player.openInventory(inventory);
        });
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonEditGUI] Returning from sub-GUI for " + player.getName());
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
            plugin.getLogger().info("[DungeonEditGUI] Player " + player.getName() + " clicked on " + type.name() + " at slot " + slot);
        }

        if (slot == 10 && type == Material.NAME_TAG) { 
            closingForChatInput = true;
            player.closeInventory();
            waitingForChatInput.put(player.getUniqueId(), this);
            currentInputType = InputType.NAME;
            player.sendMessage(Component.text("§bEscribe el nuevo nombre para la dungeon. Escribe 'cancelar' para abortar."));
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Player " + player.getName() + " is now waiting for NAME input via chat.");
            }
        } else if (slot == 12 && type == Material.PAPER) { 
            closingForChatInput = true;
            player.closeInventory();
            waitingForChatInput.put(player.getUniqueId(), this);
            currentInputType = InputType.SCHEMATIC_NAME;
            player.sendMessage(Component.text("§bEscribe el nombre del archivo de esquema (ej. 'my_dungeon_layout'). Escribe 'cancelar' para abortar."));
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Player " + player.getName() + " is now waiting for SCHEMATIC_NAME input via chat.");
            }
        } else if (slot == 14 && type == Material.COMPASS) { 
            dungeonConfig.setSpawnLocation(player.getLocation());
            plugin.getDungeonManager().saveDungeonConfigToFile(dungeonConfig);
            player.sendMessage(Component.text("§aSpawn de la dungeon establecido a tu ubicación actual.", NamedTextColor.GREEN));
            initializeItems(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Spawn location set for dungeon " + dungeonConfig.getName() + " to " + player.getLocation());
            }
        } else if (slot == 16 && type == Material.STONE_PICKAXE) { 
            PlayerSelectionManager selectionManager = plugin.getPlayerSelectionManager();
            // CORRECCIÓN: hasBothPointsSelected ya toma UUID
            if (!selectionManager.hasBothPointsSelected(player.getUniqueId())) {
                player.sendMessage(Component.text("§cNecesitas seleccionar ambos puntos de la región con la herramienta de selección primero.", NamedTextColor.RED));
                // Optional: Start selection mode if not active.
                selectionManager.startSelection(player, this); // Pass this GUI instance to reopen it later
                return;
            }
            // CORRECCIÓN: getMinPoint y getMaxPoint ya toman UUID
            Location minLoc = selectionManager.getMinPoint(player.getUniqueId());
            Location maxLoc = selectionManager.getMaxPoint(player.getUniqueId());

            if (minLoc == null || maxLoc == null || !minLoc.getWorld().equals(maxLoc.getWorld())) {
                player.sendMessage(Component.text("§cLos puntos de la región deben estar en el mismo mundo y ser válidos.", NamedTextColor.RED));
                return;
            }

            dungeonConfig.setMinLocation(minLoc);
            dungeonConfig.setMaxLocation(maxLoc);
            plugin.getDungeonManager().saveDungeonConfigToFile(dungeonConfig);
            player.sendMessage(Component.text("§aRegión de la dungeon establecida exitosamente.", NamedTextColor.GREEN));
            selectionManager.clearSelection(player); // Clear selection after applying
            selectionManager.stopSelection(player); // Stop selection mode after applying
            initializeItems(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Region set for dungeon " + dungeonConfig.getName() + " to " + minLoc + " - " + maxLoc);
            }
        } else if (slot == 28 && type == Material.ITEM_FRAME) { 
            closingForSubGUI = true;
            player.closeInventory();
            // Abrir ObjectiveListGUI en contexto de dungeon
            new ObjectiveListGUI(player, dungeonConfig, this).open(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Player " + player.getName() + " opened ObjectiveListGUI for dungeon " + dungeonConfig.getName());
            }
        } else if (slot == 30 && (type == Material.LIME_WOOL || type == Material.RED_WOOL)) { 
            dungeonConfig.setActive(!dungeonConfig.isActive());
            plugin.getDungeonManager().saveDungeonConfigToFile(dungeonConfig);
            player.sendMessage(Component.text("§aEstado de la dungeon '" + dungeonConfig.getName() + "' cambiado a: " + (dungeonConfig.isActive() ? "Activa" : "Inactiva"), NamedTextColor.GREEN));
            initializeItems(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Dungeon " + dungeonConfig.getName() + " active status changed to " + dungeonConfig.isActive());
            }
        } else if (slot == 49 && type == Material.EMERALD_BLOCK) { 
            plugin.getDungeonManager().addDungeonConfig(dungeonConfig.getId(), dungeonConfig); 
            player.sendMessage(Component.text("§aDungeon '" + dungeonConfig.getName() + "' actualizada exitosamente!", NamedTextColor.GREEN));
            player.closeInventory();
            parentGUI.returnFromSubGUI(); 
            unregisterListener();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Dungeon " + dungeonConfig.getId() + " saved and returning to parent GUI.");
            }
        } else if (slot == 53 && type == Material.RED_BED) { 
            player.closeInventory();
            parentGUI.returnFromSubGUI(); 
            unregisterListener();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Back button clicked. Returning to parent GUI.");
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!waitingForChatInput.containsKey(event.getPlayer().getUniqueId())) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        String message = event.getMessage().trim();
        DungeonEditGUI guiInstance = waitingForChatInput.get(player.getUniqueId());

        if (message.equalsIgnoreCase("cancelar") || message.equalsIgnoreCase("abortar")) {
            player.sendMessage(Component.text("§cOperación cancelada.", NamedTextColor.RED));
            waitingForChatInput.remove(player.getUniqueId());
            guiInstance.currentInputType = null;
            Bukkit.getScheduler().runTask(plugin, guiInstance::open);
            return;
        }

        boolean inputAccepted = false;

        if (guiInstance.currentInputType == InputType.NAME) {
            if (ALPHANUMERIC_PATTERN.matcher(message).matches()) {
                boolean nameExists = plugin.getDungeonManager().getAllDungeonConfigs().values().stream()
                    .filter(d -> !d.getId().equals(guiInstance.dungeonConfig.getId()))
                    .anyMatch(d -> d.getName().equalsIgnoreCase(message));

                if (nameExists) {
                    player.sendMessage(Component.text("§cError: Ya existe una dungeon con ese nombre. Por favor, elige otro.", NamedTextColor.RED));
                } else {
                    guiInstance.dungeonConfig.setName(message);
                    player.sendMessage(Component.text("§aNombre de la dungeon establecido a: §f" + message, NamedTextColor.GREEN));
                    inputAccepted = true;
                }
            } else {
                player.sendMessage(Component.text("§cError: El nombre solo puede contener letras, números y espacios. Intenta de nuevo o escribe 'cancelar'.", NamedTextColor.RED));
            }
        } else if (guiInstance.currentInputType == InputType.SCHEMATIC_NAME) {
            if (ALPHANUMERIC_PATTERN.matcher(message).matches()) {
                guiInstance.dungeonConfig.setSchematicName(message);
                player.sendMessage(Component.text("§aNombre del esquema establecido a: §f" + message, NamedTextColor.GREEN));
                inputAccepted = true;
            } else {
                player.sendMessage(Component.text("§cError: El nombre del esquema solo puede contener letras, números y espacios. Intenta de nuevo o escribe 'cancelar'.", NamedTextColor.RED));
            }
        }

        if (inputAccepted) {
            waitingForChatInput.remove(player.getUniqueId());
            guiInstance.currentInputType = null;
        }
        Bukkit.getScheduler().runTask(plugin, guiInstance::open);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        if (closingForChatInput || closingForSubGUI) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonEditGUI] Inventory closed for " + player.getName() + " (intentional for chat/sub-GUI). Listener remains registered.");
            }
            closingForChatInput = false;
            closingForSubGUI = false;
            return;
        }

        if (waitingForChatInput.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("§cEdición de dungeon cancelada debido al cierre del inventario.",
                    NamedTextColor.RED));
            waitingForChatInput.remove(player.getUniqueId());
            currentInputType = null;
        }
        unregisterListener();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonEditGUI] Inventory closed for " + player.getName() + ". Listener unregistered.");
        }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonEditGUI] Listener for " + player.getName() + " fully unregistered.");
        }
    }
}
