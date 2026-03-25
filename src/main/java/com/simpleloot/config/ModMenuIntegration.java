package com.simpleloot.config;

import com.simpleloot.SimpleLootClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenuIntegration implements ModMenuApi {
    
    //? if <26.1 {
    /*private static Boolean clothConfigCompatible = null;*/
    //?}
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createConfigScreen;
    }
    
    private Screen createConfigScreen(Screen parent) {
        //? if <26.1 {
        /*if (clothConfigCompatible == null) {
            clothConfigCompatible = checkClothConfigCompatibility();
        }
        
        if (clothConfigCompatible) {
            try {
                SimpleLootClient.LOGGER.debug("Using Cloth Config for config screen");
                return ModConfigScreen.createConfigScreen(parent);
            } catch (Throwable e) {
                clothConfigCompatible = false;
                SimpleLootClient.LOGGER.warn("Cloth Config failed at runtime, switching to fallback: {}", e.getMessage());
            }
        }
        
        SimpleLootClient.LOGGER.info("Using fallback config screen (Cloth Config unavailable or incompatible)");*/
        //?}
        return new SimpleLootConfigScreen(parent);
    }
    
    //? if <26.1 {
    /*private boolean checkClothConfigCompatibility() {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            SimpleLootClient.LOGGER.debug("Cloth Config found, assuming compatible");
            return true;
        } catch (ClassNotFoundException e) {
            SimpleLootClient.LOGGER.debug("Cloth Config not found: {}", e.getMessage());
            return false;
        } catch (Throwable e) {
            SimpleLootClient.LOGGER.warn("Error checking Cloth Config compatibility: {}", e.getMessage());
            return false;
        }
    }*/
    //?}
}

