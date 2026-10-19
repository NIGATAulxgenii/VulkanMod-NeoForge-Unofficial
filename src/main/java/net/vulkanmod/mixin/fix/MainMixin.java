package net.vulkanmod.mixin.fix;

import net.minecraft.client.main.Main;
import org.lwjgl.system.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
   @Inject(method = "main", at = @At("HEAD"), remap = false)
   private static void inj1(String[] strings, CallbackInfo ci) {
      Configuration.STACK_SIZE.set(256);
   }
}
