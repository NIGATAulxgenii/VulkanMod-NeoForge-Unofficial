package net.vulkanmod.mixin.texture.update;

import net.minecraft.client.renderer.texture.SpriteContents.AnimatedTexture;
import net.minecraft.client.renderer.texture.SpriteContents.FrameInfo;
import net.minecraft.client.renderer.texture.SpriteContents.Ticker;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ticker.class)
public class MSpriteContents {
   @Shadow
   int subFrame;
   @Shadow
   int frame;
   @Shadow
   @Final
   AnimatedTexture animationInfo;

   @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
   private void checkUpload(int i, int j, CallbackInfo ci) {
      if (!SpriteUpdateUtil.doUploadFrame()) {
         this.subFrame++;
         FrameInfo frameInfo = (FrameInfo)this.animationInfo.frames.get(this.frame);
         if (this.subFrame >= frameInfo.time) {
            this.frame = (this.frame + 1) % this.animationInfo.frames.size();
            this.subFrame = 0;
         }

         ci.cancel();
      } else {
         SpriteUpdateUtil.addTransitionedLayout(VTextureSelector.getBoundTexture());
      }
   }
}
