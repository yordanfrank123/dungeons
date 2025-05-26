package com.applebuild.dungeons;

import com.applebuild.dungeons.commands.DungeonCommandHandler;
import com.applebuild.dungeons.listeners.ObjectiveListener;
import com.applebuild.dungeons.selection.PlayerSelectionManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public final class CustomDungeons extends JavaPlugin {

    private DungeonManager dungeonManager;
    private DungeonInstanceManager dungeonInstanceManager;
    private PlayerSelectionManager playerSelectionManager;
    private boolean debugMode = false; // Variable para el modo de depuración

    @Override
    public void onEnable() {
        // Guardar la configuración por defecto si no existe
        saveDefaultConfig();
        // Cargar el modo de depuración desde la configuración
        this.debugMode = getConfig().getBoolean("debug-mode", false);
        if (debugMode) {
            getLogger().info("Debug mode is ENABLED.");
        }

        // Crear carpetas si no existen
        createDataFolders();

        // Inicializar PlayerSelectionManager
        playerSelectionManager = new PlayerSelectionManager(this);

        // Inicializar DungeonManager
        dungeonManager = new DungeonManager(this, playerSelectionManager);
        dungeonManager.loadAllDungeonConfigsFromFiles();
        dungeonManager.loadAllGlobalObjectivesFromFiles();

        // Inicializar DungeonInstanceManager
        dungeonInstanceManager = new DungeonInstanceManager(this, dungeonManager);

        // Registrar comandos
        // CORRECCIÓN: Añadir dungeonInstanceManager al constructor de DungeonCommandHandler
        getCommand("dungeon").setExecutor(new DungeonCommandHandler(this, dungeonManager, playerSelectionManager, dungeonInstanceManager));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(playerSelectionManager, this);
        getServer().getPluginManager().registerEvents(new ObjectiveListener(this, dungeonInstanceManager), this);

        getLogger().info("CustomDungeons ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomDungeons ha sido deshabilitado.");
    }

    private void createDataFolders() {
        File dungeonsFolder = new File(getDataFolder(), "Dungeons");
        File globalObjectivesFolder = new File(getDataFolder(), "GlobalObjectives");

        if (!dungeonsFolder.exists()) {
            if (!dungeonsFolder.mkdirs()) {
                getLogger().log(Level.SEVERE, "Could not create Dungeons folder!");
            }
        }
        if (!globalObjectivesFolder.exists()) {
            if (!globalObjectivesFolder.mkdirs()) {
                getLogger().log(Level.SEVERE, "Could not create GlobalObjectives folder!");
            }
        }
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public DungeonInstanceManager getDungeonInstanceManager() {
        return dungeonInstanceManager;
    }

    public PlayerSelectionManager getPlayerSelectionManager() {
        return playerSelectionManager;
    }

    public boolean isDebugMode() {
        return debugMode;
    }
}
