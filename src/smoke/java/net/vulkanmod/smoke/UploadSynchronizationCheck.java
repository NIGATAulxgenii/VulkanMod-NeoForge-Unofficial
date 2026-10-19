package net.vulkanmod.smoke;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.queue.CommandPool;

import java.util.HashSet;
import java.util.Set;

public final class UploadSynchronizationCheck {
    public static void run() {
        Synchronization.INSTANCE.waitFences();
        var queue = DeviceManager.getGraphicsQueue();
        Set<Long> buffers = new HashSet<>();
        for (int round = 0; round < 3; round++) {
            var commands = new java.util.ArrayList<CommandPool.CommandBuffer>();
            for (int i = 0; i < 120; i++) {
                var commandBuffer = queue.beginCommands();
                buffers.add(commandBuffer.getHandle().address());
                if (!commandBuffer.isRecording()) throw new IllegalStateException("Command buffer not recording");
                queue.submitCommands(commandBuffer, true);
                Synchronization.INSTANCE.addCommandBuffer(commandBuffer, true);
                commands.add(commandBuffer);
            }
            Synchronization.INSTANCE.waitFences();
            if (commands.stream().anyMatch(CommandPool.CommandBuffer::isSubmitted))
                throw new IllegalStateException("Drained command buffer remains submitted");
        }
        if (buffers.size() > 60) throw new IllegalStateException("Upload command buffers grow without reuse: " + buffers.size());
        Initializer.LOGGER.info("SMOKE upload synchronization PASS: 360 semaphore submissions, bounded reuse, explicit drain");
    }
}
