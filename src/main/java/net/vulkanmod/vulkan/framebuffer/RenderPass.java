package net.vulkanmod.vulkan.framebuffer;

import java.nio.LongBuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkAttachmentDescription.Buffer;

public class RenderPass {
   Framebuffer framebuffer;
   long id;
   final int attachmentCount;
   RenderPass.AttachmentInfo colorAttachmentInfo;
   RenderPass.AttachmentInfo depthAttachmentInfo;

   public RenderPass(Framebuffer framebuffer, RenderPass.AttachmentInfo colorAttachmentInfo, RenderPass.AttachmentInfo depthAttachmentInfo) {
      this.framebuffer = framebuffer;
      this.colorAttachmentInfo = colorAttachmentInfo;
      this.depthAttachmentInfo = depthAttachmentInfo;
      int count = 0;
      if (colorAttachmentInfo != null) {
         count++;
      }

      if (depthAttachmentInfo != null) {
         count++;
      }

      this.attachmentCount = count;
      this.createRenderPass();
   }

   private void createRenderPass() {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         Buffer attachments = VkAttachmentDescription.calloc(this.attachmentCount, stack);
         org.lwjgl.vulkan.VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.calloc(this.attachmentCount, stack);
         org.lwjgl.vulkan.VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
         subpass.pipelineBindPoint(0);
         int i = 0;
         if (this.colorAttachmentInfo != null) {
            VkAttachmentDescription colorAttachment = (VkAttachmentDescription)attachments.get(i);
            colorAttachment.format(this.colorAttachmentInfo.format)
               .samples(1)
               .loadOp(this.colorAttachmentInfo.loadOp)
               .storeOp(this.colorAttachmentInfo.storeOp)
               .stencilLoadOp(2)
               .stencilStoreOp(1)
               .initialLayout(2)
               .finalLayout(this.colorAttachmentInfo.finalLayout);
            VkAttachmentReference colorAttachmentRef = ((VkAttachmentReference)attachmentRefs.get(0)).attachment(0).layout(2);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments((org.lwjgl.vulkan.VkAttachmentReference.Buffer)VkAttachmentReference.calloc(1, stack).put(0, colorAttachmentRef));
            i++;
         }

         if (this.depthAttachmentInfo != null) {
            VkAttachmentDescription depthAttachment = (VkAttachmentDescription)attachments.get(i);
            depthAttachment.format(this.depthAttachmentInfo.format)
               .samples(1)
               .loadOp(this.depthAttachmentInfo.loadOp)
               .storeOp(this.depthAttachmentInfo.storeOp)
               .stencilLoadOp(2)
               .stencilStoreOp(1)
               .initialLayout(3)
               .finalLayout(this.depthAttachmentInfo.finalLayout);
            VkAttachmentReference depthAttachmentRef = ((VkAttachmentReference)attachmentRefs.get(1)).attachment(1).layout(3);
            subpass.pDepthStencilAttachment(depthAttachmentRef);
         }

         VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
         renderPassInfo.sType$Default().pAttachments(attachments).pSubpasses(subpass);
         switch (this.colorAttachmentInfo.finalLayout) {
            case 5: {
               org.lwjgl.vulkan.VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
               ((VkSubpassDependency)subpassDependencies.get(0))
                  .srcSubpass(0)
                  .dstSubpass(-1)
                  .srcStageMask(1024)
                  .dstStageMask(128)
                  .srcAccessMask(256)
                  .dstAccessMask(32);
               renderPassInfo.pDependencies(subpassDependencies);
               break;
            }
            case 1000001002: {
               org.lwjgl.vulkan.VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
               ((VkSubpassDependency)subpassDependencies.get(0))
                  .srcSubpass(-1)
                  .dstSubpass(0)
                  .srcStageMask(1024)
                  .dstStageMask(8192)
                  .srcAccessMask(0)
                  .dstAccessMask(0);
               renderPassInfo.pDependencies(subpassDependencies);
            }
         }

         LongBuffer pRenderPass = stack.mallocLong(1);
         if (VK10.vkCreateRenderPass(Vulkan.getVkDevice(), renderPassInfo, null, pRenderPass) != 0) {
            throw new RuntimeException("Failed to create render pass");
         }

         this.id = pRenderPass.get(0);
      } catch (Throwable var9) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public void beginRenderPass(VkCommandBuffer commandBuffer, long framebufferId, MemoryStack stack) {
      if (this.colorAttachmentInfo != null && this.framebuffer.getColorAttachment().getCurrentLayout() != 2) {
         this.framebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, 2);
      }

      if (this.depthAttachmentInfo != null && this.framebuffer.getDepthAttachment().getCurrentLayout() != 3) {
         this.framebuffer.getDepthAttachment().transitionImageLayout(stack, commandBuffer, 3);
      }

      VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
      renderPassInfo.sType$Default();
      renderPassInfo.renderPass(this.id);
      renderPassInfo.framebuffer(framebufferId);
      VkRect2D renderArea = VkRect2D.malloc(stack);
      renderArea.offset().set(0, 0);
      renderArea.extent().set(this.framebuffer.getWidth(), this.framebuffer.getHeight());
      renderPassInfo.renderArea(renderArea);
      org.lwjgl.vulkan.VkClearValue.Buffer clearValues = VkClearValue.malloc(2, stack);
      ((VkClearValue)clearValues.get(0)).color().float32(VRenderSystem.clearColor);
      ((VkClearValue)clearValues.get(1)).depthStencil().set(1.0F, 0);
      renderPassInfo.pClearValues(clearValues);
      VK10.vkCmdBeginRenderPass(commandBuffer, renderPassInfo, 0);
      Renderer.getInstance().setBoundRenderPass(this);
   }

   public void endRenderPass(VkCommandBuffer commandBuffer) {
      VK10.vkCmdEndRenderPass(commandBuffer);
      if (this.colorAttachmentInfo != null) {
         this.framebuffer.getColorAttachment().setCurrentLayout(this.colorAttachmentInfo.finalLayout);
      }

      if (this.depthAttachmentInfo != null) {
         this.framebuffer.getDepthAttachment().setCurrentLayout(this.depthAttachmentInfo.finalLayout);
      }

      Renderer.getInstance().setBoundRenderPass(null);
   }

   public void beginDynamicRendering(VkCommandBuffer commandBuffer, MemoryStack stack) {
      if (this.colorAttachmentInfo != null && this.framebuffer.getColorAttachment().getCurrentLayout() != 2) {
         this.framebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, 2);
      }

      if (this.depthAttachmentInfo != null && this.framebuffer.getDepthAttachment().getCurrentLayout() != 3) {
         this.framebuffer.getDepthAttachment().transitionImageLayout(stack, commandBuffer, 3);
      }

      VkRect2D renderArea = VkRect2D.malloc(stack);
      renderArea.offset().set(0, 0);
      renderArea.extent().set(this.framebuffer.getWidth(), this.framebuffer.getHeight());
      org.lwjgl.vulkan.VkClearValue.Buffer clearValues = VkClearValue.malloc(2, stack);
      ((VkClearValue)clearValues.get(0)).color().float32(VRenderSystem.clearColor);
      ((VkClearValue)clearValues.get(1)).depthStencil().set(1.0F, 0);
      VkRenderingInfo renderingInfo = VkRenderingInfo.calloc(stack);
      renderingInfo.sType(1000044000);
      renderingInfo.renderArea(renderArea);
      renderingInfo.layerCount(1);
      if (this.colorAttachmentInfo != null) {
         org.lwjgl.vulkan.VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
         colorAttachment.sType(1000044001);
         colorAttachment.imageView(this.framebuffer.getColorAttachment().getImageView());
         colorAttachment.imageLayout(2);
         colorAttachment.loadOp(this.colorAttachmentInfo.loadOp);
         colorAttachment.storeOp(this.colorAttachmentInfo.storeOp);
         colorAttachment.clearValue((VkClearValue)clearValues.get(0));
         renderingInfo.pColorAttachments(colorAttachment);
      }

      if (this.depthAttachmentInfo != null) {
         VkRenderingAttachmentInfo depthAttachment = VkRenderingAttachmentInfo.calloc(stack);
         depthAttachment.sType(1000044001);
         depthAttachment.imageView(this.framebuffer.getDepthAttachment().getImageView());
         depthAttachment.imageLayout(3);
         depthAttachment.loadOp(this.depthAttachmentInfo.loadOp);
         depthAttachment.storeOp(this.depthAttachmentInfo.storeOp);
         depthAttachment.clearValue((VkClearValue)clearValues.get(1));
         renderingInfo.pDepthAttachment(depthAttachment);
      }

      KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);
   }

   public Framebuffer getFramebuffer() {
      return this.framebuffer;
   }

   public void setFramebuffer(Framebuffer framebuffer) {
      this.framebuffer = framebuffer;
   }

   public void cleanUp() {
      MemoryManager.getInstance().addFrameOp(() -> VK10.vkDestroyRenderPass(Vulkan.getVkDevice(), this.id, null));
   }

   public long getId() {
      return this.id;
   }

   public static RenderPass.Builder builder(Framebuffer framebuffer) {
      return new RenderPass.Builder(framebuffer);
   }

   public static class AttachmentInfo {
      final RenderPass.AttachmentInfo.Type type;
      final int format;
      int finalLayout;
      int loadOp;
      int storeOp;

      public AttachmentInfo(RenderPass.AttachmentInfo.Type type, int format) {
         this.type = type;
         this.format = format;
         this.finalLayout = type.defaultLayout;
         this.loadOp = 2;
         this.storeOp = 0;
      }

      public RenderPass.AttachmentInfo setOps(int loadOp, int storeOp) {
         this.loadOp = loadOp;
         this.storeOp = storeOp;
         return this;
      }

      public RenderPass.AttachmentInfo setLoadOp(int loadOp) {
         this.loadOp = loadOp;
         return this;
      }

      public RenderPass.AttachmentInfo setFinalLayout(int finalLayout) {
         this.finalLayout = finalLayout;
         return this;
      }

      public static enum Type {
         COLOR(2),
         DEPTH(3);

         final int defaultLayout;

         private Type(int layout) {
            this.defaultLayout = layout;
         }
      }
   }

   public static class Builder {
      Framebuffer framebuffer;
      RenderPass.AttachmentInfo colorAttachmentInfo;
      RenderPass.AttachmentInfo depthAttachmentInfo;

      public Builder(Framebuffer framebuffer) {
         this.framebuffer = framebuffer;
         if (framebuffer.hasColorAttachment) {
            this.colorAttachmentInfo = new RenderPass.AttachmentInfo(RenderPass.AttachmentInfo.Type.COLOR, framebuffer.format).setOps(1, 0);
         }

         if (framebuffer.hasDepthAttachment) {
            this.depthAttachmentInfo = new RenderPass.AttachmentInfo(RenderPass.AttachmentInfo.Type.DEPTH, framebuffer.depthFormat).setOps(1, 1);
         }
      }

      public RenderPass build() {
         return new RenderPass(this.framebuffer, this.colorAttachmentInfo, this.depthAttachmentInfo);
      }

      public RenderPass.Builder setLoadOp(int loadOp) {
         if (this.colorAttachmentInfo != null) {
            this.colorAttachmentInfo.setLoadOp(loadOp);
         }

         if (this.depthAttachmentInfo != null) {
            this.depthAttachmentInfo.setLoadOp(loadOp);
         }

         return this;
      }

      public RenderPass.AttachmentInfo getColorAttachmentInfo() {
         return this.colorAttachmentInfo;
      }

      public RenderPass.AttachmentInfo getDepthAttachmentInfo() {
         return this.depthAttachmentInfo;
      }
   }
}
