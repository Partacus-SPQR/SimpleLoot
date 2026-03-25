package com.simpleloot;

//? if <26.1 {
/*import com.simpleloot.config.ModConfigScreen;*/
//?}
import com.simpleloot.config.SimpleLootConfig;
import com.simpleloot.config.SimpleLootConfigScreen;
import com.simpleloot.loot.HoverLootHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;*/
//?}
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main client-side entry point for the SimpleLoot mod.
 * 
 * This mod implements a "hover loot" mechanic similar to Rust, allowing players
 * to quickly transfer items from containers by holding a key and swiping their
 * mouse over items.
 */
public class SimpleLootClient implements ClientModInitializer {
    public static final String MOD_ID = "simpleloot";
    public static final Logger LOGGER = LoggerFactory.getLogger("SimpleLoot");

    // Keybinding category
    private static final KeyMapping.Category KEYBIND_CATEGORY = new KeyMapping.Category(
            //? if >=1.21.11 {
            Identifier.fromNamespaceAndPath(MOD_ID, "category")
            //?} else {
            /*ResourceLocation.fromNamespaceAndPath(MOD_ID, "category")*/
            //?}
    );

    // Keybindings - all unbound by default to prevent conflicts
    public static KeyMapping hoverLootKeyBinding;  // Hold to hover loot
    public static KeyMapping hoverDropKeyBinding;  // Hold to hover drop (alternative to Ctrl+HoverLoot)
    public static KeyMapping toggleKeyBinding;     // Enable/disable the mod
    public static KeyMapping configKeyBinding;     // Open config screen
    public static KeyMapping reloadConfigKeyBinding; // Reload config from file

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing SimpleLoot Client");

        // Load configuration
        SimpleLootConfig.load();

        // Register keybindings with no default key assigned
        
        // Main hover loot key - hold to transfer items you hover over
        //? if >=26.1 {
        hoverLootKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //?} else {
        /*hoverLootKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(*/
        //?}
                "key.simpleloot.hover_loot",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Hover drop key - hold to drop items you hover over (alternative to Ctrl+HoverLoot)
        //? if >=26.1 {
        hoverDropKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //?} else {
        /*hoverDropKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(*/
        //?}
                "key.simpleloot.hover_drop",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Enable/Disable the mod entirely
        //? if >=26.1 {
        toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //?} else {
        /*toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(*/
        //?}
                "key.simpleloot.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Open config screen
        //? if >=26.1 {
        configKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //?} else {
        /*configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(*/
        //?}
                "key.simpleloot.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Reload config from file
        //? if >=26.1 {
        reloadConfigKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
        //?} else {
        /*reloadConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyMapping(*/
        //?}
                "key.simpleloot.reload_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Register tick event for keybinding handling
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);

        // Initialize the hover loot handler
        HoverLootHandler.init();

        LOGGER.info("SimpleLoot Client initialized successfully!");
    }
    
    /**
     * Handles all keybind processing each tick.
     */
    private void handleKeybinds(Minecraft client) {
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
        // Handle enable/disable mod keybind
        while (toggleKeyBinding.consumeClick()) {
            config.enabled = !config.enabled;
            config.save();
            LOGGER.info("SimpleLoot {}", config.enabled ? "enabled" : "disabled");
        }
        
        // Handle config keybind (only when no screen is open)
        while (configKeyBinding.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(createConfigScreen());
            }
        }
        
        // Handle reload config keybind
        while (reloadConfigKeyBinding.consumeClick()) {
            SimpleLootConfig.reload();
            LOGGER.info("SimpleLoot config reloaded from file");
            if (client.player != null) {
                //? if >=26.1 {
                client.player.sendOverlayMessage(
                    Component.literal("SimpleLoot config reloaded")
                );
                //?} else {
                /*client.player.displayClientMessage(
                    Component.literal("SimpleLoot config reloaded"),
                    true
                );*/
                //?}
            }
        }
    }
    
    /**
     * Creates the config screen, trying Cloth Config first with fallback.
     */
    private static net.minecraft.client.gui.screens.Screen createConfigScreen() {
        //? if <26.1 {
        /*try {
            return ModConfigScreen.createConfigScreen(null);
        } catch (Throwable e) {
            LOGGER.warn("Cloth Config unavailable, using fallback config screen");
        }*/
        //?}
        return new SimpleLootConfigScreen(null);
    }
    
    /**
     * Checks if the hover loot key is currently being held down.
     */
    public static boolean isHoverLootKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        
        InputConstants.Key boundKey = InputConstants.getKey(hoverLootKeyBinding.saveString());
        
        if (boundKey.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        
        long windowHandle = client.getWindow().handle();
        
        if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(windowHandle, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        
        return false;
    }
    
    /**
     * Checks if hover loot functionality is currently active.
     * Active when the hover loot key is held down.
     * 
     * @return true if hover loot is currently active
     */
    public static boolean isHoverLootActive() {
        return isHoverLootKeyHeld();
    }
    
    /**
     * Checks if the hover drop key is currently being held down.
     */
    public static boolean isHoverDropKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        
        InputConstants.Key boundKey = InputConstants.getKey(hoverDropKeyBinding.saveString());
        
        if (boundKey.equals(InputConstants.UNKNOWN)) {
            return false;
        }
        
        long windowHandle = client.getWindow().handle();
        
        if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(windowHandle, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        
        return false;
    }
    
    /**
     * Creates a Minecraft Identifier for this mod.
     */
    //? if >=1.21.11 {
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    //?} else {
    /*public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }*/
    //?}
}
