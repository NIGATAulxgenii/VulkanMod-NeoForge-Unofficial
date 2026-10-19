package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
   @Shadow
   private static Matrix4f projectionMatrix;
   @Shadow
   private static Matrix4f savedProjectionMatrix;
   @Shadow
   @Final
   private static Matrix4fStack modelViewStack;
   @Shadow
   private static Matrix4f modelViewMatrix;
   @Shadow
   private static Matrix4f textureMatrix;
   @Shadow
   @Final
   private static float[] shaderColor;
   @Shadow
   @Final
   private static Vector3f[] shaderLightDirections;
   @Shadow
   @Final
   private static float[] shaderFogColor;
   @Shadow
   @Nullable
   private static Thread renderThread;
   @Shadow
   public static VertexSorting vertexSorting;
   @Shadow
   private static VertexSorting savedVertexSorting;
   @Shadow
   @Final
   private static int[] shaderTextures;

   @Shadow
   public static void assertOnRenderThread() {
   }

   @Overwrite(remap = false)
   public static void initRenderer(int debugVerbosity, boolean debugSync) {
      VRenderSystem.initRenderer();
      renderThread.setPriority(7);
   }

   @Overwrite(remap = false)
   public static void setupDefaultState(int x, int y, int width, int height) {
   }

   @Overwrite(remap = false)
   public static int maxSupportedTextureSize() {
      return VRenderSystem.maxSupportedTextureSize();
   }

   @Overwrite(remap = false)
   public static void setShaderLights(Vector3f dir0, Vector3f dir1) {
      shaderLightDirections[0] = dir0;
      shaderLightDirections[1] = dir1;
      VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
      VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
      VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());
      VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
      VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
      VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());
   }

   @Overwrite(remap = false)
   private static void _setShaderColor(float r, float g, float b, float a) {
      shaderColor[0] = r;
      shaderColor[1] = g;
      shaderColor[2] = b;
      shaderColor[3] = a;
      VRenderSystem.setShaderColor(r, g, b, a);
   }

   @Overwrite(remap = false)
   public static void setShaderFogColor(float f, float g, float h, float i) {
      shaderFogColor[0] = f;
      shaderFogColor[1] = g;
      shaderFogColor[2] = h;
      shaderFogColor[3] = i;
      VRenderSystem.setShaderFogColor(f, g, h, i);
   }

   @Overwrite(remap = false)
   public static void setProjectionMatrix(Matrix4f projectionMatrix, VertexSorting vertexSorting) {
      Matrix4f matrix4f = new Matrix4f(projectionMatrix);
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            RenderSystemMixin.projectionMatrix = matrix4f;
            RenderSystem.vertexSorting = vertexSorting;
            VRenderSystem.applyProjectionMatrix(matrix4f);
            VRenderSystem.calculateMVP();
         });
      } else {
         RenderSystemMixin.projectionMatrix = matrix4f;
         RenderSystem.vertexSorting = vertexSorting;
         VRenderSystem.applyProjectionMatrix(matrix4f);
         VRenderSystem.calculateMVP();
      }
   }

   @Overwrite(remap = false)
   public static void setTextureMatrix(Matrix4f matrix4f) {
      Matrix4f matrix4f2 = new Matrix4f(matrix4f);
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            textureMatrix = matrix4f2;
            VRenderSystem.setTextureMatrix(matrix4f);
         });
      } else {
         textureMatrix = matrix4f2;
         VRenderSystem.setTextureMatrix(matrix4f);
      }
   }

   @Overwrite(remap = false)
   public static void resetTextureMatrix() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> textureMatrix.identity());
      } else {
         textureMatrix.identity();
         VRenderSystem.setTextureMatrix(textureMatrix);
      }
   }

   @Overwrite(remap = false)
   public static void applyModelViewMatrix() {
      Matrix4f matrix4f = new Matrix4f(modelViewStack);
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            modelViewMatrix = matrix4f;
            VRenderSystem.applyModelViewMatrix(matrix4f);
            VRenderSystem.calculateMVP();
         });
      } else {
         modelViewMatrix = matrix4f;
         VRenderSystem.applyModelViewMatrix(matrix4f);
         VRenderSystem.calculateMVP();
      }
   }

   @Overwrite(remap = false)
   private static void _restoreProjectionMatrix() {
      projectionMatrix = savedProjectionMatrix;
      vertexSorting = savedVertexSorting;
      VRenderSystem.applyProjectionMatrix(projectionMatrix);
      VRenderSystem.calculateMVP();
   }

   @Overwrite
   public static void _setShaderTexture(int i, ResourceLocation resourceLocation) {
      if (i >= 0 && i < shaderTextures.length) {
         TextureManager textureManager = Minecraft.getInstance().getTextureManager();
         AbstractTexture abstractTexture = textureManager.getTexture(resourceLocation);
         shaderTextures[i] = abstractTexture.getId();
         VRenderSystem.setShaderTexture(i, abstractTexture.getId());
      }
   }

   @Overwrite(remap = false)
   public static void _setShaderTexture(int i, int texId) {
      if (i >= 0 && i < shaderTextures.length) {
         shaderTextures[i] = texId;
      }

      VRenderSystem.setShaderTexture(i, texId);
   }

   @Overwrite(remap = false)
   public static int getShaderTexture(int i) {
      assertOnRenderThread();
      return i >= 0 && i < shaderTextures.length ? shaderTextures[i] : 0;
   }
}
