package net.vulkanmod.smoke;

import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.task.CompileResult;
import net.vulkanmod.render.vertex.TerrainBuilder;
import net.vulkanmod.render.vertex.VertexBuilder;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

public final class CancelledUploadCheck {
    public static void run() throws ReflectiveOperationException {
        var constructor = CompileResult.class.getDeclaredConstructor(RenderSection.class, boolean.class);
        constructor.setAccessible(true);
        var renderer = WorldRenderer.getInstance();
        var dispatcher = renderer.getTaskDispatcher();
        var area = new ChunkArea(0, new Vector3i(), 0);
        if (renderer.getChunkAreaManager().getChunkArea(0) == area) throw new IllegalStateException("Area is not stale");
        var section = new RenderSection(0, 0, 0, 0);
        section.setChunkArea(area);
        var builder = new TerrainBuilder(256, new VertexBuilder.CompressedVertexBuilder(), false);
        var vertices = builder.getBufferBuilder(QuadFacing.UNDEFINED.ordinal());
        TrackingUpload solid = null, translucent = null, sorted = null;
        try {
            builder.begin();
            vertices.vertex(0, 0, 0, -1, 0, 0, 0, 0);
            vertices.vertex(1, 0, 0, -1, 0, 0, 0, 0);
            vertices.vertex(1, 1, 0, -1, 0, 0, 0, 0);
            vertices.vertex(0, 1, 0, -1, 0, 0, 0, 0);
            builder.setupQuadSortingPoints();
            builder.setupQuadSorting(0, 0, 0);
            var sortState = builder.getSortState();
            var rendered = builder.endDrawing();
            solid = new TrackingUpload(builder, rendered);
            translucent = new TrackingUpload(builder, rendered);
            builder.clear();
            var full = constructor.newInstance(section, true);
            full.renderedLayers.put(TerrainRenderType.SOLID, solid);
            full.renderedLayers.put(TerrainRenderType.TRANSLUCENT, translucent);
            dispatcher.scheduleSectionUpdate(full);

            builder.begin();
            builder.restoreSortState(sortState);
            builder.setupQuadSorting(1, 1, 1);
            rendered = builder.endDrawing();
            sorted = new TrackingUpload(builder, rendered);
            builder.clear();
            if (!sorted.indexOnly || sorted.getIndexBuffer() == null) throw new IllegalStateException("Missing index-only fixture");
            var resort = constructor.newInstance(section, false);
            resort.renderedLayers.put(TerrainRenderType.TRANSLUCENT, sorted);
            dispatcher.scheduleSectionUpdate(resort);
            dispatcher.updateSections();
            for (var upload : new TrackingUpload[] {solid, translucent, sorted}) {
                if (upload.releases != 1) throw new IllegalStateException("Dismissed upload released " + upload.releases + " times");
            }
        } finally {
            for (var upload : new TrackingUpload[] {solid, translucent, sorted}) {
                if (upload != null && upload.releases == 0) upload.release();
            }
            builder.free();
            area.getDrawBuffers().free();
        }
    }

    private static final class TrackingUpload extends UploadBuffer {
        int releases;

        TrackingUpload(TerrainBuilder builder, TerrainBuilder.DrawState state) {
            super(builder, state);
        }

        @Override
        public void release() {
            if (++releases != 1) throw new IllegalStateException("Upload released twice");
            super.release();
        }
    }
}
