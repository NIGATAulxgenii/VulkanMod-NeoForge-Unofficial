package net.vulkanmod.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.nio.file.Files;

public final class PortSettingsCheck {
    public static void configAndAnimations(Minecraft mc) throws Exception {
        var fixture = mc.gameDirectory.toPath().resolve("verification/legacy-settings.json");
        Files.createDirectories(fixture.getParent());
        Files.writeString(fixture, "{\"frameQueueSize\":3,\"windowedFullscreen\":true,\"indirectDraw\":true}");
        try {
            Config legacy = Config.load(fixture);
            if (legacy.frameQueueSize != 3 || !legacy.windowedFullscreen || !legacy.indirectDraw
                    || legacy.windowMode != 1 || legacy.cloudDistance < 2 || legacy.cloudDistance > 128
                    || legacy.builderThreads != 0 || !legacy.textureAnimations || !legacy.backFaceCulling)
                throw new IllegalStateException("Legacy config migration changed existing values/defaults");
            legacy.builderThreads = 2;
            legacy.textureAnimations = false;
            legacy.cloudDistance = 12;
            legacy.windowMode = 0;
            legacy.write();
            var loaded = Config.load(fixture);
            if (loaded.builderThreads != 2 || loaded.textureAnimations || loaded.cloudDistance != 12
                    || loaded.windowMode != 0 || loaded.windowedFullscreen) throw new IllegalStateException("New settings not persisted");
        } finally {
            Config.load(FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json"));
        }
        var location = ResourceLocation.fromNamespaceAndPath("vulkansmoke", "animation-probe");
        var texture = new TickProbe();
        boolean original = Initializer.CONFIG.textureAnimations;
        mc.getTextureManager().register(location, texture);
        try {
            Initializer.CONFIG.textureAnimations = false;
            mc.getTextureManager().tick();
            if (texture.ticks != 0) throw new IllegalStateException("Disabled textures still tick");
            Initializer.CONFIG.textureAnimations = true;
            mc.getTextureManager().tick();
            if (texture.ticks != 1) throw new IllegalStateException("Texture ticks did not resume");
        } finally {
            Initializer.CONFIG.textureAnimations = original;
            mc.getTextureManager().release(location);
        }
        int id = com.mojang.blaze3d.platform.TextureUtil.generateTextureId();
        try {
            com.mojang.blaze3d.platform.TextureUtil.prepareImage(id, 2, 2);
            net.vulkanmod.gl.GlTexture.bindTexture(id);
            net.vulkanmod.gl.GlTexture.texParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            net.vulkanmod.gl.GlTexture.texParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            var image = net.vulkanmod.gl.GlTexture.getTexture(id).getVulkanImage();
            long mixed = image.getSampler();
            net.vulkanmod.gl.GlTexture.texParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            long nearest = image.getSampler();
            net.vulkanmod.gl.GlTexture.texParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_LINEAR);
            long reverseMixed = image.getSampler();
            if (mixed == nearest || nearest == reverseMixed || mixed == reverseMixed)
                throw new IllegalStateException("Independent min/mag filters collapsed to the same sampler");
        } finally {
            com.mojang.blaze3d.platform.TextureUtil.releaseTextureId(id);
        }
    }

    public static void workers() {
        var dispatcher = WorldRenderer.getInstance().getTaskDispatcher();
        int maximum = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        for (int requested : new int[] {1, 2, 0}) {
            dispatcher.createThreads(requested);
            int expected = requested == 0 ? Math.max(maximum / 2, 1) : Math.min(requested, maximum);
            if (dispatcher.getResourcesArray().length != expected) throw new IllegalStateException("Incorrect worker count");
        }
    }

    public static void spriteFrames() throws Exception {
        var metadata = ResourceMetadata.fromJsonStream(new java.io.ByteArrayInputStream(
                "{\"animation\":{\"frametime\":2,\"interpolate\":true}}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        int texture = com.mojang.blaze3d.platform.TextureUtil.generateTextureId();
        boolean upload = SpriteUtil.shouldUpload();
        try (var contents = new SpriteContents(ResourceLocation.fromNamespaceAndPath("vulkansmoke", "interpolation"),
                new FrameSize(1, 1), new NativeImage(1, 2, false), metadata);
             var ticker = contents.createTicker()) {
            contents.getOriginalImage().setPixelRGBA(0, 0, 0xff0000ff);
            contents.getOriginalImage().setPixelRGBA(0, 1, 0xffff0000);
            var interpolation = PortRenderCheck.field(ticker, "interpolationData");
            NativeImage frame = ((NativeImage[]) PortRenderCheck.field(interpolation, "activeFrame"))[0];
            frame.setPixelRGBA(0, 0, 0x12345678);
            SpriteUtil.setDoUpload(false);
            ticker.tickAndUpload(0, 0);
            if ((int) PortRenderCheck.field(ticker, "subFrame") != 1 || frame.getPixelRGBA(0, 0) != 0x12345678)
                throw new IllegalStateException("Skipped upload still interpolated or stopped sprite time");
            ticker.tickAndUpload(0, 0);
            if ((int) PortRenderCheck.field(ticker, "frame") != 1) throw new IllegalStateException("Sprite frame did not advance");
            com.mojang.blaze3d.platform.TextureUtil.prepareImage(texture, 1, 1);
            SpriteUtil.setDoUpload(true);
            ticker.tickAndUpload(0, 0);
            if (frame.getPixelRGBA(0, 0) == 0x12345678) throw new IllegalStateException("Sprite interpolation did not resume");
            Initializer.LOGGER.info("SMOKE sprite frames PASS: skipped interpolation, advancing timeline, resumed upload");
        } finally {
            SpriteUtil.setDoUpload(upload);
            com.mojang.blaze3d.platform.TextureUtil.releaseTextureId(texture);
        }
    }

    public static void readbackDuringTick(Minecraft mc) {
        var location = ResourceLocation.fromNamespaceAndPath("vulkansmoke", "readback-during-tick");
        var probe = new ReadbackProbe();
        boolean animations = Initializer.CONFIG.textureAnimations;
        boolean upload = SpriteUtil.shouldUpload();
        mc.getTextureManager().register(location, probe);
        try {
            Initializer.CONFIG.textureAnimations = true;
            SpriteUtil.setDoUpload(true);
            mc.getTextureManager().tick();
            if (!probe.passed) throw new IllegalStateException("Tick/readback probe did not run");
            Initializer.LOGGER.info("SMOKE texture batch readback PASS: submit pending uploads, continue batch, deferred read-only transition");
        } finally {
            Initializer.CONFIG.textureAnimations = animations;
            SpriteUtil.setDoUpload(upload);
            mc.getTextureManager().release(location);
        }
    }

    private static final class ReadbackProbe extends AbstractTexture implements Tickable {
        boolean passed;

        @Override
        public void load(ResourceManager resources) {}

        @Override
        public void tick() {
            com.mojang.blaze3d.platform.TextureUtil.prepareImage(getId(), 1, 1);
            try (var pixels = new NativeImage(1, 1, false); var read = new NativeImage(1, 1, false)) {
                pixels.setPixelRGBA(0, 0, 0xffabcdef);
                pixels.upload(0, 0, 0, false);
                read.downloadTexture(0, false);
                if (read.getPixelRGBA(0, 0) != 0xffabcdef) throw new IllegalStateException("Readback missed pending upload");
                pixels.setPixelRGBA(0, 0, 0xff123456);
                pixels.upload(0, 0, 0, false);
                net.vulkanmod.gl.GlTexture.getTexture(getId()).getVulkanImage().readOnlyLayout();
                read.downloadTexture(0, false);
                if (read.getPixelRGBA(0, 0) != 0xff123456) throw new IllegalStateException("Texture batch did not resume after readback");
                passed = true;
            }
        }
    }

    private static final class TickProbe extends AbstractTexture implements Tickable {
        int ticks;

        @Override
        public void load(ResourceManager resources) {}

        @Override
        public void tick() { ticks++; }
    }
}
