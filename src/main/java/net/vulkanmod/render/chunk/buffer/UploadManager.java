package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.nio.ByteBuffer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.queue.TransferQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryBarrier;

public class UploadManager {
   public static UploadManager INSTANCE;
   Queue queue = DeviceManager.getTransferQueue();
   CommandPool.CommandBuffer commandBuffer;
   LongOpenHashSet dstBuffers = new LongOpenHashSet();

   public static void createInstance() {
      INSTANCE = new UploadManager();
   }

   public void submitUploads() {
      if (this.commandBuffer != null) {
         this.queue.submitCommands(this.commandBuffer);
         Synchronization.INSTANCE.addCommandBuffer(this.commandBuffer);
         this.commandBuffer = null;
         this.dstBuffers.clear();
      }
   }

   public void recordUpload(Buffer buffer, long dstOffset, long bufferSize, ByteBuffer src) {
      StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
      stagingBuffer.copyBuffer((int)bufferSize, src);
      this.beginCommands();
      VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();
      if (!this.dstBuffers.add(buffer.getId())) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            org.lwjgl.vulkan.VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
            barrier.sType$Default();
            barrier.srcAccessMask(4096);
            barrier.dstAccessMask(4096);
            VK10.vkCmdPipelineBarrier(commandBuffer, 4096, 4096, 0, barrier, null, null);
         } catch (Throwable var13) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (stack != null) {
            stack.close();
         }

         this.dstBuffers.clear();
      }

      TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.getId(), stagingBuffer.getOffset(), buffer.getId(), dstOffset, bufferSize);
   }

   public void copyBuffer(Buffer src, Buffer dst) {
      this.copyBuffer(src, 0L, dst, 0L, src.getBufferSize());
   }

   public void copyBuffer(Buffer src, long srcOffset, Buffer dst, long dstOffset, long size) {
      this.beginCommands();
      VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         org.lwjgl.vulkan.VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
         barrier.sType$Default();
         org.lwjgl.vulkan.VkBufferMemoryBarrier.Buffer bufferMemoryBarriers = VkBufferMemoryBarrier.calloc(1, stack);
         VkBufferMemoryBarrier bufferMemoryBarrier = (VkBufferMemoryBarrier)bufferMemoryBarriers.get(0);
         bufferMemoryBarrier.sType$Default();
         bufferMemoryBarrier.buffer(src.getId());
         bufferMemoryBarrier.srcAccessMask(4096);
         bufferMemoryBarrier.dstAccessMask(2048);
         bufferMemoryBarrier.size(-1L);
         VK10.vkCmdPipelineBarrier(commandBuffer, 4096, 4096, 0, barrier, bufferMemoryBarriers, null);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      this.dstBuffers.add(dst.getId());
      TransferQueue.uploadBufferCmd(commandBuffer, src.getId(), srcOffset, dst.getId(), dstOffset, size);
   }

   public void syncUploads() {
      this.submitUploads();
      Synchronization.INSTANCE.waitFences();
   }

   private void beginCommands() {
      if (this.commandBuffer == null) {
         this.commandBuffer = this.queue.beginCommands();
      }
   }
}
