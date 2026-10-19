package net.vulkanmod.vulkan;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.lwjgl.system.MemoryStack;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Synchronization {
    private static final int ALLOCATION_SIZE = 50;

    public static final Synchronization INSTANCE = new Synchronization(ALLOCATION_SIZE);

    private final LongBuffer fences;
    private int idx = 0;

    private ObjectArrayList<CommandPool.CommandBuffer> commandBuffers = new ObjectArrayList<>();
    private final ObjectArrayList<CommandPool.CommandBuffer> semaphoreBuffers = new ObjectArrayList<>();

    Synchronization(int allocSize) {
        this.fences = MemoryUtil.memAllocLong(allocSize);
    }

    public synchronized void addCommandBuffer(CommandPool.CommandBuffer commandBuffer) {
        this.addFence(commandBuffer.getFence());
        this.commandBuffers.add(commandBuffer);
    }

    public synchronized void addCommandBuffer(CommandPool.CommandBuffer commandBuffer, boolean useSemaphore) {
        if (!useSemaphore) {
            addCommandBuffer(commandBuffer);
            return;
        }
        semaphoreBuffers.add(commandBuffer);
        if (semaphoreBuffers.size() >= ALLOCATION_SIZE) waitSemaphoreUploads();
    }

    public synchronized LongBuffer getWaitSemaphores(MemoryStack stack, long imageAvailable) {
        LongBuffer waits = stack.mallocLong(semaphoreBuffers.size() + 1);
        for (var commands : semaphoreBuffers) waits.put(commands.getSemaphore());
        waits.put(imageAvailable).flip();
        return waits;
    }

    public synchronized void scheduleFrameReset() {
        if (semaphoreBuffers.isEmpty()) return;
        var completed = semaphoreBuffers.clone();
        MemoryManager.getInstance().addFrameOp(() -> completed.forEach(CommandPool.CommandBuffer::reset));
        semaphoreBuffers.clear();
    }

    public synchronized void addFence(long fence) {
        if (idx == ALLOCATION_SIZE)
            waitFences();

        fences.put(idx, fence);
        idx++;
    }

    public synchronized void waitFences() {
        waitLegacyFences();
        waitSemaphoreUploads();
    }

    public synchronized void waitLegacyFences() {
        if (idx == 0)
            return;

        VkDevice device = Vulkan.getVkDevice();

        fences.limit(idx);

        vkWaitForFences(device, fences, true, VUtil.UINT64_MAX);

        this.commandBuffers.forEach(CommandPool.CommandBuffer::reset);
        this.commandBuffers.clear();

        fences.limit(ALLOCATION_SIZE);
        idx = 0;
    }

    private void waitSemaphoreUploads() {
        if (semaphoreBuffers.isEmpty()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer waits = stack.mallocLong(semaphoreBuffers.size());
            var stages = stack.mallocInt(semaphoreBuffers.size());
            for (var commands : semaphoreBuffers) {
                waits.put(commands.getSemaphore());
                stages.put(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT);
            }
            waits.flip();
            stages.flip();
            var info = VkSubmitInfo.calloc(stack).sType$Default().pWaitSemaphores(waits)
                    .waitSemaphoreCount(waits.remaining()).pWaitDstStageMask(stages);
            var fenceInfo = VkFenceCreateInfo.calloc(stack).sType$Default();
            LongBuffer fence = stack.mallocLong(1);
            int result = vkCreateFence(Vulkan.getVkDevice(), fenceInfo, null, fence);
            if (result != VK_SUCCESS) throw new IllegalStateException("Cannot create upload drain fence: " + result);
            try {
                result = vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), info, fence.get(0));
                if (result != VK_SUCCESS) throw new IllegalStateException("Cannot submit upload waits: " + result);
                result = vkWaitForFences(Vulkan.getVkDevice(), fence.get(0), true, VUtil.UINT64_MAX);
                if (result != VK_SUCCESS) throw new IllegalStateException("Cannot wait for uploads: " + result);
                semaphoreBuffers.forEach(CommandPool.CommandBuffer::reset);
                semaphoreBuffers.clear();
            } finally { vkDestroyFence(Vulkan.getVkDevice(), fence.get(0), null); }
        }
    }

    public static void waitFence(long fence) {
        VkDevice device = Vulkan.getVkDevice();

        vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
    }

    public static boolean checkFenceStatus(long fence) {
        VkDevice device = Vulkan.getVkDevice();
        return vkGetFenceStatus(device, fence) == VK_SUCCESS;
    }

}
