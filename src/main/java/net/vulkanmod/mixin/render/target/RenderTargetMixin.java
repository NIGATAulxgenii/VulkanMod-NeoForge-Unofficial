package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
   @Shadow
   public int viewWidth;
   @Shadow
   public int viewHeight;
   @Shadow
   public int width;
   @Shadow
   public int height;
   @Shadow
   protected int depthBufferId;
   @Shadow
   protected int colorTextureId;
   @Shadow
   public int frameBufferId;
   @Shadow
   @Final
   private float[] clearChannels;
   @Shadow
   @Final
   public boolean useDepth;
   boolean needClear = false;
   boolean bound = false;

   @Overwrite
   public void clear(boolean getError) {
      RenderSystem.assertOnRenderThreadOrInit();
      Renderer renderer = Renderer.getInstance();
      if (Renderer.isRecording()) {
         VkGlFramebuffer glFramebuffer = VkGlFramebuffer.getFramebuffer(this.frameBufferId);
         if (this.bound && VkGlFramebuffer.getBoundFramebuffer() == glFramebuffer) {
            GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
            int i = 16384;
            if (this.useDepth) {
               GlStateManager._clearDepth(1.0);
               i |= 256;
            }

            GlStateManager._clear(i, getError);
            this.needClear = false;
         } else {
            this.needClear = true;
         }
      }
   }

   @Overwrite
   public void bindRead() {
      RenderSystem.assertOnRenderThread();
      this.applyClear();
      VkGlTexture.bindTexture(this.colorTextureId);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkGlTexture.getBoundTexture().getVulkanImage().readOnlyLayout(stack, Renderer.getCommandBuffer());
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Overwrite
   public void unbindRead() {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlTexture.bindTexture(0);
   }

   @Overwrite
   private void _bindWrite(boolean bl) {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlFramebuffer.bindFramebuffer(36160, this.frameBufferId);
      if (bl) {
         GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
      }

      this.bound = true;
      if (this.needClear) {
         this.clear(false);
      }
   }

   @Overwrite
   public void unbindWrite() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            GlStateManager._glBindFramebuffer(36160, 0);
            this.bound = false;
         });
      } else {
         GlStateManager._glBindFramebuffer(36160, 0);
         this.bound = false;
      }
   }

   @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
   private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {
      if (!this.needClear) {
         Framebuffer framebuffer = VkGlFramebuffer.getFramebuffer(this.frameBufferId).getFramebuffer();
         VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());
         DrawUtil.blitToScreen();
      }

      ci.cancel();
   }

   @Inject(method = "getColorTextureId", at = @At("HEAD"))
   private void injClear(CallbackInfoReturnable<Integer> cir) {
      this.applyClear();
   }

   @Unique
   private void applyClear() {
      if (this.needClear) {
         VkGlFramebuffer currentFramebuffer = VkGlFramebuffer.getBoundFramebuffer();
         this._bindWrite(false);
         if (currentFramebuffer != null) {
            VkGlFramebuffer.beginRendering(currentFramebuffer);
         }
      }
   }
}
