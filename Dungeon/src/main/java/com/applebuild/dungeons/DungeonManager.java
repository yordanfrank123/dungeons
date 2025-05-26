package com.applebuild.dungeons;

import com.applebuild.dungeons.selection.PlayerSelectionManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonManager {

    private final CustomDungeons plugin;
    private final Map<String, DungeonConfig> dungeonConfigs;
    private final Map<String, GlobalObjective> globalObjectives;
    private final PlayerSelectionManager playerSelectionManager;

    public DungeonManager(CustomDungeons plugin, PlayerSelectionManager playerSelectionManager) {
        this.plugin = plugin;
        this.dungeonConfigs = new HashMap<>();
        this.globalObjectives = new HashMap<>();
        this.playerSelectionManager = playerSelectionManager;
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManager] Initialized.");
        }
    }

    // Método corregido: Ahora acepta el ID y el DungeonConfig
    public void addDungeonConfig(String id, DungeonConfig config) {
        dungeonConfigs.put(id, config);
        saveDungeonConfigToFile(config);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManager] Added/Updated DungeonConfig: " + config.getName() + " (ID: " + id + ")");
        }
    }

    public DungeonConfig getDungeonConfig(String id) {
        return dungeonConfigs.get(id);
    }

    public Map<String, DungeonConfig> getAllDungeonConfigs() {
        return new HashMap<>(dungeonConfigs);
    }

    public DungeonConfig getDungeonConfigByName(String name) {
        return dungeonConfigs.values().stream()
                .filter(dungeon -> dungeon.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public boolean deleteDungeonConfig(String id) {
        DungeonConfig removed = dungeonConfigs.remove(id);
        if (removed != null) {
            File file = new File(plugin.getDataFolder() + File.separator + "Dungeons", removed.getName() + ".yml");
            if (file.exists()) {
                if (file.delete()) {
                    plugin.getLogger().info("[DungeonManager] Deleted dungeon config file: " + file.getName());
                    return true;
                } else {
                    plugin.getLogger().warning("[DungeonManager] Failed to delete dungeon config file: " + file.getName());
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void saveDungeonConfigToFile(DungeonConfig config) {
        File file = new File(plugin.getDataFolder() + File.separator + "Dungeons", config.getName() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        config.save(yaml);
        try {
            yaml.save(file);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManager] Saved DungeonConfig to file: " + file.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[DungeonManager] Could not save DungeonConfig " + config.getName() + " to file " + file.getName(), e);
        }
    }

    public void loadAllDungeonConfigsFromFiles() {
        dungeonConfigs.clear();
        File dungeonsFolder = new File(plugin.getDataFolder(), "Dungeons");
        if (!dungeonsFolder.exists()) {
            return;
        }

        File[] files = dungeonsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    DungeonConfig dungeonConfig = new DungeonConfig(config);
                    dungeonConfigs.put(dungeonConfig.getId(), dungeonConfig);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DungeonManager] Loaded DungeonConfig: " + dungeonConfig.getName() + " (ID: " + dungeonConfig.getId() + ")");
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "[DungeonManager] Could not load dungeon config from file: " + file.getName(), e);
                }
            }
        }
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManager] Loaded " + dungeonConfigs.size() + " dungeon configs.");
        }
    }

    public String generateUniqueNumericId() {
        long timestamp = System.currentTimeMillis();
        String uniqueId = String.valueOf(timestamp);
        while (dungeonConfigs.containsKey(uniqueId) || globalObjectives.containsKey(uniqueId)) {
            timestamp++;
            uniqueId = String.valueOf(timestamp);
        }
        return uniqueId;
    }

    public boolean doesDungeonNameExist(String name) {
        for (DungeonConfig config : dungeonConfigs.values()) {
            if (config.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    // CORRECCIÓN: Método addGlobalObjective ahora acepta el ID y el GlobalObjective
    public void addGlobalObjective(String id, GlobalObjective objective) {
        globalObjectives.put(id, objective); // Usar el ID pasado
        saveGlobalObjectiveToFile(objective);
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManager] Added/Updated GlobalObjective: " + objective.getName() + " (ID: " + objective.getId() + ")");
        }
    }

    public GlobalObjective getGlobalObjective(String id) {
        return globalObjectives.get(id);
    }

    public Map<String, GlobalObjective> getAllGlobalObjectives() {
        return new HashMap<>(globalObjectives);
    }

    public boolean deleteGlobalObjective(String id) {
        GlobalObjective removed = globalObjectives.remove(id);
        if (removed != null) {
            for (DungeonConfig dungeonConfig : dungeonConfigs.values()) {
                if (dungeonConfig.getObjectiveIds().contains(id)) {
                    dungeonConfig.removeObjective(id);
                    saveDungeonConfigToFile(dungeonConfig);
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("[DungeonManager] Removed objective " + id + " from DungeonConfig: " + dungeonConfig.getName());
                    }
                }
            }

            File file = new File(plugin.getDataFolder() + File.separator + "GlobalObjectives", removed.getName() + ".yml");
            if (file.exists()) {
                if (file.delete()) {
                    plugin.getLogger().info("[DungeonManager] Deleted global objective file: " + file.getName());
                    return true;
                } else {
                    plugin.getLogger().warning("[DungeonManager] Failed to delete global objective file: " + file.getName());
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void saveGlobalObjectiveToFile(GlobalObjective objective) {
        File file = new File(plugin.getDataFolder() + File.separator + "GlobalObjectives", objective.getName() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        
        yaml.set("id", objective.getId());
        yaml.set("name", objective.getName());
        yaml.set("type", objective.getType().name());
        yaml.set("description", objective.getDescription());

        objective.save(yaml);

        try {
            yaml.save(file);
            if (plugin.isDebugMode()) {
                plugin.getLogger().info("[DungeonManager] Saved GlobalObjective to file: " + file.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[DungeonManager] Could not save GlobalObjective " + objective.getName() + " to file " + file.getName(), e);
        }
    }

    public void loadAllGlobalObjectivesFromFiles() {
        globalObjectives.clear();
        File objectivesFolder = new File(plugin.getDataFolder(), "GlobalObjectives");
        if (!objectivesFolder.exists()) {
            return;
        }

        File[] files = objectivesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    GlobalObjective objective = GlobalObjective.loadObjective(file);
                    if (objective != null) {
                        globalObjectives.put(objective.getId(), objective);
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().info("[DungeonManager] Loaded GlobalObjective: " + objective.getName() + " (ID: " + objective.getId() + ")");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "[DungeonManager] Could not load global objective from file: " + file.getName(), e);
                }
            }
        }
        if (plugin.isDebugMode()) {
            plugin.getLogger().info("[DungeonManager] Loaded " + globalObjectives.size() + " global objectives.");
        }
    }

    public boolean doesObjectiveNameExist(String name) {
        for (GlobalObjective objective : globalObjectives.values()) {
            if (objective.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}


