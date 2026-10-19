package net.vulkanmod.mixin.texture.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Locale;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NativeImage.class)
public abstract class MNativeImage {
   @Shadow
   private long pixels;
   @Shadow
   private long size;
   @Shadow
   @Final
   private Format format;
   @Shadow
   @Final
   private int width;
   @Shadow
   @Final
   private int height;
   private ByteBuffer buffer;

   @Shadow
   public abstract void close();

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract int getHeight();

   @Shadow
   public abstract void setPixelRGBA(int var1, int var2, int var3);

   @Shadow
   public abstract int getPixelRGBA(int var1, int var2);

   @Shadow
   protected abstract void checkAllocated();

   @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZ)V", at = @At("RETURN"))
   private void constr(Format format, int width, int height, boolean useStb, CallbackInfo ci) {
      if (this.pixels != 0L) {
         this.buffer = MemoryUtil.memByteBuffer(this.pixels, (int)this.size);
      }
   }

   @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
   private void constr(Format format, int width, int height, boolean useStb, long pixels, CallbackInfo ci) {
      if (this.pixels != 0L) {
         this.buffer = MemoryUtil.memByteBuffer(this.pixels, (int)this.size);
      }
   }

   @Overwrite
   private void _upload(
      int level,
      int xOffset,
      int yOffset,
      int unpackSkipPixels,
      int unpackSkipRows,
      int widthIn,
      int heightIn,
      boolean blur,
      boolean clamp,
      boolean mipmap,
      boolean autoClose
   ) {
      RenderSystem.assertOnRenderThreadOrInit();
      VTextureSelector.uploadSubTexture(level, widthIn, heightIn, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, this.getWidth(), this.buffer);
      long sampler = SamplerManager.getSampler(clamp, blur, 100);
      VTextureSelector.getBoundTexture().setSampler(sampler);
      if (autoClose) {
         this.close();
      }
   }

   @Overwrite
   public void downloadTexture(int level, boolean removeAlpha) {
      RenderSystem.assertOnRenderThread();
      ImageUtil.downloadTexture(VTextureSelector.getBoundTexture(0), this.pixels);
      if (removeAlpha && this.format.hasAlpha()) {
         if (this.format != Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
         }

         VkGlTexture colorAttachment = Renderer.getInstance().getMainPass().getColorAttachment();
         boolean isBGRAFormat = colorAttachment.getVulkanImage().format == 44;

         for (long l = 0L; l < this.width * this.height * 4L; l += 4L) {
            int v = MemoryUtil.memGetInt(this.pixels + l);
            if (isBGRAFormat) {
               v = ColorUtil.BGRAtoRGBA(v);
            }

            v |= 255 << this.format.alphaOffset();
            MemoryUtil.memPutInt(this.pixels + l, v);
         }
      }
   }
}
