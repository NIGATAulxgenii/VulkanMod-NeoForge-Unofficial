package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MainTarget.class)
public class MainTargetMixin extends RenderTarget {
   public MainTargetMixin(boolean useDepth) {
      super(useDepth);
   }

   @Overwrite
   private void createFrameBuffer(int width, int height) {
      this.frameBufferId = 0;
      this.viewWidth = width;
      this.viewHeight = height;
      this.width = width;
      this.height = height;
   }

   public void bindWrite(boolean updateScissor) {
      Renderer.getInstance().getMainPass().rebindMainTarget();
   }

   public void bindRead() {
      Renderer.getInstance().getMainPass().bindAsTexture();
   }

   public int getColorTextureId() {
      return Renderer.getInstance().getMainPass().getColorAttachment().id;
   }
}
