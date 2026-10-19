package net.vulkanmod.smoke;

import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.vulkanmod.mixin.texture.image.NativeImageAccessor;

public final class PortCompatibilityCheck {
    public static void run() {
        nativeFormats();
        int source = GlTexture.genTextureId(), destination = GlTexture.genTextureId();
        int read = GlFramebuffer.genFramebufferId(), draw = GlFramebuffer.genFramebufferId();
        ByteBuffer pixels = MemoryUtil.memAlloc(64), result = MemoryUtil.memAlloc(64);
        try {
            resetUnpack();
            GlTexture.bindTexture(source);
            for (int i = 0; i < 64; i += 4) pixels.putInt(i, 0xff0000b5);
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 4, 4, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer) null);
            GlStateManager._texSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 4, 4, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(pixels));
            GlTexture.texImage2D(GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GlStateManager._getTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(result));
            require(result.getInt(0) == 0xff0000b5 && result.getInt(60) == 0xff0000b5, "Mip growth discarded base pixels");
            require(GlTexture.getTexLevelParameter(GL11.GL_TEXTURE_2D, 1, GL11.GL_TEXTURE_WIDTH) == 2, "Mip dimensions");
            GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 4);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 1);
            GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 1);
            for (int i = 0; i < 64; i += 4) pixels.putInt(i, 0xffb50000);
            GlTexture.texSubImage2D(GL11.GL_TEXTURE_2D, 1, 0, 0, 2, 2, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            GlStateManager._getTexImage(GL11.GL_TEXTURE_2D, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(result));
            require(result.getInt(0) == 0xffb50000 && result.getInt(12) == 0xffb50000, "Mip/strided upload");
            resetUnpack();
            GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, read);
            require(GlFramebuffer.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT, "Empty framebuffer reports complete");
            GlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, source, 0);
            GlTexture.bindTexture(destination);
            GlTexture.texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 2, 2, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GlFramebuffer.framebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, destination, 0);
            GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._glBlitFrameBuffer(1, 1, 3, 3, 0, 0, 2, 2, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            GlStateManager._getTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, MemoryUtil.memAddress(result));
            require(result.getInt(0) == 0xff0000b5 && result.getInt(12) == 0xff0000b5, "Framebuffer blit copied the wrong source or destination");
            int pbo = GlBuffer.glGenBuffers();
            try {
                GlBuffer.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, pbo);
                pixels.clear().limit(16);
                GlBuffer.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, pixels, GL30.GL_STATIC_DRAW);
                GlTexture.texSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 2, 2, GL30.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0L);
                GlBuffer.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, 0);
                GlBuffer.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, pbo);
                GlBuffer.glBufferData(GL30.GL_PIXEL_PACK_BUFFER, 20, GL30.GL_STATIC_READ);
                GlStateManager._getTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 4L);
                ByteBuffer mapped = GlBuffer.glMapBuffer(GL30.GL_PIXEL_PACK_BUFFER, GL30.GL_READ_ONLY);
                require(mapped.getInt(4) == 0xff0000b5 && mapped.getInt(16) == 0xff0000b5, "PBO upload/readback or BGRA conversion");
            } finally { GlBuffer.glDeleteBuffers(pbo); }
            Initializer.LOGGER.info("SMOKE compatibility PASS: native API upload/readback, mip growth preservation, mip dimensions, pixel strides/skips, framebuffer status/rectangular blit, BGRA/PBO");
        } finally {
            resetUnpack();
            GlFramebuffer.deleteFramebuffer(read);
            GlFramebuffer.deleteFramebuffer(draw);
            GlTexture.glDeleteTextures(source);
            GlTexture.glDeleteTextures(destination);
            GlTexture.bindTexture(0);
            MemoryUtil.memFree(pixels);
            MemoryUtil.memFree(result);
        }
    }

    private static void nativeFormats() {
        var formats = NativeImage.Format.values();
        var storage = NativeImage.InternalGlFormat.values();
        for (int index = 0; index < formats.length; index++) {
            var format = formats[index];
            int texture = TextureUtil.generateTextureId();
            try (var source = new NativeImage(format, 2, 2, false);
                 var destination = new NativeImage(format, 2, 2, false)) {
                TextureUtil.prepareImage(storage[index], texture, 0, 2, 2);
                long image = GlTexture.getTexture(texture).getVulkanImage().getId();
                TextureUtil.prepareImage(storage[index], texture, 0, 2, 2);
                require(GlTexture.getTexture(texture).getVulkanImage().getId() == image, "Repeated texture preparation reallocates storage");
                long pointer = ((NativeImageAccessor) (Object) source).getPixels();
                int bytes = 4 * format.components();
                for (int pixel = 0; pixel < bytes; pixel++) MemoryUtil.memPutByte(pointer + pixel, (byte) (17 + pixel));
                source.upload(0, 0, 0, false);
                destination.downloadTexture(0, false);
                long read = ((NativeImageAccessor) (Object) destination).getPixels();
                for (int pixel = 0; pixel < bytes; pixel++) require(MemoryUtil.memGetByte(read + pixel) == (byte) (17 + pixel), "Native format round trip: " + format);
                if (format == NativeImage.Format.RGBA) {
                    destination.downloadTexture(0, true);
                    require(destination.getPixelRGBA(0, 0) == (source.getPixelRGBA(0, 0) | 0xff000000), "Removing alpha swapped texture color channels");
                }
            } finally { TextureUtil.releaseTextureId(texture); }
        }
        Initializer.LOGGER.info("SMOKE native texture formats PASS: RGBA, RGB, RG, RED; storage reuse and upload/readback");
    }

    private static void resetUnpack() {
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
