package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.Window;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public abstract class VRenderSystem {
   private static final float DEFAULT_DEPTH_VALUE = 1.0F;
   private static long window;
   public static boolean depthTest = true;
   public static boolean depthMask = true;
   public static int depthFun = 515;
   public static int topology = 3;
   public static int polygonMode = 0;
   public static boolean canSetLineWidth = false;
   public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
   public static boolean cull = true;
   public static boolean logicOp = false;
   public static int logicOpFun = 0;
   public static float clearDepthValue = 1.0F;
   public static FloatBuffer clearColor = MemoryUtil.memCallocFloat(4);
   public static MappedBuffer modelViewMatrix = new MappedBuffer(64);
   public static MappedBuffer projectionMatrix = new MappedBuffer(64);
   public static MappedBuffer TextureMatrix = new MappedBuffer(64);
   public static MappedBuffer MVP = new MappedBuffer(64);
   public static MappedBuffer modelOffset = new MappedBuffer(12);
   public static MappedBuffer lightDirection0 = new MappedBuffer(12);
   public static MappedBuffer lightDirection1 = new MappedBuffer(12);
   public static MappedBuffer shaderColor = new MappedBuffer(16);
   public static MappedBuffer shaderFogColor = new MappedBuffer(16);
   public static MappedBuffer screenSize = new MappedBuffer(8);
   public static float alphaCutout = 0.0F;
   private static boolean depthBiasEnabled = false;
   private static float depthBiasConstant = 0.0F;
   private static float depthBiasSlope = 0.0F;
   private static final int[] shaderGlTextures = new int[12];
   private static final VulkanImage[] shaderImages = new VulkanImage[12];

   public static void initRenderer() {
      Vulkan.initVulkan(window);
      Renderer.initRenderer();
   }

   public static MappedBuffer getScreenSize() {
      updateScreenSize();
      return screenSize;
   }

   public static void updateScreenSize() {
      Window window = Minecraft.getInstance().getWindow();
      screenSize.putFloat(0, window.getWidth());
      screenSize.putFloat(4, window.getHeight());
   }

   public static void setWindow(long window) {
      VRenderSystem.window = window;
   }

   public static ByteBuffer getModelOffset() {
      return modelOffset.buffer;
   }

   public static int maxSupportedTextureSize() {
      return DeviceManager.deviceProperties.limits().maxImageDimension2D();
   }

   public static void applyMVP(Matrix4f MV, Matrix4f P) {
      applyModelViewMatrix(MV);
      applyProjectionMatrix(P);
      calculateMVP();
   }

   public static void applyModelViewMatrix(Matrix4f mat) {
      mat.get(modelViewMatrix.buffer.asFloatBuffer());
   }

   public static void applyProjectionMatrix(Matrix4f mat) {
      toVulkanProjection(mat).get(projectionMatrix.buffer.asFloatBuffer());
   }

   public static Matrix4f toVulkanProjection(Matrix4f mat) {
      return new Matrix4f(mat)
         .m02(0.5f * (mat.m02() + mat.m03()))
         .m12(0.5f * (mat.m12() + mat.m13()))
         .m22(0.5f * (mat.m22() + mat.m23()))
         .m32(0.5f * (mat.m32() + mat.m33()));
   }

   public static void calculateMVP() {
      Matrix4f MV = new Matrix4f(modelViewMatrix.buffer.asFloatBuffer());
      Matrix4f P = new Matrix4f(projectionMatrix.buffer.asFloatBuffer());
      P.mul(MV).get(MVP.buffer);
   }

   public static void setTextureMatrix(Matrix4f mat) {
      mat.get(TextureMatrix.buffer.asFloatBuffer());
   }

   public static MappedBuffer getTextureMatrix() {
      return TextureMatrix;
   }

   public static MappedBuffer getModelViewMatrix() {
      return modelViewMatrix;
   }

   public static MappedBuffer getProjectionMatrix() {
      return projectionMatrix;
   }

   public static MappedBuffer getMVP() {
      return MVP;
   }

   public static void setModelOffset(float x, float y, float z) {
      long ptr = modelOffset.ptr;
      VUtil.UNSAFE.putFloat(ptr, x);
      VUtil.UNSAFE.putFloat(ptr + 4L, y);
      VUtil.UNSAFE.putFloat(ptr + 8L, z);
   }

   public static void setShaderColor(int color) {
      ColorUtil.setRGBA_Buffer(
         shaderColor, ColorUtil.ARGB.unpackR(color), ColorUtil.ARGB.unpackG(color), ColorUtil.ARGB.unpackB(color), ColorUtil.ARGB.unpackA(color)
      );
   }

   public static void setShaderColor(float f1, float f2, float f3, float f4) {
      ColorUtil.setRGBA_Buffer(shaderColor, f1, f2, f3, f4);
   }

   public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
      ColorUtil.setRGBA_Buffer(shaderFogColor, f1, f2, f3, f4);
   }

   public static MappedBuffer getShaderColor() {
      return shaderColor;
   }

   public static MappedBuffer getShaderFogColor() {
      return shaderFogColor;
   }

   public static void setClearColor(float f1, float f2, float f3, float f4) {
      ColorUtil.setRGBA_Buffer(clearColor, f1, f2, f3, f4);
   }

   public static void clear(int mask) {
      Renderer.clearAttachments(mask);
   }

   public static void clearDepth(double depth) {
      clearDepthValue = (float)depth;
   }

   public static void disableDepthTest() {
      depthTest = false;
   }

   public static void depthMask(boolean b) {
      depthMask = b;
   }

   public static void setPrimitiveTopologyGL(int mode) {
      topology = switch (mode) {
         case 1, 3 -> 1;
         default -> throw new RuntimeException(String.format("Unknown GL primitive topology: %s", mode));
         case 4, 5, 6 -> 3;
      };
   }

   public static void setPolygonModeGL(int mode) {
      polygonMode = switch (mode) {
         case 6912 -> 2;
         case 6913 -> 1;
         case 6914 -> 0;
         default -> throw new RuntimeException(String.format("Unknown GL polygon mode: %s", mode));
      };
   }

   public static void setLineWidth(float width) {
      if (canSetLineWidth) {
         Renderer.setLineWidth(width);
      }
   }

   public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
      colorMask = PipelineState.ColorMask.getColorMask(b, b1, b2, b3);
   }

   public static int getColorMask() {
      return colorMask;
   }

   public static void enableDepthTest() {
      depthTest = true;
   }

   public static void enableCull() {
      cull = true;
   }

   public static void disableCull() {
      cull = false;
   }

   public static void depthFunc(int depthFun) {
      VRenderSystem.depthFun = depthFun;
   }

   public static void enableBlend() {
      PipelineState.blendInfo.enabled = true;
   }

   public static void disableBlend() {
      PipelineState.blendInfo.enabled = false;
   }

   public static void blendFunc(int srcFactor, int dstFactor) {
      PipelineState.blendInfo.setBlendFunction(srcFactor, dstFactor);
   }

   public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
      PipelineState.blendInfo.setBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
   }

   public static void blendOp(int op) {
      PipelineState.blendInfo.setBlendOp(op);
   }

   public static void enableColorLogicOp() {
      logicOp = true;
   }

   public static void disableColorLogicOp() {
      logicOp = false;
   }

   public static void logicOp(int glLogicOp) {
      logicOpFun = glLogicOp;
   }

   public static void polygonOffset(float slope, float biasConstant) {
      if (depthBiasConstant != biasConstant || depthBiasSlope != slope) {
         depthBiasConstant = biasConstant;
         depthBiasSlope = slope;
         Renderer.setDepthBias(depthBiasConstant, depthBiasSlope);
      }
   }

   public static void enablePolygonOffset() {
      if (!depthBiasEnabled) {
         Renderer.setDepthBias(depthBiasConstant, depthBiasSlope);
         depthBiasEnabled = true;
      }
   }

   public static void disablePolygonOffset() {
      if (depthBiasEnabled) {
         Renderer.setDepthBias(0.0F, 0.0F);
         depthBiasEnabled = false;
      }
   }

   public static VulkanImage getShaderTexture(int imageIdx) {
      if (shaderImages[imageIdx] == null) {
         VkGlTexture glTexture = VkGlTexture.getTexture(shaderGlTextures[imageIdx]);
         VulkanImage image = glTexture != null ? glTexture.getVulkanImage() : null;
         shaderImages[imageIdx] = image;
      }

      return shaderImages[imageIdx];
   }

   public static void setShaderTexture(int i, int glId) {
      shaderGlTextures[i] = glId;
      VkGlTexture glTexture = VkGlTexture.getTexture(glId);
      if (glTexture != null) {
         glTexture.getVulkanImage();
      } else {
         Object var10000 = null;
      }

      setShaderTexture(i, null);
   }

   public static void setShaderTexture(int i, VulkanImage image) {
      if (i >= 0 && i < shaderImages.length) {
         shaderImages[i] = image;
      }
   }
}
