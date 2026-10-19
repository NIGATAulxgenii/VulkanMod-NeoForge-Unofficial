package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.MemoryType;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.VertexBuffer;
import net.vulkanmod.vulkan.memory.buffer.index.AutoIndexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;

public class VBO {
   private final MemoryType memoryType;
   private VertexBuffer vertexBuffer;
   private IndexBuffer indexBuffer;
   private Mode mode;
   private boolean autoIndexed = false;
   private int indexCount;
   private int vertexCount;

   public VBO(boolean useGpuMem) {
      this.memoryType = useGpuMem ? MemoryTypes.GPU_MEM : MemoryTypes.HOST_MEM;
   }

   public void upload(MeshData meshData) {
      DrawState parameters = meshData.drawState();
      this.indexCount = parameters.indexCount();
      this.vertexCount = parameters.vertexCount();
      this.mode = parameters.mode();
      this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
      this.uploadIndexBuffer(meshData.indexBuffer());
      meshData.close();
   }

   private void uploadVertexBuffer(DrawState parameters, ByteBuffer data) {
      if (data != null) {
         if (this.vertexBuffer != null) {
            this.vertexBuffer.scheduleFree();
         }

         int size = parameters.format().getVertexSize() * parameters.vertexCount();
         this.vertexBuffer = new VertexBuffer(size, this.memoryType);
         this.vertexBuffer.copyBuffer(data, size);
      }
   }

   public void uploadIndexBuffer(ByteBuffer data) {
      if (data == null) {
         if (this.indexBuffer != null && !this.autoIndexed) {
            this.indexBuffer.scheduleFree();
         }

         this.autoIndexed = true;
      } else {
         if (this.indexBuffer != null && !this.autoIndexed) {
            this.indexBuffer.scheduleFree();
         }

         this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
         this.indexBuffer.copyBuffer(data, data.remaining());
      }
   }

   private IndexBuffer getAutoIndexBuffer() {
      AutoIndexBuffer autoIndexBuffer;
      switch (this.mode) {
         case TRIANGLE_FAN:
            autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
            this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
            break;
         case TRIANGLE_STRIP:
            autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
            this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
            break;
         case QUADS:
            autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
            break;
         case LINES:
            autoIndexBuffer = Renderer.getDrawer().getLinesIndexBuffer();
            break;
         case DEBUG_LINE_STRIP:
            autoIndexBuffer = Renderer.getDrawer().getDebugLineStripIndexBuffer();
            break;
         case TRIANGLES:
         case DEBUG_LINES:
            autoIndexBuffer = null;
            break;
         default:
            throw new IllegalStateException("Unexpected draw mode: %s".formatted(this.mode));
      }

      if (autoIndexBuffer != null) {
         autoIndexBuffer.checkCapacity(this.vertexCount);
         return autoIndexBuffer.getIndexBuffer();
      } else {
         return null;
      }
   }

   public void bind(GraphicsPipeline pipeline) {
      Renderer renderer = Renderer.getInstance();
      renderer.bindGraphicsPipeline(pipeline);
      VTextureSelector.bindShaderTextures(pipeline);
      renderer.uploadAndBindUBOs(pipeline);
   }

   public void drawWithShader(Matrix4f modelView, Matrix4f projection, ShaderInstance shaderInstance) {
      shaderInstance.setDefaultUniforms(this.mode, modelView, projection, null);
      this.drawWithShader(modelView, projection, ShaderMixed.of(shaderInstance).getPipeline());
   }

   public void drawWithShader(Matrix4f modelView, Matrix4f projection, GraphicsPipeline pipeline) {
      if (this.indexCount != 0) {
         RenderSystem.assertOnRenderThread();
         VRenderSystem.applyModelViewMatrix(modelView);
         VRenderSystem.applyProjectionMatrix(projection);
         VRenderSystem.applyMVP(modelView, projection);
         VRenderSystem.setPrimitiveTopologyGL(this.mode.asGLMode);
         this.bind(pipeline);
         if (this.autoIndexed) {
            this.indexBuffer = this.getAutoIndexBuffer();
         }

         if (this.indexBuffer != null) {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
         } else {
            Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
         }

         VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
         VRenderSystem.applyProjectionMatrix(RenderSystem.getProjectionMatrix());
         VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
      }
   }

   public void draw() {
      if (this.indexCount != 0) {
         Renderer renderer = Renderer.getInstance();
         Pipeline pipeline = renderer.getBoundPipeline();
         renderer.uploadAndBindUBOs(pipeline);
         if (this.autoIndexed) {
            this.indexBuffer = this.getAutoIndexBuffer();
         }

         if (this.indexBuffer != null) {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
         } else {
            Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
         }
      }
   }

   public void close() {
      if (this.vertexCount > 0) {
         this.vertexBuffer.scheduleFree();
         this.vertexBuffer = null;
         if (!this.autoIndexed) {
            this.indexBuffer.scheduleFree();
            this.indexBuffer = null;
         }

         this.vertexCount = 0;
         this.indexCount = 0;
      }
   }
}
