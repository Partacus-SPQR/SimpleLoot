package com.simpleloot.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-style fallback config screen for SimpleLoot.
 * Used when Cloth Config is unavailable or incompatible.
 * Each button contains both the label and the current value, like vanilla Minecraft settings.
 * Features hover tooltips for each option and a proper slider for transfer delay.
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

    // Reference to transfer delay slider for reset
    private TransferDelaySlider transferDelaySlider;

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
        int resetBtnWidth = 40;
        int widgetWidth = buttonWidth - resetBtnWidth - 4;
        int centerX = this.width / 2 - buttonWidth / 2;
        int resetX = centerX + widgetWidth + 4;
        int startY = 32;
        int spacing = 22;
        int row = 0;
        
        // Enable SimpleLoot
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Enable or disable SimpleLoot completely.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.enabled", () -> enabled, v -> enabled = v, true);
        
        // Debug Mode
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Enable debug logging for troubleshooting issues.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.debugMode", () -> debugMode, v -> debugMode = v, false);
        
        // Hotbar Protection
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Prevent items in hotbar slots from being transferred.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.hotbarProtection", () -> hotbarProtection, v -> hotbarProtection = v, true);
        
        // Transfer Delay Slider
        int delayRowY = startY + spacing * row++;
        addTooltip(centerX, delayRowY, buttonWidth, buttonHeight, 
            "Delay between item transfers in milliseconds. 0 = instant, 30+ recommended for visual effect.");
        
        transferDelaySlider = new TransferDelaySlider(centerX, delayRowY, widgetWidth, buttonHeight, transferDelayMs);
        this.addDrawableChild(transferDelaySlider);
        
        // Reset button for slider (resets to 30ms default)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("↺"), button -> {
            transferDelaySlider.setValue(30);
            transferDelayMs = 30;
        }).dimensions(resetX, delayRowY, resetBtnWidth, buttonHeight).build());
        
        // Container type toggles
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from single chests.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowChests", () -> allowChests, v -> allowChests = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from double (large) chests.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowDoubleChests", () -> allowDoubleChests, v -> allowDoubleChests = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from barrels.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowBarrels", () -> allowBarrels, v -> allowBarrels = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from shulker boxes.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowShulkerBoxes", () -> allowShulkerBoxes, v -> allowShulkerBoxes = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from ender chests.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowEnderChests", () -> allowEnderChests, v -> allowEnderChests = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from hoppers.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowHoppers", () -> allowHoppers, v -> allowHoppers = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from droppers.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowDroppers", () -> allowDroppers, v -> allowDroppers = v, true);
        
        addTooltip(centerX, startY + spacing * row, buttonWidth, buttonHeight, 
            "Allow hover-looting from dispensers.");
        addToggleButtonWithReset(centerX, startY + spacing * row++, widgetWidth, resetX, resetBtnWidth, buttonHeight,
            "config.simpleloot.allowDispensers", () -> allowDispensers, v -> allowDispensers = v, true);
        
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
            transferDelayMs = transferDelaySlider.getIntValue();
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
    
    private void addToggleButtonWithReset(int x, int y, int width, int resetX, int resetWidth, int height, 
            String translationKey, java.util.function.Supplier<Boolean> getter, 
            java.util.function.Consumer<Boolean> setter, boolean defaultValue) {
        ButtonWidget toggleBtn = this.addDrawableChild(ButtonWidget.builder(getBooleanText(translationKey, getter.get()), button -> {
            boolean newValue = !getter.get();
            setter.accept(newValue);
            button.setMessage(getBooleanText(translationKey, newValue));
        }).dimensions(x, y, width, height).build());
        
        // Reset button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("↺"), button -> {
            setter.accept(defaultValue);
            toggleBtn.setMessage(getBooleanText(translationKey, defaultValue));
        }).dimensions(resetX, y, resetWidth, height).build());
    }
    
    private Text getBooleanText(String translationKey, boolean value) {
        return Text.translatable(translationKey)
                .append(Text.literal(": "))
                .append(value ? Text.literal("ON").styled(s -> s.withColor(0x55FF55)) 
                              : Text.literal("OFF").styled(s -> s.withColor(0xFF5555)));
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
    
    /**
     * Custom slider widget for transfer delay (0-500ms range).
     */
    private class TransferDelaySlider extends SliderWidget {
        private static final int MIN = 0;
        private static final int MAX = 500;
        
        public TransferDelaySlider(int x, int y, int width, int height, int initialValue) {
            super(x, y, width, height, Text.empty(), (double)(initialValue - MIN) / (MAX - MIN));
            updateMessage();
        }
        
        public int getIntValue() {
            return (int) Math.round(this.value * (MAX - MIN) + MIN);
        }
        
        public void setValue(int newValue) {
            this.value = (double)(Math.max(MIN, Math.min(MAX, newValue)) - MIN) / (MAX - MIN);
            updateMessage();
        }
        
        @Override
        protected void updateMessage() {
            int val = getIntValue();
            setMessage(Text.translatable("config.simpleloot.transferDelayMs")
                    .append(Text.literal(": "))
                    .append(Text.literal(val + " ms").styled(s -> s.withColor(0xFFFF55))));
        }
        
        @Override
        protected void applyValue() {
            transferDelayMs = getIntValue();
        }
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
