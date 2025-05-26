package com.applebuild.dungeons;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit; // ¡IMPORTANTE! Añadir esta línea

public class DungeonConfig {

    private String id;
    private String name;
    private boolean active;
    private Location spawnLocation;
    private Location minLocation;
    private Location maxLocation;
    private String schematicName;
    private List<String> objectiveIds;

    public DungeonConfig(String id, String name, String schematicName) {
        this.id = id;
        this.name = name;
        this.active = false;
        this.schematicName = schematicName;
        this.objectiveIds = new ArrayList<>();
    }

    public DungeonConfig(YamlConfiguration config) {
        this.id = config.getString("id");
        this.name = config.getString("name");
        this.active = config.getBoolean("active");
        
        String spawnWorldName = config.getString("spawnLocation.world");
        if (spawnWorldName != null && Bukkit.getWorld(spawnWorldName) != null) { // Aquí Bukkit
            this.spawnLocation = new Location(
                Bukkit.getWorld(spawnWorldName), // Aquí Bukkit
                config.getDouble("spawnLocation.x"),
                config.getDouble("spawnLocation.y"),
                config.getDouble("spawnLocation.z"),
                (float) config.getDouble("spawnLocation.yaw"),
                (float) config.getDouble("spawnLocation.pitch")
            );
        }

        String minWorldName = config.getString("minLocation.world");
        if (minWorldName != null && Bukkit.getWorld(minWorldName) != null) { // Aquí Bukkit
            this.minLocation = new Location(
                Bukkit.getWorld(minWorldName), // Aquí Bukkit
                config.getDouble("minLocation.x"),
                config.getDouble("minLocation.y"),
                config.getDouble("minLocation.z")
            );
        }

        String maxWorldName = config.getString("maxLocation.world");
        if (maxWorldName != null && Bukkit.getWorld(maxWorldName) != null) { // Aquí Bukkit
            this.maxLocation = new Location(
                Bukkit.getWorld(maxWorldName), // Aquí Bukkit
                config.getDouble("maxLocation.x"),
                config.getDouble("maxLocation.y"),
                config.getDouble("maxLocation.z")
            );
        }

        this.schematicName = config.getString("schematicName");
        this.objectiveIds = config.getStringList("objectiveIds");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getMinLocation() {
        return minLocation;
    }

    public void setMinLocation(Location minLocation) {
        this.minLocation = minLocation;
    }

    public Location getMaxLocation() {
        return maxLocation;
    }

    public void setMaxLocation(Location maxLocation) {
        this.maxLocation = maxLocation;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
    }

    public List<String> getObjectiveIds() {
        return new ArrayList<>(objectiveIds);
    }

    public void addObjective(String objectiveId) {
        if (!this.objectiveIds.contains(objectiveId)) {
            this.objectiveIds.add(objectiveId);
        }
    }

    public void removeObjective(String objectiveId) {
        this.objectiveIds.remove(objectiveId);
    }

    public void save(YamlConfiguration config) {
        config.set("id", id);
        config.set("name", name);
        config.set("active", active);

        if (spawnLocation != null) {
            config.set("spawnLocation.world", spawnLocation.getWorld().getName());
            config.set("spawnLocation.x", spawnLocation.getX());
            config.set("spawnLocation.y", spawnLocation.getY());
            config.set("spawnLocation.z", spawnLocation.getZ());
            config.set("spawnLocation.yaw", spawnLocation.getYaw());
            config.set("spawnLocation.pitch", spawnLocation.getPitch());
        } else {
            config.set("spawnLocation", null);
        }

        if (minLocation != null) {
            config.set("minLocation.world", minLocation.getWorld().getName());
            config.set("minLocation.x", minLocation.getX());
            config.set("minLocation.y", minLocation.getY());
            config.set("minLocation.z", minLocation.getZ());
        } else {
            config.set("minLocation", null);
        }

        if (maxLocation != null) {
            config.set("maxLocation.world", maxLocation.getWorld().getName());
            config.set("maxLocation.x", maxLocation.getX());
            config.set("maxLocation.y", maxLocation.getY());
            config.set("maxLocation.z", maxLocation.getZ());
        } else {
            config.set("maxLocation", null);
        }

        config.set("schematicName", schematicName);
        config.set("objectiveIds", objectiveIds);
    }
}
