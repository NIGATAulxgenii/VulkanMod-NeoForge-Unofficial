package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.NativeImage.InternalGlFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TextureUtil.class)
public class MTextureUtil {
   @Overwrite(remap = false)
   public static int generateTextureId() {
      RenderSystem.assertOnRenderThreadOrInit();
      return VkGlTexture.genTextureId();
   }

   @Overwrite(remap = false)
   public static void prepareImage(InternalGlFormat internalGlFormat, int id, int mipLevels, int width, int height) {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlTexture.bindTexture(id);
      VkGlTexture glTexture = VkGlTexture.getBoundTexture();
      VulkanImage image = glTexture.getVulkanImage();
      if (mipLevels > 0) {
         GlStateManager._texParameter(3553, 33085, mipLevels);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, mipLevels);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      if (image == null || image.mipLevels != mipLevels || image.width != width || image.height != height) {
         if (image != null) {
            image.free();
         }

         image = new VulkanImage.Builder(width, height)
            .setName(String.format("Texture %d", id))
            .setMipLevels(mipLevels + 1)
            .setFormat(convertFormat(internalGlFormat))
            .setLinearFiltering(false)
            .setClamp(false)
            .createVulkanImage();
         glTexture.setVulkanImage(image);
         VTextureSelector.bindTexture(image);
      }
   }

   @Unique
   private static int convertFormat(InternalGlFormat format) {
      return switch (format) {
         case RGBA -> 37;
         case RED -> 9;
         default -> throw new IllegalArgumentException(String.format("Unxepcted format: %s", format));
      };
   }
}
