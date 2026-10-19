package net.vulkanmod.mixin.render;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.shaders.Program.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.VkGlProgram;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineConfig;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public class ShaderInstanceM implements ShaderMixed {
   @Shadow
   @Final
   private Map<String, Uniform> uniformMap;
   @Shadow
   @Final
   private String name;
   @Shadow
   @Final
   @Nullable
   public Uniform MODEL_VIEW_MATRIX;
   @Shadow
   @Final
   @Nullable
   public Uniform PROJECTION_MATRIX;
   @Shadow
   @Final
   @Nullable
   public Uniform COLOR_MODULATOR;
   @Shadow
   @Final
   @Nullable
   public Uniform LINE_WIDTH;
   @Shadow
   @Final
   @Nullable
   public Uniform GLINT_ALPHA;
   @Shadow
   @Final
   @Nullable
   public Uniform FOG_START;
   @Shadow
   @Final
   @Nullable
   public Uniform FOG_END;
   @Shadow
   @Final
   @Nullable
   public Uniform FOG_COLOR;
   @Shadow
   @Final
   @Nullable
   public Uniform FOG_SHAPE;
   @Shadow
   @Final
   @Nullable
   public Uniform TEXTURE_MATRIX;
   @Shadow
   @Final
   @Nullable
   public Uniform GAME_TIME;
   @Shadow
   @Final
   @Nullable
   public Uniform SCREEN_SIZE;
   @Shadow
   @Final
   private Map<String, Object> samplerMap;
   @Shadow
   @Final
   private List<Integer> samplerLocations;
   @Shadow
   @Final
   private List<String> samplerNames;
   @Shadow
   @Final
   private List<Uniform> uniforms;
   @Shadow
   @Final
   private VertexFormat vertexFormat;
   @Shadow
   @Final
   private int programId;
   @Shadow
   private static int lastProgramId;
   @Unique
   private String vsPath;
   @Unique
   private String fsName;
   @Unique
   private GraphicsPipeline pipeline;
   @Unique
   boolean doUniformUpdate = false;

   @Override
   public GraphicsPipeline getPipeline() {
      return this.pipeline;
   }

   @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("RETURN"))
   private void create(ResourceProvider resourceProvider, ResourceLocation location, VertexFormat format, CallbackInfo ci) {
      String name = location.getNamespace().equals("minecraft") ? location.getPath() : location.toString();
      if (ShaderLoadUtil.REMAPPED_SHADERS.contains("core/" + name)) {
         this.createPipeline("core/", name, format);
      } else {
         this.createLegacyShader(resourceProvider, format);
      }

      VkGlProgram program = VkGlProgram.getProgram(this.programId);
      program.bindPipeline(this.pipeline);
   }

   @Redirect(
      method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ShaderInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;"
      )
   )
   private Program loadNames(ResourceProvider resourceProvider, Type type, String name) {
      String path;
      if (this.name.contains(String.valueOf(':'))) {
         ResourceLocation location = ResourceLocation.tryParse(name);
         path = location.withPath("shaders/core/%s".formatted(location.getPath())).toString();
      } else {
         path = "shaders/core/%s".formatted(name);
      }

      switch (type) {
         case VERTEX:
            this.vsPath = path;
            break;
         case FRAGMENT:
            this.fsName = path;
      }

      return null;
   }

   @Redirect(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"))
   private void bindAttr(int program, int index, CharSequence name) {
   }

   @Overwrite
   public void close() {
      if (this.pipeline != null) {
         this.pipeline.cleanUp();
      }
   }

   @Overwrite
   public void apply() {
      if (this.doUniformUpdate) {
         for (int j = 0; j < this.samplerLocations.size(); j++) {
            String string = this.samplerNames.get(j);
            if (this.samplerMap.get(string) != null) {
               Object object = this.samplerMap.get(string);
               int texId = -1;
               if (object instanceof RenderTarget) {
                  texId = ((RenderTarget)object).getColorTextureId();
               } else if (object instanceof AbstractTexture) {
                  texId = ((AbstractTexture)object).getId();
               } else if (object instanceof Integer) {
                  texId = (Integer)object;
               }

               if (texId != -1) {
                  RenderSystem.setShaderTexture(j, texId);
               }
            }
         }

         for (Uniform uniform : this.uniforms) {
            uniform.upload();
         }
      }

      if (this.programId != lastProgramId) {
         ProgramManager.glUseProgram(this.programId);
         lastProgramId = this.programId;
      }

      this.bindPipeline();
   }

   @Overwrite
   public void setDefaultUniforms(Mode mode, Matrix4f modelView, Matrix4f projection, Window window) {
      if (this.doUniformUpdate) {
         if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(modelView);
         }

         if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(net.vulkanmod.vulkan.VRenderSystem.toVulkanProjection(projection));
         }

         if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
         }

         if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
         }

         if (this.FOG_START != null) {
            this.FOG_START.set(RenderSystem.getShaderFogStart());
         }

         if (this.FOG_END != null) {
            this.FOG_END.set(RenderSystem.getShaderFogEnd());
         }

         if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(RenderSystem.getShaderFogColor());
         }

         if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
         }

         if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
         }

         if (this.GAME_TIME != null) {
            this.GAME_TIME.set(RenderSystem.getShaderGameTime());
         }

         if (this.SCREEN_SIZE != null) {
            this.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
         }

         if (this.LINE_WIDTH != null && (mode == Mode.LINES || mode == Mode.LINE_STRIP)) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
         }

         RenderSystem.setupShaderLights((ShaderInstance)(Object)this);
      }
   }

   @Overwrite
   public void clear() {
   }

   @Unique
   private void bindPipeline() {
      if (this.pipeline == null) {
         throw new NullPointerException("Shader %s has no initialized pipeline".formatted(this.name));
      } else {
         Renderer renderer = Renderer.getInstance();
         renderer.bindGraphicsPipeline(this.pipeline);
         VTextureSelector.bindShaderTextures(this.pipeline);
         renderer.uploadAndBindUBOs(this.pipeline);
      }
   }

   @Override
   public void setupUniformSuppliers(UBO ubo) {
      for (net.vulkanmod.vulkan.shader.layout.Uniform vUniform : ubo.getUniforms()) {
         Uniform uniform = this.uniformMap.get(vUniform.getName());
         ByteBuffer byteBuffer;
         if (uniform == null) {
            Initializer.LOGGER.error(String.format("Error: field %s not present in uniform map", vUniform.getName()));
            int size = vUniform.getSize();
            byteBuffer = MemoryUtil.memAlloc(size * 4);
         } else if (uniform.getType() <= 3) {
            byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
         } else {
            if (uniform.getType() > 10) {
               throw new RuntimeException("out of bounds value for uniform " + uniform);
            }

            byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
         }

         MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
         Supplier<MappedBuffer> supplier = () -> mappedBuffer;
         vUniform.setSupplier(supplier);
      }
   }

   @Override
   public Supplier<MappedBuffer> getUniformSupplier(String name) {
      Uniform uniform1 = this.uniformMap.get(name);
      if (uniform1 == null) {
         Initializer.LOGGER.error(String.format("Error: field %s not present in uniform map", name));
         return null;
      } else {
         ByteBuffer byteBuffer;
         if (uniform1.getType() <= 3) {
            byteBuffer = MemoryUtil.memByteBuffer(uniform1.getIntBuffer());
         } else {
            if (uniform1.getType() > 10) {
               throw new RuntimeException("out of bounds value for uniform " + uniform1);
            }

            byteBuffer = MemoryUtil.memByteBuffer(uniform1.getFloatBuffer());
         }

         MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
         return () -> mappedBuffer;
      }
   }

   @Override
   public void setDoUniformsUpdate() {
      this.doUniformUpdate = true;
   }

   @Override
   public void setPipeline(GraphicsPipeline graphicsPipeline) {
      this.pipeline = graphicsPipeline;
   }

   @Unique
   private void createPipeline(String sPath, String name, VertexFormat format) {
      Pipeline.Builder builder = new Pipeline.Builder(format, name);
      builder.setUniformSupplierGetter(info -> this.getUniformSupplier(info.name));
      String path = ShaderLoadUtil.resolveShaderPath(sPath);
      JsonObject config = ShaderLoadUtil.getJsonConfig(path, name);
      PipelineConfig pipelineConfig = PipelineConfig.fromJson(name, config);
      builder.applyConfig(pipelineConfig);
      builder.setShaderSrc(
         SPIRVUtils.ShaderKind.VERTEX_SHADER,
         ShaderLoadUtil.loadShader(path, "%s.vsh".formatted(pipelineConfig.shaderPaths.get(SPIRVUtils.ShaderKind.VERTEX_SHADER)))
      );
      builder.setShaderSrc(
         SPIRVUtils.ShaderKind.FRAGMENT_SHADER,
         ShaderLoadUtil.loadShader(path, "%s.fsh".formatted(pipelineConfig.shaderPaths.get(SPIRVUtils.ShaderKind.FRAGMENT_SHADER)))
      );
      GraphicsPipeline pipeline = builder.createGraphicsPipeline();
      this.pipeline = pipeline;
   }

   @Unique
   private void createLegacyShader(ResourceProvider resourceProvider, VertexFormat format) {
      try {
         String vertPath = this.vsPath + ".vsh";
         Resource resource = resourceProvider.getResourceOrThrow(ResourceLocation.tryParse(vertPath));
         InputStream inputStream = resource.open();
         String vshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
         String fragPath = this.fsName + ".fsh";
         resource = resourceProvider.getResourceOrThrow(ResourceLocation.tryParse(fragPath));
         inputStream = resource.open();
         String fshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
         String vsPath = "shaders/core/" + vertPath;
         String fsPath = "shaders/core/" + fragPath;
         GlslPreprocessor glslPreprocessor = ShaderLoadUtil.createPreprocessor(resourceProvider, ResourceLocation.tryParse("shaders/include/"), vsPath);
         vshSrc = String.join("", glslPreprocessor.process(vshSrc));
         glslPreprocessor = ShaderLoadUtil.createPreprocessor(resourceProvider, ResourceLocation.tryParse("shaders/include/"), fsPath);
         fshSrc = String.join("", glslPreprocessor.process(fshSrc));
         Pipeline.Builder builder = new Pipeline.Builder(format, this.name);
         Lexer lexer = new Lexer(vshSrc);
         GLSLParser parser = new GLSLParser();
         parser.setVertexFormat(format);

         try {
            parser.parse(lexer, GLSLParser.Stage.VERTEX);
            lexer = new Lexer(fshSrc);
            parser.parse(lexer, GLSLParser.Stage.FRAGMENT);
         } catch (Exception var22) {
            throw new RuntimeException("Caught exception while parsing: %s".formatted(this.name), var22);
         }

         UBO[] ubos = parser.createUBOs();
         List<ImageDescriptor> samplers = parser.getSamplerList();
         if (ubos.length > 0) {
            this.setupUniformSuppliers(ubos[0]);
         }

         String vshProcessed = parser.getOutput(GLSLParser.Stage.VERTEX);
         String fshProcessed = parser.getOutput(GLSLParser.Stage.FRAGMENT);
         builder.setUniforms(List.of(ubos), samplers);
         builder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER, vshProcessed);
         builder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, fshProcessed);

         for (ImageDescriptor imageDescriptor : samplers) {
            imageDescriptor.setImageIdx(VTextureSelector.getTextureIdx(imageDescriptor.name));
         }

         this.pipeline = builder.createGraphicsPipeline();
         this.doUniformUpdate = true;
      } catch (Exception var23) {
         Initializer.LOGGER.error("Error on shader {} conversion/compilation", this.name);
         var23.printStackTrace();
      }
   }
}
