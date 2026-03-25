package com.simpleloot.loot;

import com.simpleloot.mixin.HandledScreenMixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

// Accessor for AbstractContainerScreen private methods.
// Provides access to the getHoveredSlot method through our mixin.
public class HandledScreenAccessor {
    
    // Gets the slot at the given screen coordinates.
    public static Slot getSlotAt(AbstractContainerScreen<?> screen, double x, double y) {
        return ((HandledScreenMixin) screen).invokeGetSlotAt(x, y);
    }
}
