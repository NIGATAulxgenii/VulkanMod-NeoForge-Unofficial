package net.vulkanmod.mixin.window;

import net.minecraft.client.KeyboardHandler;
import net.vulkanmod.config.InactivityLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardActivityMixin {
    @Inject(method = {"keyPress", "charTyped"}, at = @At("HEAD"))
    private void recordActivity(CallbackInfo ci) {
        InactivityLimit.recordInput();
    }
}
