package net.vulkanmod.vulkan.pass;

import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface MainPass {
   void begin(VkCommandBuffer var1, MemoryStack var2);

   void end(VkCommandBuffer var1);

   void cleanUp();

   void onResize();

   default void mainTargetBindWrite() {
   }

   default void mainTargetUnbindWrite() {
   }

   default void rebindMainTarget() {
   }

   default void bindAsTexture() {
   }

   default Framebuffer getMainFramebuffer() {
      return null;
   }

   default VkGlTexture getColorAttachment() {
      return null;
   }

   default VulkanImage getDepthAttachment() {
      return null;
   }
}
