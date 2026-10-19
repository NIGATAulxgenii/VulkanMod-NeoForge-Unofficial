package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Map<String, ShaderInstance> shaders;
   @Shadow
   @Nullable
   private static ShaderInstance positionShader;
   @Shadow
   @Nullable
   private static ShaderInstance positionColorShader;
   @Shadow
   @Nullable
   private static ShaderInstance positionTexShader;
   @Shadow
   @Nullable
   private static ShaderInstance positionTexColorShader;
   @Shadow
   @Nullable
   private static ShaderInstance rendertypeTextShader;
   @Shadow
   @Nullable
   private static ShaderInstance rendertypeGuiShader;
   @Shadow
   @Nullable
   private static ShaderInstance rendertypeGuiOverlayShader;
   @Shadow
   public ShaderInstance blitShader;

   @Shadow
   protected abstract ShaderInstance preloadShader(ResourceProvider var1, String var2, VertexFormat var3);

   @Shadow
   public abstract float getRenderDistance();

   @Overwrite
   private void shutdownShaders() {
      RenderSystem.assertOnRenderThread();
      ImmutableList<ShaderInstance> clearList = ImmutableList.copyOf(this.shaders.values());
      MemoryManager.getInstance().addFrameOp(() -> clearList.forEach(ShaderInstance::close));
      this.shaders.clear();
   }

   @Overwrite
   public void preloadUiShader(ResourceProvider resourceProvider) {
      if (this.blitShader != null) {
         throw new RuntimeException("Blit shader already preloaded");
      } else {
         try {
            this.blitShader = new ShaderInstance(resourceProvider, "blit_screen", DefaultVertexFormat.POSITION_TEX);
         } catch (IOException var3) {
            throw new RuntimeException("could not preload blit shader", var3);
         }

         positionShader = this.preloadShader(resourceProvider, "position", DefaultVertexFormat.POSITION);
         positionColorShader = this.preloadShader(resourceProvider, "position_color", DefaultVertexFormat.POSITION_COLOR);
         positionTexShader = this.preloadShader(resourceProvider, "position_tex", DefaultVertexFormat.POSITION_TEX);
         positionTexColorShader = this.preloadShader(resourceProvider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
         rendertypeTextShader = this.preloadShader(resourceProvider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
         rendertypeGuiShader = positionColorShader;
         rendertypeGuiOverlayShader = positionColorShader;
      }
   }

   @Overwrite
   public float getDepthFar() {
      return Float.POSITIVE_INFINITY;
   }
}
