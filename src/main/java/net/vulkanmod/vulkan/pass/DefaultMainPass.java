package net.vulkanmod.vulkan.pass;

import java.util.List;
import java.util.function.IntSupplier;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D.Buffer;

public class DefaultMainPass implements MainPass {
   private Framebuffer mainFramebuffer;
   private RenderPass mainRenderPass;
   private RenderPass auxRenderPass;
   private VkGlTexture[] colorAttachmentTextures;
   IntSupplier imageIdxSupplier;

   public static DefaultMainPass create() {
      return new DefaultMainPass();
   }

   DefaultMainPass() {
      this.createResources();
   }

   private void createResources() {
      if (this.mainFramebuffer != null) {
         if (this.mainFramebuffer != Renderer.getInstance().getSwapChain()) {
            this.mainFramebuffer.cleanUp(true);
         }

         this.mainRenderPass.cleanUp();
         this.auxRenderPass.cleanUp();
      }

      Framebuffer framebuffer;
      if (Renderer.getInstance().getSwapChain().hasImages()) {
         framebuffer = Renderer.getInstance().getSwapChain();
      } else {
         framebuffer = Framebuffer.builder(10, 10, 1, true).build();
      }

      this.mainFramebuffer = framebuffer;
      this.createRenderPasses();
      this.createAttachmentTextures();
   }

   private void createRenderPasses() {
      RenderPass.Builder builder = RenderPass.builder(this.mainFramebuffer);
      builder.getColorAttachmentInfo().setFinalLayout(2);
      builder.getColorAttachmentInfo().setOps(2, 0);
      builder.getDepthAttachmentInfo().setOps(2, 0);
      this.mainRenderPass = builder.build();
      builder = RenderPass.builder(this.mainFramebuffer);
      builder.getColorAttachmentInfo().setOps(0, 0);
      builder.getDepthAttachmentInfo().setOps(0, 0);
      builder.getColorAttachmentInfo().setFinalLayout(2);
      this.auxRenderPass = builder.build();
   }

   @Override
   public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
      Framebuffer framebuffer = this.mainFramebuffer;
      VulkanImage colorAttachment = framebuffer.getColorAttachment();
      colorAttachment.transitionImageLayout(stack, commandBuffer, 2);
      Renderer.getInstance().beginRenderPass(this.mainRenderPass, framebuffer);
      Renderer.setViewport(0, 0, framebuffer.getWidth(), framebuffer.getHeight(), stack);
      Buffer pScissor = framebuffer.scissor(stack);
      VK10.vkCmdSetScissor(commandBuffer, 0, pScissor);
   }

   @Override
   public void end(VkCommandBuffer commandBuffer) {
      Renderer.getInstance().endRenderPass(commandBuffer);
      if (this.mainFramebuffer == Renderer.getInstance().getSwapChain()) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, 1000001002);
         } catch (Throwable var6) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (stack != null) {
            stack.close();
         }
      }

      int result = VK10.vkEndCommandBuffer(commandBuffer);
      if (result != 0) {
         throw new RuntimeException("Failed to record command buffer:" + result);
      }
   }

   @Override
   public void cleanUp() {
      this.mainRenderPass.cleanUp();
      this.auxRenderPass.cleanUp();
   }

   @Override
   public void onResize() {
      this.createResources();
   }

   @Override
   public void rebindMainTarget() {
      VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
      RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
      if (boundRenderPass != this.mainRenderPass && boundRenderPass != this.auxRenderPass) {
         Renderer.getInstance().endRenderPass(commandBuffer);
         Renderer.getInstance().beginRenderPass(this.auxRenderPass, this.mainFramebuffer);
      }
   }

   @Override
   public void bindAsTexture() {
      VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
      RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
      if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass) {
         Renderer.getInstance().endRenderPass(commandBuffer);
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, 5);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
   }

   @Override
   public Framebuffer getMainFramebuffer() {
      return this.mainFramebuffer;
   }

   @Override
   public VkGlTexture getColorAttachment() {
      return this.colorAttachmentTextures[this.imageIdxSupplier.getAsInt()];
   }

   private void createAttachmentTextures() {
      SwapChain swapChain = Renderer.getInstance().getSwapChain();
      if (this.mainFramebuffer == swapChain) {
         List<VulkanImage> swapChainImages = swapChain.getImages();
         int imageCount = swapChainImages.size();
         this.colorAttachmentTextures = new VkGlTexture[imageCount];

         for (int i = 0; i < imageCount; i++) {
            int id = VkGlTexture.genTextureId();
            VkGlTexture glTexture = VkGlTexture.getTexture(id);
            glTexture.setVulkanImage(swapChainImages.get(i));
            this.colorAttachmentTextures[i] = glTexture;
         }

         this.imageIdxSupplier = Renderer::getCurrentImage;
      } else {
         this.colorAttachmentTextures = new VkGlTexture[1];
         int id = VkGlTexture.genTextureId();
         VkGlTexture glTexture = VkGlTexture.getTexture(id);
         glTexture.setVulkanImage(this.mainFramebuffer.getColorAttachment());
         this.colorAttachmentTextures[0] = glTexture;
         this.imageIdxSupplier = () -> 0;
      }
   }
}
