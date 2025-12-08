package com.simpleloot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.simpleloot.SimpleLootClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration class for SimpleLoot mod.
 * Handles saving/loading settings from disk and provides default values.
 */
public class SimpleLootConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("simpleloot.json");
    
    private static SimpleLootConfig INSTANCE;
    
    // General Settings
    public boolean enabled = true;
    public boolean debugMode = false; // Enable debug logging for troubleshooting
    public boolean hotbarProtection = true; // Protect hotbar slots from being transferred
    
    // Transfer Settings
    public int transferDelayMs = 0; // Delay between transfers in milliseconds (0 = instant)
    
    // Container Settings
    public boolean allowChests = true;
    public boolean allowDoubleChests = true;
    public boolean allowBarrels = true;
    public boolean allowShulkerBoxes = true;
    public boolean allowEnderChests = true;
    public boolean allowDispensers = true;
    public boolean allowDroppers = true;
    public boolean allowHoppers = true;
    
    /**
     * Gets the singleton config instance.
     */
    public static SimpleLootConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }
    
    /**
     * Loads the configuration from disk, or creates default if not present.
     */
    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, SimpleLootConfig.class);
                SimpleLootClient.LOGGER.info("SimpleLoot config loaded from {}", CONFIG_PATH);
            } catch (IOException e) {
                SimpleLootClient.LOGGER.error("Failed to load SimpleLoot config", e);
                INSTANCE = new SimpleLootConfig();
            }
        } else {
            INSTANCE = new SimpleLootConfig();
            INSTANCE.save();
            SimpleLootClient.LOGGER.info("SimpleLoot config created with defaults at {}", CONFIG_PATH);
        }
    }
    
    /**
     * Reloads the configuration from disk.
     */
    public static void reload() {
        load();
    }
    
    /**
     * Saves the current configuration to disk.
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            SimpleLootClient.LOGGER.debug("SimpleLoot config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            SimpleLootClient.LOGGER.error("Failed to save SimpleLoot config", e);
        }
    }
    
    /**
     * Resets all settings to their default values.
     */
    public void resetToDefaults() {
        SimpleLootConfig defaults = new SimpleLootConfig();
        this.enabled = defaults.enabled;
        this.debugMode = defaults.debugMode;
        this.hotbarProtection = defaults.hotbarProtection;
        this.transferDelayMs = defaults.transferDelayMs;
        this.allowChests = defaults.allowChests;
        this.allowDoubleChests = defaults.allowDoubleChests;
        this.allowBarrels = defaults.allowBarrels;
        this.allowShulkerBoxes = defaults.allowShulkerBoxes;
        this.allowEnderChests = defaults.allowEnderChests;
        this.allowDispensers = defaults.allowDispensers;
        this.allowDroppers = defaults.allowDroppers;
        this.allowHoppers = defaults.allowHoppers;
        save();
    }
}
