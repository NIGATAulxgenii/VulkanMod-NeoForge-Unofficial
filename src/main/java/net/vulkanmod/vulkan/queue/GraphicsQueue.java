package net.vulkanmod.vulkan.queue;

import net.vulkanmod.vulkan.Synchronization;
import org.lwjgl.system.MemoryStack;

public class GraphicsQueue extends Queue {
   public static GraphicsQueue INSTANCE;
   private static CommandPool.CommandBuffer currentCmdBuffer;

   public GraphicsQueue(MemoryStack stack, int familyIndex) {
      super(stack, familyIndex);
   }

   public void startRecording() {
      currentCmdBuffer = this.beginCommands();
   }

   public void endRecordingAndSubmit() {
      long fence = this.submitCommands(currentCmdBuffer);
      Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer);
      currentCmdBuffer = null;
   }

   public CommandPool.CommandBuffer getCommandBuffer() {
      return currentCmdBuffer != null ? currentCmdBuffer : this.beginCommands();
   }

   public long endIfNeeded(CommandPool.CommandBuffer commandBuffer) {
      return currentCmdBuffer != null ? 0L : this.submitCommands(commandBuffer);
   }
}
