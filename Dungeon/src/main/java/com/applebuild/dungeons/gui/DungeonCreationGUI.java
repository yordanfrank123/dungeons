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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.List;

public class DungeonCreationGUI implements Listener {

    private final Inventory inventory;
    private final Player player;
    private final CustomDungeons plugin;
    private final DungeonManagementGUI parentGUI; // Ahora siempre se pasa

    private String dungeonName;
    // private String schematicName; // REMOVED

    private static final Map<UUID, DungeonCreationGUI> waitingForChatInput = new HashMap<>();
    private InputType currentInputType = null;
    private boolean closingForChatInput = false;

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9 ]+$");

    private enum InputType {
        NAME // , SCHEMATIC_NAME // REMOVED
    }

    // CORRECCIÓN: El constructor ahora SIEMPRE requiere un parentGUI
    public DungeonCreationGUI(Player player, DungeonManagementGUI parentGUI) {
        this.player = player;
        this.plugin = CustomDungeons.getPlugin(CustomDungeons.class);
        this.parentGUI = parentGUI; // Asignar el parentGUI
        this.dungeonName = "Nueva Dungeon";
        // this.schematicName = "default"; // REMOVED

        this.inventory = Bukkit.createInventory(null, 27, Component.text("Crear Nueva Dungeon", NamedTextColor.DARK_AQUA));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonCreationGUI] New GUI instance created and listener registered for " + player.getName());
        }
    }

    private void initializeItems() {
        inventory.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", ""));
        }

        inventory.setItem(10, createGuiItem(Material.NAME_TAG, "§bNombre: §f" + dungeonName,
                "§7Clic para cambiar el nombre."));

        // inventory.setItem(12, createGuiItem(Material.PAPER, "§bEsquema: §f" + schematicName, // REMOVED
        //         "§7Clic para cambiar el nombre del esquema.", "§7(Ej: my_dungeon_layout)")); // REMOVED

        inventory.setItem(22, createGuiItem(Material.LIME_CONCRETE, "§aCrear Dungeon",
                "§7Haz clic para crear esta", "§7nueva dungeon."));

        inventory.setItem(26, createGuiItem(Material.RED_BED, "§cVolver",
                "§7Regresar al menú anterior."));
        
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonCreationGUI] Items initialized for " + player.getName());
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
            plugin.getLogger().info("[DungeonCreationGUI] GUI opened for " + player.getName());
        }
    }

    public void returnFromSubGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            initializeItems();
            player.openInventory(inventory);
        });
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonCreationGUI] Returning from sub-GUI for " + player.getName());
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
            plugin.getLogger().info("[DungeonCreationGUI] Player " + player.getName() + " clicked on " + type.name() + " at slot " + slot);
        }

        if (slot == 10 && type == Material.NAME_TAG) { 
            closingForChatInput = true;
            player.closeInventory();
            waitingForChatInput.put(player.getUniqueId(), this);
            currentInputType = InputType.NAME;
            player.sendMessage(Component.text("§bEscribe el nombre para la nueva dungeon. Escribe 'cancelar' para abortar."));
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonCreationGUI] Player " + player.getName() + " is now waiting for NAME input via chat.");
            }
        // REMOVED: else if (slot == 12 && type == Material.PAPER) block
        } else if (slot == 22 && type == Material.LIME_CONCRETE) { 
            if (dungeonName == null || dungeonName.trim().isEmpty() || dungeonName.equalsIgnoreCase("Nueva Dungeon")) {
                player.sendMessage(Component.text("§cPor favor, define un nombre válido para la dungeon antes de crearla.", NamedTextColor.RED));
                return;
            }
            if (plugin.getDungeonManager().doesDungeonNameExist(dungeonName)) {
                player.sendMessage(Component.text("§cYa existe una dungeon con este nombre. Por favor, elige uno diferente.", NamedTextColor.RED));
                return;
            }

            String newId = plugin.getDungeonManager().generateUniqueNumericId();
            DungeonConfig newDungeon = new DungeonConfig(newId, dungeonName); // schematicName parameter REMOVED
            plugin.getDungeonManager().addDungeonConfig(newId, newDungeon);

            player.sendMessage(Component.text("§aDungeon '" + dungeonName + "' creada exitosamente!", NamedTextColor.GREEN));
            player.closeInventory();
            parentGUI.returnFromSubGUI(); 
            unregisterListener();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonCreationGUI] Dungeon " + newId + " created and returning to parent GUI.");
            }
        } else if (slot == 26 && type == Material.RED_BED) { 
            player.closeInventory();
            parentGUI.returnFromSubGUI(); 
            unregisterListener();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonCreationGUI] Back button clicked. Returning to parent GUI.");
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!waitingForChatInput.containsKey(event.getPlayer().getUniqueId())) return;
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        String message = event.getMessage().trim();
        DungeonCreationGUI guiInstance = waitingForChatInput.get(player.getUniqueId());

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
                if (plugin.getDungeonManager().doesDungeonNameExist(message)) {
                    player.sendMessage(Component.text("§cError: Ya existe una dungeon con ese nombre. Por favor, elige otro.", NamedTextColor.RED));
                } else {
                    guiInstance.dungeonName = message;
                    player.sendMessage(Component.text("§aNombre de la dungeon establecido a: §f" + message, NamedTextColor.GREEN));
                    inputAccepted = true;
                }
            } else {
                player.sendMessage(Component.text("§cError: El nombre solo puede contener letras, números y espacios. Intenta de nuevo o escribe 'cancelar'.", NamedTextColor.RED));
            }
        // REMOVED: else if (guiInstance.currentInputType == InputType.SCHEMATIC_NAME) block
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

        if (closingForChatInput) {
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonCreationGUI] Inventory closed for " + player.getName() + " (intentional for chat input). Listener remains registered.");
            }
            closingForChatInput = false;
            return;
        }

        if (waitingForChatInput.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("§cCreación de dungeon cancelada debido al cierre del inventario.",
                    NamedTextColor.RED));
            waitingForChatInput.remove(player.getUniqueId());
            currentInputType = null;
        }
        unregisterListener();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonCreationGUI] Inventory closed for " + player.getName() + ". Listener unregistered.");
        }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonCreationGUI] Listener for " + player.getName() + " fully unregistered.");
        }
    }
}
