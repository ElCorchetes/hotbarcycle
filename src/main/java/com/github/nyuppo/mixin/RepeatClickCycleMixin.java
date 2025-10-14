package com.github.nyuppo.mixin;

import com.github.nyuppo.HotbarCycleClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class RepeatClickCycleMixin {
    @Final
    @Mutable
    @Shadow
    public GameOptions options;

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void repeatClickCycleMixin(CallbackInfo ci) {
        if (player == null || player.getInventory() == null) return;

        int selectedSlot = player.getInventory().getSelectedSlot(); // ✅ nuevo método público

        if (HotbarCycleClient.getConfig().getRepeatSlotToCycle() &&
            this.options.hotbarKeys[selectedSlot].wasPressed()) {

            HotbarCycleClient.shiftSingle(
                (MinecraftClient)(Object)this,
                selectedSlot,
                HotbarCycleClient.Direction.DOWN
            );
        }
    }
}
