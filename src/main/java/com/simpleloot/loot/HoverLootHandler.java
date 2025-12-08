package com.simpleloot.loot;

import com.simpleloot.SimpleLootClient;
import com.simpleloot.config.SimpleLootConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

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
    
    // Track the last screen we were in
    private static HandledScreen<?> lastScreen = null;
    
    // Track last mouse position for interpolation
    private static double lastMouseX = -1;
    private static double lastMouseY = -1;
    
    // Track if key was pressed last tick (for detecting release)
    private static boolean wasKeyPressed = false;
    
    // Last time an item was transferred (for rate limiting / visual delay)
    private static long lastTransferTime = 0;
    
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
                        lastMouseX = -1;
                        lastMouseY = -1;
                        wasKeyPressed = false;
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
                    lastMouseX = -1;
                    lastMouseY = -1;
                    wasKeyPressed = false;
                    lastScreen = null;
                }
            }
        });
        
        SimpleLootClient.LOGGER.info("HoverLootHandler initialized");
    }
    
    /**
     * Checks if the given screen is a supported container screen.
     */
    private static boolean isSupportedScreen(net.minecraft.client.gui.screen.Screen screen) {
        if (!(screen instanceof HandledScreen<?>)) {
            return false;
        }
        
        SimpleLootConfig config = SimpleLootConfig.getInstance();
        
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
            if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Hover loot deactivated, slots can now be re-selected");
        }
        wasKeyPressed = hoverLootActive;
        
        if (hoverLootActive) {
            // Hover loot is active - detect slots and add to queue
            // Use interpolation to catch slots we moved over quickly
            List<Slot> slotsToQueue = getSlotsAlongPath(screen, lastMouseX, lastMouseY, mouseX, mouseY);
            
            for (Slot slot : slotsToQueue) {
                if (slot != null && slot.hasStack()) {
                    int slotId = slot.id;
                    
                    // Check hotbar protection
                    if (config.hotbarProtection && isHotbarSlot(screen.getScreenHandler(), slot)) {
                        continue;
                    }
                    
                    // Add to queue if not already queued in THIS session
                    // (prevents adding same slot multiple times while active)
                    if (!currentlyQueued.contains(slotId)) {
                        pendingSlots.add(slotId);
                        currentlyQueued.add(slotId);
                        if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Queued slot {} with item: {}", 
                                slotId, slot.getStack().getName().getString());
                    }
                }
            }
        }
        
        // ALWAYS process the queue (even if key is released - like Rust!)
        // Only closing the container stops the queue
        if (!pendingSlots.isEmpty()) {
            processQueue(client, screen, config, DEBUG);
        }
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
     * Gets all slots along the mouse path from last position to current position.
     * This catches slots we might have "skipped over" when moving fast.
     */
    private static List<Slot> getSlotsAlongPath(HandledScreen<?> screen, double fromX, double fromY, double toX, double toY) {
        List<Slot> slots = new ArrayList<>();
        
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
        
        // Sample every ~4 pixels along the path (slot size is ~16-18 pixels)
        int samples = Math.max(1, (int) (distance / 4));
        
        Set<Integer> foundSlotIds = new HashSet<>();
        
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            double x = fromX + dx * t;
            double y = fromY + dy * t;
            
            Slot slot = getSlotAt(screen, x, y);
            if (slot != null && !foundSlotIds.contains(slot.id)) {
                slots.add(slot);
                foundSlotIds.add(slot.id);
            }
        }
        
        return slots;
    }
    
    /**
     * Processes the pending transfer queue.
     * Respects the transfer delay setting for visual effect.
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
        
        // Process items - if delay is 0, process many per tick for instant transfer
        // If delay > 0, process just one per delay period for visual effect
        int maxPerTick = (config.transferDelayMs == 0) ? 20 : 1;
        int processedThisTick = 0;
        
        while (!pendingSlots.isEmpty() && processedThisTick < maxPerTick) {
            Integer nextSlotId = pendingSlots.peek();
            if (nextSlotId == null) break;
            
            // Find the slot by ID
            Slot slotToTransfer = findSlotById(screen, nextSlotId);
            
            if (slotToTransfer != null && slotToTransfer.hasStack()) {
                // Perform the transfer
                performQuickMove(client, screen, slotToTransfer);
                processedThisTick++;
                lastTransferTime = System.currentTimeMillis();
                
                if (DEBUG) SimpleLootClient.LOGGER.info("[DEBUG] Transferred slot {} ({} pending remaining)", 
                        nextSlotId, pendingSlots.size() - 1);
                
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
     * Resets the handler state (called when the player closes a container).
     */
    public static void reset() {
        currentlyQueued.clear();
        pendingSlots.clear();
        lastMouseX = -1;
        lastMouseY = -1;
        wasKeyPressed = false;
        lastScreen = null;
    }
}
