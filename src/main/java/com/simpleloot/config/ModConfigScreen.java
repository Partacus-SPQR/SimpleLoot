package com.simpleloot.config;

import com.simpleloot.SimpleLootClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Configuration screen for SimpleLoot using Cloth Config API.
 */
public class ModConfigScreen {
    
    /**
     * Creates the configuration screen for SimpleLoot.
     * 
     * @param parent The parent screen to return to when closing
     * @return The configuration screen
     */
    public static Screen createConfigScreen(Screen parent) {
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.simpleloot.title"))
                .setSavingRunnable(config::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // General Settings Category
        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("config.simpleloot.category.general"));
        
        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.enabled"), config.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.simpleloot.enabled.tooltip"))
                .setSaveConsumer(value -> config.enabled = value)
                .build());
        
        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.hotbar_protection"), config.hotbarProtection)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.simpleloot.hotbar_protection.tooltip"))
                .setSaveConsumer(value -> config.hotbarProtection = value)
                .build());
        
        general.addEntry(entryBuilder
                .startIntSlider(Text.translatable("config.simpleloot.transfer_delay"), config.transferDelayMs, 0, 200)
                .setDefaultValue(0)
                .setTooltip(Text.translatable("config.simpleloot.transfer_delay.tooltip"))
                .setSaveConsumer(value -> config.transferDelayMs = value)
                .build());
        
        general.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.debug_mode"), config.debugMode)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.simpleloot.debug_mode.tooltip"))
                .setSaveConsumer(value -> config.debugMode = value)
                .build());
        
        // Container Settings Category
        ConfigCategory containers = builder.getOrCreateCategory(
                Text.translatable("config.simpleloot.category.containers"));
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_chests"), config.allowChests)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowChests = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_double_chests"), config.allowDoubleChests)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowDoubleChests = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_barrels"), config.allowBarrels)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowBarrels = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_shulkers"), config.allowShulkerBoxes)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowShulkerBoxes = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_ender_chests"), config.allowEnderChests)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowEnderChests = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_dispensers"), config.allowDispensers)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowDispensers = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_droppers"), config.allowDroppers)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowDroppers = value)
                .build());
        
        containers.addEntry(entryBuilder
                .startBooleanToggle(Text.translatable("config.simpleloot.allow_hoppers"), config.allowHoppers)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.allowHoppers = value)
                .build());
        
        // Keybindings Category
        ConfigCategory keybinds = builder.getOrCreateCategory(
                Text.translatable("config.simpleloot.category.keybinds"));
        
        // Add instruction text for keybinds
        keybinds.addEntry(entryBuilder.startTextDescription(
                Text.literal("Note: ").formatted(Formatting.GOLD)
                        .append(Text.literal("Keybinds set here are also accessible in Options > Controls > Key Binds.").formatted(Formatting.WHITE)))
                .build());
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Text.translatable("key.simpleloot.hover_loot"),
                SimpleLootClient.hoverLootKeyBinding)
                .build());
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Text.translatable("key.simpleloot.toggle"),
                SimpleLootClient.toggleKeyBinding)
                .build());
        
        keybinds.addEntry(entryBuilder.fillKeybindingField(
                Text.translatable("key.simpleloot.config"),
                SimpleLootClient.configKeyBinding)
                .build());
        
        return builder.build();
    }
}
