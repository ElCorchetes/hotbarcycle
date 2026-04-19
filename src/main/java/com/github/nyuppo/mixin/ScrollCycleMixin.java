package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class ScrollCycleMixin {
    @WrapOperation(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"),
            require = 0
    )
    private int hotbarcycleScrollInHotbar(double scrollAmount, int selectedSlot, int hotbarSize, Operation<Integer> original) {
        if (scrollAmount == 0.0D) {
            return original.call(scrollAmount, selectedSlot, hotbarSize);
        }

        if (!HotbarCycleClient.getConfig().getHoldAndScroll()) {
            return original.call(scrollAmount, selectedSlot, hotbarSize);
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (HotbarCycleClient.getCycleKeyBinding() == null || HotbarCycleClient.getSingleCycleKeyBinding() == null) {
            return original.call(scrollAmount, selectedSlot, hotbarSize);
        }

        final HotbarCycleClient.Direction direction = scrollAmount < 0.0D
                ? HotbarCycleClient.Direction.UP
                : HotbarCycleClient.Direction.DOWN;

        if (HotbarCycleClient.getCycleKeyBinding().isDown()) {
            HotbarCycleClient.shiftRows(minecraft, direction);
            return selectedSlot;
        }

        if (HotbarCycleClient.getSingleCycleKeyBinding().isDown()) {
            HotbarCycleClient.shiftSingle(minecraft, selectedSlot, direction);
            return selectedSlot;
        }

        return original.call(scrollAmount, selectedSlot, hotbarSize);
    }
}
