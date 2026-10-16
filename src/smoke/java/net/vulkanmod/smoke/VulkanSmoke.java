package net.vulkanmod.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;

/** Development-only integration test mod; excluded from the release JAR. */
@Mod(value = "vulkansmoke", dist = Dist.CLIENT)
public final class VulkanSmoke {
    private final long started = System.nanoTime();
    private int phase, ticks;
    private String world;
    private final AtomicInteger additionalGeometryCalls = new AtomicInteger();
    private int shaderRegistrations;
    private boolean whiteTextPassed;
    private final String existingWorld = System.getProperty("vulkanmod.smokeWorld");

    private void beginWorld(Minecraft mc) {
        phase = 1;
        ticks = 0;
        if (existingWorld != null) {
            world = existingWorld;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().openWorld(world, () -> mc.setScreen(new TitleScreen()));
            return;
        }
        mc.options.renderDistance().set(6);
        mc.options.simulationDistance().set(5);
        mc.options.pauseOnLostFocus = false;
        world = "VulkanSmoke-" + System.currentTimeMillis();
        mc.createWorldOpenFlows().createFreshLevel(world,
                new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
    }

    private void checkWhiteText(Minecraft mc) throws Exception {
        try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var directory = mc.gameDirectory.toPath().resolve("verification");
            Files.createDirectories(directory);
            frame.writeToFile(directory.resolve("white-text.png"));
            double scale = mc.getWindow().getGuiScale();
            int[] bright = new int[3];
            int[] max = new int[3];
            for (int row = 0; row < 3; row++) {
                int top = 16 + row * 24;
                for (int y = (int)(top * scale); y < (top + 12) * scale; y++)
                    for (int x = (int)(16 * scale); x < 170 * scale; x++) {
                        int pixel = frame.getPixelRGBA(x, y);
                        int brightness = Math.min(pixel & 255, Math.min((pixel >>> 8) & 255, (pixel >>> 16) & 255));
                        max[row] = Math.max(max[row], brightness);
                        if (brightness >= 220) bright[row]++;
                    }
            }
            whiteTextPassed = bright[0] >= 20 && bright[1] >= 20 && bright[2] >= 20;
            Initializer.LOGGER.info("FONT PROBE: plain={}/{} shadow={}/{} unicode={}/{} whitePassed={} shaderColor={} fog={}/{}", bright[0], max[0], bright[1], max[1], bright[2], max[2], whiteTextPassed,
                    java.util.Arrays.toString(RenderSystem.getShaderColor()), RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd());
        }
    }

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
        // UnihexProvider uses this IntBuffer upload path instead of NativeImage._upload.
        int glyphTexture = com.mojang.blaze3d.platform.TextureUtil.generateTextureId();
        com.mojang.blaze3d.platform.TextureUtil.prepareImage(glyphTexture, 2, 2);
        var glyphPixels = org.lwjgl.system.MemoryUtil.memAllocInt(4);
        glyphPixels.put(new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF}).flip();
        var glyphRelease = new AtomicInteger();
        try {
            Initializer.LOGGER.info("SMOKE: testing native Unihex glyph upload");
            com.mojang.blaze3d.platform.GlStateManager.upload(0, 0, 0, 2, 2,
                    com.mojang.blaze3d.platform.NativeImage.Format.RGBA, glyphPixels, pixels -> {
                        glyphRelease.incrementAndGet();
                        org.lwjgl.system.MemoryUtil.memFree(pixels);
                    });
            if (glyphRelease.get() != 1) throw new IllegalStateException("Glyph upload did not release its pixels once");
            try (var image = new com.mojang.blaze3d.platform.NativeImage(2, 2, false)) {
                image.downloadTexture(0, false);
                for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++)
                    if (image.getPixelRGBA(x, y) != 0xFFFFFFFF) throw new IllegalStateException("Unicode glyph pixels were not uploaded");
            }
        } finally {
            com.mojang.blaze3d.platform.GlStateManager._deleteTexture(glyphTexture);
        }
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
            GLFW.glfwSetWindowTitle(mc.getWindow().getWindow(), "VulkanMod automated test - do not interact");
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
                phase = 6;
                ticks = 0;
                Files.deleteIfExists(mc.gameDirectory.toPath().resolve("verification/PASS.txt"));
                mc.setScreen(new Screen(Component.literal("Font probe")) {
                    @Override
                    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                        graphics.fill(0, 0, width, height, 0xFF000000);
                        graphics.drawString(font, "WHITE TEXT", 16, 16, 0xFFFFFFFF, false);
                        graphics.drawString(font, "WHITE SHADOW TEXT", 16, 40, 0xFFFFFFFF, true);
                        graphics.drawString(font, "Русский Ёё Її ∀ 中文", 16, 64, 0xFFFFFFFF, true);
                        graphics.flush();
                    }
                });
            } else if (phase == 6) {
                if (++ticks < 40) return;
                checkWhiteText(mc);
                var title = new TitleScreen();
                mc.setScreen(title);
                var singleplayer = title.children().stream().filter(child -> child instanceof Button button
                        && button.getMessage().getContents() instanceof TranslatableContents translation
                        && translation.getKey().equals("menu.singleplayer")).findFirst().orElseThrow();
                Initializer.LOGGER.info("SMOKE: pressing the Singleplayer title button");
                ((Button) singleplayer).playDownSound(mc.getSoundManager());
                ((Button) singleplayer).onPress();
                phase = 7;
                ticks = 0;
            } else if (phase == 7) {
                if (!(mc.screen instanceof SelectWorldScreen)) throw new IllegalStateException("Singleplayer did not open the world list");
                if (++ticks < 100) return;
                screenshot(mc, "singleplayer-list");
                Initializer.LOGGER.info("SMOKE: world list rendered before opening a world");
                beginWorld(mc);
            } else if (phase == 1 && mc.level != null && mc.player != null && mc.getOverlay() == null) {
                mc.setScreen(null);
                phase = 2;
                ticks = 0;
                if (existingWorld != null) return;
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
                if (ticks % 20 == 0) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                if (existingWorld == null) {
                    mc.player.setYRot(180);
                    mc.player.setXRot(15);
                }
                if (++ticks < 240) return;
                if (mc.screen != null) throw new IllegalStateException("World view obscured by " + mc.screen);
                if (WorldRenderer.getInstance().getVisibleSectionsCount() == 0) throw new IllegalStateException("No visible Vulkan chunk sections");
                if (existingWorld == null) {
                    if (!mc.level.getBlockState(new BlockPos(-1, 101, 0)).is(Blocks.GLASS)
                            || !mc.level.getBlockState(new BlockPos(0, 101, 0)).is(Blocks.CHEST))
                        throw new IllegalStateException("Test scene did not reach the client");
                    if (!WorldRenderer.getInstance().isSectionCompiled(new BlockPos(0, 101, 0))) throw new IllegalStateException("Test blocks were not compiled by Vulkan");
                    if (additionalGeometryCalls.get() == 0) throw new IllegalStateException("NeoForge additional geometry callbacks were not executed");
                    var emptyRegion = new RenderRegionBuilder().createRegion(mc.level, 0, 16, 0);
                    if (emptyRegion == null || emptyRegion.additionalRenderers.isEmpty()) throw new IllegalStateException("Geometry in an empty section was discarded");
                    if (emptyRegion.getAuxLightManager(new net.minecraft.world.level.ChunkPos(0, 0)) == null)
                        throw new IllegalStateException("Region discarded NeoForge's auxiliary light manager");
                }
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
                if (!whiteTextPassed) {
                    Initializer.LOGGER.error("SMOKE FAIL: white text regression (see white-text.png)");
                    phase = 5;
                    mc.stop();
                    return;
                }
                Initializer.LOGGER.info("SMOKE PASS: native NeoForge, NO_API window, Vulkan device, rendered {} world, resource reload, video settings, white text, sound playback", existingWorld == null ? "fresh (fluids, block entities, additional geometry)" : "copied");
                Files.writeString(mc.gameDirectory.toPath().resolve("verification/PASS.txt"), "Minecraft 1.21.1 / NeoForge 21.1.252\nWorld=" + world + "\nWhiteText=true\nAdditionalGeometryCalls=" + additionalGeometryCalls.get() + "\nShaderRegistrations=" + shaderRegistrations + "\n");
                phase = 5;
                mc.stop();
            }
        } catch (Throwable error) {
            throw new IllegalStateException("Vulkan integration smoke test failed at phase " + phase, error);
        }
    }
}
