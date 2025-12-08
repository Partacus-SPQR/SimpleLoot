package com.simpleloot.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Mixin to access protected methods in HandledScreen.
 * Specifically provides access to getSlotAt for hover detection.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenMixin {
    
    /**
     * Invokes the protected getSlotAt method.
     * 
     * @param x Mouse X coordinate
     * @param y Mouse Y coordinate
     * @return The slot at the coordinates, or null if none
     */
    @Invoker("getSlotAt")
    Slot invokeGetSlotAt(double x, double y);
}
