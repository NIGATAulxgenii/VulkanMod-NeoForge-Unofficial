package net.vulkanmod.mixin.render.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.ModelQuadNormals;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.model.quad.ModelQuadFlags;
import net.vulkanmod.render.model.quad.ModelQuadView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public class BakedQuadM implements ModelQuadView {
   @Shadow
   @Final
   protected int[] vertices;
   @Shadow
   @Final
   protected Direction direction;
   @Shadow
   @Final
   protected int tintIndex;
   private int flags;
   private int normal;
   private volatile QuadFacing facing;

   @Override
   public int getFlags() {
      this.ensureInitialized();
      return this.flags;
   }

   @Override
   public float getX(int idx) {
      return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 0]);
   }

   @Override
   public float getY(int idx) {
      return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 1]);
   }

   @Override
   public float getZ(int idx) {
      return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 2]);
   }

   @Override
   public int getColor(int idx) {
      return this.vertices[vertexOffset(idx) + 3];
   }

   @Override
   public float getU(int idx) {
      return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 4]);
   }

   @Override
   public float getV(int idx) {
      return Float.intBitsToFloat(this.vertices[vertexOffset(idx) + 5]);
   }

   @Override
   public int getColorIndex() {
      return this.tintIndex;
   }

   @Override
   public Direction lightFace() {
      return this.direction;
   }

   @Override
   public Direction getFacingDirection() {
      return this.direction;
   }

   @Override
   public QuadFacing getQuadFacing() {
      this.ensureInitialized();
      return this.facing;
   }

   @Override
   public int getNormal() {
      this.ensureInitialized();
      return this.normal;
   }

   // Computed on first use: quads baked after a resource reload were seen without constructor init.
   private void ensureInitialized() {
      if (this.facing == null) {
         this.flags = ModelQuadFlags.getQuadFlags(this, this.direction);
         this.normal = ModelQuadNormals.computePackedNormal(this);
         this.facing = QuadFacing.fromNormal(this.normal);
      }
   }

   @Override
   public boolean isTinted() {
      return this.tintIndex != -1;
   }

   private static int vertexOffset(int vertexIndex) {
      return vertexIndex * 8;
   }
}
