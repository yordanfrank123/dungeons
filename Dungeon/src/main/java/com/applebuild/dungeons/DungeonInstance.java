package com.applebuild.dungeons;

import org.bukkit.Location;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap; // Para playersInDungeon

public class DungeonInstance {

    public enum DungeonStatus {
        ACTIVE,
        COMPLETED,
        FAILED,
        EXPIRED // Podría usarse para dungeons con tiempo límite
    }

    private final UUID instanceId;
    private final DungeonConfig dungeonConfig;
    private final Location minLocation;
    private final Location maxLocation;
    private final Set<UUID> playersInDungeon; // Jugadores actualmente en la instancia
    private final Map<String, String> instanceObjectiveStates; // ObjectiveID -> Estado (ej. "INCOMPLETO", "COMPLETADO", "PROGRESO: 3/5")
    private DungeonStatus status; // Estado de la instancia

    public DungeonInstance(DungeonConfig dungeonConfig, Location minLocation, Location maxLocation, Map<String, String> initialObjectiveStates) {
        this.instanceId = UUID.randomUUID(); // Generar un ID único para cada instancia
        this.dungeonConfig = dungeonConfig;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
        this.playersInDungeon = ConcurrentHashMap.newKeySet(); // Usar ConcurrentHashMap.newKeySet() para un Set concurrente
        this.instanceObjectiveStates = new ConcurrentHashMap<>(initialObjectiveStates); // Copia inicial de estados
        this.status = DungeonStatus.ACTIVE; // La instancia comienza activa
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public DungeonConfig getDungeonConfig() {
        return dungeonConfig;
    }

    public String getDungeonConfigName() {
        return dungeonConfig.getName();
    }

    public Location getMinLocation() {
        return minLocation;
    }

    public Location getMaxLocation() {
        return maxLocation;
    }

    public Set<UUID> getPlayersInDungeon() {
        return playersInDungeon;
    }

    public void addPlayer(UUID playerUuid) {
        this.playersInDungeon.add(playerUuid);
    }

    public void removePlayer(UUID playerUuid) {
        this.playersInDungeon.remove(playerUuid);
    }

    public Map<String, String> getInstanceObjectiveStates() {
        return instanceObjectiveStates;
    }

    public DungeonStatus getStatus() {
        return status;
    }

    public void setStatus(DungeonStatus status) {
        this.status = status;
    }
}
