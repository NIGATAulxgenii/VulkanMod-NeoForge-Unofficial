package net.vulkanmod.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource.EntityOutlineGenerator;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityOutlineGenerator.class)
public class EntityOutlineGeneratorM implements ExtendedVertexBuilder {
   @Shadow
   @Final
   private int comp_2852;
   @Unique
   private ExtendedVertexBuilder extDelegate;
   @Unique
   private boolean canUseFastVertex = false;

   @Inject(method = "<init>*", at = @At("RETURN"))
   private void getExtBuilder(VertexConsumer vertexConsumer, int i, int j, int k, int l, CallbackInfo ci) {
      if (vertexConsumer instanceof ExtendedVertexBuilder) {
         this.extDelegate = (ExtendedVertexBuilder)vertexConsumer;
         this.canUseFastVertex = true;
      }
   }

   @Override
   public boolean canUseFastVertex() {
      return this.canUseFastVertex;
   }

   @Override
   public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
      this.extDelegate.vertex(x, y, z, this.comp_2852, u, v, overlay, light, packedNormal);
   }
}
