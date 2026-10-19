package net.vulkanmod.mixin.matrix;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public abstract class Matrix4fM {
   @Shadow
   public abstract Matrix4f perspective(float var1, float var2, float var3, float var4, boolean var5);

   @Shadow
   public abstract Matrix4f ortho(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7);

   @Shadow
   public abstract Matrix4f setPerspective(float var1, float var2, float var3, float var4, boolean var5);

   @Shadow
   public abstract Matrix4f setOrtho(float var1, float var2, float var3, float var4, float var5, float var6, boolean var7);

   @Overwrite(remap = false)
   public Matrix4f setOrtho(float left, float right, float bottom, float top, float zNear, float zFar) {
      this.setOrtho(left, right, bottom, top, zNear, zFar, true);
      return (Matrix4f)(Object)this;
   }

   @Overwrite(remap = false)
   public Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
      return this.ortho(left, right, bottom, top, zNear, zFar, true);
   }

   @Overwrite(remap = false)
   public Matrix4f perspective(float fovy, float aspect, float zNear, float zFar) {
      return this.perspective(fovy, aspect, zNear, zFar, true);
   }

   @Overwrite(remap = false)
   public Matrix4f setPerspective(float fovy, float aspect, float zNear, float zFar) {
      this.setPerspective(fovy, aspect, zNear, zFar, true);
      return (Matrix4f)(Object)this;
   }
}
