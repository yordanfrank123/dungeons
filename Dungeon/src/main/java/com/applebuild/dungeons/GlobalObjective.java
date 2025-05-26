package com.applebuild.dungeons;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

public abstract class GlobalObjective {

    // Definición del enum ObjectiveType como public static
    public static enum ObjectiveType {
        KILL_MOBS,
        FIND_ITEM,
        REACH_LOCATION
    }

    protected String id;
    protected String name;
    protected ObjectiveType type;
    protected String description;

    public GlobalObjective(String id, String name, ObjectiveType type, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
    }

    // Constructor para cargar desde YamlConfiguration
    public GlobalObjective(YamlConfiguration config) {
        this.id = config.getString("id");
        this.name = config.getString("name");
        this.type = ObjectiveType.valueOf(config.getString("type")); // Esto ya estaba correcto aquí
        this.description = config.getString("description");
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

    public ObjectiveType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    // Método abstracto para guardar los datos específicos de cada tipo de objetivo
    public abstract void save(YamlConfiguration config);

    // Método estático para cargar un objetivo de tipo específico
    public static GlobalObjective loadObjective(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String typeString = config.getString("type");
        if (typeString == null) {
            // Manejar error o devolver null
            return null;
        }

        ObjectiveType type = ObjectiveType.valueOf(typeString);

        switch (type) {
            case KILL_MOBS:
                return new KillMobsObjective(config);
            case FIND_ITEM:
                return new FindItemObjective(config);
            case REACH_LOCATION:
                return new ReachLocationObjective(config);
            default:
                return null;
        }
    }
}
