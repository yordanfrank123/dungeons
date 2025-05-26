package com.applebuild.dungeons;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;

public class ReachLocationObjective extends GlobalObjective {

    private Location targetLocation;

    public ReachLocationObjective(String id, String name, String description, Location targetLocation) { // Añadido 'description'
        super(id, name, ObjectiveType.REACH_LOCATION, description);
        this.targetLocation = targetLocation;
    }

    public ReachLocationObjective(YamlConfiguration config) {
        super(config.getString("id"), config.getString("name"), GlobalObjective.ObjectiveType.valueOf(config.getString("type")), config.getString("description"));
        
        String worldName = config.getString("targetLocation.world");
        if (worldName != null && Bukkit.getWorld(worldName) != null) {
            this.targetLocation = new Location(
                Bukkit.getWorld(worldName),
                config.getDouble("targetLocation.x"),
                config.getDouble("targetLocation.y"),
                config.getDouble("targetLocation.z")
            );
        }
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
    }

    @Override
    public void save(YamlConfiguration config) {
        // SUPER.SAVE(CONFIG) ELIMINADO - Las propiedades comunes se guardan en DungeonManager
        if (targetLocation != null) {
            config.set("targetLocation.world", targetLocation.getWorld().getName());
            config.set("targetLocation.x", targetLocation.getX());
            config.set("targetLocation.y", targetLocation.getY());
            config.set("targetLocation.z", targetLocation.getZ());
        } else {
            config.set("targetLocation", null);
        }
    }
}
