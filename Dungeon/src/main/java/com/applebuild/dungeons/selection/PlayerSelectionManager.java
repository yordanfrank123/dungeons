package com.applebuild.dungeons.selection;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.gui.DungeonCreationGUI;
import com.applebuild.dungeons.gui.DungeonEditGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerSelectionManager implements Listener {

    private final CustomDungeons plugin;
    private final Map<UUID, Location> playerPoint1 = new HashMap<>();
    private final Map<UUID, Location> playerPoint2 = new HashMap<>();

    // Keep track of which GUI (if any) opened the selection tool
    private final Map<UUID, Object> activeSelectionGUI = new HashMap<>(); 

    public PlayerSelectionManager(CustomDungeons plugin) {
        this.plugin = plugin;
    }

    public void startSelection(Player player, Object guiInstance) {
        activeSelectionGUI.put(player.getUniqueId(), guiInstance);
        player.sendMessage(Component.text("§aModo de selección activado."));
        player.sendMessage(Component.text("§7Haz clic izquierdo para seleccionar el primer punto y clic derecho para el segundo."));
        player.sendMessage(Component.text("§7Usa /dungeon setregion <nombre> o vuelve a la GUI para aplicar la selección."));
        player.sendMessage(Component.text("§7Usa /dungeon select reset para limpiar tu selección."));
    }

    public void stopSelection(Player player) {
        activeSelectionGUI.remove(player.getUniqueId());
        player.sendMessage(Component.text("§aModo de selección desactivado.", NamedTextColor.GREEN));
    }

    public void clearSelection(Player player) {
        playerPoint1.remove(player.getUniqueId());
        playerPoint2.remove(player.getUniqueId());
        player.sendMessage(Component.text("§aTu selección ha sido limpiada.", NamedTextColor.GREEN));
    }

    public boolean hasBothPointsSelected(UUID playerId) {
        return playerPoint1.containsKey(playerId) && playerPoint2.containsKey(playerId);
    }

    public Location getMinPoint(UUID playerId) {
        Location p1 = playerPoint1.get(playerId);
        Location p2 = playerPoint2.get(playerId);

        if (p1 == null || p2 == null) return null;

        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());

        return new Location(p1.getWorld(), minX, minY, minZ);
    }

    public Location getMaxPoint(UUID playerId) {
        Location p1 = playerPoint1.get(playerId);
        Location p2 = playerPoint2.get(playerId);

        if (p1 == null || p2 == null) return null;

        double maxX = Math.max(p1.getX(), p2.getX());
        double maxY = Math.max(p1.getY(), p2.getY());
        double maxZ = Math.max(p1.getZ(), p2.getZ());

        return new Location(p1.getWorld(), maxX, maxY, maxZ);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the player is in selection mode (i.e., holding the selection tool)
        // You might define a specific "selection tool" item, e.g., a wooden axe
        if (item != null && item.getType() == Material.WOODEN_AXE && event.getHand() == EquipmentSlot.HAND) { // Ensure it's the main hand
            event.setCancelled(true); // Cancel block interaction

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                playerPoint1.put(player.getUniqueId(), clickedBlock.getLocation());
                player.sendMessage(Component.text("§aPunto 1 seleccionado en: §f" + formatLocation(clickedBlock.getLocation()), NamedTextColor.GREEN));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                playerPoint2.put(player.getUniqueId(), clickedBlock.getLocation());
                player.sendMessage(Component.text("§aPunto 2 seleccionado en: §f" + formatLocation(clickedBlock.getLocation()), NamedTextColor.GREEN));
            }

            // After selection, if a GUI was waiting, try to re-open it
            Object gui = activeSelectionGUI.get(player.getUniqueId());
            if (gui != null) {
                // CORRECCIÓN: Usar returnFromSubGUI() que es el método estándar.
                // Estos casts necesitan que DungeonCreationGUI y DungeonEditGUI tengan ese método.
                // Si la GUI original no tiene un método para "volver de la selección", simplemente abrimos.
                if (gui instanceof DungeonCreationGUI) {
                    ((DungeonCreationGUI) gui).returnFromSubGUI(); 
                } else if (gui instanceof DungeonEditGUI) {
                    ((DungeonEditGUI) gui).returnFromSubGUI();
                }
                // Stop selection mode after points are selected, or if the GUI re-opens.
                // It might be better to only stop selection when the GUI explicitely asks for it,
                // or if the player uses a command. For now, we keep it active until player leaves GUI/uses command.
            }
        }
    }

    private String formatLocation(Location loc) {
        return String.format("Mundo: %s, X: %d, Y: %d, Z: %d",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
