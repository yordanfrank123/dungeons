package com.applebuild.dungeons;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask; 
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap; 

public class DungeonInstanceManager {

    private final CustomDungeons plugin;
    private final Map<UUID, DungeonInstance> activeDungeonInstances; 
    private final DungeonManager dungeonManager;
    private BukkitTask playerCheckTask; 

    private final Map<UUID, UUID> playerCurrentDungeon; 

    public DungeonInstanceManager(CustomDungeons plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.activeDungeonInstances = new ConcurrentHashMap<>(); 
        this.playerCurrentDungeon = new ConcurrentHashMap<>(); 
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonInstanceManager] Initialized. (Region-based dungeon instances)");
        }
    }

    public void startPlayerCheckTask() {
        if (playerCheckTask != null && !playerCheckTask.isCancelled()) {
            playerCheckTask.cancel();
        }
        playerCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                checkPlayerLocation(player);
            }
        }, 0L, 20L); 
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonInstanceManager] Player location check task started.");
        }
    }

    public void stopPlayerCheckTask() {
        if (playerCheckTask != null) {
            playerCheckTask.cancel();
            playerCheckTask = null;
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonInstanceManager] Player location check task stopped.");
            }
        }
    }

    private void checkPlayerLocation(Player player) {
        UUID playerUuid = player.getUniqueId();
        DungeonInstance currentInstance = getDungeonInstanceAtLocation(player.getLocation());
        UUID playerDungeonId = playerCurrentDungeon.get(playerUuid);

        if (currentInstance != null) {
            if (playerDungeonId == null || !playerDungeonId.equals(currentInstance.getInstanceId())) {
                if (playerDungeonId != null) {
                    DungeonInstance previousInstance = activeDungeonInstances.get(playerDungeonId);
                    if (previousInstance != null) {
                        previousInstance.removePlayer(playerUuid);
                        // CAMBIO: Mensaje de salida en Action Bar
                        player.sendActionBar(Component.text("§eHas salido de la dungeon: §f" + previousInstance.getDungeonConfigName()));
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DungeonInstanceManager] Player " + player.getName() + " left dungeon " + previousInstance.getDungeonConfigName());
                        }
                    }
                }
                currentInstance.addPlayer(playerUuid);
                playerCurrentDungeon.put(playerUuid, currentInstance.getInstanceId());
                // CAMBIO: Mensaje de entrada en Action Bar
                player.sendActionBar(Component.text("§aHas entrado en la dungeon: §f" + currentInstance.getDungeonConfigName()));
                if (plugin.isDebugMode()) {
                    plugin.getLogger().info("[DungeonInstanceManager] Player " + player.getName() + " entered dungeon " + currentInstance.getDungeonConfigName());
                }
            }
        } else {
            if (playerDungeonId != null) {
                DungeonInstance previousInstance = activeDungeonInstances.get(playerDungeonId);
                if (previousInstance != null) {
                    previousInstance.removePlayer(playerUuid);
                    // CAMBIO: Mensaje de salida en Action Bar
                    player.sendActionBar(Component.text("§eHas salido de la dungeon: §f" + previousInstance.getDungeonConfigName()));
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DungeonInstanceManager] Player " + player.getName() + " left dungeon " + previousInstance.getDungeonConfigName());
                    }
                }
                playerCurrentDungeon.remove(playerUuid);
            }
        }
    }


    public DungeonInstance createDungeonInstance(DungeonConfig config, Location minLocation, Location maxLocation) {
        if (config == null) {
            plugin.getLogger().warning("[DungeonInstanceManager] Cannot create instance: DungeonConfig is null.");
            return null;
        }
        if (minLocation == null || maxLocation == null) {
            plugin.getLogger().warning("[DungeonInstanceManager] Cannot create instance for dungeon " + config.getName() + ": Region locations are null.");
            return null;
        }
        if (!minLocation.getWorld().equals(maxLocation.getWorld())) {
            plugin.getLogger().warning("[DungeonInstanceManager] Cannot create instance: Min and Max locations are in different worlds.");
            return null;
        }

        Map<String, String> initialObjectiveStates = new HashMap<>();
        for (String objId : config.getObjectiveIds()) {
            initialObjectiveStates.put(objId, "INCOMPLETO"); 
        }

        DungeonInstance newInstance = new DungeonInstance(config, minLocation, maxLocation, initialObjectiveStates);
        activeDungeonInstances.put(newInstance.getInstanceId(), newInstance);
        plugin.getLogger().info("[DungeonInstanceManager] Created new dungeon instance with ID: " + newInstance.getInstanceId() + " for config: " + config.getName() + " in region " + minLocation.toVector().toString() + " to " + maxLocation.toVector().toString());
        plugin.getLogger().info("[DungeonInstanceManager] NOTE: The dungeon structure itself is assumed to be already present in the world within this region.");
        return newInstance;
    }

    public boolean deleteDungeonInstance(UUID instanceId) {
        DungeonInstance instance = activeDungeonInstances.get(instanceId);
        if (instance == null) {
            plugin.getLogger().warning("[DungeonInstanceManager] No dungeon instance found with ID: " + instanceId);
            return false;
        }

        for (UUID playerUuid : instance.getPlayersInDungeon()) {
            playerCurrentDungeon.remove(playerUuid);
            Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null) {
                // Mensaje de eliminación de dungeon en Action Bar
                onlinePlayer.sendActionBar(Component.text("§cLa dungeon en la que estabas ha sido eliminada."));
            }
        }
        instance.getPlayersInDungeon().clear(); 

        plugin.getLogger().info("[DungeonInstanceManager] Removing dungeon instance record for ID: " + instanceId);
        plugin.getLogger().info("[DungeonInstanceManager] NOTE: The physical blocks of the dungeon in the world are NOT removed by the plugin. Please clean the area manually (Region: " + instance.getMinLocation().toVector().toString() + " to " + instance.getMaxLocation().toVector().toString() + ").");

        activeDungeonInstances.remove(instanceId);
        plugin.getLogger().info("[DungeonInstanceManager] Dungeon instance " + instanceId + " removed from registry.");
        return true;
    }


    public DungeonInstance getDungeonInstance(UUID instanceId) {
        return activeDungeonInstances.get(instanceId);
    }

    public DungeonInstance getDungeonInstanceAtLocation(Location location) {
        for (DungeonInstance instance : activeDungeonInstances.values()) {
            if (!instance.getMinLocation().getWorld().equals(location.getWorld())) {
                continue;
            }

            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();

            double minX = Math.min(instance.getMinLocation().getX(), instance.getMaxLocation().getX());
            double minY = Math.min(instance.getMinLocation().getY(), instance.getMaxLocation().getY());
            double minZ = Math.min(instance.getMinLocation().getZ(), instance.getMaxLocation().getZ());

            double maxX = Math.max(instance.getMinLocation().getX(), instance.getMaxLocation().getX());
            double maxY = Math.max(instance.getMinLocation().getY(), instance.getMaxLocation().getY());
            double maxZ = Math.max(instance.getMinLocation().getZ(), instance.getMaxLocation().getZ()); 

            if (x >= minX && x <= maxX &&
                y >= minY && y <= maxY &&
                z >= minZ && z <= maxZ) {
                return instance;
            }
        }
        return null;
    }

    public Map<UUID, DungeonInstance> getAllDungeonInstances() {
        return new HashMap<>(activeDungeonInstances); 
    }

    public boolean updateObjectiveState(UUID instanceId, String objectiveId, String newState) {
        DungeonInstance instance = activeDungeonInstances.get(instanceId);
        if (instance == null) {
            plugin.getLogger().warning("[DungeonInstanceManager] Attempted to update objective for non-existent instance: " + instanceId);
            return false;
        }
        if (!instance.getInstanceObjectiveStates().containsKey(objectiveId)) {
            plugin.getLogger().warning("[DungeonInstanceManager] Attempted to update non-existent objective " + objectiveId + " for instance: " + instanceId);
            return false;
        }

        instance.getInstanceObjectiveStates().put(objectiveId, newState);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonInstanceManager] Updated objective " + objectiveId + " in instance " + instanceId + " to state: " + newState);
        }
        return true;
    }

    public String getObjectiveState(UUID instanceId, String objectiveId) {
        DungeonInstance instance = activeDungeonInstances.get(instanceId);
        if (instance == null) {
            return null;
        }
        return instance.getInstanceObjectiveStates().get(objectiveId);
    }
}

