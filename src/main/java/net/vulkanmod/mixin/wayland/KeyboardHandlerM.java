package net.vulkanmod.mixin.wayland;

import net.minecraft.client.KeyboardHandler;
import net.vulkanmod.config.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerM {
   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void cancelCtrlCharTyped(long l, int i, int modifiers, CallbackInfo ci) {
      if (Platform.isWayLand() && (modifiers & 2) != 0) {
         ci.cancel();
      }
   }
}
