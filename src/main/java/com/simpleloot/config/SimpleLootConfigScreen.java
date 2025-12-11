package com.simpleloot.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-style fallback config screen for SimpleLoot.
 * Used when Cloth Config is unavailable or incompatible.
 * Each button contains both the label and the current value, like vanilla Minecraft settings.
 * Features hover tooltips for each option.
 */
public class SimpleLootConfigScreen extends Screen {
    private final Screen parent;
    private final SimpleLootConfig config;
    
    // Tooltip system
    private record TooltipEntry(int x, int y, int width, int height, String tooltip) {}
    private final List<TooltipEntry> tooltips = new ArrayList<>();
    
    // Store current values (we modify these, then save on Done)
    private boolean enabled;
    private boolean debugMode;
    private boolean hotbarProtection;
    private int transferDelayMs;
    private boolean allowChests;
    private boolean allowDoubleChests;
    private boolean allowBarrels;
    private boolean allowShulkerBoxes;
    private boolean allowEnderChests;
    private boolean allowHoppers;
    private boolean allowDroppers;
    private boolean allowDispensers;

    // Reference to transfer delay button for updates
    private ButtonWidget transferDelayButton;

    public SimpleLootConfigScreen(Screen parent) {
        super(Text.translatable("config.simpleloot.title"));
        this.parent = parent;
        this.config = SimpleLootConfig.getInstance();
        
        // Load current values
        this.enabled = config.enabled;
        this.debugMode = config.debugMode;
        this.hotbarProtection = config.hotbarProtection;
        this.transferDelayMs = config.transferDelayMs;
        this.allowChests = config.allowChests;
        this.allowDoubleChests = config.allowDoubleChests;
        this.allowBarrels = config.allowBarrels;
        this.allowShulkerBoxes = config.allowShulkerBoxes;
        this.allowEnderChests = config.allowEnderChests;
        this.allowHoppers = config.allowHoppers;
        this.allowDroppers = config.allowDroppers;
        this.allowDispensers = config.allowDispensers;
    }

    @Override
    protected void init() {
        super.init();
        tooltips.clear();
        
        int buttonWidth = 250;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 32;
        int spacing = 22;
        int row = 0;
        
        // Enable SimpleLoot
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Enable or disable SimpleLoot completely.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.enabled", () -> enabled, v -> enabled = v);
        
        // Debug Mode
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Enable debug logging for troubleshooting issues.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.debugMode", () -> debugMode, v -> debugMode = v);
        
        // Hotbar Protection
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Prevent items in hotbar slots from being transferred.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.hotbarProtection", () -> hotbarProtection, v -> hotbarProtection = v);
        
        // Transfer Delay with --/-/+/++ buttons
        int delayRowY = startY + spacing * row++;
        int delayButtonWidth = 130;
        int pmButtonWidth = 28;
        int totalDelayWidth = pmButtonWidth * 4 + delayButtonWidth + 8;
        int delayStartX = this.width / 2 - totalDelayWidth / 2;
        
        // Tooltip for entire transfer delay row
        addTooltip(delayStartX, delayRowY, totalDelayWidth, buttonHeight, 
            "Delay between item transfers in milliseconds. 0 = instant, 30+ recommended for visual effect.");
        
        // -- button (subtract 10)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("--"), button -> {
            transferDelayMs = Math.max(0, transferDelayMs - 10);
            transferDelayButton.setMessage(getTransferDelayText());
        }).dimensions(delayStartX, delayRowY, pmButtonWidth, buttonHeight).build());
        
        // - button (subtract 1)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), button -> {
            transferDelayMs = Math.max(0, transferDelayMs - 1);
            transferDelayButton.setMessage(getTransferDelayText());
        }).dimensions(delayStartX + pmButtonWidth + 2, delayRowY, pmButtonWidth, buttonHeight).build());
        
        // Main transfer delay display button (click to reset to 0)
        transferDelayButton = ButtonWidget.builder(getTransferDelayText(), button -> {
            transferDelayMs = 0;
            button.setMessage(getTransferDelayText());
        }).dimensions(delayStartX + pmButtonWidth * 2 + 4, delayRowY, delayButtonWidth, buttonHeight).build();
        this.addDrawableChild(transferDelayButton);
        
        // + button (add 1)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> {
            transferDelayMs = Math.min(1000, transferDelayMs + 1);
            transferDelayButton.setMessage(getTransferDelayText());
        }).dimensions(delayStartX + pmButtonWidth * 2 + delayButtonWidth + 6, delayRowY, pmButtonWidth, buttonHeight).build());
        
        // ++ button (add 10)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("++"), button -> {
            transferDelayMs = Math.min(1000, transferDelayMs + 10);
            transferDelayButton.setMessage(getTransferDelayText());
        }).dimensions(delayStartX + pmButtonWidth * 3 + delayButtonWidth + 8, delayRowY, pmButtonWidth, buttonHeight).build());
        
        // Container type toggles
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from single chests.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowChests", () -> allowChests, v -> allowChests = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from double (large) chests.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowDoubleChests", () -> allowDoubleChests, v -> allowDoubleChests = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from barrels.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowBarrels", () -> allowBarrels, v -> allowBarrels = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from shulker boxes.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowShulkerBoxes", () -> allowShulkerBoxes, v -> allowShulkerBoxes = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from ender chests.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowEnderChests", () -> allowEnderChests, v -> allowEnderChests = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from hoppers.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowHoppers", () -> allowHoppers, v -> allowHoppers = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from droppers.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowDroppers", () -> allowDroppers, v -> allowDroppers = v);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from dispensers.");
        addToggleButton(centerX, startY + spacing * row++, buttonWidth, buttonHeight,
            "config.simpleloot.allowDispensers", () -> allowDispensers, v -> allowDispensers = v);
        
        // Bottom buttons row
        int bottomY = this.height - 28;
        int bottomButtonWidth = 76;
        int totalBottomWidth = bottomButtonWidth * 3 + 8;
        int bottomStartX = this.width / 2 - totalBottomWidth / 2;
        
        // Keybinds button - opens vanilla keybinds screen directly
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("controls.keybinds"), button -> {
            this.client.setScreen(new KeybindsScreen(this, this.client.options));
        }).dimensions(bottomStartX, bottomY, bottomButtonWidth, 20).build());
        
        // Done button - saves config
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            saveConfig();
            this.client.setScreen(parent);
        }).dimensions(bottomStartX + bottomButtonWidth + 4, bottomY, bottomButtonWidth, 20).build());
        
        // Cancel button - discards changes
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(bottomStartX + bottomButtonWidth * 2 + 8, bottomY, bottomButtonWidth, 20).build());
    }
    
    private void addTooltip(int x, int y, int width, int height, String tooltip) {
        tooltips.add(new TooltipEntry(x, y, width, height, tooltip));
    }
    
    private void addToggleButton(int x, int y, int width, int height, String translationKey, 
            java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
        this.addDrawableChild(ButtonWidget.builder(getBooleanText(translationKey, getter.get()), button -> {
            boolean newValue = !getter.get();
            setter.accept(newValue);
            button.setMessage(getBooleanText(translationKey, newValue));
        }).dimensions(x, y, width, height).build());
    }
    
    private Text getBooleanText(String translationKey, boolean value) {
        return Text.translatable(translationKey)
                .append(Text.literal(": "))
                .append(value ? Text.literal("ON").styled(s -> s.withColor(0x55FF55)) 
                              : Text.literal("OFF").styled(s -> s.withColor(0xFF5555)));
    }
    
    private Text getTransferDelayText() {
        return Text.translatable("config.simpleloot.transferDelayMs")
                .append(Text.literal(": "))
                .append(Text.literal(transferDelayMs + " ms").styled(s -> s.withColor(0xFFFF55)));
    }
    
    private void saveConfig() {
        config.enabled = this.enabled;
        config.debugMode = this.debugMode;
        config.hotbarProtection = this.hotbarProtection;
        config.transferDelayMs = this.transferDelayMs;
        config.allowChests = this.allowChests;
        config.allowDoubleChests = this.allowDoubleChests;
        config.allowBarrels = this.allowBarrels;
        config.allowShulkerBoxes = this.allowShulkerBoxes;
        config.allowEnderChests = this.allowEnderChests;
        config.allowHoppers = this.allowHoppers;
        config.allowDroppers = this.allowDroppers;
        config.allowDispensers = this.allowDispensers;
        config.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        
        // Render widgets
        super.render(context, mouseX, mouseY, delta);
        
        // Check for tooltip hover and draw tooltip LAST (on top of everything)
        for (TooltipEntry entry : tooltips) {
            if (mouseX >= entry.x && mouseX < entry.x + entry.width &&
                mouseY >= entry.y && mouseY < entry.y + entry.height) {
                // Draw tooltip using Minecraft's native method
                context.drawTooltip(this.textRenderer, Text.literal(entry.tooltip), mouseX, mouseY);
                break; // Only show one tooltip at a time
            }
        }
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
