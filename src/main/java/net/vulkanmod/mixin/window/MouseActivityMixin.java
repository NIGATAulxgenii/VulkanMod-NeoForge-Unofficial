package net.vulkanmod.mixin.window;

import net.minecraft.client.MouseHandler;
import net.vulkanmod.config.InactivityLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseActivityMixin {
    @Inject(method = {"onMove", "onPress", "onScroll"}, at = @At("HEAD"))
    private void recordActivity(CallbackInfo ci) {
        InactivityLimit.recordInput();
    }
}
