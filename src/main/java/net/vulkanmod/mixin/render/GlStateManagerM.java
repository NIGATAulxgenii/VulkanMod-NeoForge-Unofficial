package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.vulkanmod.gl.VkGlBuffer;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlProgram;
import net.vulkanmod.gl.VkGlRenderbuffer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlStateManager.class)
public class GlStateManagerM {
   @Overwrite(remap = false)
   public static void _glDeleteFramebuffers(int id) {
      VkGlFramebuffer.deleteFramebuffer(id);
   }

   @Overwrite(remap = false)
   public static void _glDeleteRenderbuffers(int id) {
      VkGlRenderbuffer.deleteRenderbuffer(id);
   }

   @Overwrite(remap = false)
   public static void _getTexImage(int target, int level, int format, int type, long pixels) {
      VkGlTexture.getTexImage(target, level, format, type, pixels);
   }

   @Overwrite(remap = false)
   public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                                       int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
      VkGlFramebuffer.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
   }

   @Overwrite(remap = false)
   public static void _bindTexture(int i) {
      VkGlTexture.bindTexture(i);
   }

   @Overwrite(remap = false)
   public static void _disableBlend() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.disableBlend();
   }

   @Overwrite(remap = false)
   public static void _enableBlend() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.enableBlend();
   }

   @Overwrite(remap = false)
   public static void _blendFunc(int i, int j) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.blendFunc(i, j);
   }

   @Overwrite(remap = false)
   public static void _blendFuncSeparate(int i, int j, int k, int l) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.blendFuncSeparate(i, j, k, l);
   }

   @Overwrite(remap = false)
   public static void _blendEquation(int i) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.blendOp(i);
   }

   @Overwrite(remap = false)
   public static void _disableScissorTest() {
      Renderer.resetScissor();
   }

   @Overwrite(remap = false)
   public static void _enableScissorTest() {
   }

   @Overwrite(remap = false)
   public static void _enableCull() {
      VRenderSystem.enableCull();
   }

   @Overwrite(remap = false)
   public static void _disableCull() {
      VRenderSystem.disableCull();
   }

   @Redirect(method = "_viewport", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V"), remap = false)
   private static void _viewport(int x, int y, int width, int height) {
      Renderer.setViewport(x, y, width, height);
   }

   @Overwrite(remap = false)
   public static void _scissorBox(int x, int y, int width, int height) {
      Renderer.setScissor(x, y, width, height);
   }

   @Overwrite(remap = false)
   public static int _getError() {
      return 0;
   }

   @Overwrite(remap = false)
   public static void _texImage2D(
      int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels
   ) {
      RenderSystem.assertOnRenderThread();
      VkGlTexture.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels != null ? MemoryUtil.memByteBuffer(pixels) : null);
   }

   @Overwrite(remap = false)
   public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
      RenderSystem.assertOnRenderThread();
      VkGlTexture.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
   }

   @Overwrite(remap = false)
   public static void _activeTexture(int i) {
      VkGlTexture.activeTexture(i);
   }

   @Overwrite(remap = false)
   public static void _texParameter(int i, int j, int k) {
      VkGlTexture.texParameteri(i, j, k);
   }

   @Overwrite(remap = false)
   public static void _texParameter(int i, int j, float k) {
   }

   @Overwrite(remap = false)
   public static int _getTexLevelParameter(int i, int j, int k) {
      return VkGlTexture.getTexLevelParameter(i, j, k);
   }

   @Overwrite(remap = false)
   public static void _pixelStore(int pname, int param) {
      RenderSystem.assertOnRenderThread();
      VkGlTexture.pixelStoreI(pname, param);
   }

   @Overwrite(remap = false)
   public static int _genTexture() {
      RenderSystem.assertOnRenderThread();
      return VkGlTexture.genTextureId();
   }

   @Overwrite(remap = false)
   public static void _deleteTexture(int i) {
      RenderSystem.assertOnRenderThread();
      VkGlTexture.glDeleteTextures(i);
   }

   @Overwrite(remap = false)
   public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.colorMask(red, green, blue, alpha);
   }

   @Overwrite(remap = false)
   public static void _polygonMode(int face, int mode) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.setPolygonModeGL(mode);
   }

   @Overwrite(remap = false)
   public static void _enablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.enablePolygonOffset();
   }

   @Overwrite(remap = false)
   public static void _disablePolygonOffset() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.disablePolygonOffset();
   }

   @Overwrite(remap = false)
   public static void _polygonOffset(float f, float g) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.polygonOffset(g, f);
   }

   @Overwrite(remap = false)
   public static void _enableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.enableColorLogicOp();
   }

   @Overwrite(remap = false)
   public static void _disableColorLogicOp() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.disableColorLogicOp();
   }

   @Overwrite(remap = false)
   public static void _logicOp(int i) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.logicOp(i);
   }

   @Overwrite(remap = false)
   public static void _clearColor(float f, float g, float h, float i) {
      RenderSystem.assertOnRenderThreadOrInit();
      VRenderSystem.setClearColor(f, g, h, i);
   }

   @Overwrite(remap = false)
   public static void _clearDepth(double d) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.clearDepth(d);
   }

   @Overwrite(remap = false)
   public static void _clear(int mask, boolean bl) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.clear(mask);
   }

   @Overwrite(remap = false)
   public static void _disableDepthTest() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.disableDepthTest();
   }

   @Overwrite(remap = false)
   public static void _enableDepthTest() {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.enableDepthTest();
   }

   @Overwrite(remap = false)
   public static void _depthFunc(int i) {
      RenderSystem.assertOnRenderThreadOrInit();
      VRenderSystem.depthFunc(i);
   }

   @Overwrite(remap = false)
   public static void _depthMask(boolean bl) {
      RenderSystem.assertOnRenderThread();
      VRenderSystem.depthMask(bl);
   }

   @Overwrite(remap = false)
   public static int glGenFramebuffers() {
      RenderSystem.assertOnRenderThread();
      return VkGlFramebuffer.genFramebufferId();
   }

   @Overwrite(remap = false)
   public static int glGenRenderbuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      return VkGlRenderbuffer.genId();
   }

   @Overwrite(remap = false)
   public static void _glBindFramebuffer(int i, int j) {
      RenderSystem.assertOnRenderThread();
      VkGlFramebuffer.bindFramebuffer(i, j);
   }

   @Overwrite(remap = false)
   public static void _glFramebufferTexture2D(int i, int j, int k, int l, int m) {
      RenderSystem.assertOnRenderThread();
      VkGlFramebuffer.framebufferTexture2D(i, j, k, l, m);
   }

   @Overwrite(remap = false)
   public static void _glBindRenderbuffer(int i, int j) {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlRenderbuffer.bindRenderbuffer(i, j);
   }

   @Overwrite(remap = false)
   public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlFramebuffer.framebufferRenderbuffer(i, j, k, l);
   }

   @Overwrite(remap = false)
   public static void _glRenderbufferStorage(int i, int j, int k, int l) {
      RenderSystem.assertOnRenderThreadOrInit();
      VkGlRenderbuffer.renderbufferStorage(i, j, k, l);
   }

   @Overwrite(remap = false)
   public static int glCheckFramebufferStatus(int i) {
      RenderSystem.assertOnRenderThreadOrInit();
      return VkGlFramebuffer.glCheckFramebufferStatus(i);
   }

   @Overwrite(remap = false)
   public static int _glGenBuffers() {
      RenderSystem.assertOnRenderThreadOrInit();
      return VkGlBuffer.glGenBuffers();
   }

   @Overwrite(remap = false)
   public static void _glBindBuffer(int i, int j) {
      RenderSystem.assertOnRenderThread();
      VkGlBuffer.glBindBuffer(i, j);
   }

   @Overwrite(remap = false)
   public static void _glBufferData(int i, ByteBuffer byteBuffer, int j) {
      RenderSystem.assertOnRenderThread();
      VkGlBuffer.glBufferData(i, byteBuffer, j);
   }

   @Overwrite(remap = false)
   public static void _glBufferData(int i, long l, int j) {
      RenderSystem.assertOnRenderThread();
      VkGlBuffer.glBufferData(i, l, j);
   }

   @Overwrite(remap = false)
   @Nullable
   public static ByteBuffer _glMapBuffer(int i, int j) {
      RenderSystem.assertOnRenderThreadOrInit();
      return VkGlBuffer.glMapBuffer(i, j);
   }

   @Overwrite(remap = false)
   public static void _glUnmapBuffer(int i) {
      RenderSystem.assertOnRenderThread();
      VkGlBuffer.glUnmapBuffer(i);
   }

   @Overwrite(remap = false)
   public static void _glDeleteBuffers(int i) {
      RenderSystem.assertOnRenderThread();
      VkGlBuffer.glDeleteBuffers(i);
   }

   @Overwrite(remap = false)
   public static void _disableVertexAttribArray(int i) {
   }

   @Overwrite(remap = false)
   public static void _glUseProgram(int i) {
      RenderSystem.assertOnRenderThread();
      VkGlProgram.glUseProgram(i);
   }

   @Overwrite(remap = false)
   public static int glCreateProgram() {
      RenderSystem.assertOnRenderThread();
      return VkGlProgram.genProgramId();
   }

   @Overwrite(remap = false)
   public static void glDeleteProgram(int i) {
      RenderSystem.assertOnRenderThread();
   }

   @Overwrite(remap = false)
   public static int _glGenVertexArrays() {
      RenderSystem.assertOnRenderThreadOrInit();
      return 0;
   }
}
