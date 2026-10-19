package net.vulkanmod.mixin.screen;

import net.minecraft.client.gui.screens.Screen;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenM {
   @Inject(method = "renderBlurredBackground", at = @At("RETURN"))
   private void clearDepth(float f, CallbackInfo ci) {
      Renderer.clearAttachments(256);
   }
}
