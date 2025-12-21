package com.simpleloot.config;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-style fallback config screen for SimpleLoot.
 * Used when Cloth Config is unavailable or incompatible.
 * Features: sliders, tooltips, reset buttons, and scrollable content with interactive scrollbar.
 */
public class SimpleLootConfigScreen extends Screen {
    private final Screen parent;
    private final SimpleLootConfig config;
    
    // Layout constants per guide
    private static final int HEADER_HEIGHT = 35;
    private static final int FOOTER_HEIGHT = 35;
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 180;
    private static final int RESET_BTN_WIDTH = 40;
    private static final int SPACING = 4;
    private static final int SCROLL_SPEED = 10;
    private static final int SCROLLBAR_WIDTH = 6;
    
    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragOffset = 0;
    
    // Tooltip system
    private record TooltipEntry(int x, int y, int width, int height, String tooltip) {}
    private final List<TooltipEntry> tooltips = new ArrayList<>();
    
    // Track scrollable widgets with their original Y positions
    private record WidgetEntry(ClickableWidget widget, int originalY) {}
    private final List<WidgetEntry> scrollableWidgets = new ArrayList<>();
    
    // Track footer buttons (non-scrollable)
    private final List<ClickableWidget> footerButtons = new ArrayList<>();
    
    // Store current values (we modify these, then save on Done)
    private boolean enabled;
    private boolean debugMode;
    private boolean hotbarProtection;
    private int transferDelayMs;
    private boolean allowHoverDrop;
    private boolean allowCraftingGrid;
    private boolean allowArmorEquip;
    private int armorSwapDelayMs;
    // Storage containers
    private boolean allowChests;
    private boolean allowDoubleChests;
    private boolean allowBarrels;
    private boolean allowShulkerBoxes;
    private boolean allowEnderChests;
    private boolean allowHoppers;
    private boolean allowDroppers;
    private boolean allowDispensers;
    // Processing containers
    private boolean allowFurnaces;
    private boolean allowBlastFurnaces;
    private boolean allowSmokers;
    private boolean allowBrewingStands;
    // Workstations
    private boolean allowAnvils;
    private boolean allowSmithingTables;
    private boolean allowGrindstones;
    private boolean allowStonecutters;
    private boolean allowLooms;
    private boolean allowEnchantingTables;
    private boolean allowBeacons;
    private boolean allowCrafters;
    private boolean allowCartographyTables;

    // Reference to transfer delay slider for reset
    private TransferDelaySlider transferDelaySlider;
    private ArmorSwapDelaySlider armorSwapDelaySlider;

    public SimpleLootConfigScreen(Screen parent) {
        super(Text.translatable("config.simpleloot.title"));
        this.parent = parent;
        this.config = SimpleLootConfig.getInstance();
        
        // Load current values
        this.enabled = config.enabled;
        this.debugMode = config.debugMode;
        this.hotbarProtection = config.hotbarProtection;
        this.transferDelayMs = config.transferDelayMs;
        this.allowHoverDrop = config.allowHoverDrop;
        this.allowCraftingGrid = config.allowCraftingGrid;
        this.allowArmorEquip = config.allowArmorEquip;
        this.armorSwapDelayMs = config.armorSwapDelayMs;
        // Storage containers
        this.allowChests = config.allowChests;
        this.allowDoubleChests = config.allowDoubleChests;
        this.allowBarrels = config.allowBarrels;
        this.allowShulkerBoxes = config.allowShulkerBoxes;
        this.allowEnderChests = config.allowEnderChests;
        this.allowHoppers = config.allowHoppers;
        this.allowDroppers = config.allowDroppers;
        this.allowDispensers = config.allowDispensers;
        // Processing containers
        this.allowFurnaces = config.allowFurnaces;
        this.allowBlastFurnaces = config.allowBlastFurnaces;
        this.allowSmokers = config.allowSmokers;
        this.allowBrewingStands = config.allowBrewingStands;
        // Workstations
        this.allowAnvils = config.allowAnvils;
        this.allowSmithingTables = config.allowSmithingTables;
        this.allowGrindstones = config.allowGrindstones;
        this.allowStonecutters = config.allowStonecutters;
        this.allowLooms = config.allowLooms;
        this.allowEnchantingTables = config.allowEnchantingTables;
        this.allowBeacons = config.allowBeacons;
        this.allowCrafters = config.allowCrafters;
        this.allowCartographyTables = config.allowCartographyTables;
    }

    @Override
    protected void init() {
        super.init();
        tooltips.clear();
        scrollableWidgets.clear();
        footerButtons.clear();
        
        int centerX = this.width / 2;
        int totalWidth = WIDGET_WIDTH + SPACING + RESET_BTN_WIDTH;
        int widgetX = centerX - totalWidth / 2;
        int resetX = widgetX + WIDGET_WIDTH + SPACING;
        int y = HEADER_HEIGHT;
        
        // Count options for scroll calculation (29 options total with new containers)
        int numberOfOptions = 29;
        contentHeight = numberOfOptions * ROW_HEIGHT;
        int contentAreaHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        maxScrollOffset = Math.max(0, contentHeight - contentAreaHeight);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);
        
        // Enable SimpleLoot
        addTooltip(widgetX, y, totalWidth, 20, "Enable or disable SimpleLoot completely. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.enabled", 
            () -> enabled, v -> enabled = v, true);
        y += ROW_HEIGHT;
        
        // Debug Mode
        addTooltip(widgetX, y, totalWidth, 20, "Enable debug logging for troubleshooting issues. Default: OFF");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.debugMode", 
            () -> debugMode, v -> debugMode = v, false);
        y += ROW_HEIGHT;
        
        // Hotbar Protection
        addTooltip(widgetX, y, totalWidth, 20, "Prevent items in hotbar slots from being transferred. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.hotbarProtection", 
            () -> hotbarProtection, v -> hotbarProtection = v, true);
        y += ROW_HEIGHT;
        
        // Transfer Delay Slider
        addTooltip(widgetX, y, totalWidth, 20, "Delay between item transfers in milliseconds. 0 = instant. Default: 20ms");
        transferDelaySlider = new TransferDelaySlider(widgetX, y, WIDGET_WIDTH, 20, transferDelayMs);
        addScrollableWidget(transferDelaySlider, y);
        ButtonWidget delayReset = ButtonWidget.builder(Text.literal("↺"), button -> {
            transferDelaySlider.setValue(20);
            transferDelayMs = 20;
        }).dimensions(resetX, y, RESET_BTN_WIDTH, 20).build();
        addScrollableWidget(delayReset, y);
        y += ROW_HEIGHT;
        
        // Allow Hover Drop
        addTooltip(widgetX, y, totalWidth, 20, "Enable Ctrl + Hover Loot to drop entire stacks on the ground. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowHoverDrop", 
            () -> allowHoverDrop, v -> allowHoverDrop = v, true);
        y += ROW_HEIGHT;
        
        // Allow Crafting Grid
        addTooltip(widgetX, y, totalWidth, 20, "Enable hover loot to send items to/from crafting grids. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowCraftingGrid", 
            () -> allowCraftingGrid, v -> allowCraftingGrid = v, true);
        y += ROW_HEIGHT;
        
        // Allow Armor Equip
        addTooltip(widgetX, y, totalWidth, 20, "Enable hover loot on armor in inventory to equip/unequip. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowArmorEquip", 
            () -> allowArmorEquip, v -> allowArmorEquip = v, true);
        y += ROW_HEIGHT;
        
        // Armor Swap Delay Slider
        addTooltip(widgetX, y, totalWidth, 20, "Delay between armor swaps in milliseconds. Lower = faster but may cause issues. Default: 70ms");
        armorSwapDelaySlider = new ArmorSwapDelaySlider(widgetX, y, WIDGET_WIDTH, 20, armorSwapDelayMs);
        addScrollableWidget(armorSwapDelaySlider, y);
        ButtonWidget armorDelayReset = ButtonWidget.builder(Text.literal("↺"), button -> {
            armorSwapDelaySlider.setValue(70);
            armorSwapDelayMs = 70;
        }).dimensions(resetX, y, RESET_BTN_WIDTH, 20).build();
        addScrollableWidget(armorDelayReset, y);
        y += ROW_HEIGHT;
        
        // Container type toggles
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from single chests. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowChests", 
            () -> allowChests, v -> allowChests = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from double (large) chests. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowDoubleChests", 
            () -> allowDoubleChests, v -> allowDoubleChests = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from barrels. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowBarrels", 
            () -> allowBarrels, v -> allowBarrels = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from shulker boxes. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowShulkerBoxes", 
            () -> allowShulkerBoxes, v -> allowShulkerBoxes = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from ender chests. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowEnderChests", 
            () -> allowEnderChests, v -> allowEnderChests = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from hoppers. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowHoppers", 
            () -> allowHoppers, v -> allowHoppers = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from droppers. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowDroppers", 
            () -> allowDroppers, v -> allowDroppers = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from dispensers. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowDispensers", 
            () -> allowDispensers, v -> allowDispensers = v, true);
        y += ROW_HEIGHT;
        
        // Processing containers
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from furnaces. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowFurnaces", 
            () -> allowFurnaces, v -> allowFurnaces = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from blast furnaces. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowBlastFurnaces", 
            () -> allowBlastFurnaces, v -> allowBlastFurnaces = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from smokers. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowSmokers", 
            () -> allowSmokers, v -> allowSmokers = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from brewing stands. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowBrewingStands", 
            () -> allowBrewingStands, v -> allowBrewingStands = v, true);
        y += ROW_HEIGHT;
        
        // Workstations
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from anvils. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowAnvils", 
            () -> allowAnvils, v -> allowAnvils = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from smithing tables. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowSmithingTables", 
            () -> allowSmithingTables, v -> allowSmithingTables = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from grindstones. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowGrindstones", 
            () -> allowGrindstones, v -> allowGrindstones = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from stonecutters. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowStonecutters", 
            () -> allowStonecutters, v -> allowStonecutters = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from looms. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowLooms", 
            () -> allowLooms, v -> allowLooms = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from enchanting tables. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowEnchantingTables", 
            () -> allowEnchantingTables, v -> allowEnchantingTables = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from beacons. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowBeacons", 
            () -> allowBeacons, v -> allowBeacons = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from crafters. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowCrafters", 
            () -> allowCrafters, v -> allowCrafters = v, true);
        y += ROW_HEIGHT;
        
        addTooltip(widgetX, y, totalWidth, 20, "Allow hover-looting from cartography tables. Default: ON");
        addScrollableToggleWithReset(widgetX, y, resetX, "config.simpleloot.allowCartographyTables", 
            () -> allowCartographyTables, v -> allowCartographyTables = v, true);
        
        // Bottom buttons (fixed, not scrollable)
        int bottomY = this.height - FOOTER_HEIGHT + 7;
        int bottomButtonWidth = 76;
        int totalBottomWidth = bottomButtonWidth * 3 + 8;
        int bottomStartX = this.width / 2 - totalBottomWidth / 2;
        
        // Save & Close button
        ButtonWidget saveBtn = ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            transferDelayMs = transferDelaySlider.getIntValue();
            saveConfig();
            this.client.setScreen(parent);
        }).dimensions(bottomStartX, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(saveBtn);
        footerButtons.add(saveBtn);
        
        // Keybinds button
        ButtonWidget keybindsBtn = ButtonWidget.builder(Text.translatable("controls.keybinds"), button -> {
            this.client.setScreen(new KeybindsScreen(this, this.client.options));
        }).dimensions(bottomStartX + bottomButtonWidth + 4, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(keybindsBtn);
        footerButtons.add(keybindsBtn);
        
        // Cancel button
        ButtonWidget cancelBtn = ButtonWidget.builder(Text.translatable("gui.cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(bottomStartX + bottomButtonWidth * 2 + 8, bottomY, bottomButtonWidth, 20).build();
        this.addDrawableChild(cancelBtn);
        footerButtons.add(cancelBtn);
        
        // Update widget positions based on initial scroll
        updateWidgetPositions();
    }
    
    private void addScrollableWidget(ClickableWidget widget, int originalY) {
        this.addDrawableChild(widget);
        scrollableWidgets.add(new WidgetEntry(widget, originalY));
    }
    
    private void addScrollableToggleWithReset(int x, int y, int resetX, String translationKey,
            java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter, boolean defaultValue) {
        ButtonWidget toggleBtn = ButtonWidget.builder(getBooleanText(translationKey, getter.get()), button -> {
            boolean newValue = !getter.get();
            setter.accept(newValue);
            button.setMessage(getBooleanText(translationKey, newValue));
        }).dimensions(x, y, WIDGET_WIDTH, 20).build();
        addScrollableWidget(toggleBtn, y);
        
        // Reset button
        ButtonWidget resetBtn = ButtonWidget.builder(Text.literal("↺"), button -> {
            setter.accept(defaultValue);
            toggleBtn.setMessage(getBooleanText(translationKey, defaultValue));
        }).dimensions(resetX, y, RESET_BTN_WIDTH, 20).build();
        addScrollableWidget(resetBtn, y);
    }
    
    private void addTooltip(int x, int y, int width, int height, String tooltip) {
        tooltips.add(new TooltipEntry(x, y, width, height, tooltip));
    }
    
    private Text getBooleanText(String translationKey, boolean value) {
        return Text.translatable(translationKey)
                .append(Text.literal(": "))
                .append(value ? Text.literal("ON").styled(s -> s.withColor(0x55FF55)) 
                              : Text.literal("OFF").styled(s -> s.withColor(0xFF5555)));
    }
    
    private void updateWidgetPositions() {
        int scrollAreaTop = HEADER_HEIGHT;
        int scrollAreaBottom = this.height - FOOTER_HEIGHT;
        
        for (WidgetEntry entry : scrollableWidgets) {
            int adjustedY = entry.originalY - scrollOffset;
            entry.widget.setY(adjustedY);
            
            // Hide widgets outside visible scroll area
            boolean visible = adjustedY >= scrollAreaTop - 20 && adjustedY < scrollAreaBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
        }
    }
    
    private void saveConfig() {
        config.enabled = this.enabled;
        config.debugMode = this.debugMode;
        config.hotbarProtection = this.hotbarProtection;
        config.transferDelayMs = this.transferDelayMs;
        config.allowHoverDrop = this.allowHoverDrop;
        config.allowCraftingGrid = this.allowCraftingGrid;
        config.allowArmorEquip = this.allowArmorEquip;
        config.armorSwapDelayMs = this.armorSwapDelayMs;
        // Storage containers
        config.allowChests = this.allowChests;
        config.allowDoubleChests = this.allowDoubleChests;
        config.allowBarrels = this.allowBarrels;
        config.allowShulkerBoxes = this.allowShulkerBoxes;
        config.allowEnderChests = this.allowEnderChests;
        config.allowHoppers = this.allowHoppers;
        config.allowDroppers = this.allowDroppers;
        config.allowDispensers = this.allowDispensers;
        // Processing containers
        config.allowFurnaces = this.allowFurnaces;
        config.allowBlastFurnaces = this.allowBlastFurnaces;
        config.allowSmokers = this.allowSmokers;
        config.allowBrewingStands = this.allowBrewingStands;
        // Workstations
        config.allowAnvils = this.allowAnvils;
        config.allowSmithingTables = this.allowSmithingTables;
        config.allowGrindstones = this.allowGrindstones;
        config.allowStonecutters = this.allowStonecutters;
        config.allowLooms = this.allowLooms;
        config.allowEnchantingTables = this.allowEnchantingTables;
        config.allowBeacons = this.allowBeacons;
        config.allowCrafters = this.allowCrafters;
        config.allowCartographyTables = this.allowCartographyTables;
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
    
    /**
     * Custom slider widget for armor swap delay (0-500ms range).
     */
    private class ArmorSwapDelaySlider extends SliderWidget {
        private static final int MIN = 0;
        private static final int MAX = 500;
        
        public ArmorSwapDelaySlider(int x, int y, int width, int height, int initialValue) {
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
            setMessage(Text.translatable("config.simpleloot.armorSwapDelayMs")
                    .append(Text.literal(": "))
                    .append(Text.literal(val + " ms").styled(s -> s.withColor(0xFFFF55))));
        }
        
        @Override
        protected void applyValue() {
            armorSwapDelayMs = getIntValue();
        }
    }
    
    // ========================================
    // Mouse wheel scrolling
    // ========================================
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY > HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
            scrollOffset -= (int)(verticalAmount * SCROLL_SPEED);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    // ========================================
    // Interactive scrollbar - click to start drag (MC 1.21.11 uses Click class)
    // ========================================
    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        
        if (button == 0 && maxScrollOffset > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            int scrollbarTrackTop = HEADER_HEIGHT;
            int scrollbarTrackBottom = this.height - FOOTER_HEIGHT;
            
            if (mouseX >= scrollbarX && mouseX <= this.width - 2 &&
                mouseY >= scrollbarTrackTop && mouseY <= scrollbarTrackBottom) {
                
                int trackHeight = scrollbarTrackBottom - scrollbarTrackTop;
                int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
                int thumbY = scrollbarTrackTop + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
                
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    // Clicked on thumb - start dragging
                    isDraggingScrollbar = true;
                    scrollbarDragOffset = (int)(mouseY - thumbY);
                } else {
                    // Clicked on track - jump to position
                    int clickOffset = (int)mouseY - scrollbarTrackTop - thumbHeight / 2;
                    float scrollPercent = (float)clickOffset / (trackHeight - thumbHeight);
                    scrollOffset = (int)(scrollPercent * maxScrollOffset);
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
                    isDraggingScrollbar = true;
                    scrollbarDragOffset = thumbHeight / 2;
                    updateWidgetPositions();
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }
    
    // ========================================
    // Interactive scrollbar - drag (MC 1.21.11 uses Click class)
    // ========================================
    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseY = click.y();
        int button = click.button();
        
        if (isDraggingScrollbar && button == 0 && maxScrollOffset > 0) {
            int scrollbarTrackTop = HEADER_HEIGHT;
            int scrollbarTrackBottom = this.height - FOOTER_HEIGHT;
            int trackHeight = scrollbarTrackBottom - scrollbarTrackTop;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
            
            int thumbY = (int)mouseY - scrollbarDragOffset - scrollbarTrackTop;
            float scrollPercent = (float)thumbY / (trackHeight - thumbHeight);
            scrollOffset = (int)(scrollPercent * maxScrollOffset);
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
            updateWidgetPositions();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }
    
    // ========================================
    // Interactive scrollbar - release (MC 1.21.11 uses Click class)
    // ========================================
    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Title in header area (fixed)
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        
        // Enable scissor to clip scrollable content ONLY
        int scissorTop = HEADER_HEIGHT;
        int scissorBottom = this.height - FOOTER_HEIGHT;
        context.enableScissor(0, scissorTop, this.width, scissorBottom);
        
        // Render ONLY scrollable widgets (not footer buttons)
        for (WidgetEntry entry : scrollableWidgets) {
            entry.widget.render(context, mouseX, mouseY, delta);
        }
        
        // Disable scissor
        context.disableScissor();
        
        // Render footer buttons OUTSIDE scissor (so they're not clipped)
        for (ClickableWidget button : footerButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
        
        // Draw scrollbar if needed
        if (maxScrollOffset > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - 4;
            int trackHeight = scissorBottom - scissorTop;
            int thumbHeight = Math.max(20, trackHeight * trackHeight / (maxScrollOffset + trackHeight));
            int thumbY = scissorTop + (int)((trackHeight - thumbHeight) * ((float)scrollOffset / maxScrollOffset));
            
            // Track background
            context.fill(scrollbarX, scissorTop, scrollbarX + SCROLLBAR_WIDTH, scissorBottom, 0x40FFFFFF);
            // Thumb
            context.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
        }
        
        // Draw scroll indicators
        if (scrollOffset > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("▲"), this.width / 2, scissorTop + 2, 0xAAAAAA);
        }
        if (scrollOffset < maxScrollOffset) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("▼"), this.width / 2, scissorBottom - 12, 0xAAAAAA);
        }
        
        // Draw tooltips LAST (after scissor disabled, so they render on top)
        if (mouseY > HEADER_HEIGHT && mouseY < this.height - FOOTER_HEIGHT) {
            for (TooltipEntry entry : tooltips) {
                // Adjust tooltip Y for scroll
                int adjustedY = entry.y - scrollOffset;
                if (mouseX >= entry.x && mouseX < entry.x + entry.width &&
                    mouseY >= adjustedY && mouseY < adjustedY + 20) {
                    context.drawTooltip(this.textRenderer, Text.literal(entry.tooltip), mouseX, mouseY);
                    break;
                }
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
