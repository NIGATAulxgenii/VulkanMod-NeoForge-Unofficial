package net.vulkanmod.smoke;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.buffer.DrawParametersBuffer;
import net.vulkanmod.render.chunk.cull.QuadFacing;
import net.vulkanmod.render.sky.CloudRenderer;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.lang.reflect.Field;
import java.util.Collection;

public final class PortRenderCheck {
    private final boolean originalCulling = Initializer.CONFIG.backFaceCulling;
    private final boolean originalIndirect = Initializer.CONFIG.indirectDraw;
    private final CloudStatus originalClouds;
    private int step, ticks, referenceIndices;
    private CloudRenderer clouds;
    private Object cachedCloudBuffer;
    private int cachedX, cachedZ;
    private final int originalCloudDistance = Initializer.CONFIG.cloudDistance;

    public PortRenderCheck(Minecraft mc) {
        originalClouds = mc.options.cloudStatus().get();
        Initializer.CONFIG.cloudDistance = 6;
        teleport(mc, 0.5, 202, 20.5, 15);
        mode(mc, 0);
    }

    public boolean tick(Minecraft mc) throws Exception {
        mc.player.setYRot(180);
        mc.player.setXRot(step >= 4 && step <= 6 ? -20 : 15);
        if (++ticks < (step < 4 ? 60 : 30)) return false;
        ticks = 0;
        if (step < 4) {
            verifyGeometry(mc, step < 2);
            VulkanSmoke.screenshot(mc, "terrain-" + ((step & 1) == 0 ? "direct" : "indirect") + (step < 2 ? "-culling" : "-no-culling"));
            step++;
            if (step < 4) mode(mc, step);
            else {
                Initializer.CONFIG.backFaceCulling = originalCulling;
                Initializer.CONFIG.indirectDraw = originalIndirect;
                mc.levelRenderer.allChanged();
                mc.options.cloudStatus().set(CloudStatus.FANCY);
                teleport(mc, -30.5, 170, -30.5, -20);
            }
        } else if (step == 4) {
            clouds = cloudRenderer(mc);
            cachedCloudBuffer = field(clouds, "cloudBuffer");
            require(cachedCloudBuffer != null && (int) field(cachedCloudBuffer, "vertexCount") > 0, "Cloud VBO was not built");
            cachedX = (int) field(clouds, "prevCloudX");
            cachedZ = (int) field(clouds, "prevCloudZ");
            VulkanSmoke.screenshot(mc, "clouds-fancy");
            step++;
        } else if (step == 5) {
            if (cachedX != (int) field(clouds, "prevCloudX") || cachedZ != (int) field(clouds, "prevCloudZ")) {
                step = 4;
                return false;
            }
            require(field(clouds, "cloudBuffer") == cachedCloudBuffer, "Cloud cache rebuilt without grid/mode changes");
            mc.options.cloudStatus().set(CloudStatus.FAST);
            step++;
        } else if (step == 6) {
            require(field(clouds, "cloudBuffer") != cachedCloudBuffer && field(clouds, "prevCloudsType") == CloudStatus.FAST, "Cloud mode did not rebuild geometry");
            VulkanSmoke.screenshot(mc, "clouds-fast");
            cachedCloudBuffer = field(clouds, "cloudBuffer");
            clouds.resetBuffer();
            require(field(clouds, "cloudBuffer") == null, "Cloud reset retained VBO");
            step++;
        } else if (step == 7) {
            require(field(clouds, "cloudBuffer") != null && field(clouds, "cloudBuffer") != cachedCloudBuffer, "Cloud buffer not recreated");
            cachedCloudBuffer = field(clouds, "cloudBuffer");
            Initializer.CONFIG.cloudDistance = 12;
            step++;
        } else if (step == 8) {
            require(field(clouds, "cloudBuffer") != cachedCloudBuffer && (int) field(clouds, "prevCloudDistance") == 12, "Cloud distance did not rebuild geometry");
            require(net.vulkanmod.vulkan.shader.Uniforms.vec1f_uniformMap.get("FogCloudsEnd").get() == 192.0f, "Cloud fog follows terrain instead of the independent distance");
            Initializer.CONFIG.cloudDistance = originalCloudDistance;
            mc.options.cloudStatus().set(originalClouds);
            teleport(mc, 0.5, 202, 20.5, 15);
            step++;
        } else if (step == 9) {
            require(hasBeacon(mc), "Beacon not present in native global block entities");
            mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().setBlock(new BlockPos(2, 201, 2), Blocks.AIR.defaultBlockState(), 3));
            step++;
        } else if (step == 10) {
            require(!hasBeacon(mc), "Removed beacon retained by native global block entities");
            mc.gameRenderer.loadEffect(net.minecraft.resources.ResourceLocation.withDefaultNamespace("shaders/post/invert.json"));
            require(field(mc.gameRenderer, "postEffect") != null, "Vanilla invert post effect did not load");
            step++;
        } else if (step == 11) {
            VulkanSmoke.screenshot(mc, "post-invert");
            mc.gameRenderer.shutdownEffect();
            mc.gameRenderer.loadEffect(net.minecraft.resources.ResourceLocation.withDefaultNamespace("shaders/post/creeper.json"));
            require(field(mc.gameRenderer, "postEffect") != null, "Vanilla creeper post effect did not load");
            step++;
        } else if (step == 12) {
            VulkanSmoke.screenshot(mc, "post-creeper");
            mc.gameRenderer.shutdownEffect();
            step++;
        } else {
            Initializer.LOGGER.info("SMOKE port renderer PASS: direct/indirect, culling on/off, identical geometry counts, negative camera, fancy/fast clouds, cache reuse/reset, global beacon removal");
            Initializer.LOGGER.info("SMOKE post effects PASS: vanilla invert/creeper shaders, intermediate framebuffers, removal");
            return true;
        }
        return false;
    }

    private void mode(Minecraft mc, int mode) {
        Initializer.CONFIG.indirectDraw = (mode & 1) != 0;
        Initializer.CONFIG.backFaceCulling = mode < 2;
        if ((mode & 1) == 0) mc.levelRenderer.allChanged();
    }

    private void verifyGeometry(Minecraft mc, boolean culling) {
        require(WorldRenderer.getInstance().isSectionCompiled(new BlockPos(0, 201, 0)), "Fixture section not rebuilt");
        var renderer = WorldRenderer.getInstance();
        var section = new RenderSection(0, 0, 192, 0);
        var area = renderer.getChunkAreaManager().getChunkArea(section, 0, 192, 0);
        section.setChunkArea(area);
        int total = 0, directional = 0;
        for (var layer : TerrainRenderType.VALUES) for (var facing : QuadFacing.VALUES) {
            long ptr = DrawParametersBuffer.getParamsPtr(area.getDrawBuffers().getDrawParamsPtr(), section.inAreaIndex, layer.ordinal(), facing.ordinal());
            int count = DrawParametersBuffer.getIndexCount(ptr);
            total += count;
            if (facing != QuadFacing.UNDEFINED) directional += count;
        }
        require(total > 0 && renderer.getVisibleSectionsCount() > 0, "Empty rendered geometry");
        require(culling ? directional > 0 : directional == 0, "Facing bucket configuration not applied");
        if (referenceIndices == 0) referenceIndices = total;
        require(total == referenceIndices, "Geometry changed when switching renderer modes: " + total + "/" + referenceIndices);
    }

    static CloudRenderer cloudRenderer(Minecraft mc) throws Exception {
        for (Field field : LevelRenderer.class.getDeclaredFields()) {
            if (field.getType() == CloudRenderer.class) {
                field.setAccessible(true);
                return (CloudRenderer) field.get(mc.levelRenderer);
            }
        }
        throw new IllegalStateException("Cloud renderer mixin missing");
    }

    private static boolean hasBeacon(Minecraft mc) throws Exception {
        for (Object entry : (Collection<?>) field(mc.levelRenderer, "globalBlockEntities")) {
            if (((BlockEntity) entry).getBlockPos().equals(new BlockPos(2, 201, 2))) return true;
        }
        return false;
    }

    static Object field(Object instance, String name) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    private static void teleport(Minecraft mc, double x, double y, double z, float pitch) {
        var uuid = mc.player.getUUID();
        mc.player.setYRot(180);
        mc.player.setXRot(pitch);
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(x, y, z);
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
