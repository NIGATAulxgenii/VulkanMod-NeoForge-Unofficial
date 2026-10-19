package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(GlStateManager.class)
public class GlStateManagerM {

    // UnihexProvider uploads through this helper, bypassing NativeImage._upload.
    // LWJGL's bootstrap GL11 cannot be transformed by our GL compatibility mixins.
    @Redirect(method = "_upload", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glTexSubImage2D(IIIIIIIILjava/nio/IntBuffer;)V"), remap = false)
    private static void uploadGlyphToVulkan(int target, int level, int x, int y, int width, int height,
                                          int format, int type, IntBuffer pixels) {
        GlTexture.texSubImage2D(target, level, x, y, width, height, format, type, MemoryUtil.memByteBuffer(pixels));
    }

    /**
     * @author NeoForge port
     * @reason LWJGL classes live in NeoForge's bootstrap layer. Translate buffer
     *         destruction at Minecraft's native render API instead of relying on GL30 mixins.
     */
    @Overwrite(remap = false)
    public static void _glDeleteFramebuffers(int framebuffer) {
        GlFramebuffer.deleteFramebuffer(framebuffer);
    }

    /** @author NeoForge port @reason Remove the renderbuffer registration. */
    @Overwrite(remap = false)
    public static void _glDeleteRenderbuffers(int renderbuffer) {
        GlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _bindTexture(int i) {
        GlTexture.bindTexture(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _disableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.disableBlend();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _enableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.enableBlend();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _blendFunc(int i, int j) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFunc(i, j);

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _blendFuncSeparate(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFuncSeparate(i, j, k, l);

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _disableScissorTest() {
        Renderer.resetScissor();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _enableScissorTest() {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _enableCull() {
        VRenderSystem.enableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _disableCull() {
        VRenderSystem.disableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _viewport(int x, int y, int width, int height) {
        Renderer.setViewport(x, y, width, height);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _scissorBox(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    //TODO
    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int _getError() {
        return 0;
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
        GlTexture.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels != null ? MemoryUtil.memByteBuffer(pixels) : null);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
        GlTexture.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _activeTexture(int i) {
        GlTexture.activeTexture(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, int k) {
        GlTexture.texParameteri(i, j, k);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, float k) {
        //TODO
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int _getTexLevelParameter(int i, int j, int k) {
        return GlTexture.getTexLevelParameter(i, j, k);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _pixelStore(int pname, int param) {
        GlTexture.pixelStoreI(pname, param);
    }

    /** @author NeoForge port @reason Route texture readback around bootstrap OpenGL. */
    @Overwrite(remap = false)
    public static void _getTexImage(int target, int level, int format, int type, long pixels) {
        GlTexture.getTexImage(target, level, format, type, pixels);
    }

    /** @author NeoForge port @reason Translate framebuffer copies without an OpenGL context. */
    @Overwrite(remap = false)
    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                                         int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GlFramebuffer.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int _genTexture() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlTexture.genTextureId();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _deleteTexture(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.glDeleteTextures(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _depthFunc(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.depthFunc(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _clearColor(float f, float g, float h, float i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.setClearColor(f, g, h, i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _clearDepth(double d) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _clear(int mask, boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.clear(mask);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glUseProgram(int i) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.disableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _depthMask(boolean bl) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.depthMask(bl);

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int glGenFramebuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlFramebuffer.genFramebufferId();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int glGenRenderbuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlRenderbuffer.genId();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glBindFramebuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.bindFramebuffer(i, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glFramebufferTexture2D(int i, int j, int k, int l, int m) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferTexture2D(i, j, k, l, m);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glBindRenderbuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.bindRenderbuffer(i, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferRenderbuffer(i, j, k, l);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glRenderbufferStorage(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.renderbufferStorage(i, j, k, l);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int glCheckFramebufferStatus(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlFramebuffer.glCheckFramebufferStatus(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int _glGenBuffers() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlBuffer.glGenBuffers();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glBindBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBindBuffer(i, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glBufferData(int i, ByteBuffer byteBuffer, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, byteBuffer, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glBufferData(int i, long l, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, l, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    @Nullable
    public static ByteBuffer _glMapBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlBuffer.glMapBuffer(i, j);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glUnmapBuffer(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glUnmapBuffer(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _glDeleteBuffers(int i) {
        RenderSystem.assertOnRenderThread();
        GlBuffer.glDeleteBuffers(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _disableVertexAttribArray(int i) {}
}
