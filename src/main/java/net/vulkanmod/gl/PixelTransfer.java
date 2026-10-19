package net.vulkanmod.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

final class PixelTransfer {
    static int components(int format) {
        return switch (format) {
            case GL11.GL_RED -> 1;
            case GL30.GL_RG -> 2;
            case GL11.GL_RGB -> 3;
            case GL11.GL_RGBA, GL30.GL_BGRA -> 4;
            default -> throw new IllegalArgumentException("Unsupported pixel format: " + format);
        };
    }

    static int requiredBytes(int width, int height, int components, int rowLength, int skipRows, int skipPixels, int alignment) {
        if (width < 0 || height < 0 || rowLength < 0 || skipRows < 0 || skipPixels < 0)
            throw new IllegalArgumentException("Negative pixel transfer dimensions");
        if (alignment != 1 && alignment != 2 && alignment != 4 && alignment != 8)
            throw new IllegalArgumentException("Invalid pixel alignment: " + alignment);
        if (width == 0 || height == 0) return 0;
        long stride = ((long) (rowLength == 0 ? width : rowLength) * components + alignment - 1) / alignment * alignment;
        return Math.toIntExact((skipRows + (long) height - 1) * stride + (skipPixels + (long) width) * components);
    }

    static ByteBuffer unpack(ByteBuffer source, int width, int height, int format, int type,
                             int destinationComponents, int rowLength, int skipRows, int skipPixels, int alignment) {
        int components = components(format);
        boolean reversed = type == GL30.GL_UNSIGNED_INT_8_8_8_8;
        if (type != GL11.GL_UNSIGNED_BYTE && type != GL11.GL_BYTE
                && type != GL30.GL_UNSIGNED_INT_8_8_8_8_REV && !reversed)
            throw new IllegalArgumentException("Unsupported pixel type: " + type);
        if ((reversed || type == GL30.GL_UNSIGNED_INT_8_8_8_8_REV) && components != 4)
            throw new IllegalArgumentException("Packed pixel type requires four components");
        if (components != destinationComponents && !(components == 3 && destinationComponents == 4))
            throw new IllegalArgumentException("Pixel format does not match texture storage");
        int required = requiredBytes(width, height, components, rowLength, skipRows, skipPixels, alignment);
        if (source.remaining() < required) throw new IllegalArgumentException("Pixel buffer is too small");
        int stride = Math.toIntExact(((long) (rowLength == 0 ? width : rowLength) * components + alignment - 1) / alignment * alignment);
        ByteBuffer result = MemoryUtil.memAlloc(Math.multiplyExact(Math.multiplyExact(width, height), destinationComponents));
        int origin = source.position() + skipRows * stride + skipPixels * components;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = origin + y * stride + x * components;
                for (int channel = 0; channel < destinationComponents; channel++) {
                    int inputChannel = format == GL30.GL_BGRA && channel < 3 ? 2 - channel : channel;
                    result.put(channel < components ? source.get(pixel + (reversed ? 3 - inputChannel : inputChannel)) : (byte) 255);
                }
            }
        }
        return result.flip();
    }
}
