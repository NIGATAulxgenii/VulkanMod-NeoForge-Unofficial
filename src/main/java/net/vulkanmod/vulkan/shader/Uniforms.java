package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.function.Supplier;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;

public class Uniforms {
   public static Object2ReferenceOpenHashMap<String, Supplier<Integer>> vec1i_uniformMap = new Object2ReferenceOpenHashMap();
   public static Object2ReferenceOpenHashMap<String, Supplier<Float>> vec1f_uniformMap = new Object2ReferenceOpenHashMap();
   public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec2f_uniformMap = new Object2ReferenceOpenHashMap();
   public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec3f_uniformMap = new Object2ReferenceOpenHashMap();
   public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec4f_uniformMap = new Object2ReferenceOpenHashMap();
   public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> mat4f_uniformMap = new Object2ReferenceOpenHashMap();

   public static void setupDefaultUniforms() {
      mat4f_uniformMap.put("ModelViewMat", VRenderSystem::getModelViewMatrix);
      mat4f_uniformMap.put("ProjMat", VRenderSystem::getProjectionMatrix);
      mat4f_uniformMap.put("MVP", VRenderSystem::getMVP);
      mat4f_uniformMap.put("TextureMat", VRenderSystem::getTextureMatrix);
      vec1i_uniformMap.put("EndPortalLayers", (Supplier<Integer>)() -> 15);
      vec1i_uniformMap.put("FogShape", (Supplier<Integer>)() -> RenderSystem.getShaderFogShape().getIndex());
      vec1f_uniformMap.put("FogStart", RenderSystem::getShaderFogStart);
      vec1f_uniformMap.put("FogEnd", RenderSystem::getShaderFogEnd);
      vec1f_uniformMap.put("LineWidth", RenderSystem::getShaderLineWidth);
      vec1f_uniformMap.put("GameTime", RenderSystem::getShaderGameTime);
      vec1f_uniformMap.put("GlintAlpha", RenderSystem::getShaderGlintAlpha);
      vec1f_uniformMap.put("AlphaCutout", (Supplier<Float>)() -> VRenderSystem.alphaCutout);
      vec2f_uniformMap.put("ScreenSize", VRenderSystem::getScreenSize);
      vec3f_uniformMap.put("Light0_Direction", (Supplier<MappedBuffer>)() -> VRenderSystem.lightDirection0);
      vec3f_uniformMap.put("Light1_Direction", (Supplier<MappedBuffer>)() -> VRenderSystem.lightDirection1);
      vec3f_uniformMap.put("ModelOffset", (Supplier<MappedBuffer>)() -> VRenderSystem.modelOffset);
      vec3f_uniformMap.put("ChunkOffset", (Supplier<MappedBuffer>)() -> VRenderSystem.modelOffset);
      vec4f_uniformMap.put("ColorModulator", VRenderSystem::getShaderColor);
      vec4f_uniformMap.put("FogColor", VRenderSystem::getShaderFogColor);
   }

   public static Supplier<MappedBuffer> getUniformSupplier(String type, String name) {
      return switch (type) {
         case "mat4" -> (Supplier)mat4f_uniformMap.get(name);
         case "vec4" -> (Supplier)vec4f_uniformMap.get(name);
         case "vec3" -> (Supplier)vec3f_uniformMap.get(name);
         case "vec2" -> (Supplier)vec2f_uniformMap.get(name);
         default -> null;
      };
   }
}
