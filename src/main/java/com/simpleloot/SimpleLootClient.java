package com.simpleloot;

import com.simpleloot.config.ModConfigScreen;
import com.simpleloot.config.SimpleLootConfig;
import com.simpleloot.config.SimpleLootConfigScreen;
import com.simpleloot.loot.HoverLootHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
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
    private static final KeyBinding.Category KEYBIND_CATEGORY = new KeyBinding.Category(
            Identifier.of(MOD_ID, "category")
    );

    // Keybindings - all unbound by default to prevent conflicts
    public static KeyBinding hoverLootKeyBinding;  // Hold to hover loot
    public static KeyBinding toggleKeyBinding;     // Enable/disable the mod
    public static KeyBinding configKeyBinding;     // Open config screen
    public static KeyBinding reloadConfigKeyBinding; // Reload config from file

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing SimpleLoot Client");

        // Load configuration
        SimpleLootConfig.load();

        // Register keybindings with no default key assigned
        
        // Main hover loot key - hold to transfer items you hover over
        hoverLootKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleloot.hover_loot",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Enable/Disable the mod entirely
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleloot.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Open config screen
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleloot.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEYBIND_CATEGORY
        ));

        // Reload config from file
        reloadConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simpleloot.reload_config",
                InputUtil.Type.KEYSYM,
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
    private void handleKeybinds(MinecraftClient client) {
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
        // Handle enable/disable mod keybind
        while (toggleKeyBinding.wasPressed()) {
            config.enabled = !config.enabled;
            config.save();
            LOGGER.info("SimpleLoot {}", config.enabled ? "enabled" : "disabled");
        }
        
        // Handle config keybind (only when no screen is open)
        while (configKeyBinding.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(createConfigScreen());
            }
        }
        
        // Handle reload config keybind
        while (reloadConfigKeyBinding.wasPressed()) {
            SimpleLootConfig.reload();
            LOGGER.info("SimpleLoot config reloaded from file");
            if (client.player != null) {
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("SimpleLoot config reloaded"),
                    true
                );
            }
        }
    }
    
    /**
     * Creates the config screen, trying Cloth Config first with fallback.
     */
    private static net.minecraft.client.gui.screen.Screen createConfigScreen() {
        try {
            return ModConfigScreen.createConfigScreen(null);
        } catch (Throwable e) {
            LOGGER.warn("Cloth Config unavailable, using fallback config screen");
            return new SimpleLootConfigScreen(null);
        }
    }
    
    /**
     * Checks if the hover loot key is currently being held down.
     * 
     * @return true if the hover loot key is currently pressed
     */
    public static boolean isHoverLootKeyHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        
        // Get the bound key code from the keybinding
        InputUtil.Key boundKey = InputUtil.fromTranslationKey(hoverLootKeyBinding.getBoundKeyTranslationKey());
        
        // If the key is unbound (UNKNOWN), return false
        if (boundKey.equals(InputUtil.UNKNOWN_KEY)) {
            return false;
        }
        
        long windowHandle = client.getWindow().getHandle();
        
        // For keyboard keys, check if the key is currently pressed
        if (boundKey.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(windowHandle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        
        // For mouse buttons, check if the button is pressed
        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, boundKey.getCode()) == GLFW.GLFW_PRESS;
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
     * Creates a Minecraft Identifier for this mod.
     */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
