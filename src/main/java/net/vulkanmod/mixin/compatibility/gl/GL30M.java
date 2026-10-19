package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlRenderbuffer;
import net.vulkanmod.gl.VkGlTexture;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GL30.class)
public class GL30M {
   @Overwrite(remap = false)
   public static void glGenerateMipmap(@NativeType("GLenum") int target) {
      VkGlTexture.generateMipmap(target);
   }

   @NativeType("void")
   @Overwrite(remap = false)
   public static int glGenFramebuffers() {
      return VkGlFramebuffer.genFramebufferId();
   }

   @Overwrite(remap = false)
   public static void glBindFramebuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
      VkGlFramebuffer.bindFramebuffer(target, framebuffer);
   }

   @Overwrite(remap = false)
   public static void glFramebufferTexture2D(
      @NativeType("GLenum") int target,
      @NativeType("GLenum") int attachment,
      @NativeType("GLenum") int textarget,
      @NativeType("GLuint") int texture,
      @NativeType("GLint") int level
   ) {
      VkGlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
   }

   @Overwrite(remap = false)
   public static void glFramebufferRenderbuffer(
      @NativeType("GLenum") int target,
      @NativeType("GLenum") int attachment,
      @NativeType("GLenum") int renderbuffertarget,
      @NativeType("GLuint") int renderbuffer
   ) {
   }

   @Overwrite(remap = false)
   public static void glDeleteFramebuffers(@NativeType("GLuint const *") int framebuffer) {
      VkGlFramebuffer.deleteFramebuffer(framebuffer);
   }

   @Overwrite(remap = false)
   @NativeType("GLenum")
   public static int glCheckFramebufferStatus(@NativeType("GLenum") int target) {
      return VkGlFramebuffer.glCheckFramebufferStatus(target);
   }

   @Overwrite(remap = false)
   public static void glBlitFramebuffer(
      @NativeType("GLint") int srcX0,
      @NativeType("GLint") int srcY0,
      @NativeType("GLint") int srcX1,
      @NativeType("GLint") int srcY1,
      @NativeType("GLint") int dstX0,
      @NativeType("GLint") int dstY0,
      @NativeType("GLint") int dstX1,
      @NativeType("GLint") int dstY1,
      @NativeType("GLbitfield") int mask,
      @NativeType("GLenum") int filter
   ) {
      VkGlFramebuffer.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
   }

   @NativeType("void")
   @Overwrite(remap = false)
   public static int glGenRenderbuffers() {
      return VkGlRenderbuffer.genId();
   }

   @Overwrite(remap = false)
   public static void glBindRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
      VkGlRenderbuffer.bindRenderbuffer(target, framebuffer);
   }

   @Overwrite(remap = false)
   public static void glRenderbufferStorage(
      @NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height
   ) {
      VkGlRenderbuffer.renderbufferStorage(target, internalformat, width, height);
   }

   @Overwrite(remap = false)
   public static void glDeleteRenderbuffers(@NativeType("GLuint const *") int renderbuffer) {
      VkGlRenderbuffer.deleteRenderbuffer(renderbuffer);
   }
}
