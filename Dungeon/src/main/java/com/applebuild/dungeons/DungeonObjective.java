package com.applebuild.dungeons;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import java.util.Map;

/**
 * Clase abstracta base para todos los objetivos de una dungeon.
 * Implementa ConfigurationSerializable para poder ser guardada en archivos YAML.
 */
public abstract class DungeonObjective implements ConfigurationSerializable {

    protected String type; // Tipo de objetivo (ej. "KILL_MOBS", "FIND_ITEM")
    protected String description; // Descripción legible del objetivo

    public DungeonObjective(String type, String description) {
        this.type = type;
        this.description = description;
    }

    // Constructor para deserialización
    public DungeonObjective(Map<String, Object> map) {
        this.type = (String) map.get("type");
        this.description = (String) map.get("description");
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    // Método abstracto que las subclases deben implementar para la serialización
    @Override
    public abstract Map<String, Object> serialize();
}
