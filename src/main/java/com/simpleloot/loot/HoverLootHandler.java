package com.simpleloot.loot;

import com.simpleloot.SimpleLootClient;
import com.simpleloot.config.SimpleLootConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
//?} else {
/*import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;*/
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;*/
//?}
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Core handler for the hover loot functionality.
 * 
 * This class manages the detection of hover events over inventory slots
 * and triggers item transfers when the hover loot key is held.
 * 
 * Like Rust's hover loot: items stay queued until the container is closed,
 * even if you release the key.
 */
public class HoverLootHandler {
    
    // Version-aware constants for container input types
    //? if >=26.1 {
    private static final ContainerInput SLOT_PICKUP = ContainerInput.PICKUP;
    private static final ContainerInput SLOT_QUICK_MOVE = ContainerInput.QUICK_MOVE;
    private static final ContainerInput SLOT_THROW = ContainerInput.THROW;
    //?} else {
    /*private static final ClickType SLOT_PICKUP = ClickType.PICKUP;
    private static final ClickType SLOT_QUICK_MOVE = ClickType.QUICK_MOVE;
    private static final ClickType SLOT_THROW = ClickType.THROW;*/
    //?}
    
    // Track which slots are currently in the pending queue
    // This prevents adding the same slot multiple times during one key-hold session
    private static final Set<Integer> currentlyQueued = new HashSet<>();
    
    // Queue of slots waiting to be transferred
    private static final Queue<Integer> pendingSlots = new LinkedList<>();
    
    // For creative mode: store slot indices (not Slot references as they become stale)
    // Store the actualSlotId found during hover detection
    private static final Set<Integer> creativeQueuedSlotIds = new HashSet<>();
    private static final Queue<Integer> creativePendingSlotIds = new LinkedList<>();
    
    // Track the last screen we were in
    private static AbstractContainerScreen<?> lastScreen = null;
    
    // Track last mouse position for interpolation
    private static double lastMouseX = -1;
    private static double lastMouseY = -1;
    
    // Track if key was pressed last tick (for detecting release)
    private static boolean wasKeyPressed = false;
    
    // Track if we're in drop mode (Ctrl held when hover loot started)
    private static boolean isDropMode = false;
    
    // Last time an item was transferred (for rate limiting / visual delay)
    private static long lastTransferTime = 0;
    
    // Track armor swap times per slot to prevent rapid re-swapping
    // Maps slotId -> timestamp of last swap for that slot
    private static final java.util.Map<Integer, Long> armorSwapTimes = new java.util.HashMap<>();
    
    /**
     * Initializes the hover loot handler and registers tick-based hover detection.
     */
    public static void init() {
        // Use client tick events to check for hover loot every tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.screen instanceof AbstractContainerScreen<?> handledScreen) {
                if (isSupportedScreen(client.screen)) {
                    // Reset state if screen changed (new container opened)
                    if (lastScreen != handledScreen) {
                        currentlyQueued.clear();
                        pendingSlots.clear();
                        creativeQueuedSlotIds.clear();
                        creativePendingSlotIds.clear();
                        armorSwapTimes.clear();
                        lastMouseX = -1;
                        lastMouseY = -1;
                        wasKeyPressed = false;
                        isDropMode = false;
                        lastScreen = handledScreen;
                    }
                    
                    // Get current mouse position
                    double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getWidth();
                    double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getHeight();
                    
                    handleHoverLoot(client, handledScreen, mouseX, mouseY);
                    
                    // Update last mouse position
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
            } else {
                // Screen closed, reset ALL state
                if (lastScreen != null) {
                    currentlyQueued.clear();
                    pendingSlots.clear();
                    armorSwapTimes.clear();
                    lastMouseX = -1;
                    lastMouseY = -1;
                    wasKeyPressed = false;
                    isDropMode = false;
                    lastScreen = null;
                }
            }
        });
        
        SimpleLootClient.LOGGER.info("HoverLootHandler initialized");
    }
    
    /**
     * Checks if the given screen is a supported container screen.
     * Supports:
     * - All container types (chests, barrels, etc.)
     * - InventoryScreen for crafting grid transfers and drop mode
     * - CreativeModeInventoryScreen for drop mode
     * - CraftingScreen (crafting table) for crafting grid transfers
     */
    private static boolean isSupportedScreen(net.minecraft.client.gui.screens.Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
        // InventoryScreen - always allow for crafting grid transfers (if enabled) or drop mode
        if (screen instanceof InventoryScreen) {
            return config.allowHoverDrop || config.allowCraftingGrid;
        }
        
        // CreativeModeInventoryScreen - only for drop mode
        if (screen instanceof CreativeModeInventoryScreen) {
            return config.allowHoverDrop;
        }
        
        // CraftingScreen (crafting table) - allow for crafting grid transfers
        if (screen instanceof CraftingScreen) {
            return config.allowCraftingGrid;
        }
        
        // Check each container type against config
        if (screen instanceof ContainerScreen) {
            ContainerScreen containerScreen = (ContainerScreen) screen;
            int rows = containerScreen.getMenu().getRowCount();
            // Single chest = 3 rows, Double chest = 6 rows
            if (rows <= 3 && !config.allowChests) return false;
            if (rows > 3 && !config.allowDoubleChests) return false;
            return true;
        }
        
        if (screen instanceof ShulkerBoxScreen) {
            return config.allowShulkerBoxes;
        }
        
        if (screen instanceof DispenserScreen) {
            // This handles dispensers and droppers
            return config.allowDispensers || config.allowDroppers;
        }
        
        if (screen instanceof HopperScreen) {
            return config.allowHoppers;
        }
        
        // Processing screens (furnaces, etc.)
        if (screen instanceof FurnaceScreen) {
            return config.allowFurnaces;
        }
        
        if (screen instanceof BlastFurnaceScreen) {
            return config.allowBlastFurnaces;
        }
        
        if (screen instanceof SmokerScreen) {
            return config.allowSmokers;
        }
        
        if (screen instanceof BrewingStandScreen) {
            return config.allowBrewingStands;
        }
        
        // Workstation screens
        if (screen instanceof AnvilScreen) {
            return config.allowAnvils;
        }
        
        if (screen instanceof SmithingScreen) {
            return config.allowSmithingTables;
        }
        
        if (screen instanceof GrindstoneScreen) {
            return config.allowGrindstones;
        }
        
        if (screen instanceof StonecutterScreen) {
            return config.allowStonecutters;
        }
        
        if (screen instanceof LoomScreen) {
            return config.allowLooms;
        }
        
        if (screen instanceof EnchantmentScreen) {
            return config.allowEnchantingTables;
        }
        
        if (screen instanceof BeaconScreen) {
            return config.allowBeacons;
        }
        
        if (screen instanceof CrafterScreen) {
            return config.allowCrafters;
        }
        
        if (screen instanceof CartographyTableScreen) {
            return config.allowCartographyTables;
        }
        
        // Ender chest uses ContainerScreen, but we can check by title or other means
        // For now, ContainerScreen covers ender chests as well
        
        // Barrel also uses ContainerScreen (3 rows)
        
        return false;
    }
    
    /**
     * Handles hover loot detection and queue processing.
     * 
     * Key behavior (like Rust):
     * - Hold key OR toggle mode active + hover over slots = add to queue
     * - Queue keeps processing even after releasing key
     * - Releasing and re-pressing key allows re-selecting slots
     * - Only closing the container clears the queue completely
     * 
     * Modes:
     * - Normal: Quick move items (shift-click equivalent)
     * - Drop (Ctrl + hover OR hover drop key): Drop items on ground
     * - Shift + hover: Quick move in inventory/crafting screens
     * - Crafting grid: Send items to/from crafting grid slots
     */
    private static void handleHoverLoot(Minecraft client, AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        boolean DEBUG = config.debugMode;
        
        // Check if mod is enabled
        if (!config.enabled) {
            return;
        }
        
        // Check if hover loot is active (either key held OR toggle mode)
        boolean hoverLootActive = SimpleLootClient.isHoverLootActive();
        
        // Detect deactivation - clear the "currently queued" set so slots can be re-selected
        if (wasKeyPressed && !hoverLootActive) {
            // Hover loot was just deactivated - allow all slots to be queued again on next activation
            currentlyQueued.clear();
            creativeQueuedSlotIds.clear();
            armorSwapTimes.clear();
            isDropMode = false;
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Hover loot deactivated, slots can now be re-selected");
        }
        
        // Check modifier keys
        boolean hoverDropKeyHeld = SimpleLootClient.isHoverDropKeyHeld();
        long windowHandle = client.getWindow().handle();
        boolean ctrlHeld = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                           GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shiftHeld = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        // Determine the operation mode
        // Drop mode: Ctrl + hover OR dedicated hover drop key
        boolean shouldBeDropMode = (ctrlHeld || hoverDropKeyHeld) && config.allowHoverDrop;
        
        // If switching from transfer to drop mode mid-hover, log it
        if (DEBUG && !isDropMode && shouldBeDropMode && hoverLootActive) {
            SimpleLootClient.LOGGER.info("[DEBUG] Switched to DROP mode mid-hover");
        }
        
        isDropMode = shouldBeDropMode;
        
        // Also activate hover loot if hover drop key is held (can be pressed anytime)
        if (hoverDropKeyHeld && config.allowHoverDrop) {
            hoverLootActive = true; // Treat hover drop key as activating hover loot
        }
        
        wasKeyPressed = hoverLootActive;
        
        // Determine screen type and allowed operations
        boolean isInventoryScreen = screen instanceof InventoryScreen;
        boolean isCreativeInventory = screen instanceof CreativeModeInventoryScreen;
        
        // Check if creative inventory is on the survival inventory tab (the only tab where drop should work)
        boolean isCreativeSurvivalTab = false;
        if (isCreativeInventory) {
            CreativeModeInventoryScreen creativeScreen = (CreativeModeInventoryScreen) screen;
            // Use Fabric API to get the currently selected item group
            // The survival inventory tab is identified by getting its registry key and comparing to CreativeModeTabs.INVENTORY
            //? if >=26.1 {
            if (creativeScreen instanceof FabricCreativeModeInventoryScreen fabricScreen) {
                CreativeModeTab selectedGroup = fabricScreen.getSelectedTab();
            //?} else {
            /*if (creativeScreen instanceof FabricCreativeInventoryScreen fabricScreen) {
                CreativeModeTab selectedGroup = fabricScreen.getSelectedItemGroup();*/
            //?}
                // Get the registry key of the selected group and compare to INVENTORY key
                if (selectedGroup != null) {
                    var selectedKey = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(selectedGroup);
                    isCreativeSurvivalTab = selectedKey.isPresent() && selectedKey.get().equals(CreativeModeTabs.INVENTORY);
                }
                
                if (DEBUG && hoverLootActive) {
                    String groupName = selectedGroup != null ? selectedGroup.getDisplayName().getString() : "null";
                    SimpleLootClient.LOGGER.info("[DEBUG] Creative selected tab: '{}', isSurvivalTab={}", 
                            groupName, isCreativeSurvivalTab);
                }
            }
        }
        
        if (DEBUG && isCreativeInventory && hoverLootActive) {
            SimpleLootClient.LOGGER.info("[DEBUG] Creative inventory detected. isDropMode={}, hoverLootActive={}, hoverDropKeyHeld={}, ctrlHeld={}, isCreativeSurvivalTab={}", 
                    isDropMode, hoverLootActive, hoverDropKeyHeld, ctrlHeld, isCreativeSurvivalTab);
        }
        
        // For creative inventory, only drop mode is allowed AND only on the survival inventory tab
        // But always process pending queue items even if drop mode was turned off
        if (isCreativeInventory && (!isDropMode || !isCreativeSurvivalTab) && creativePendingSlotIds.isEmpty()) {
            return;
        }
        
        // For player inventory without modifiers:
        // - If just hover loot key: try to send to crafting grid (if enabled)
        // - If shift + hover: quick move within inventory
        // - If ctrl + hover / drop key: drop items
        if (isInventoryScreen && !isDropMode && !shiftHeld) {
            // Plain hover in inventory - check if we should send to crafting grid
            if (!config.allowCraftingGrid) {
                return; // No crafting grid support, nothing to do
            }
            // Allow crafting grid mode - handled in processQueue
        }
        
        if (hoverLootActive && (isDropMode || !isCreativeInventory)) {
            // Hover loot is active - detect slots and add to queue
            // Use interpolation to catch slots we moved over quickly
            List<Slot> slotsToQueue = getSlotsAlongPath(screen, lastMouseX, lastMouseY, mouseX, mouseY);
            
            for (Slot slot : slotsToQueue) {
                if (slot != null && slot.hasItem()) {
                    // Check hotbar protection (only for non-crafting operations)
                    if (config.hotbarProtection && !isCraftingSlot(screen, slot) && isHotbarSlot(screen.getMenu(), slot)) {
                        continue;
                    }
                    
                    // For creative inventory survival tab, use the creative queue
                    // We queue items and process them one at a time to ensure proper game state updates
                    if (isCreativeInventory && isDropMode && isCreativeSurvivalTab) {
                        // Find the actual slot ID
                        int actualSlotId = findActualSlotId(screen, slot);
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Creative slot detection: slot.index={}, actualSlotId={}, item={}, alreadyQueued={}", 
                                slot.index, actualSlotId, slot.getItem().getHoverName().getString(), creativeQueuedSlotIds.contains(actualSlotId));
                        if (actualSlotId >= 0 && !creativeQueuedSlotIds.contains(actualSlotId)) {
                            // Add to queue for processing
                            creativePendingSlotIds.add(actualSlotId);
                            creativeQueuedSlotIds.add(actualSlotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued creative survival slot actualId={} with item: {}", 
                                    actualSlotId, slot.getItem().getHoverName().getString());
                        }
                    } else if (isCreativeInventory) {
                        // Non-survival tab creative - queue for later (though this shouldn't happen due to earlier checks)
                        int actualSlotId = findActualSlotId(screen, slot);
                        if (actualSlotId >= 0 && !creativeQueuedSlotIds.contains(actualSlotId)) {
                            creativePendingSlotIds.add(actualSlotId);
                            creativeQueuedSlotIds.add(actualSlotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued creative slot actualId={} with item: {}", 
                                    actualSlotId, slot.getItem().getHoverName().getString());
                        }
                    } else {
                        int slotId = slot.index;
                        // Add to queue if not already queued in THIS session
                        // (prevents adding same slot multiple times while active)
                        if (!currentlyQueued.contains(slotId)) {
                            // For armor equippable items, check if this slot was recently involved in an armor swap
                            // This prevents rapid re-swapping when the cursor stays on the same slot
                            if (isEquippableArmor(slot.getItem())) {
                                Long lastSwapTime = armorSwapTimes.get(slotId);
                                if (lastSwapTime != null) {
                                    long timeSinceLastSwap = System.currentTimeMillis() - lastSwapTime;
                                    if (config.armorSwapDelayMs > 0 && timeSinceLastSwap < config.armorSwapDelayMs) {
                                        // Still within delay period for this slot, skip queueing
                                        continue;
                                    }
                                    // Delay has passed, remove from tracking
                                    armorSwapTimes.remove(slotId);
                                }
                            }
                            
                            // Validate item for specific screen types
                            if (!isValidSlotTransfer(screen, slot, slot.getItem(), isPlayerInventorySlotGeneric(screen, slot))) {
                                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Skipped slot {} - item {} not valid for this screen type", 
                                        slotId, slot.getItem().getHoverName().getString());
                                continue;
                            }
                            
                            pendingSlots.add(slotId);
                            currentlyQueued.add(slotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued slot {} with item: {}", 
                                    slotId, slot.getItem().getHoverName().getString());
                        }
                        // For armor: slot stays in currentlyQueued to prevent re-queueing until key is released
                        // This prevents rapid swapping when hovering over same slot after armor swap returns different armor
                    }
                }
            }
        }
        
        // ALWAYS process the queue (even if key is released - like Rust!)
        // Only closing the container stops the queue
        if (!pendingSlots.isEmpty() || !creativePendingSlotIds.isEmpty()) {
            processQueue(client, screen, config, DEBUG);
        }
    }
    
    /**
     * Finds the actual slot ID for a given slot in the screen handler.
     * This is needed for creative inventory where slot.index is unreliable.
     */
    private static int findActualSlotId(AbstractContainerScreen<?> screen, Slot slot) {
        var slots = screen.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) == slot) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Checks if a slot is in the player's hotbar.
     */
    private static boolean isHotbarSlot(net.minecraft.world.inventory.AbstractContainerMenu handler, Slot slot) {
        int containerSlotCount = handler.slots.size() - 36;
        int playerSlotIndex = slot.index - containerSlotCount;
        // Hotbar is the last 9 slots of player inventory (indices 27-35)
        return playerSlotIndex >= 27 && playerSlotIndex < 36;
    }
    
    /**
     * Checks if a slot is a crafting input slot (2x2 in inventory, 3x3 in crafting table).
     */
    private static boolean isCraftingSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            // Player inventory crafting grid: slots 1-4 (slot 0 is output)
            return slot.index >= 1 && slot.index <= 4;
        }
        if (screen instanceof CraftingScreen) {
            // Crafting table: slots 1-9 (slot 0 is output)
            return slot.index >= 1 && slot.index <= 9;
        }
        return false;
    }
    
    /**
     * Checks if a slot is the crafting output slot.
     */
    private static boolean isCraftingOutputSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
            // Output is always slot 0 in crafting screens
            return slot.index == 0;
        }
        return false;
    }
    
    /**
     * Gets the first available crafting input slot index.
     * Returns -1 if all slots are full.
     */
    private static int getAvailableCraftingSlot(AbstractContainerScreen<?> screen) {
        var handler = screen.getMenu();
        int startSlot = 1; // Skip output slot
        int endSlot;
        
        if (screen instanceof InventoryScreen) {
            endSlot = 4; // 2x2 grid
        } else if (screen instanceof CraftingScreen) {
            endSlot = 9; // 3x3 grid
        } else {
            return -1;
        }
        
        for (int i = startSlot; i <= endSlot; i++) {
            Slot slot = handler.slots.get(i);
            if (!slot.hasItem()) {
                return i;
            }
        }
        return -1; // All slots full
    }
    
    /**
     * Checks if a slot is a player inventory slot (not crafting, not armor, not offhand).
     * In player inventory screen, main inventory is slots 9-35 (excluding armor 5-8 and offhand 45).
     */
    private static boolean isPlayerInventorySlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            // Player inventory: main inventory is 9-35, hotbar would be 36-44 but in this screen
            // Layout: 0=output, 1-4=crafting, 5-8=armor, 9-35=main inventory, 36-44=hotbar, 45=offhand
            return slot.index >= 9 && slot.index <= 44;
        }
        if (screen instanceof CraftingScreen) {
            // Crafting table: 0=output, 1-9=crafting grid, 10-36=main inventory, 37-45=hotbar
            return slot.index >= 10 && slot.index <= 45;
        }
        return false;
    }
    
    /**
     * Gets all slots along the mouse path from last position to current position.
     * This catches slots we might have "skipped over" when moving fast.
     */
    private static List<Slot> getSlotsAlongPath(AbstractContainerScreen<?> screen, double fromX, double fromY, double toX, double toY) {
        List<Slot> slots = new ArrayList<>();
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        // If no previous position, just get current slot
        if (fromX < 0 || fromY < 0) {
            Slot currentSlot = getSlotAt(screen, toX, toY);
            if (currentSlot != null) {
                slots.add(currentSlot);
            }
            return slots;
        }
        
        // Calculate distance and number of samples needed
        double dx = toX - fromX;
        double dy = toY - fromY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        // Sample every ~2 pixels along the path (slot size is ~16-18 pixels)
        // More frequent sampling helps catch fast mouse movements
        int samples = Math.max(1, (int) (distance / 2));
        
        if (DEBUG && distance > 10) {
            SimpleLootClient.LOGGER.info("[DEBUG] getSlotsAlongPath: distance={}, samples={}", String.format("%.1f", distance), samples);
        }
        
        Set<Integer> foundSlotIds = new HashSet<>();
        
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            double x = fromX + dx * t;
            double y = fromY + dy * t;
            
            Slot slot = getSlotAt(screen, x, y);
            if (slot != null) {
                // For creative inventory, use actualSlotId for deduplication to match queue logic
                int slotIdForDedup = slot.index;
                if (screen instanceof CreativeModeInventoryScreen) {
                    int actualId = findActualSlotId(screen, slot);
                    if (actualId >= 0) {
                        slotIdForDedup = actualId;
                    }
                }
                
                if (!foundSlotIds.contains(slotIdForDedup)) {
                    slots.add(slot);
                    foundSlotIds.add(slotIdForDedup);
                    if (DEBUG && screen instanceof CreativeModeInventoryScreen) {
                        SimpleLootClient.LOGGER.info("[DEBUG] Path found slot: slot.index={}, actualId={}, hasStack={}, item={}", 
                                slot.index, slotIdForDedup, slot.hasItem(), slot.hasItem() ? slot.getItem().getHoverName().getString() : "empty");
                    }
                }
            }
        }
        
        return slots;
    }
    
    /**
     * Processes the pending transfer queue.
     * Respects the transfer delay setting for visual effect.
     * 
     * Handles different modes:
     * - Drop mode: Drop items on ground
     * - Armor equip: Equip armor from inventory
     * - Crafting mode: Send items to/from crafting grid
     * - Normal mode: Quick move items between containers
     */
    private static void processQueue(Minecraft client, AbstractContainerScreen<?> screen, SimpleLootConfig config, boolean DEBUG) {
        long currentTime = System.currentTimeMillis();
        
        // Check if we need to wait for the delay
        if (config.transferDelayMs > 0) {
            if (currentTime - lastTransferTime < config.transferDelayMs) {
                // Still waiting for delay, don't process yet
                return;
            }
        }
        
        // Determine screen type for specialized handling
        boolean isInventoryScreen = screen instanceof InventoryScreen;
        boolean isCreativeInventory = screen instanceof CreativeModeInventoryScreen;
        boolean isCraftingTable = screen instanceof CraftingScreen;
        boolean hasCraftingGrid = isInventoryScreen || isCraftingTable;
        
        // Process items - if delay is 0, process many per tick for instant transfer
        // If delay > 0, process just one per delay period for visual effect
        int maxPerTick = (config.transferDelayMs == 0) ? 20 : 1;
        int processedThisTick = 0;
        
        // For creative inventory, use the creative-specific queue (integer slot IDs)
        // IMPORTANT: Creative drops require two network actions (pickup + throw), so
        // we must process only ONE item per tick to ensure game state updates properly
        if (isCreativeInventory) {
            if (!creativePendingSlotIds.isEmpty()) {
                Integer actualSlotId = creativePendingSlotIds.peek();
                if (actualSlotId != null) {
                    // Find the slot at this ID
                    Slot slotToTransfer = findSlotById(screen, actualSlotId);
                    
                    if (slotToTransfer != null && slotToTransfer.hasItem()) {
                        if (isDropMode) {
                            // Drop mode: Drop items on ground using the actual slot ID
                            performThrowById(client, screen, actualSlotId, slotToTransfer.getItem());
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Dropped creative slot actualId={} with item: {}", 
                                    actualSlotId, slotToTransfer.getItem().getHoverName().getString());
                        }
                        // Creative mode only supports drop for now
                        
                        processedThisTick = 1;
                        lastTransferTime = System.currentTimeMillis();
                    }
                    
                    // Always remove from queued set and pending queue
                    creativeQueuedSlotIds.remove(actualSlotId);
                    creativePendingSlotIds.poll();
                }
            }
            
            if (DEBUG && processedThisTick > 0) {
                SimpleLootClient.LOGGER.info("[DEBUG] Processed {} creative items this tick, {} still pending", 
                        processedThisTick, creativePendingSlotIds.size());
            }
            return;
        }
        
        // Standard processing for non-creative inventories
        while (!pendingSlots.isEmpty() && processedThisTick < maxPerTick) {
            Integer nextSlotId = pendingSlots.peek();
            if (nextSlotId == null) break;
            
            // Find the slot by ID
            Slot slotToTransfer = findSlotById(screen, nextSlotId);
            
            if (slotToTransfer != null && slotToTransfer.hasItem()) {
                boolean handled = false;
                ItemStack stack = slotToTransfer.getItem();
                
                // Determine the action to perform
                if (isDropMode) {
                    // Drop mode: Drop items on ground
                    performThrow(client, screen, slotToTransfer);
                    handled = true;
                    if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Dropped slot {}", nextSlotId);
                } else if (isInventoryScreen && config.allowArmorEquip && isEquippableArmor(stack)) {
                    // Check armor swap delay for this specific slot
                    Long lastSwapTime = armorSwapTimes.get(nextSlotId);
                    if (lastSwapTime != null && config.armorSwapDelayMs > 0) {
                        long armorTimeSinceLastSwap = currentTime - lastSwapTime;
                        if (armorTimeSinceLastSwap < config.armorSwapDelayMs) {
                            // Still waiting for armor swap delay, skip this tick but don't remove from queue
                            break;
                        }
                    }
                    
                    // Check if hovering over armor in player inventory - try to equip/swap it
                    if (isPlayerInventorySlot(screen, slotToTransfer)) {
                        // Get the armor slot type for this item
                        EquipmentSlot armorType = getArmorSlotType(stack);
                        if (armorType != null) {
                            int targetArmorSlotId = getArmorSlotId(armorType);
                            Slot targetArmorSlot = findSlotById(screen, targetArmorSlotId);
                            
                            if (targetArmorSlot != null && targetArmorSlot.hasItem()) {
                                // Armor slot has something - need to SWAP
                                // Use pickup on source, then pickup on target (swaps), then pickup to place back
                                performArmorSwap(client, screen, slotToTransfer, targetArmorSlot);
                                handled = true;
                                armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this slot
                                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Swapped armor from slot {} to slot {}: {}", 
                                        nextSlotId, targetArmorSlotId, stack.getHoverName().getString());
                            } else {
                                // Armor slot is empty - use quick move (shift-click) to equip
                                performQuickMove(client, screen, slotToTransfer);
                                handled = true;
                                armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this slot
                                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Equipped armor from slot {}: {}", nextSlotId, stack.getHoverName().getString());
                            }
                        }
                    } else if (isArmorSlot(screen, slotToTransfer)) {
                        // Hovering over worn armor - unequip it
                        performQuickMove(client, screen, slotToTransfer);
                        handled = true;
                        armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this armor slot
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Unequipped armor from slot {}: {}", nextSlotId, stack.getHoverName().getString());
                    }
                } else if (hasCraftingGrid && config.allowCraftingGrid) {
                    // Crafting grid handling
                    if (isCraftingOutputSlot(screen, slotToTransfer)) {
                        // Output slot: Quick move the result to inventory
                        performQuickMove(client, screen, slotToTransfer);
                        handled = true;
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Took crafting output from slot {}", nextSlotId);
                    } else if (isCraftingSlot(screen, slotToTransfer)) {
                        // Crafting input slot: Move back to inventory
                        performQuickMove(client, screen, slotToTransfer);
                        handled = true;
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Moved crafting input back to inventory from slot {}", nextSlotId);
                    } else if (isPlayerInventorySlot(screen, slotToTransfer)) {
                        // Player inventory slot: Try to send to crafting grid
                        int targetSlot = getAvailableCraftingSlot(screen);
                        if (targetSlot != -1) {
                            performCraftingTransfer(client, screen, slotToTransfer, targetSlot);
                            handled = true;
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Sent item to crafting slot {} from slot {}", targetSlot, nextSlotId);
                        } else {
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Crafting grid full, cannot send item from slot {}", nextSlotId);
                            // Still mark as handled so we don't retry
                            handled = true;
                        }
                    }
                }
                
                // Fallback to normal quick move for container screens
                if (!handled) {
                    performQuickMove(client, screen, slotToTransfer);
                    if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Quick-moved slot {}", nextSlotId);
                }
                
                processedThisTick++;
                lastTransferTime = System.currentTimeMillis();
                
                // For armor swaps, keep the slot in currentlyQueued to prevent immediate re-queueing
                // The armor swap delay check will handle when it can be re-queued
                // For other operations, remove from currentlyQueued so the slot can be re-selected
                boolean wasArmorSwap = isInventoryScreen && config.allowArmorEquip && isEquippableArmor(stack) && handled;
                if (!wasArmorSwap) {
                    currentlyQueued.remove(nextSlotId);
                }
                // Note: For armor swaps, currentlyQueued.remove happens in the queueing logic
                // when the delay has passed
                
                // If using delay, break after one item to respect the delay
                if (config.transferDelayMs > 0) {
                    pendingSlots.poll();
                    break;
                }
            }
            
            // Remove from pending
            pendingSlots.poll();
        }
        
        if (DEBUG && processedThisTick > 0) {
            SimpleLootClient.LOGGER.info("[DEBUG] Processed {} items this tick, {} still pending", 
                    processedThisTick, pendingSlots.size());
        }
    }
    
    /**
     * Checks if an item is equippable armor (helmet, chestplate, leggings, boots, or elytra).
     */
    private static boolean isEquippableArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check for equippable component (1.21+ method)
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            // Only armor slots (HEAD, CHEST, LEGS, FEET), not MAINHAND or OFFHAND
            return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || 
                   slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
        }
        return false;
    }
    
    /**
     * Gets the equipment slot type for an armor item.
     * Returns null if the item is not equippable armor.
     */
    private static EquipmentSlot getArmorSlotType(ItemStack stack) {
        if (stack.isEmpty()) return null;
        
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || 
                slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                return slot;
            }
        }
        return null;
    }
    
    /**
     * Gets the inventory slot ID for a given equipment slot.
     * In InventoryScreen: 5=helmet, 6=chest, 7=legs, 8=boots
     */
    private static int getArmorSlotId(EquipmentSlot equipSlot) {
        return switch (equipSlot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }
    
    /**
     * Checks if a slot is an armor slot in the inventory screen.
     * In InventoryScreen: slots 5-8 are armor (5=helmet, 6=chest, 7=legs, 8=boots)
     */
    private static boolean isArmorSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            return slot.index >= 5 && slot.index <= 8;
        }
        return false;
    }
    
    /**
     * Finds a slot by its ID in the screen handler.
     */
    private static Slot findSlotById(AbstractContainerScreen<?> screen, int slotId) {
        var slots = screen.getMenu().slots;
        if (slotId >= 0 && slotId < slots.size()) {
            return slots.get(slotId);
        }
        return null;
    }
    
    /**
     * Gets the slot at the given screen coordinates.
     * Uses the AbstractContainerScreen's slot detection.
     */
    private static Slot getSlotAt(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        // Access the slot at coordinates through the HandledScreenAccessor mixin
        return HandledScreenAccessor.getSlotAt(screen, mouseX, mouseY);
    }
    
    /**
     * Version-aware wrapper for container slot interaction.
     * In 26.1+, the method was renamed and ClickType became ContainerInput.
     */
    private static void containerInput(MultiPlayerGameMode gameMode, int containerId, int slotId, int button,
            //? if >=26.1 {
            ContainerInput action,
            //?} else {
            /*ClickType action,*/
            //?}
            net.minecraft.world.entity.player.Player player) {
        //? if >=26.1 {
        gameMode.handleContainerInput(containerId, slotId, button, action, player);
        //?} else {
        /*gameMode.handleInventoryMouseClick(containerId, slotId, button, action, player);*/
        //?}
    }
    
    /**
     * Performs a quick-move (shift-click) on the given slot.
     * This naturally handles bidirectional transfer:
     * - Container slot → Player inventory
     * - Player inventory slot → Container
     */
    private static void performQuickMove(Minecraft client, AbstractContainerScreen<?> screen, Slot slot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.gameMode == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        MultiPlayerGameMode interactionManager = client.gameMode;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing QUICK_MOVE on slot {} with item {}", 
                slot.index, slot.getItem().getHoverName().getString());
        
        // Perform shift-click (quick move) on the slot
        containerInput(interactionManager,
                screen.getMenu().containerId,
                slot.index,
                0, // Button (0 = left click)
                SLOT_QUICK_MOVE,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Quick-moved item from slot {}", slot.index);
    }
    
    /**
     * Performs an armor swap between an inventory slot and an armor slot.
     * This picks up the inventory armor, clicks on the armor slot (which swaps), 
     * then places the old armor back in the inventory.
     */
    private static void performArmorSwap(Minecraft client, AbstractContainerScreen<?> screen, Slot sourceSlot, Slot armorSlot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.gameMode == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        MultiPlayerGameMode interactionManager = client.gameMode;
        int syncId = screen.getMenu().containerId;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing armor swap: inventory slot {} <-> armor slot {}", 
                sourceSlot.index, armorSlot.index);
        
        // Step 1: Pick up the armor from inventory slot (left-click)
        containerInput(interactionManager,
                syncId,
                sourceSlot.index,
                0, // Button (0 = left click = pick up)
                SLOT_PICKUP,
                client.player
        );
        
        // Step 2: Click on the armor slot - this will swap the held item with the worn armor
        containerInput(interactionManager,
                syncId,
                armorSlot.index,
                0, // Button (0 = left click = swap/place)
                SLOT_PICKUP,
                client.player
        );
        
        // Step 3: Put the old armor back into the original inventory slot
        containerInput(interactionManager,
                syncId,
                sourceSlot.index,
                0, // Button (0 = left click = place)
                SLOT_PICKUP,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Swapped armor between slot {} and armor slot {}", sourceSlot.index, armorSlot.index);
    }
    
    /**
     * Performs a transfer from a player inventory slot to a specific crafting grid slot.
     * This picks up the item and places it in the target crafting slot.
     */
    private static void performCraftingTransfer(Minecraft client, AbstractContainerScreen<?> screen, Slot sourceSlot, int targetSlotId) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.gameMode == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        MultiPlayerGameMode interactionManager = client.gameMode;
        int syncId = screen.getMenu().containerId;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing crafting transfer: slot {} -> crafting slot {}", 
                sourceSlot.index, targetSlotId);
        
        // Step 1: Pick up the entire stack from the source slot (left-click)
        containerInput(interactionManager,
                syncId,
                sourceSlot.index,
                0, // Button (0 = left click = pick up full stack)
                SLOT_PICKUP,
                client.player
        );
        
        // Step 2: Place the stack in the target crafting slot
        containerInput(interactionManager,
                syncId,
                targetSlotId,
                0, // Button (0 = left click = place all held items)
                SLOT_PICKUP,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Transferred item to crafting slot {} from slot {}", targetSlotId, sourceSlot.index);
    }
    
    /**
     * Performs a throw (Ctrl+Q style drop) on the given slot.
     * This drops the entire stack on the ground.
     * Works in both survival and creative mode.
     */
    private static void performThrow(Minecraft client, AbstractContainerScreen<?> screen, Slot slot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.gameMode == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        MultiPlayerGameMode interactionManager = client.gameMode;
        
        // Creative mode inventory (CreativeModeInventoryScreen) needs special handling
        // Regular containers in creative mode work with normal THROW
        boolean isCreativeInventory = screen instanceof CreativeModeInventoryScreen;
        
        // For creative inventory, slot.index is unreliable (always 0).
        // We need to find the slot's actual index in the screen handler.
        int actualSlotId = slot.index;
        if (isCreativeInventory) {
            // Search for this slot in the screen handler's slot list
            var slots = screen.getMenu().slots;
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i) == slot) {
                    actualSlotId = i;
                    break;
                }
            }
        }
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing THROW on slot {} (actualId={}) with item {}, creative={}, isCreativeInventory={}, screenClass={}", 
                slot.index, actualSlotId, slot.getItem().getHoverName().getString(), client.player.isCreative(), isCreativeInventory, screen.getClass().getSimpleName());
        
        if (isCreativeInventory) {
            // In creative inventory, we need to pick up and drop outside
            // The creative inventory uses a special screen handler
            // Step 1: Pick up the entire stack (left click)
            containerInput(interactionManager,
                    screen.getMenu().containerId,
                    actualSlotId,
                    0, // Button (0 = left click to pick up)
                    SLOT_PICKUP,
                    client.player
            );
            
            // Step 2: Click outside the inventory to drop (-999 is the "outside" slot)
            containerInput(interactionManager,
                    screen.getMenu().containerId,
                    -999, // -999 = click outside inventory = drop held item
                    0, // Button (0 = left click to drop all)
                    SLOT_PICKUP,
                    client.player
            );
            
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Used PICKUP method for creative inventory (actualSlotId={})", actualSlotId);
        } else {
            // For all other screens (including containers in creative mode), use THROW
            containerInput(interactionManager,
                    screen.getMenu().containerId,
                    actualSlotId,
                    1, // Button (1 = Ctrl modifier for THROW action = throw entire stack)
                    SLOT_THROW,
                    client.player
            );
            
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Used THROW method");
        }
        
        SimpleLootClient.LOGGER.debug("Threw item stack from slot {}", actualSlotId);
    }
    
    /**
     * Performs a throw using a pre-computed slot ID.
     * Used for creative inventory where we've already computed the actual slot ID.
     */
    private static void performThrowById(Minecraft client, AbstractContainerScreen<?> screen, int actualSlotId, ItemStack stack) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.gameMode == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        MultiPlayerGameMode interactionManager = client.gameMode;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing THROW by ID on actualSlotId={} with item {}", 
                actualSlotId, stack.getHoverName().getString());
        
        // In creative inventory, we need to pick up and drop outside
        // Step 1: Pick up the entire stack (left click)
        containerInput(interactionManager,
                screen.getMenu().containerId,
                actualSlotId,
                0, // Button (0 = left click to pick up)
                SLOT_PICKUP,
                client.player
        );
        
        // Step 2: Click outside the inventory to drop (-999 is the "outside" slot)
        containerInput(interactionManager,
                screen.getMenu().containerId,
                -999, // -999 = click outside inventory = drop held item
                0, // Button (0 = left click to drop all)
                SLOT_PICKUP,
                client.player
        );
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Used PICKUP method for creative inventory (actualSlotId={})", actualSlotId);
    }
    
    /**
     * Checks if a slot is a player inventory slot in any screen type.
     * This is a generic check that works for all container screens.
     */
    private static boolean isPlayerInventorySlotGeneric(AbstractContainerScreen<?> screen, Slot slot) {
        // Most container screens have the player inventory in the last 36 slots
        // The container slots come first, then player inventory (27 main + 9 hotbar)
        var handler = screen.getMenu();
        int totalSlots = handler.slots.size();
        
        // Player inventory is typically the last 36 slots
        // Slot IDs: totalSlots - 36 to totalSlots - 1
        int playerInvStart = totalSlots - 36;
        return slot.index >= playerInvStart;
    }
    
    /**
     * Checks if an item can be used as fuel in a furnace.
     * Note: We rely on Minecraft's quick-move behavior which already validates items.
     * The fuel slot in furnaces accepts any valid fuel - this method is kept for future use.
     */
    @SuppressWarnings("unused")
    private static boolean isFuelItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // For now, we let Minecraft's quick-move handle fuel validation
        // The slot's canInsert() method properly validates fuel items
        // This is a placeholder for more detailed validation if needed
        return true;
    }
    
    /**
     * Checks if an item is enchantable (can be placed in enchanting table slot).
     * Items that are enchantable:
     * - Items with durability (tools, weapons, armor)
     * - Books (for enchanted books)
     * - Items that already have the ENCHANTABLE component
     */
    private static boolean isEnchantableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // Check if item is a book (can become enchanted book)
        if (stack.is(Items.BOOK)) {
            return true;
        }
        
        // Check if item has max damage (durability) - this covers tools, weapons, armor
        if (stack.getMaxDamage() > 0) {
            return true;
        }
        
        // Check for enchantable component (some items may be enchantable without durability)
        // This handles edge cases like fishing rods, flint and steel, shears, etc.
        if (stack.has(DataComponents.ENCHANTABLE)) {
            return true;
        }
        
        // Check for items that can already have enchantments
        if (stack.has(DataComponents.ENCHANTMENTS)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a slot transfer should be allowed based on the screen type and slot position.
     * This validates that items are appropriate for specific slots in specialized screens.
     * 
     * @param screen The current screen
     * @param slot The slot being hovered
     * @param stack The item stack in the slot
     * @param isFromPlayerInventory True if the slot is in player inventory (transferring TO container)
     * @return True if the transfer should be allowed
     */
    private static boolean isValidSlotTransfer(AbstractContainerScreen<?> screen, Slot slot, ItemStack stack, boolean isFromPlayerInventory) {
        // For furnace-type screens, all transfers are allowed
        // Minecraft's quick-move handles putting items in the correct slots automatically
        if (screen instanceof FurnaceScreen || screen instanceof BlastFurnaceScreen || screen instanceof SmokerScreen) {
            // All transfers allowed - Minecraft will put items in correct slots
            return true;
        }
        
        // For enchanting table, validate that only enchantable items or lapis can be transferred
        if (screen instanceof EnchantmentScreen) {
            if (isFromPlayerInventory) {
                // Transferring FROM player inventory TO enchanting table
                // Only allow enchantable items or lapis lazuli
                if (!isEnchantableItem(stack) && !stack.is(Items.LAPIS_LAZULI)) {
                    return false;
                }
            }
            // Transferring FROM enchanting table slots to inventory is always allowed
            return true;
        }
        
        // For beacon, validate payment items
        if (screen instanceof BeaconScreen) {
            if (isFromPlayerInventory) {
                // Beacon accepts specific payment items: iron/gold/emerald/diamond/netherite ingot
                if (!isBeaconPaymentItem(stack)) {
                    return false;
                }
            }
            return true;
        }
        
        // All other screens - allow transfers
        return true;
    }
    
    /**
     * Checks if an item is valid for beacon payment slot.
     */
    private static boolean isBeaconPaymentItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.IRON_INGOT) || 
               stack.is(Items.GOLD_INGOT) || 
               stack.is(Items.EMERALD) || 
               stack.is(Items.DIAMOND) || 
               stack.is(Items.NETHERITE_INGOT);
    }
    
    /**
     * Resets the handler state (called when the player closes a container).
     */
    public static void reset() {
        currentlyQueued.clear();
        pendingSlots.clear();
        lastMouseX = -1;
        lastMouseY = -1;
        wasKeyPressed = false;
        isDropMode = false;
        lastScreen = null;
    }
}
