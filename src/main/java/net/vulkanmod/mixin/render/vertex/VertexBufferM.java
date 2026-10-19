package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferM {
   @Unique
   private VBO vbo;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void constructor(Usage usage, CallbackInfo ci) {
      this.vbo = new VBO(usage == Usage.STATIC);
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
   private int doNothing() {
      return 0;
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
   private int doNothing2() {
      return 0;
   }

   @Overwrite
   public void bind() {
   }

   @Overwrite
   public static void unbind() {
   }

   @Overwrite
   public void upload(MeshData meshData) {
      this.vbo.upload(meshData);
   }

   @Overwrite
   public void uploadIndexBuffer(Result result) {
      this.vbo.uploadIndexBuffer(result.byteBuffer());
   }

   @Overwrite
   public void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader) {
      this.vbo.drawWithShader(viewMatrix, projectionMatrix, shader);
   }

   @Overwrite
   public void draw() {
      this.vbo.draw();
   }

   @Overwrite
   public void close() {
      this.vbo.close();
   }
}
