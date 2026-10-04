package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Adapts NeoForge's section geometry callbacks to Vulkan's terrain format. */
public final class TerrainVertexConsumer implements VertexConsumer {
    private final TerrainBufferBuilder builder;
    private float x, y, z, u, v;
    private int color, light, normal;
    private boolean pending;

    public TerrainVertexConsumer(TerrainBufferBuilder builder) {
        this.builder = builder;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        finish();
        this.x = x;
        this.y = y;
        this.z = z;
        color = -1;
        u = v = 0;
        light = normal = 0;
        pending = true;
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        color = (red & 255) | (green & 255) << 8 | (blue & 255) << 16 | (alpha & 255) << 24;
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this; // Terrain has no overlay attribute.
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        light = (u & 65535) | (v & 65535) << 16;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        normal = VertexUtil.packNormal(x, y, z);
        return this;
    }

    public void finish() {
        if (!pending) return;
        builder.ensureCapacity();
        builder.vertex(x, y, z, color, u, v, light, normal);
        pending = false;
    }
}
