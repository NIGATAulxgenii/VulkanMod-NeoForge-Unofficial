package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.profiling.BuildTimeProfiler;
import net.vulkanmod.render.profiling.ProfilerOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerM {
   @Inject(
      method = "keyPress",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/KeyMapping;set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
         ordinal = 0,
         shift = Shift.AFTER
      )
   )
   private void injOverlayToggle(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
      if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342)) {
         switch (key) {
            case 297:
               ProfilerOverlay.toggle();
               break;
            case 299:
               BuildTimeProfiler.startBench();
         }
      } else if (ProfilerOverlay.shouldRender) {
         ProfilerOverlay.onKeyPress(key);
      }
   }
}
