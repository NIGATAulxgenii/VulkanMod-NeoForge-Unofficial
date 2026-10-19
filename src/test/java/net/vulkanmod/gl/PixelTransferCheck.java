package net.vulkanmod.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class PixelTransferCheck {
    public static void main(String[] args) {
        ByteBuffer source = ByteBuffer.allocate(40);
        source.position(3);
        source.put(3 + 12 + 3, (byte) 11).put(3 + 12 + 4, (byte) 22).put(3 + 12 + 5, (byte) 33);
        source.put(3 + 24 + 3, (byte) 44).put(3 + 24 + 4, (byte) 55).put(3 + 24 + 5, (byte) 66);
        ByteBuffer pixels = PixelTransfer.unpack(source, 1, 2, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, 4, 3, 1, 1, 4);
        try {
            require(pixels.remaining() == 8 && pixels.get(0) == 11 && pixels.get(1) == 22 && pixels.get(2) == 33
                    && pixels.get(3) == (byte) 255 && pixels.get(4) == 44 && pixels.get(6) == 66, "RGB stride/skip/alpha");
            require(source.position() == 3, "Source position changed");
        } finally { MemoryUtil.memFree(pixels); }
        pixels = PixelTransfer.unpack(ByteBuffer.wrap(new byte[]{33, 22, 11, 44}), 1, 1, GL30.GL_BGRA,
                GL11.GL_UNSIGNED_BYTE, 4, 0, 0, 0, 4);
        try { require(pixels.get(0) == 11 && pixels.get(1) == 22 && pixels.get(2) == 33 && pixels.get(3) == 44, "BGRA channels"); }
        finally { MemoryUtil.memFree(pixels); }
        boolean failed = false;
        try { PixelTransfer.unpack(ByteBuffer.allocate(3), 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 4, 0, 0, 0, 4); }
        catch (IllegalArgumentException expected) { failed = true; }
        require(failed, "Short pixel buffer accepted");
        int id = GlBuffer.glGenBuffers();
        try {
            GlBuffer.glBindBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, id);
            GlBuffer.glBufferData(GL30.GL_PIXEL_UNPACK_BUFFER, ByteBuffer.wrap(new byte[]{7, 8, 9}), GL30.GL_STATIC_DRAW);
            ByteBuffer mapped = GlBuffer.glMapBuffer(GL30.GL_PIXEL_UNPACK_BUFFER, GL30.GL_READ_ONLY);
            require(mapped.remaining() == 3 && mapped.get(1) == 8, "PBO data not copied");
        } finally { GlBuffer.glDeleteBuffers(id); }
        require(GlBuffer.getPixelUnpackBufferBound() == null, "Deleted PBO remains bound");
        System.out.println("Pixel transfer PASS: stride/alignment/skips, source position, RGB/BGRA, bounds, PBO data/deletion");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
