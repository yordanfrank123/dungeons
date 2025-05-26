package com.applebuild.dungeons.listeners;

import com.applebuild.dungeons.CustomDungeons;
import com.applebuild.dungeons.DungeonConfig;
import com.applebuild.dungeons.DungeonInstance;
import com.applebuild.dungeons.DungeonInstanceManager;
import com.applebuild.dungeons.FindItemObjective; 
import com.applebuild.dungeons.GlobalObjective;
import com.applebuild.dungeons.KillMobsObjective;
import com.applebuild.dungeons.ReachLocationObjective; 
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block; // Importar la clase Block
import org.bukkit.Bukkit; // Importar la clase Bukkit

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectiveListener implements Listener {

    private final CustomDungeons plugin;
    private final DungeonInstanceManager dungeonInstanceManager;

    private final Map<UUID, Map<UUID, Map<String, Integer>>> killObjectiveProgress;

    public ObjectiveListener(CustomDungeons plugin, DungeonInstanceManager dungeonInstanceManager) {
        this.plugin = plugin;
        this.dungeonInstanceManager = dungeonInstanceManager;
        this.killObjectiveProgress = new ConcurrentHashMap<>(); 
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[ObjectiveListener] Initialized and registered.");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller(); 

        if (killer == null) return; 

        UUID playerUuid = killer.getUniqueId();
        DungeonInstance dungeonInstance = dungeonInstanceManager.getDungeonInstanceAtLocation(killer.getLocation());

        if (dungeonInstance == null) return; 

        DungeonConfig config = dungeonInstance.getDungeonConfig();
        if (config == null) return;

        for (String objectiveId : config.getObjectiveIds()) {
            GlobalObjective globalObjective = plugin.getDungeonManager().getGlobalObjective(objectiveId);

            if (globalObjective instanceof KillMobsObjective kmo) {
                String currentInstanceState = dungeonInstance.getInstanceObjectiveStates().get(objectiveId);
                if (currentInstanceState != null && currentInstanceState.startsWith("COMPLETADO")) {
                    continue; 
                }

                if (entity.getType() == kmo.getMobType()) {
                    killObjectiveProgress.computeIfAbsent(dungeonInstance.getInstanceId(), k -> new ConcurrentHashMap<>())
                                         .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                                         .computeIfAbsent(objectiveId, k -> 0);

                    int currentCount = killObjectiveProgress.get(dungeonInstance.getInstanceId()).get(playerUuid).get(objectiveId);
                    currentCount++;
                    killObjectiveProgress.get(dungeonInstance.getInstanceId()).get(playerUuid).put(objectiveId, currentCount);

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[ObjectiveListener] Player " + killer.getName() + " killed " + entity.getType().name() + " in dungeon " + dungeonInstance.getDungeonConfigName() + ". Progress for objective " + kmo.getName() + ": " + currentCount + "/" + kmo.getQuantity());
                    }

                    if (currentCount >= kmo.getQuantity()) {
                        dungeonInstanceManager.updateObjectiveState(dungeonInstance.getInstanceId(), objectiveId, "COMPLETADO");
                        killer.sendMessage(Component.text("§a¡Objetivo completado: §f" + kmo.getName() + "!", NamedTextColor.GREEN));
                        checkDungeonCompletion(dungeonInstance); 
                    } else {
                        dungeonInstanceManager.updateObjectiveState(dungeonInstance.getInstanceId(), objectiveId, "PROGRESO: " + currentCount + "/" + kmo.getQuantity());
                        killer.sendActionBar(Component.text("§bObjetivo: §f" + kmo.getName() + " §7(" + currentCount + "/" + kmo.getQuantity() + ")", NamedTextColor.AQUA));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        DungeonInstance dungeonInstance = dungeonInstanceManager.getDungeonInstanceAtLocation(to);

        if (dungeonInstance == null) return; 

        DungeonConfig config = dungeonInstance.getDungeonConfig();
        if (config == null) return;

        for (String objectiveId : config.getObjectiveIds()) {
            GlobalObjective globalObjective = plugin.getDungeonManager().getGlobalObjective(objectiveId);

            if (globalObjective instanceof ReachLocationObjective rlo) {
                String currentInstanceState = dungeonInstance.getInstanceObjectiveStates().get(objectiveId);
                if (currentInstanceState != null && currentInstanceState.startsWith("COMPLETADO")) {
                    continue; 
                }

                Location target = rlo.getTargetLocation();
                if (target.getWorld().equals(to.getWorld()) &&
                    to.distance(target) <= 1.5) { 
                    
                    dungeonInstanceManager.updateObjectiveState(dungeonInstance.getInstanceId(), objectiveId, "COMPLETADO");
                    player.sendMessage(Component.text("§a¡Objetivo completado: §f" + rlo.getName() + "!", NamedTextColor.GREEN));
                    checkDungeonCompletion(dungeonInstance); 
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[ObjectiveListener] Player " + player.getName() + " reached location objective " + rlo.getName() + " in dungeon " + dungeonInstance.getDungeonConfigName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return; 
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        DungeonInstance dungeonInstance = dungeonInstanceManager.getDungeonInstanceAtLocation(player.getLocation());

        if (dungeonInstance == null) return; 

        DungeonConfig config = dungeonInstance.getDungeonConfig();
        if (config == null) return;

        for (String objectiveId : config.getObjectiveIds()) {
            GlobalObjective globalObjective = plugin.getDungeonManager().getGlobalObjective(objectiveId);

            if (globalObjective instanceof FindItemObjective fio) {
                String currentInstanceState = dungeonInstance.getInstanceObjectiveStates().get(objectiveId);
                if (currentInstanceState != null && currentInstanceState.startsWith("COMPLETADO")) {
                    continue; 
                }

                if (clickedBlock.getType() == fio.getItemType()) {
                    dungeonInstanceManager.updateObjectiveState(dungeonInstance.getInstanceId(), objectiveId, "COMPLETADO");
                    player.sendMessage(Component.text("§a¡Objetivo completado: §f" + fio.getName() + "!", NamedTextColor.GREEN));
                    checkDungeonCompletion(dungeonInstance); 
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[ObjectiveListener] Player " + player.getName() + " found item objective " + fio.getName() + " by interacting with " + clickedBlock.getType().name() + " in dungeon " + dungeonInstance.getDungeonConfigName());
                    }
                }
            }
        }
    }

    private void checkDungeonCompletion(DungeonInstance instance) {
        boolean allObjectivesCompleted = true;
        for (String objectiveId : instance.getDungeonConfig().getObjectiveIds()) {
            String state = instance.getInstanceObjectiveStates().get(objectiveId);
            if (state == null || !state.startsWith("COMPLETADO")) {
                allObjectivesCompleted = false;
                break;
            }
        }

        if (allObjectivesCompleted && instance.getStatus() != DungeonInstance.DungeonStatus.COMPLETED) {
            instance.setStatus(DungeonInstance.DungeonStatus.COMPLETED);
            for (UUID playerUuid : instance.getPlayersInDungeon()) {
                Player onlinePlayer = Bukkit.getPlayer(playerUuid); // Ahora Bukkit debería ser reconocido
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(Component.text("§a¡Felicidades! Has completado la dungeon: §f" + instance.getDungeonConfigName() + "!", NamedTextColor.GOLD));
                }
            }
            plugin.getLogger().info("[ObjectiveListener] Dungeon instance " + instance.getInstanceId() + " (" + instance.getDungeonConfigName() + ") has been COMPLETED.");
        }
    }
}
