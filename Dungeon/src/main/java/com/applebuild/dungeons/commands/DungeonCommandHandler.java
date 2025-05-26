package com.applebuild.dungeons.commands;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.DungeonConfig;
import com.applebuild.dungeons.DungeonInstanceManager;
import com.applebuild.dungeons.DungeonManager;
import com.applebuild.dungeons.gui.DungeonCreationGUI;
import com.applebuild.dungeons.gui.DungeonManagementGUI;
import com.applebuild.dungeons.gui.ObjectiveListGUI;
import com.applebuild.dungeons.selection.PlayerSelectionManager; // ¡IMPORTANTE! Añadir esta línea
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender; // ¡IMPORTANTE! Añadir esta línea
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class DungeonCommandHandler implements CommandExecutor, TabCompleter {

    private final CustomDungeons plugin;
    private final DungeonManager dungeonManager;
    private final PlayerSelectionManager playerSelectionManager;
    private final DungeonInstanceManager dungeonInstanceManager;

    public DungeonCommandHandler(CustomDungeons plugin, DungeonManager dungeonManager, PlayerSelectionManager playerSelectionManager, DungeonInstanceManager dungeonInstanceManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.playerSelectionManager = playerSelectionManager;
        this.dungeonInstanceManager = dungeonInstanceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            new DungeonManagementGUI(player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                player.sendMessage(Component.text("§a--- Lista de Dungeons ---", NamedTextColor.GREEN));
                if (dungeonManager.getAllDungeonConfigs().isEmpty()) {
                    player.sendMessage(Component.text("§7No hay dungeons creadas.", NamedTextColor.GRAY));
                } else {
                    dungeonManager.getAllDungeonConfigs().values().stream()
                            .sorted(Comparator.comparing(DungeonConfig::getName))
                            .forEach(dungeon ->
                                    player.sendMessage(Component.text("§b- " + dungeon.getName() + " §7(ID: " + dungeon.getId() + ", Activa: " + dungeon.isActive() + ")", NamedTextColor.AQUA))
                            );
                }
                return true;

            case "create":
                // No se crea directamente desde el comando, se abre la GUI de creación
                new DungeonCreationGUI(player, new DungeonManagementGUI(player)).open(); // Abre la GUI de creación
                return true;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon delete <nombreDungeon>", NamedTextColor.RED));
                    return true;
                }
                String nameToDelete = args[1];
                DungeonConfig dungeonToDelete = dungeonManager.getDungeonConfigByName(nameToDelete);

                if (dungeonToDelete == null) {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameToDelete + "' no existe.", NamedTextColor.RED));
                    return true;
                }

                if (dungeonManager.deleteDungeonConfig(dungeonToDelete.getId())) {
                    player.sendMessage(Component.text("§aDungeon '" + nameToDelete + "' eliminada exitosamente.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("§cError al eliminar la dungeon '" + nameToDelete + "'.", NamedTextColor.RED));
                }
                return true;

            case "edit":
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon edit <nombreDungeon>", NamedTextColor.RED));
                    return true;
                }
                String nameToEdit = args[1];
                DungeonConfig dungeonToEdit = dungeonManager.getDungeonConfigByName(nameToEdit);

                if (dungeonToEdit == null) {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameToEdit + "' no existe.", NamedTextColor.RED));
                    return true;
                }
                // Redirigir a la GUI de gestión para editarla
                player.sendMessage(Component.text("§aAbriendo GUI de gestión de dungeons. Selecciona '" + nameToEdit + "' para editar.", NamedTextColor.GREEN));
                new DungeonManagementGUI(player).open();
                return true;

            case "setspawn":
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon setspawn <nombreDungeon>", NamedTextColor.RED));
                    return true;
                }
                String nameForSpawn = args[1];
                DungeonConfig dungeonForSpawn = dungeonManager.getDungeonConfigByName(nameForSpawn);

                if (dungeonForSpawn == null) {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameForSpawn + "' no existe.", NamedTextColor.RED));
                    return true;
                }
                dungeonForSpawn.setSpawnLocation(player.getLocation());
                dungeonManager.saveDungeonConfigToFile(dungeonForSpawn);
                player.sendMessage(Component.text("§aSpawn de la dungeon '" + nameForSpawn + "' establecido a tu ubicación actual.", NamedTextColor.GREEN));
                return true;

            case "setregion":
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon setregion <nombreDungeon>", NamedTextColor.RED));
                    player.sendMessage(Component.text("§7Debes seleccionar 2 puntos con la herramienta de selección primero.", NamedTextColor.GRAY));
                    return true;
                }
                String nameForRegion = args[1];
                DungeonConfig dungeonForRegion = dungeonManager.getDungeonConfigByName(nameForRegion);

                if (dungeonForRegion == null) {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameForRegion + "' no existe.", NamedTextColor.RED));
                    return true;
                }

                if (!playerSelectionManager.hasBothPointsSelected(player.getUniqueId())) {
                    player.sendMessage(Component.text("§cNecesitas seleccionar ambos puntos de la región primero.", NamedTextColor.RED));
                    return true;
                }
                
                Location minLoc = playerSelectionManager.getMinPoint(player.getUniqueId());
                Location maxLoc = playerSelectionManager.getMaxPoint(player.getUniqueId());

                if (minLoc == null || maxLoc == null || !minLoc.getWorld().equals(maxLoc.getWorld())) {
                    player.sendMessage(Component.text("§cLos puntos de la región deben estar en el mismo mundo y ser válidos.", NamedTextColor.RED));
                    return true;
                }

                dungeonForRegion.setMinLocation(minLoc);
                dungeonForRegion.setMaxLocation(maxLoc);
                dungeonManager.saveDungeonConfigToFile(dungeonForRegion);
                player.sendMessage(Component.text("§aRegión de la dungeon '" + nameForRegion + "' establecida exitosamente.", NamedTextColor.GREEN));
                return true;

            case "tp":
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon tp <nombreDungeon>", NamedTextColor.RED));
                    return true;
                }
                String nameToTp = args[1];
                DungeonConfig dungeonToTp = dungeonManager.getDungeonConfigByName(nameToTp);

                if (dungeonToTp == null) {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameToTp + "' no existe.", NamedTextColor.RED));
                    return true;
                }
                if (dungeonToTp.getSpawnLocation() != null) {
                    player.teleport(dungeonToTp.getSpawnLocation());
                    player.sendMessage(Component.text("§aTeletransportado a la dungeon '" + nameToTp + "'.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("§cLa dungeon '" + nameToTp + "' no tiene un punto de spawn establecido.", NamedTextColor.RED));
                }
                return true;

            case "objectives":
                new ObjectiveListGUI(player).open(); // Abre la GUI de objetivos globales
                return true;
            case "select": // Added for selection tool commands
                if (args.length < 2) {
                    player.sendMessage(Component.text("§cUso: /dungeon select [start|stop|reset]", NamedTextColor.RED));
                    return true;
                }
                String selectAction = args[1].toLowerCase();
                if (selectAction.equals("start")) {
                    playerSelectionManager.startSelection(player, null); // null as no GUI is explicitly requesting it
                } else if (selectAction.equals("stop")) {
                    playerSelectionManager.stopSelection(player);
                } else if (selectAction.equals("reset")) {
                    playerSelectionManager.clearSelection(player);
                } else {
                    player.sendMessage(Component.text("§cUso: /dungeon select [start|stop|reset]", NamedTextColor.RED));
                }
                return true;

            default:
                player.sendMessage(Component.text("§cUso: /dungeon [list|create|delete|edit|setspawn|setregion|tp|objectives|select]", NamedTextColor.RED));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("list", "create", "delete", "edit", "setspawn", "setregion", "tp", "objectives", "select"), completions);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete":
                case "edit":
                case "setspawn":
                case "setregion":
                case "tp":
                    StringUtil.copyPartialMatches(args[1], dungeonManager.getAllDungeonConfigs().values().stream()
                            .map(DungeonConfig::getName)
                            .collect(Collectors.toList()), completions);
                    break;
                case "create":
                    // No hay sugerencias para el nombre de la nueva dungeon
                    break;
                case "select":
                    StringUtil.copyPartialMatches(args[1], Arrays.asList("start", "stop", "reset"), completions);
                    break;
            }
        }
        Collections.sort(completions);
        return completions;
    }
}
