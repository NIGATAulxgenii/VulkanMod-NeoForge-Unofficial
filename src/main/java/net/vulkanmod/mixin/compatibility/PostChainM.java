package net.vulkanmod.mixin.compatibility;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.server.ChainedJsonException;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PostChain.class)
public abstract class PostChainM {
   @Shadow
   private int screenWidth;
   @Shadow
   private int screenHeight;
   @Shadow
   @Final
   private Map<String, RenderTarget> customRenderTargets;
   @Shadow
   @Final
   private RenderTarget screenTarget;
   @Shadow
   @Final
   private List<PostPass> passes;
   @Shadow
   private float lastStamp;
   @Shadow
   private float time;

   @Shadow
   public abstract void addTempTarget(String var1, int var2, int var3);

   @Shadow
   protected abstract void parseTargetNode(JsonElement var1) throws ChainedJsonException;

   @Shadow
   protected abstract void parseUniformNode(JsonElement var1) throws ChainedJsonException;

   @Shadow
   protected abstract void setFilterMode(int var1);

   @Overwrite
   public void process(float f) {
      if (f < this.lastStamp) {
         this.time = this.time + (1.0F - this.lastStamp);
         this.time += f;
      } else {
         this.time = this.time + (f - this.lastStamp);
      }

      this.lastStamp = f;

      while (this.time > 20.0F) {
         this.time -= 20.0F;
      }

      int filterMode = 9728;

      for (PostPass postPass : this.passes) {
         int passFilterMode = postPass.getFilterMode();
         if (filterMode != passFilterMode) {
            this.setFilterMode(passFilterMode);
            filterMode = passFilterMode;
         }

         postPass.process(this.time / 20.0F);
      }

      this.setFilterMode(9728);
      Renderer.resetViewport();
   }
}
