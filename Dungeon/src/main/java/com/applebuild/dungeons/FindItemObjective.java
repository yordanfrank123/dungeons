package com.applebuild.dungeons;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Material;

public class FindItemObjective extends GlobalObjective {

    private Material itemType;
    private int quantity;

    public FindItemObjective(String id, String name, String description, Material itemType, int quantity) {
        super(id, name, ObjectiveType.FIND_ITEM, description); // Añadido 'description'
        this.itemType = itemType;
        this.quantity = quantity;
    }

    public FindItemObjective(YamlConfiguration config) {
        super(config.getString("id"), config.getString("name"), GlobalObjective.ObjectiveType.valueOf(config.getString("type")), config.getString("description"));
        this.itemType = Material.valueOf(config.getString("itemType"));
        this.quantity = config.getInt("quantity");
    }

    public Material getItemType() {
        return itemType;
    }

    public void setItemType(Material itemType) {
        this.itemType = itemType;
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
        config.set("itemType", itemType.name());
        config.set("quantity", quantity);
    }
}
