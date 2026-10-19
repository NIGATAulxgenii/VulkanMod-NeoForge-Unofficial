package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public abstract class ViewAreaM {
   @Shadow
   public RenderSection[] sections;

   @Shadow
   protected abstract void setViewDistance(int var1);

   @Inject(method = "createSections", at = @At("HEAD"))
   private void skipAllocation(SectionRenderDispatcher sectionRenderDispatcher, CallbackInfo ci) {
      this.setViewDistance(0);
   }
}
