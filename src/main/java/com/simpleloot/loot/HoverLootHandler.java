package com.simpleloot.loot;

import com.simpleloot.SimpleLootClient;
import com.simpleloot.config.SimpleLootConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
    private static HandledScreen<?> lastScreen = null;
    
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
            if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
                if (isSupportedScreen(client.currentScreen)) {
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
                    double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
                    double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
                    
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
     * - CreativeInventoryScreen for drop mode
     * - CraftingScreen (crafting table) for crafting grid transfers
     */
    private static boolean isSupportedScreen(net.minecraft.client.gui.screen.Screen screen) {
        if (!(screen instanceof HandledScreen<?>)) {
            return false;
        }
        
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
        // InventoryScreen - always allow for crafting grid transfers (if enabled) or drop mode
        if (screen instanceof InventoryScreen) {
            return config.allowHoverDrop || config.allowCraftingGrid;
        }
        
        // CreativeInventoryScreen - only for drop mode
        if (screen instanceof CreativeInventoryScreen) {
            return config.allowHoverDrop;
        }
        
        // CraftingScreen (crafting table) - allow for crafting grid transfers
        if (screen instanceof CraftingScreen) {
            return config.allowCraftingGrid;
        }
        
        // Check each container type against config
        if (screen instanceof GenericContainerScreen) {
            GenericContainerScreen containerScreen = (GenericContainerScreen) screen;
            int rows = containerScreen.getScreenHandler().getRows();
            // Single chest = 3 rows, Double chest = 6 rows
            if (rows <= 3 && !config.allowChests) return false;
            if (rows > 3 && !config.allowDoubleChests) return false;
            return true;
        }
        
        if (screen instanceof ShulkerBoxScreen) {
            return config.allowShulkerBoxes;
        }
        
        if (screen instanceof Generic3x3ContainerScreen) {
            // This handles dispensers and droppers
            return config.allowDispensers || config.allowDroppers;
        }
        
        if (screen instanceof HopperScreen) {
            return config.allowHoppers;
        }
        
        // Ender chest uses GenericContainerScreen, but we can check by title or other means
        // For now, GenericContainerScreen covers ender chests as well
        
        // Barrel also uses GenericContainerScreen (3 rows)
        
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
    private static void handleHoverLoot(MinecraftClient client, HandledScreen<?> screen, double mouseX, double mouseY) {
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
        long windowHandle = client.getWindow().getHandle();
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
        boolean isCreativeInventory = screen instanceof CreativeInventoryScreen;
        
        // Check if creative inventory is on the survival inventory tab (the only tab where drop should work)
        boolean isCreativeSurvivalTab = false;
        if (isCreativeInventory) {
            CreativeInventoryScreen creativeScreen = (CreativeInventoryScreen) screen;
            // Use Fabric API to get the currently selected item group
            // The survival inventory tab is identified by getting its registry key and comparing to ItemGroups.INVENTORY
            if (creativeScreen instanceof FabricCreativeInventoryScreen fabricScreen) {
                ItemGroup selectedGroup = fabricScreen.getSelectedItemGroup();
                // Get the registry key of the selected group and compare to INVENTORY key
                if (selectedGroup != null) {
                    var selectedKey = Registries.ITEM_GROUP.getKey(selectedGroup);
                    isCreativeSurvivalTab = selectedKey.isPresent() && selectedKey.get().equals(ItemGroups.INVENTORY);
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
                if (slot != null && slot.hasStack()) {
                    // Check hotbar protection (only for non-crafting operations)
                    if (config.hotbarProtection && !isCraftingSlot(screen, slot) && isHotbarSlot(screen.getScreenHandler(), slot)) {
                        continue;
                    }
                    
                    // For creative inventory survival tab, use the creative queue
                    // We queue items and process them one at a time to ensure proper game state updates
                    if (isCreativeInventory && isDropMode && isCreativeSurvivalTab) {
                        // Find the actual slot ID
                        int actualSlotId = findActualSlotId(screen, slot);
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Creative slot detection: slot.id={}, actualSlotId={}, item={}, alreadyQueued={}", 
                                slot.id, actualSlotId, slot.getStack().getName().getString(), creativeQueuedSlotIds.contains(actualSlotId));
                        if (actualSlotId >= 0 && !creativeQueuedSlotIds.contains(actualSlotId)) {
                            // Add to queue for processing
                            creativePendingSlotIds.add(actualSlotId);
                            creativeQueuedSlotIds.add(actualSlotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued creative survival slot actualId={} with item: {}", 
                                    actualSlotId, slot.getStack().getName().getString());
                        }
                    } else if (isCreativeInventory) {
                        // Non-survival tab creative - queue for later (though this shouldn't happen due to earlier checks)
                        int actualSlotId = findActualSlotId(screen, slot);
                        if (actualSlotId >= 0 && !creativeQueuedSlotIds.contains(actualSlotId)) {
                            creativePendingSlotIds.add(actualSlotId);
                            creativeQueuedSlotIds.add(actualSlotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued creative slot actualId={} with item: {}", 
                                    actualSlotId, slot.getStack().getName().getString());
                        }
                    } else {
                        int slotId = slot.id;
                        // Add to queue if not already queued in THIS session
                        // (prevents adding same slot multiple times while active)
                        if (!currentlyQueued.contains(slotId)) {
                            // For armor equippable items, check if this slot was recently involved in an armor swap
                            // This prevents rapid re-swapping when the cursor stays on the same slot
                            if (isEquippableArmor(slot.getStack())) {
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
                            
                            pendingSlots.add(slotId);
                            currentlyQueued.add(slotId);
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued slot {} with item: {}", 
                                    slotId, slot.getStack().getName().getString());
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
     * This is needed for creative inventory where slot.id is unreliable.
     */
    private static int findActualSlotId(HandledScreen<?> screen, Slot slot) {
        var slots = screen.getScreenHandler().slots;
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
    private static boolean isHotbarSlot(net.minecraft.screen.ScreenHandler handler, Slot slot) {
        int containerSlotCount = handler.slots.size() - 36;
        int playerSlotIndex = slot.id - containerSlotCount;
        // Hotbar is the last 9 slots of player inventory (indices 27-35)
        return playerSlotIndex >= 27 && playerSlotIndex < 36;
    }
    
    /**
     * Checks if a slot is a crafting input slot (2x2 in inventory, 3x3 in crafting table).
     */
    private static boolean isCraftingSlot(HandledScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            // Player inventory crafting grid: slots 1-4 (slot 0 is output)
            return slot.id >= 1 && slot.id <= 4;
        }
        if (screen instanceof CraftingScreen) {
            // Crafting table: slots 1-9 (slot 0 is output)
            return slot.id >= 1 && slot.id <= 9;
        }
        return false;
    }
    
    /**
     * Checks if a slot is the crafting output slot.
     */
    private static boolean isCraftingOutputSlot(HandledScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen || screen instanceof CraftingScreen) {
            // Output is always slot 0 in crafting screens
            return slot.id == 0;
        }
        return false;
    }
    
    /**
     * Gets the first available crafting input slot index.
     * Returns -1 if all slots are full.
     */
    private static int getAvailableCraftingSlot(HandledScreen<?> screen) {
        var handler = screen.getScreenHandler();
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
            if (!slot.hasStack()) {
                return i;
            }
        }
        return -1; // All slots full
    }
    
    /**
     * Checks if a slot is a player inventory slot (not crafting, not armor, not offhand).
     * In player inventory screen, main inventory is slots 9-35 (excluding armor 5-8 and offhand 45).
     */
    private static boolean isPlayerInventorySlot(HandledScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            // Player inventory: main inventory is 9-35, hotbar would be 36-44 but in this screen
            // Layout: 0=output, 1-4=crafting, 5-8=armor, 9-35=main inventory, 36-44=hotbar, 45=offhand
            return slot.id >= 9 && slot.id <= 44;
        }
        if (screen instanceof CraftingScreen) {
            // Crafting table: 0=output, 1-9=crafting grid, 10-36=main inventory, 37-45=hotbar
            return slot.id >= 10 && slot.id <= 45;
        }
        return false;
    }
    
    /**
     * Gets all slots along the mouse path from last position to current position.
     * This catches slots we might have "skipped over" when moving fast.
     */
    private static List<Slot> getSlotsAlongPath(HandledScreen<?> screen, double fromX, double fromY, double toX, double toY) {
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
                int slotIdForDedup = slot.id;
                if (screen instanceof CreativeInventoryScreen) {
                    int actualId = findActualSlotId(screen, slot);
                    if (actualId >= 0) {
                        slotIdForDedup = actualId;
                    }
                }
                
                if (!foundSlotIds.contains(slotIdForDedup)) {
                    slots.add(slot);
                    foundSlotIds.add(slotIdForDedup);
                    if (DEBUG && screen instanceof CreativeInventoryScreen) {
                        SimpleLootClient.LOGGER.info("[DEBUG] Path found slot: slot.id={}, actualId={}, hasStack={}, item={}", 
                                slot.id, slotIdForDedup, slot.hasStack(), slot.hasStack() ? slot.getStack().getName().getString() : "empty");
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
    private static void processQueue(MinecraftClient client, HandledScreen<?> screen, SimpleLootConfig config, boolean DEBUG) {
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
        boolean isCreativeInventory = screen instanceof CreativeInventoryScreen;
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
                    
                    if (slotToTransfer != null && slotToTransfer.hasStack()) {
                        if (isDropMode) {
                            // Drop mode: Drop items on ground using the actual slot ID
                            performThrowById(client, screen, actualSlotId, slotToTransfer.getStack());
                            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Dropped creative slot actualId={} with item: {}", 
                                    actualSlotId, slotToTransfer.getStack().getName().getString());
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
            
            if (slotToTransfer != null && slotToTransfer.hasStack()) {
                boolean handled = false;
                ItemStack stack = slotToTransfer.getStack();
                
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
                            
                            if (targetArmorSlot != null && targetArmorSlot.hasStack()) {
                                // Armor slot has something - need to SWAP
                                // Use pickup on source, then pickup on target (swaps), then pickup to place back
                                performArmorSwap(client, screen, slotToTransfer, targetArmorSlot);
                                handled = true;
                                armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this slot
                                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Swapped armor from slot {} to slot {}: {}", 
                                        nextSlotId, targetArmorSlotId, stack.getName().getString());
                            } else {
                                // Armor slot is empty - use quick move (shift-click) to equip
                                performQuickMove(client, screen, slotToTransfer);
                                handled = true;
                                armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this slot
                                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Equipped armor from slot {}: {}", nextSlotId, stack.getName().getString());
                            }
                        }
                    } else if (isArmorSlot(screen, slotToTransfer)) {
                        // Hovering over worn armor - unequip it
                        performQuickMove(client, screen, slotToTransfer);
                        handled = true;
                        armorSwapTimes.put(nextSlotId, System.currentTimeMillis()); // Track swap time for this armor slot
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Unequipped armor from slot {}: {}", nextSlotId, stack.getName().getString());
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
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
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
        
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
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
    private static boolean isArmorSlot(HandledScreen<?> screen, Slot slot) {
        if (screen instanceof InventoryScreen) {
            return slot.id >= 5 && slot.id <= 8;
        }
        return false;
    }
    
    /**
     * Finds a slot by its ID in the screen handler.
     */
    private static Slot findSlotById(HandledScreen<?> screen, int slotId) {
        var slots = screen.getScreenHandler().slots;
        if (slotId >= 0 && slotId < slots.size()) {
            return slots.get(slotId);
        }
        return null;
    }
    
    /**
     * Gets the slot at the given screen coordinates.
     * Uses the HandledScreen's slot detection.
     */
    private static Slot getSlotAt(HandledScreen<?> screen, double mouseX, double mouseY) {
        // Access the slot at coordinates through the HandledScreenAccessor mixin
        return HandledScreenAccessor.getSlotAt(screen, mouseX, mouseY);
    }
    
    /**
     * Performs a quick-move (shift-click) on the given slot.
     * This naturally handles bidirectional transfer:
     * - Container slot → Player inventory
     * - Player inventory slot → Container
     */
    private static void performQuickMove(MinecraftClient client, HandledScreen<?> screen, Slot slot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.interactionManager == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing QUICK_MOVE on slot {} with item {}", 
                slot.id, slot.getStack().getName().getString());
        
        // Perform shift-click (quick move) on the slot
        interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slot.id,
                0, // Button (0 = left click)
                SlotActionType.QUICK_MOVE,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Quick-moved item from slot {}", slot.id);
    }
    
    /**
     * Performs an armor swap between an inventory slot and an armor slot.
     * This picks up the inventory armor, clicks on the armor slot (which swaps), 
     * then places the old armor back in the inventory.
     */
    private static void performArmorSwap(MinecraftClient client, HandledScreen<?> screen, Slot sourceSlot, Slot armorSlot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.interactionManager == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        int syncId = screen.getScreenHandler().syncId;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing armor swap: inventory slot {} <-> armor slot {}", 
                sourceSlot.id, armorSlot.id);
        
        // Step 1: Pick up the armor from inventory slot (left-click)
        interactionManager.clickSlot(
                syncId,
                sourceSlot.id,
                0, // Button (0 = left click = pick up)
                SlotActionType.PICKUP,
                client.player
        );
        
        // Step 2: Click on the armor slot - this will swap the held item with the worn armor
        interactionManager.clickSlot(
                syncId,
                armorSlot.id,
                0, // Button (0 = left click = swap/place)
                SlotActionType.PICKUP,
                client.player
        );
        
        // Step 3: Put the old armor back into the original inventory slot
        interactionManager.clickSlot(
                syncId,
                sourceSlot.id,
                0, // Button (0 = left click = place)
                SlotActionType.PICKUP,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Swapped armor between slot {} and armor slot {}", sourceSlot.id, armorSlot.id);
    }
    
    /**
     * Performs a transfer from a player inventory slot to a specific crafting grid slot.
     * This picks up the item and places it in the target crafting slot.
     */
    private static void performCraftingTransfer(MinecraftClient client, HandledScreen<?> screen, Slot sourceSlot, int targetSlotId) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.interactionManager == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        int syncId = screen.getScreenHandler().syncId;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing crafting transfer: slot {} -> crafting slot {}", 
                sourceSlot.id, targetSlotId);
        
        // Step 1: Pick up the entire stack from the source slot (left-click)
        interactionManager.clickSlot(
                syncId,
                sourceSlot.id,
                0, // Button (0 = left click = pick up full stack)
                SlotActionType.PICKUP,
                client.player
        );
        
        // Step 2: Place the stack in the target crafting slot
        interactionManager.clickSlot(
                syncId,
                targetSlotId,
                0, // Button (0 = left click = place all held items)
                SlotActionType.PICKUP,
                client.player
        );
        
        SimpleLootClient.LOGGER.debug("Transferred item to crafting slot {} from slot {}", targetSlotId, sourceSlot.id);
    }
    
    /**
     * Performs a throw (Ctrl+Q style drop) on the given slot.
     * This drops the entire stack on the ground.
     * Works in both survival and creative mode.
     */
    private static void performThrow(MinecraftClient client, HandledScreen<?> screen, Slot slot) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.interactionManager == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        
        // Creative mode inventory (CreativeInventoryScreen) needs special handling
        // Regular containers in creative mode work with normal THROW
        boolean isCreativeInventory = screen instanceof CreativeInventoryScreen;
        
        // For creative inventory, slot.id is unreliable (always 0).
        // We need to find the slot's actual index in the screen handler.
        int actualSlotId = slot.id;
        if (isCreativeInventory) {
            // Search for this slot in the screen handler's slot list
            var slots = screen.getScreenHandler().slots;
            for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i) == slot) {
                    actualSlotId = i;
                    break;
                }
            }
        }
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing THROW on slot {} (actualId={}) with item {}, creative={}, isCreativeInventory={}, screenClass={}", 
                slot.id, actualSlotId, slot.getStack().getName().getString(), client.player.isCreative(), isCreativeInventory, screen.getClass().getSimpleName());
        
        if (isCreativeInventory) {
            // In creative inventory, we need to pick up and drop outside
            // The creative inventory uses a special screen handler
            // Step 1: Pick up the entire stack (left click)
            interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    actualSlotId,
                    0, // Button (0 = left click to pick up)
                    SlotActionType.PICKUP,
                    client.player
            );
            
            // Step 2: Click outside the inventory to drop (-999 is the "outside" slot)
            interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    -999, // -999 = click outside inventory = drop held item
                    0, // Button (0 = left click to drop all)
                    SlotActionType.PICKUP,
                    client.player
            );
            
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Used PICKUP method for creative inventory (actualSlotId={})", actualSlotId);
        } else {
            // For all other screens (including containers in creative mode), use THROW
            interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    actualSlotId,
                    1, // Button (1 = Ctrl modifier for THROW action = throw entire stack)
                    SlotActionType.THROW,
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
    private static void performThrowById(MinecraftClient client, HandledScreen<?> screen, int actualSlotId, ItemStack stack) {
        boolean DEBUG = SimpleLootConfig.getInstance().debugMode;
        
        if (client.interactionManager == null || client.player == null) {
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] interactionManager or player is null");
            return;
        }
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Performing THROW by ID on actualSlotId={} with item {}", 
                actualSlotId, stack.getName().getString());
        
        // In creative inventory, we need to pick up and drop outside
        // Step 1: Pick up the entire stack (left click)
        interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                actualSlotId,
                0, // Button (0 = left click to pick up)
                SlotActionType.PICKUP,
                client.player
        );
        
        // Step 2: Click outside the inventory to drop (-999 is the "outside" slot)
        interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                -999, // -999 = click outside inventory = drop held item
                0, // Button (0 = left click to drop all)
                SlotActionType.PICKUP,
                client.player
        );
        
        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Used PICKUP method for creative inventory (actualSlotId={})", actualSlotId);
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
