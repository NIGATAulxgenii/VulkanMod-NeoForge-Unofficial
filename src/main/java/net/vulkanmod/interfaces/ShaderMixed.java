package net.vulkanmod.interfaces;

import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.util.MappedBuffer;

public interface ShaderMixed {
   static ShaderMixed of(ShaderInstance compiledShaderProgram) {
      return (ShaderMixed)compiledShaderProgram;
   }

   void setPipeline(GraphicsPipeline var1);

   GraphicsPipeline getPipeline();

   void setupUniformSuppliers(UBO var1);

   Supplier<MappedBuffer> getUniformSupplier(String var1);

   void setDoUniformsUpdate();
}
