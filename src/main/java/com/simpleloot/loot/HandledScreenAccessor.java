package com.simpleloot.loot;

import com.simpleloot.mixin.HandledScreenMixin;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

/**
 * Accessor interface for HandledScreen protected methods.
 * This provides access to the getSlotAt method through our mixin.
 */
public class HandledScreenAccessor {
    
    /**
     * Gets the slot at the given screen coordinates.
     * 
     * @param screen The handled screen
     * @param x Mouse X coordinate
     * @param y Mouse Y coordinate
     * @return The slot at the coordinates, or null if none
     */
    public static Slot getSlotAt(HandledScreen<?> screen, double x, double y) {
        return ((HandledScreenMixin) screen).invokeGetSlotAt(x, y);
    }
}
