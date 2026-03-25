package com.simpleloot.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Mixin to access private methods in AbstractContainerScreen.
// Specifically provides access to getHoveredSlot for hover detection.
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenMixin {
    
    // Invokes the private getHoveredSlot method.
    @Invoker("getHoveredSlot")
    Slot invokeGetSlotAt(double x, double y);
}
