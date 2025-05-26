package com.applebuild.dungeons.gui;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.DungeonConfig;
import com.applebuild.dungeons.GlobalObjective;
import com.applebuild.dungeons.KillMobsObjective; // ¡IMPORTANTE! Añadir esta línea para instanciar un tipo concreto
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
import org.bukkit.entity.EntityType; // ¡IMPORTANTE! Añadir esta línea para EntityType

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObjectiveListGUI implements Listener {

    private final Inventory inventory;
    private final Player player;
    private final CustomDungeons plugin;
    private final DungeonConfig dungeonConfig; 
    private final Object parentGUI; 

    private static final Map<UUID, String> objectiveIdPendingDeletion = new HashMap<>();
    private static final Map<UUID, Long> deletionConfirmationTime = new HashMap<>(); 

    private boolean closingForSubGUI = false; 

    // Constructor para objetivos globales (cuando no hay dungeonConfig)
    public ObjectiveListGUI(Player player) {
        this(player, null, null);
    }

    // Constructor para objetivos de dungeon (cuando se abre desde DungeonEditGUI)
    public ObjectiveListGUI(Player player, DungeonConfig dungeonConfig, Object parentGUI) {
        this.player = player;
        this.plugin = CustomDungeons.getPlugin(CustomDungeons.class);
        this.dungeonConfig = dungeonConfig;
        this.parentGUI = parentGUI;

        String title = (dungeonConfig != null) ? "Objetivos de Dungeon: " + dungeonConfig.getName() : "Objetivos Globales";
        this.inventory = Bukkit.createInventory(null, 54, Component.text(title, NamedTextColor.DARK_GREEN));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListGUI] New GUI instance created and listener registered for " + player.getName() + " (Dungeon: " + (dungeonConfig != null ? dungeonConfig.getName() : "Global") + ")");
        }
    }

    private void initializeItems() {
        inventory.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7", ""));
        }

        List<GlobalObjective> objectivesToDisplay;
        if (dungeonConfig != null) {
            // Mostrar solo los objetivos ya adjuntos a esta dungeon
            objectivesToDisplay = dungeonConfig.getObjectiveIds().stream()
                    .map(plugin.getDungeonManager()::getGlobalObjective)
                    .filter(obj -> obj != null)
                    .sorted(Comparator.comparing(GlobalObjective::getName))
                    .collect(Collectors.toList());
        } else {
            // Mostrar todos los objetivos globales
            objectivesToDisplay = plugin.getDungeonManager().getAllGlobalObjectives().values().stream()
                    .sorted(Comparator.comparing(GlobalObjective::getName))
                    .collect(Collectors.toList());
        }

        int slot = 0;
        for (GlobalObjective objective : objectivesToDisplay) {
            if (slot >= 45) break; 

            List<String> lore = new ArrayList<>();
            lore.add("§7ID: §f" + objective.getId());
            lore.add("§7Tipo: §f" + objective.getType().name());
            lore.add("§7Descripción: §f" + objective.getDescription());
            lore.add("");
            if (dungeonConfig != null) { 
                lore.add("§eClic normal: §aDesadjuntar de esta dungeon");
                lore.add("§eClic derecho: §bAdjuntar a esta dungeon (si no está)"); 
                lore.add("§eShift + Clic izquierdo: §cEliminar globalmente (¡PELIGRO!)"); 
            } else { 
                lore.add("§eClic normal: §bVer Detalles (Edición por comando)"); 
                lore.add("§eShift + Clic izquierdo: §cEliminar globalmente (¡PELIGRO!)");
            }
            
            inventory.setItem(slot, createGuiItem(Material.PAPER, "§b" + objective.getName(), lore.toArray(new String[0])));
            slot++;
        }

        inventory.setItem(49, createGuiItem(Material.LIME_CONCRETE, "§aCrear Nuevo Objetivo Global",
                "§7Haz clic para crear un nuevo", "§7objetivo global básico (se editará por comando)."));

        inventory.setItem(53, createGuiItem(Material.RED_BED, "§cVolver",
                "§7Regresar al menú anterior."));

        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListGUI] Items initialized for " + player.getName() + ". Displaying " + objectivesToDisplay.size() + " objectives.");
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
            plugin.getLogger().info("[ObjectiveListGUI] GUI opened for " + player.getName());
        }
    }

    public void returnFromSubGUI() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            initializeItems();
            player.openInventory(inventory);
        });
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListGUI] Reopening GUI for " + player.getName());
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
            plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " clicked on " + type.name() + " at slot " + slot + " with ClickType: " + event.getClick());
        }

        if (slot == 49 && type == Material.LIME_CONCRETE) { 
            // CORRECCIÓN 1: Instanciar una clase concreta (KillMobsObjective)
            String newObjectiveId = plugin.getDungeonManager().generateUniqueNumericId();
            // Usamos KillMobsObjective como un objetivo básico por defecto
            GlobalObjective newObjective = new KillMobsObjective(newObjectiveId, "Nuevo Objetivo " + newObjectiveId.substring(0,4), "Objetivo de matar mobs básico.", EntityType.ZOMBIE, 10);
            
            // CORRECCIÓN 2: Pasar el ID junto con el objeto al método addGlobalObjective
            plugin.getDungeonManager().addGlobalObjective(newObjectiveId, newObjective);
            
            player.sendMessage(Component.text("§aNuevo objetivo global básico creado con ID: " + newObjectiveId + ". Edita sus propiedades por comando.", NamedTextColor.GREEN));
            initializeItems();
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " created a new basic global objective: " + newObjectiveId);
            }
        } else if (slot == 53 && type == Material.RED_BED) { 
            player.closeInventory();
            if (parentGUI instanceof DungeonCreationGUI) {
                ((DungeonCreationGUI) parentGUI).returnFromSubGUI(); 
            } else if (parentGUI instanceof DungeonEditGUI) {
                ((DungeonEditGUI) parentGUI).returnFromSubGUI(); 
            } else if (parentGUI instanceof MainDungeonGUI) { 
                ((MainDungeonGUI) parentGUI).open();
            } else { 
                new MainDungeonGUI(player).open(); // Fallback
            }
            unregisterListener(); 
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " returned to parent GUI.");
            }
        } else if (type == Material.PAPER) { 
            List<GlobalObjective> objectivesToDisplay;
            if (dungeonConfig != null) {
                objectivesToDisplay = dungeonConfig.getObjectiveIds().stream()
                        .map(plugin.getDungeonManager()::getGlobalObjective)
                        .filter(obj -> obj != null)
                        .sorted(Comparator.comparing(GlobalObjective::getName))
                        .collect(Collectors.toList());
            } else {
                objectivesToDisplay = plugin.getDungeonManager().getAllGlobalObjectives().values().stream()
                        .sorted(Comparator.comparing(GlobalObjective::getName))
                        .collect(Collectors.toList());
            }

            if (slot < objectivesToDisplay.size()) {
                GlobalObjective selectedObjective = objectivesToDisplay.get(slot);
                String objectiveId = selectedObjective.getId();

                if (event.getClick() == ClickType.SHIFT_LEFT) { 
                    if (objectiveIdPendingDeletion.containsKey(player.getUniqueId()) && 
                        objectiveIdPendingDeletion.get(player.getUniqueId()).equals(objectiveId) &&
                        (System.currentTimeMillis() - deletionConfirmationTime.get(player.getUniqueId()) < 5000)) { 
                        
                        for (DungeonConfig dc : plugin.getDungeonManager().getAllDungeonConfigs().values()) {
                            if (dc.getObjectiveIds().contains(objectiveId)) {
                                dc.removeObjective(objectiveId);
                                plugin.getDungeonManager().saveDungeonConfigToFile(dc);
                            }
                        }
                        
                        plugin.getDungeonManager().deleteGlobalObjective(objectiveId);
                        player.sendMessage(Component.text("§aObjetivo '" + selectedObjective.getName() + "' eliminado globalmente y desadjuntado de todas las dungeons.", NamedTextColor.GREEN));
                        objectiveIdPendingDeletion.remove(player.getUniqueId());
                        deletionConfirmationTime.remove(player.getUniqueId());
                        initializeItems(); 
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " confirmed global deletion of objective " + objectiveId);
                        }
                    } else {
                        objectiveIdPendingDeletion.put(player.getUniqueId(), objectiveId);
                        deletionConfirmationTime.put(player.getUniqueId(), System.currentTimeMillis());
                        player.sendMessage(Component.text("§c¿Estás seguro de que quieres eliminar el objetivo '" + selectedObjective.getName() + "' GLOBALMENTE? Haz clic de nuevo con SHIFT + CLIC IZQUIERDO en 5 segundos para confirmar.", NamedTextColor.RED));
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " initiated global deletion for objective " + objectiveId);
                        }
                    }
                } else if (dungeonConfig != null) { 
                    if (event.getClick() == ClickType.LEFT) { 
                        if (dungeonConfig.getObjectiveIds().contains(objectiveId)) {
                            dungeonConfig.removeObjective(objectiveId);
                            plugin.getDungeonManager().saveDungeonConfigToFile(dungeonConfig);
                            player.sendMessage(Component.text("§eObjetivo '" + selectedObjective.getName() + "' desadjuntado de esta dungeon.", NamedTextColor.YELLOW));
                            initializeItems(); 
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " unlinked objective " + objectiveId + " from dungeon " + dungeonConfig.getName());
                            }
                        } else {
                            player.sendMessage(Component.text("§cEste objetivo no está adjunto a esta dungeon.", NamedTextColor.RED));
                        }
                    } else if (event.getClick() == ClickType.RIGHT) { 
                        if (!dungeonConfig.getObjectiveIds().contains(objectiveId)) {
                            dungeonConfig.addObjective(objectiveId);
                            plugin.getDungeonManager().saveDungeonConfigToFile(dungeonConfig);
                            player.sendMessage(Component.text("§aObjetivo '" + selectedObjective.getName() + "' adjuntado a esta dungeon.", NamedTextColor.GREEN));
                            initializeItems(); 
                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " linked objective " + objectiveId + " to dungeon " + dungeonConfig.getName());
                            }
                        } else {
                            player.sendMessage(Component.text("§eEste objetivo ya está adjunto a esta dungeon.", NamedTextColor.YELLOW));
                        }
                    }
                } else { 
                    player.sendMessage(Component.text("§aDetalles de Objetivo: §b" + selectedObjective.getName() + " (" + selectedObjective.getType().name() + ")", NamedTextColor.GREEN));
                    player.sendMessage(Component.text("§7ID: " + selectedObjective.getId() + " - Descripción: " + selectedObjective.getDescription()));
                    player.sendMessage(Component.text("§ePara editar propiedades de objetivos globales, usa comandos de administración."));
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " viewed details for global objective " + objectiveId);
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
                plugin.getLogger().info("[ObjectiveListGUI] Inventory closed for " + player.getName() + " (intentional for sub-GUI). Listener remains registered.");
            }
            closingForSubGUI = false; 
            return;
        }

        if (objectiveIdPendingDeletion.containsKey(player.getUniqueId())) {
            objectiveIdPendingDeletion.remove(player.getUniqueId());
            deletionConfirmationTime.remove(player.getUniqueId());
            player.sendMessage(Component.text("§eEliminación de objetivo cancelada.", NamedTextColor.YELLOW));
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[ObjectiveListGUI] Player " + player.getName() + " closed GUI, cancelling pending objective deletion.");
            }
        }

        unregisterListener();
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListGUI] Inventory closed for " + player.getName() + ". Listener unregistered.");
        }
    }

    private void unregisterListener() {
        HandlerList.unregisterAll(this);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListGUI] Listener for " + player.getName() + " fully unregistered.");
        }
    }
}


