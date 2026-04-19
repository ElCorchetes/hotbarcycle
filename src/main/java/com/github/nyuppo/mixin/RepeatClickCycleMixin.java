package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class RepeatClickCycleMixin {
    @Final
    @Shadow
    private Options options;

    @Shadow
    @Nullable
    private LocalPlayer player;

    @Inject(method = "handleKeybinds", at = @At("HEAD"), require = 0)
    private void repeatClickCycleMixin(CallbackInfo ci) {
        final LocalPlayer localPlayer = this.player;
        if (localPlayer == null) {
            return;
        }

        final int selectedSlot = localPlayer.getInventory().getSelectedSlot();
        if (selectedSlot < 0 || selectedSlot >= this.options.keyHotbarSlots.length) {
            return;
        }

        if (HotbarCycleClient.getConfig().getRepeatSlotToCycle()
                && this.options.keyHotbarSlots[selectedSlot].consumeClick()) {

            HotbarCycleClient.shiftSingle(
                    (Minecraft) (Object) this,
                    selectedSlot,
                    HotbarCycleClient.Direction.DOWN
            );
        }
    }
}
