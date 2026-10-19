package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferUploader.class)
public abstract class BufferUploaderM {
   @Overwrite
   public static void reset() {
   }

   @Overwrite
   public static void drawWithShader(MeshData meshData) {
      RenderSystem.assertOnRenderThread();
      DrawState parameters = meshData.drawState();
      if (parameters.vertexCount() > 0) {
         ShaderInstance shaderInstance = RenderSystem.getShader();
         if (shaderInstance.getVertexFormat() != parameters.format()) {
            meshData.close();
            return;
         }

         VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);
         shaderInstance.setDefaultUniforms(
            Mode.QUADS, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow()
         );
         shaderInstance.apply();
         Renderer.getDrawer().draw(meshData.vertexBuffer(), meshData.indexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
      }

      meshData.close();
   }

   @Overwrite
   public static void draw(MeshData meshData) {
      DrawState parameters = meshData.drawState();
      if (parameters.vertexCount() > 0) {
         Renderer renderer = Renderer.getInstance();
         Pipeline pipeline = renderer.getBoundPipeline();
         renderer.uploadAndBindUBOs(pipeline);
         Renderer.getDrawer().draw(meshData.vertexBuffer(), null, parameters.mode(), parameters.format(), parameters.vertexCount());
      }

      meshData.close();
   }
}
