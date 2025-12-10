package com.simpleloot.config;

import com.simpleloot.SimpleLootClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * ModMenu integration for SimpleLoot.
 * Provides access to the configuration screen from ModMenu.
 * 
 * This implementation tries to use Cloth Config first for a richer experience,
 * but falls back to a custom lightweight config screen if Cloth Config is
 * unavailable or incompatible with the current Minecraft version.
 */
public class ModMenuIntegration implements ModMenuApi {
    
    // Cache the result of Cloth Config compatibility check
    private static Boolean clothConfigCompatible = null;
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen;
    }
    
    /**
     * Creates the configuration screen, trying Cloth Config first with fallback.
     */
    private Screen createConfigScreen(Screen parent) {
        // Check if Cloth Config is compatible (only check once)
        if (clothConfigCompatible == null) {
            clothConfigCompatible = checkClothConfigCompatibility();
        }
        
        if (clothConfigCompatible) {
            try {
                SimpleLootClient.LOGGER.debug("Using Cloth Config for config screen");
                return ModConfigScreen.createConfigScreen(parent);
            } catch (Throwable e) {
                // Cloth Config failed at runtime - mark as incompatible for future
                clothConfigCompatible = false;
                SimpleLootClient.LOGGER.warn("Cloth Config failed at runtime, switching to fallback: {}", e.getMessage());
            }
        }
        
        // Use fallback config screen
        SimpleLootClient.LOGGER.info("Using fallback config screen (Cloth Config unavailable or incompatible)");
        return new SimpleLootConfigScreen(parent);
    }
    
    /**
     * Checks if Cloth Config is compatible with the current Minecraft version.
     * This proactively tests if the library will work before trying to use it.
     */
    private boolean checkClothConfigCompatibility() {
        try {
            // Try to load a Cloth Config class that would fail if incompatible
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            
            // Try to detect the specific incompatibility with renderWidget being final
            // by checking if PressableWidget.renderWidget is final
            java.lang.reflect.Method renderWidget = net.minecraft.client.gui.widget.PressableWidget.class
                .getDeclaredMethod("renderWidget", net.minecraft.client.gui.DrawContext.class, int.class, int.class, float.class);
            
            if (java.lang.reflect.Modifier.isFinal(renderWidget.getModifiers())) {
                SimpleLootClient.LOGGER.warn("Detected incompatible Minecraft version: PressableWidget.renderWidget is final");
                SimpleLootClient.LOGGER.warn("Cloth Config 20.0.149 is not compatible with this Minecraft version");
                return false;
            }
            
            SimpleLootClient.LOGGER.debug("Cloth Config compatibility check passed");
            return true;
        } catch (ClassNotFoundException e) {
            SimpleLootClient.LOGGER.debug("Cloth Config not found: {}", e.getMessage());
            return false;
        } catch (NoSuchMethodException e) {
            // Method signature changed - Cloth Config might still work
            SimpleLootClient.LOGGER.debug("Could not find renderWidget method for compatibility check, assuming compatible");
            return true;
        } catch (Throwable e) {
            SimpleLootClient.LOGGER.warn("Error checking Cloth Config compatibility: {}", e.getMessage());
            return false;
        }
    }
}

