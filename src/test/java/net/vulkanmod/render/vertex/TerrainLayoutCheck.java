package net.vulkanmod.render.vertex;

import org.lwjgl.system.MemoryUtil;

public final class TerrainLayoutCheck {
    public static void main(String[] args) {
        var builder = new VertexBuilder.CompressedVertexBuilder();
        if (builder.getStride() != 16 || CustomVertexFormat.COMPRESSED_TERRAIN.getVertexSize() != 16)
            throw new IllegalStateException("Terrain vertex must occupy 16 bytes");
        long pointer = MemoryUtil.nmemAlloc(32);
        try {
            for (float coordinate : new float[] {-4, 0, 7.25f, 19.999f}) {
                MemoryUtil.memSet(pointer, 0x5a, 32);
                builder.vertex(pointer, coordinate, 3.5f, -2.25f, 0x78563412, 0.25f, 0.75f, 0x00f00070, 0);
                float decoded = MemoryUtil.memGetShort(pointer) / 2048.0f + 4;
                if (Math.abs(decoded - coordinate) > 1.0f / 2048) throw new IllegalStateException("Position quantization");
                if (MemoryUtil.memGetShort(pointer + 6) != (short) 0xf070) throw new IllegalStateException("Packed light");
                if (MemoryUtil.memGetInt(pointer + 8) != 0x78563412) throw new IllegalStateException("Color channels");
                if (MemoryUtil.memGetShort(pointer + 12) != 8192 || MemoryUtil.memGetShort(pointer + 14) != 24576)
                    throw new IllegalStateException("UV offsets");
                if (MemoryUtil.memGetLong(pointer + 16) != 0x5a5a5a5a5a5a5a5aL) throw new IllegalStateException("Vertex stride overrun");
            }
        } finally {
            MemoryUtil.nmemFree(pointer);
        }
        System.out.println("Terrain layout PASS: 16-byte stride, positions, light, color, UV, write bounds");
    }
}
