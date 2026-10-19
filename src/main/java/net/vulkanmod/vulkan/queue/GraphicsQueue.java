package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsQueue extends Queue {
    public static GraphicsQueue INSTANCE;

    private static CommandPool.CommandBuffer currentCmdBuffer;
    private boolean recordingSession;

    public GraphicsQueue(MemoryStack stack, int familyIndex) {
        super(stack, familyIndex);
    }

    public void startRecording() {
        flushPending();
        recordingSession = true;
        currentCmdBuffer = beginCommands();
    }

    public void endRecordingAndSubmit() {
        flushPending();
        recordingSession = false;
    }

    public void flushPending() {
        if (currentCmdBuffer == null) return;
        submitCommands(currentCmdBuffer, true);
        Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer, true);
        currentCmdBuffer = null;
    }

    public CommandPool.CommandBuffer getCommandBuffer() {
        if (currentCmdBuffer != null) {
            return currentCmdBuffer;
        } else {
            CommandPool.CommandBuffer commands = beginCommands();
            if (recordingSession) currentCmdBuffer = commands;
            return commands;
        }
    }

    public long endIfNeeded(CommandPool.CommandBuffer commandBuffer) {
        if (recordingSession) {
            return VK_NULL_HANDLE;
        } else {
            return submitCommands(commandBuffer, true);
        }
    }

}
