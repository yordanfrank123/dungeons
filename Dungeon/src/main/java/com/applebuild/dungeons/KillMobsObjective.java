package com.applebuild.dungeons;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

public class KillMobsObjective extends GlobalObjective {

    private EntityType mobType;
    private int quantity;

    public KillMobsObjective(String id, String name, String description, EntityType mobType, int quantity) { // Añadido 'description'
        super(id, name, ObjectiveType.KILL_MOBS, description);
        this.mobType = mobType;
        this.quantity = quantity;
    }

    public KillMobsObjective(YamlConfiguration config) {
        super(config.getString("id"), config.getString("name"), GlobalObjective.ObjectiveType.valueOf(config.getString("type")), config.getString("description"));
        this.mobType = EntityType.valueOf(config.getString("mobType"));
        this.quantity = config.getInt("quantity");
    }

    public EntityType getMobType() {
        return mobType;
    }

    public void setMobType(EntityType mobType) {
        this.mobType = mobType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public void save(YamlConfiguration config) {
        // SUPER.SAVE(CONFIG) ELIMINADO - Las propiedades comunes se guardan en DungeonManager
        config.set("mobType", mobType.name());
        config.set("quantity", quantity);
    }
}
