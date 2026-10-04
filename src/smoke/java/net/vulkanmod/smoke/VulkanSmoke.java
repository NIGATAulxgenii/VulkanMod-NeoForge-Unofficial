package net.vulkanmod.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.vulkanmod.config.gui.VOptionScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import java.util.concurrent.atomic.AtomicInteger;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;

/** Test mod compiled only by runClientSmoke; never included in the release jar. */
@Mod(value = "vulkansmoke", dist = Dist.CLIENT)
public final class VulkanSmoke {
    private final long started = System.nanoTime();
    private int phase, ticks;
    private String world;
    private final AtomicInteger additionalGeometryCalls = new AtomicInteger();
    private int shaderRegistrations;

    public VulkanSmoke(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::additionalGeometry);
        modBus.addListener(this::registerShader);
    }

    private void registerShader(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath("vulkansmoke", "test"), DefaultVertexFormat.POSITION_COLOR),
                    shader -> shaderRegistrations++);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Native namespaced shader registration failed", error);
        }
    }

    private void additionalGeometry(AddSectionGeometryEvent event) {
        // getLevel enforces main-thread collection. The callback runs in a meshing worker.
        event.getLevel();
        if (!event.getSectionOrigin().equals(new BlockPos(0, 96, 0))
                && !event.getSectionOrigin().equals(new BlockPos(0, 256, 0))) return;
        var sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.STONE.defaultBlockState()).getParticleIcon();
        event.addRenderer(context -> {
            var vertices = context.getOrCreateChunkBuffer(RenderType.solid());
            vertices.addVertex(3, 5, 1).setColor(0, 255, 255, 255).setUv(sprite.getU0(), sprite.getV1()).setLight(0x00F000F0).setNormal(0, 0, 1);
            vertices.addVertex(4, 5, 1).setColor(0, 255, 255, 255).setUv(sprite.getU1(), sprite.getV1()).setLight(0x00F000F0).setNormal(0, 0, 1);
            vertices.addVertex(4, 6, 1).setColor(0, 255, 255, 255).setUv(sprite.getU1(), sprite.getV0()).setLight(0x00F000F0).setNormal(0, 0, 1);
            vertices.addVertex(3, 6, 1).setColor(0, 255, 255, 255).setUv(sprite.getU0(), sprite.getV0()).setLight(0x00F000F0).setNormal(0, 0, 1);
            additionalGeometryCalls.incrementAndGet();
        });
    }

    private void screenshot(Minecraft mc, String name) throws Exception {
        var directory = mc.gameDirectory.toPath().resolve("verification");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            java.util.Set<Integer> colors = new java.util.HashSet<>();
            for (int y = 0; y < image.getHeight(); y += 16)
                for (int x = 0; x < image.getWidth(); x += 16) colors.add(image.getPixelRGBA(x, y));
            if (colors.size() < 16) throw new IllegalStateException("Blank/corrupt screenshot " + name + ": " + colors.size() + " colors");
            image.writeToFile(directory.resolve(name + ".png"));
        }
        Initializer.LOGGER.info("SMOKE screenshot: {}", name);
    }

    private void regressionChecks(Minecraft mc) throws Exception {
        var configFile = mc.gameDirectory.toPath().resolve("verification/config-" + System.nanoTime() + "/settings.json");
        try {
            if (Config.load(configFile) != null) throw new IllegalStateException("New config already exists");
            Config settings = new Config();
            settings.frameQueueSize = 3;
            settings.write();
            if (Config.load(configFile).frameQueueSize != 3) throw new IllegalStateException("Config round trip failed");
        } finally {
            Config.load(FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json"));
        }
        for (int i = 0; i < 10; i++) {
            try (var spirv = SPIRVUtils.compileShaderResource("/assets/vulkanmod/shaders/basic/terrain/terrain.vsh", SPIRVUtils.ShaderKind.VERTEX_SHADER)) {
                if (spirv.bytecode().remaining() < 20) throw new IllegalStateException("Invalid SPIR-V result");
            }
        }
        boolean rejected = false;
        try (var unused = SPIRVUtils.compileShader("missing-include-test.vsh", "#version 450\n#include \"does-not-exist.glsl\"\nvoid main() {}\n", SPIRVUtils.ShaderKind.VERTEX_SHADER)) {
            // Compilation must fail with a diagnostic instead of retaining dangling callback buffers.
        } catch (RuntimeException expected) {
            rejected = expected.getMessage().contains("include missing");
        }
        if (!rejected) throw new IllegalStateException("Missing shader include did not produce the expected diagnostic");
        Initializer.LOGGER.info("SMOKE regression checks PASS: config parents/round trip, repeated shader includes, missing include diagnostics");
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (phase == 0 && mc.screen instanceof AccessibilityOnboardingScreen && mc.getOverlay() == null) {
                mc.setScreen(new TitleScreen());
            }
            if ((System.nanoTime() - started) / 1_000_000_000L > 180) throw new IllegalStateException("SMOKE timed out at phase " + phase);
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                if (ticks == 20) GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 960, 540);
                if (++ticks < 40) return;
                if (GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_CLIENT_API) != GLFW.GLFW_NO_API)
                    throw new IllegalStateException("Minecraft window still uses OpenGL");
                if (Vulkan.getVkDevice() == null) throw new IllegalStateException("Vulkan device missing");
                regressionChecks(mc);
                screenshot(mc, "title");
                phase = 1;
                ticks = 0;
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(5);
                mc.options.pauseOnLostFocus = false;
                world = "VulkanSmoke-" + System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(world,
                        new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
            } else if (phase == 1 && mc.level != null && mc.player != null && mc.getOverlay() == null) {
                mc.setScreen(null);
                phase = 2;
                var uuid = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                        level.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 3);
                        for (int y = 101; y <= 106; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                    level.setBlock(new BlockPos(-2, 101, 0), Blocks.WATER.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(-1, 101, 0), Blocks.GLASS.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(0, 101, 0), Blocks.CHEST.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(1, 101, 0), Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    level.setBlock(new BlockPos(2, 101, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    level.setDayTime(1000);
                    server.getPlayerList().getPlayer(uuid).teleportTo(0.5, 102, 5.5);
                });
                mc.player.setYRot(180);
                mc.player.setXRot(15);
                ticks = 0;
            } else if (phase == 2 && mc.level != null) {
                mc.player.setYRot(180);
                mc.player.setXRot(15);
                if (++ticks < 240) return;
                if (mc.screen != null) throw new IllegalStateException("World view obscured by " + mc.screen);
                if (!mc.level.getBlockState(new BlockPos(-1, 101, 0)).is(Blocks.GLASS)
                        || !mc.level.getBlockState(new BlockPos(0, 101, 0)).is(Blocks.CHEST))
                    throw new IllegalStateException("Test scene did not reach the client");
                if (WorldRenderer.getInstance().getVisibleSectionsCount() == 0) throw new IllegalStateException("No visible Vulkan chunk sections");
                if (!WorldRenderer.getInstance().isSectionCompiled(new BlockPos(0, 101, 0))) throw new IllegalStateException("Test blocks were not compiled by Vulkan");
                if (additionalGeometryCalls.get() == 0) throw new IllegalStateException("NeoForge additional geometry callbacks were not executed");
                var emptyRegion = new RenderRegionBuilder().createRegion(mc.level, 0, 16, 0);
                if (emptyRegion == null || emptyRegion.additionalRenderers.isEmpty()) throw new IllegalStateException("Geometry in an empty section was discarded");
                if (emptyRegion.getAuxLightManager(new net.minecraft.world.level.ChunkPos(0, 0)) == null)
                    throw new IllegalStateException("Region discarded NeoForge's auxiliary light manager");
                screenshot(mc, "world");
                phase = 3;
                ticks = 0;
                mc.reloadResourcePacks();
            } else if (phase == 3 && mc.getOverlay() == null) {
                if (++ticks < 100) return;
                if (shaderRegistrations < 2) throw new IllegalStateException("NeoForge shader event did not survive reload");
                screenshot(mc, "world-after-reload");
                phase = 4;
                ticks = 0;
                var options = new OptionsScreen(null, mc.options);
                mc.setScreen(options);
                var video = options.children().stream().filter(child -> child instanceof Button button
                        && button.getMessage().getContents() instanceof TranslatableContents translation
                        && translation.getKey().equals("options.video")).findFirst().orElseThrow();
                ((Button) video).onPress();
                if (!(mc.screen instanceof VOptionScreen)) throw new IllegalStateException("Video button did not open Vulkan settings");
            } else if (phase == 4) {
                if (++ticks < 40) return;
                screenshot(mc, "video-settings");
                mc.screen.onClose();
                Initializer.LOGGER.info("SMOKE PASS: native NeoForge, NO_API window, Vulkan device, rendered world, fluids, block entities, additional geometry, resource reload, video settings");
                Files.writeString(mc.gameDirectory.toPath().resolve("verification/PASS.txt"), "Minecraft 1.21.1 / NeoForge 21.1.252\nWorld=" + world + "\nAdditionalGeometryCalls=" + additionalGeometryCalls.get() + "\nShaderRegistrations=" + shaderRegistrations + "\n");
                phase = 5;
                mc.stop();
            }
        } catch (Throwable error) {
            throw new IllegalStateException("Vulkan integration smoke test failed at phase " + phase, error);
        }
    }
}
