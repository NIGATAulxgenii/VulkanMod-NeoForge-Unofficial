package net.vulkanmod.vulkan.texture;

import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.opengl.GL11;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class ImageUtil {

    public static void copyBufferToImageCmd(VkCommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

        try (MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);
            region.bufferImageHeight(bufferImageHeight);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
        }
    }

    public static void downloadTexture(VulkanImage image, long ptr) {
        downloadTexture(image, 0, ptr);
    }

    public static void downloadTexture(VulkanImage image, int level, long ptr) {
        DeviceManager.getGraphicsQueue().flushPending();
        Renderer.getInstance().flushCmds();
        if (level < 0 || level >= image.mipLevels) throw new IllegalArgumentException("Invalid mip level: " + level);
        int width = Math.max(1, image.width >> level);
        int height = Math.max(1, image.height >> level);
        try (MemoryStack stack = stackPush()) {
            int prevLayout = image.getCurrentLayout();
            CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().beginCommands();
            image.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            long imageSize = (long) width * height * image.formatSize;

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);
            MemoryManager.getInstance().createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            copyImageToBuffer(commandBuffer.getHandle(), pStagingBuffer.get(0), image.getId(), level, width, height, 0, 0, 0, 0, 0);
            image.transitionImageLayout(stack, commandBuffer.getHandle(), prevLayout);

            long fence = DeviceManager.getGraphicsQueue().submitCommands(commandBuffer);
            vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX);
            commandBuffer.reset();

            MemoryManager.MapAndCopy(pStagingAllocation.get(0),
                    (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) imageSize), ptr)
            );

            MemoryManager.freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
        }
    }

    public static void copyMipLevels(VulkanImage source, VulkanImage destination) {
        var queue = DeviceManager.getGraphicsQueue();
        var commands = queue.getCommandBuffer();
        try (MemoryStack stack = stackPush()) {
            int previousLayout = source.getCurrentLayout();
            source.transitionImageLayout(stack, commands.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            destination.transitionImageLayout(stack, commands.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            int levels = Math.min(source.mipLevels, destination.mipLevels);
            VkImageCopy.Buffer regions = VkImageCopy.calloc(levels, stack);
            for (int level = 0; level < levels; level++) {
                var region = regions.get(level);
                region.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(level).layerCount(1);
                region.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(level).layerCount(1);
                region.extent().set(Math.max(1, source.width >> level), Math.max(1, source.height >> level), 1);
            }
            vkCmdCopyImage(commands.getHandle(), source.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destination.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
            source.transitionImageLayout(stack, commands.getHandle(), previousLayout);
            destination.readOnlyLayout(stack, commands.getHandle());
        }
        if (queue.endIfNeeded(commands) != VK_NULL_HANDLE) Synchronization.INSTANCE.addCommandBuffer(commands, true);
    }

    public static void blitImage(VulkanImage source, VulkanImage destination,
                                 int srcX0, int srcY0, int srcX1, int srcY1,
                                 int dstX0, int dstY0, int dstX1, int dstY1, int filter) {
        if (source == null || destination == null) throw new IllegalStateException("Missing framebuffer attachment");
        if (source == destination) throw new UnsupportedOperationException("Blits within the same attachment are not supported");
        if (source.aspect != destination.aspect) throw new IllegalArgumentException("Framebuffer attachment aspects differ");
        if (filter != GL11.GL_NEAREST && filter != GL11.GL_LINEAR) throw new IllegalArgumentException("Invalid blit filter");
        if (srcX0 == srcX1 || srcY0 == srcY1 || dstX0 == dstX1 || dstY0 == dstY1) return;
        boolean rendering = Renderer.isRecording();
        var renderer = Renderer.getInstance();
        if (rendering) renderer.endRenderPass();
        var commands = rendering ? null : DeviceManager.getGraphicsQueue().beginCommands();
        VkCommandBuffer commandBuffer = rendering ? Renderer.getCommandBuffer() : commands.getHandle();
        try (MemoryStack stack = stackPush()) {
            int sourceLayout = source.getCurrentLayout();
            int destinationLayout = destination.getCurrentLayout();
            source.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            destination.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            var region = VkImageBlit.calloc(1, stack);
            int aspect = (source.aspect & VK_IMAGE_ASPECT_DEPTH_BIT) != 0 ? VK_IMAGE_ASPECT_DEPTH_BIT : VK_IMAGE_ASPECT_COLOR_BIT;
            region.srcSubresource().aspectMask(aspect).layerCount(1);
            region.dstSubresource().aspectMask(aspect).layerCount(1);
            region.srcOffsets(0).set(srcX0, srcY0, 0);
            region.srcOffsets(1).set(srcX1, srcY1, 1);
            region.dstOffsets(0).set(dstX0, dstY0, 0);
            region.dstOffsets(1).set(dstX1, dstY1, 1);
            vkCmdBlitImage(commandBuffer, source.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destination.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region,
                    filter == GL11.GL_LINEAR ? VK_FILTER_LINEAR : VK_FILTER_NEAREST);
            source.transitionImageLayout(stack, commandBuffer, sourceLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : sourceLayout);
            destination.transitionImageLayout(stack, commandBuffer, destinationLayout == VK_IMAGE_LAYOUT_UNDEFINED ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : destinationLayout);
        }
        if (rendering) renderer.getMainPass().rebindMainTarget();
        else {
            long fence = DeviceManager.getGraphicsQueue().submitCommands(commands);
            vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX);
            commands.reset();
        }
    }

    public static void copyImageToBuffer(VkCommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {
        try (MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);
            region.bufferImageHeight(bufferImageHeight);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyImageToBuffer(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);
        }
    }

    public static void generateMipmaps(VulkanImage image) {
        try (MemoryStack stack = stackPush()) {

            CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().beginCommands();

            image.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            int level, prevLevel;

            for (level = 1; level < image.mipLevels; level++) {
                prevLevel = level - 1;

                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.oldLayout(VK_IMAGE_USAGE_TRANSFER_DST_BIT);
                barrier.newLayout(VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(image.getId());

                barrier.subresourceRange().baseMipLevel(prevLevel);
                barrier.subresourceRange().levelCount(1);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(VK_REMAINING_ARRAY_LAYERS);

                barrier.subresourceRange().aspectMask(image.aspect);

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer.getHandle(),
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        barrier);

                prevLevel = level - 1;

                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.srcOffsets(1, VkOffset3D.calloc(stack).set(image.width >> prevLevel, image.height >> prevLevel, 1));
                blit.srcSubresource()
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(prevLevel)
                        .baseArrayLayer(0)
                        .layerCount(1);

                blit.dstOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.dstOffsets(1, VkOffset3D.calloc(stack).set(image.width >> level, image.height >> level, 1));
                blit.dstSubresource()
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(level)
                        .baseArrayLayer(0)
                        .layerCount(1);

                vkCmdBlitImage(commandBuffer.getHandle(),
                        image.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit,
                        VK_FILTER_LINEAR);

            }

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image.getId());

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(image.mipLevels - 1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(VK_REMAINING_ARRAY_LAYERS);

            barrier.subresourceRange().aspectMask(image.aspect);

            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    barrier);

            barrier.oldLayout(VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            barrier.subresourceRange().baseMipLevel(image.mipLevels - 1);
            barrier.subresourceRange().levelCount(1);

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    barrier);

            image.setCurrentLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            long fence = DeviceManager.getGraphicsQueue().submitCommands(commandBuffer);

            vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX);
        }
    }
}
