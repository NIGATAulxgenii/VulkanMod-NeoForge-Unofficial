package net.vulkanmod.render.chunk.buffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.model.quad.ModelQuad;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainBuilder;
import net.vulkanmod.render.vertex.VertexBuilder;
import net.vulkanmod.render.vertex.QuadSorter;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public final class TerrainGeometryCheck {
    public static void main(String[] args) {
        PipelineManager.setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        var section = new RenderSection(0, -16, -32, -48);
        require(DrawBuffers.getMask(new Vec3(-8, -24, -40), section) == 127, "inside camera lost faces");
        int mask = DrawBuffers.getMask(new Vec3(-17, -24, -40), section);
        require((mask & 1 << QuadFacing.X_POS.ordinal()) == 0 && (mask & 1 << QuadFacing.X_NEG.ordinal()) != 0, "negative-coordinate facing mask");
        require((mask & 1 << QuadFacing.UNDEFINED.ordinal()) != 0, "undefined geometry culled");

        var quad = new ModelQuad();
        for (int i = 0; i < 4; i++) {
            quad.setX(i, i == 1 || i == 2 ? 1 : 0);
            quad.setY(i, i >= 2 ? 1 : 0);
            quad.setZ(i, 0);
        }
        require(QuadFacing.fromQuad(quad, new Vector3f()) == QuadFacing.Z_POS, "quad winding/normal");
        quad.setZ(2, 0.5f);
        require(QuadFacing.fromQuad(quad, new Vector3f()) == QuadFacing.UNDEFINED, "slanted quad classified as axial");
        quad.setZ(2, 0);
        quad.setZ(3, 0.5f);
        require(QuadFacing.fromQuad(quad, new Vector3f()) == QuadFacing.UNDEFINED, "warped fourth vertex classified as axial");
        require(QuadFacing.fromQuad(quad, new Vector3f(-1, 0, 0)) == QuadFacing.UNDEFINED, "out-of-section model culled");

        long params = DrawParametersBuffer.allocateBuffer();
        try {
            long last = DrawParametersBuffer.getParamsPtr(params, 511, 4, 6);
            DrawParametersBuffer.setIndexCount(last, 24);
            DrawParametersBuffer.setVertexOffset(last, 70000);
            require(DrawParametersBuffer.getIndexCount(params) == 0 && DrawParametersBuffer.getIndexCount(last) == 24, "draw parameter slots overlap");
            DrawParametersBuffer.resetParameters(last);
            require(DrawParametersBuffer.getVertexOffset(last) == -1, "draw parameters not reset");
        } finally { DrawParametersBuffer.freeBuffer(params); }

        var builder = new TerrainBuilder(256, new VertexBuilder.CompressedVertexBuilder(), false);
        try {
            var vertices = builder.getBufferBuilder(QuadFacing.UNDEFINED.ordinal());
            builder.begin();
            for (int z : new int[] {1, 4}) {
                for (int i = 0; i < 4; i++) vertices.vertex(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0, z, -1, 0, 0, 0, 0);
            }
            builder.setupQuadSortingPoints();
            builder.setupQuadSorting(0, 0, 0);
            long pointer = MemoryUtil.memAddress(builder.getIndexBuffer());
            MemoryUtil.memPutInt(pointer + 48, 0x5a5a5a5a);
            var state = builder.endDrawing();
            require(state.indexCount() == 12 && state.indexType() == VertexFormat.IndexType.INT, "terrain sorted index metadata");
            require(builder.getIndexBuffer().getInt(0) == 4 && builder.getIndexBuffer().getInt(24) == 0, "terrain transparency order");
            require(MemoryUtil.memGetInt(pointer + 48) == 0x5a5a5a5a, "sorted index write overrun");
            var sorter = new QuadSorter();
            sorter.setupQuadSortingPoints(vertices.getPtr(), 8, CustomVertexFormat.COMPRESSED_TERRAIN);
            sorter.setQuadSortOrigin(0, 0, 0);
            MemoryUtil.memPutInt(pointer + 24, 0x5a5a5a5a);
            sorter.putSortedQuadIndices(builder, VertexFormat.IndexType.SHORT);
            require(MemoryUtil.memGetInt(pointer + 24) == 0x5a5a5a5a, "short sorted index write overrun");
        } finally { builder.free(); }

        for (var type : new AutoIndexBuffer.DrawType[] {AutoIndexBuffer.DrawType.QUADS, AutoIndexBuffer.DrawType.LINES}) {
            var indices = AutoIndexBuffer.genIntIndices(65540, type);
            try { require(indices.asIntBuffer().get(indices.remaining() / 4 - 2) >= 65536, "large indices wrapped"); }
            finally { MemoryUtil.memFree(indices); }
        }
        System.out.println("Terrain geometry PASS: facing masks, custom geometry, parameter slots, sorted index bounds, large indices");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
