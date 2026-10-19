package net.vulkanmod.smoke;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.gui.VOptionScreen;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;

@Mod(value = "vulkansmoke", dist = Dist.CLIENT)
public final class RuntimeSmoke {
    private final long started = System.nanoTime();
    private int phase;
    private int ticks;
    private String world;
    private CompletableFuture<Void> reload;
    private CompletableFuture<?> fixture;

    public RuntimeSmoke() {
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if ((System.nanoTime() - started) / 1_000_000_000L > 180) throw new AssertionError("Packaged smoke timeout, phase " + phase);
        try {
            if (phase == 1 && fixture == null && mc.level != null && mc.player != null && mc.screen == null) {
                var server = mc.getSingleplayerServer();
                var playerId = mc.player.getUUID();
                fixture = server.submit(() -> {
                    var level = server.overworld();
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 192, z), Blocks.STONE.defaultBlockState());
                    for (int x = 3; x < 7; x++) {
                        level.setBlockAndUpdate(new BlockPos(x, 193, 3), Blocks.GLASS.defaultBlockState());
                        level.setBlockAndUpdate(new BlockPos(x, 193, 5), Blocks.WATER.defaultBlockState());
                    }
                    level.setBlockAndUpdate(new BlockPos(10, 193, 5), Blocks.CHEST.defaultBlockState());
                    var player = server.getPlayerList().getPlayer(playerId);
                    if (player == null) throw new AssertionError("Integrated server player missing");
                    player.teleportTo(level, 8.5, 196, 13.5, 180, 30);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                });
            }
            if (phase == 0 && mc.screen instanceof TitleScreen && ++ticks > 12) {
                if (GLFW.glfwGetWindowAttrib(mc.getWindow().getWindow(), GLFW.GLFW_CLIENT_API) != GLFW.GLFW_NO_API)
                    throw new AssertionError("Minecraft window has an OpenGL context");
                if (Vulkan.getVkDevice() == null) throw new AssertionError("No Vulkan device");
                world = "UpstreamPortSmoke-" + System.currentTimeMillis();
                mc.options.renderDistance().set(6);
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel(world,
                    new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(12345L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
                phase = 1;
                ticks = 0;
            } else if (phase == 1 && fixture != null && fixture.isDone() && mc.screen == null && ++ticks > 150) {
                fixture.join();
                if (WorldRenderer.getInstance().getVisibleSectionsCount() <= 0) throw new AssertionError("No visible terrain sections");
                capture(mc, "world", true);
                inspectTerrain(mc);
                Initializer.CONFIG.indirectDraw = false;
                Initializer.CONFIG.backFaceCulling = false;
                phase = 20;
                ticks = 0;
            } else if (phase == 20 && ++ticks > 30) {
                capture(mc, "terrain-direct-no-cull", true);
                inspectTerrain(mc);
                Initializer.CONFIG.indirectDraw = true;
                Initializer.CONFIG.backFaceCulling = true;
                reload = mc.reloadResourcePacks();
                phase = 2;
                ticks = 0;
            } else if (phase == 2 && reload != null && reload.isDone() && ++ticks > 40) {
                reload.join();
                mc.setScreen(new VOptionScreen(Component.literal("VulkanMod upstream port"), null));
                phase = 3;
                ticks = 0;
            } else if (phase == 3 && ++ticks > 30) {
                capture(mc, "settings", true);
                mc.setScreen(null);
                mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.FAST);
                phase = 4;
                ticks = 0;
            } else if (phase == 4 && ++ticks > 40) {
                capture(mc, "world-after-reload", true);
                mc.setScreen(new FontProbe());
                phase = 6;
                ticks = 0;
            } else if (phase == 6 && ++ticks > 12) {
                checkFont(mc);
                var directory = mc.gameDirectory.toPath().resolve("verification");
                Files.writeString(directory.resolve("PASS.txt"), "Minecraft 1.21.1 / NeoForge 21.1.252\nWorld=" + world + "\n");
                Initializer.LOGGER.info("SMOKE PASS: upstream renderer, native NeoForge startup, Vulkan NO_API window, fresh world, visible sections, resource reload, settings, screenshot readback");
                phase = 5;
                mc.stop();
            }
        } catch (Exception failure) {
            throw new AssertionError("Packaged smoke failed in phase " + phase, failure);
        }
    }

    private static void inspectTerrain(Minecraft mc) {
        var section = WorldRenderer.getInstance().getSectionGrid().getSectionAtBlockPos(new BlockPos(8, 193, 8));
        var buffers = section.getChunkArea().drawBuffers;
        for (var type : net.vulkanmod.render.vertex.TerrainRenderType.VALUES) {
            var counts = new int[net.vulkanmod.render.chunk.cull.QuadFacing.COUNT];
            for (int facing = 0; facing < counts.length; facing++) {
                long ptr = net.vulkanmod.render.chunk.buffer.DrawParametersBuffer.getParamsPtr(buffers.getDrawParamsPtr(), section.inAreaIndex, type.ordinal(), facing);
                counts[facing] = net.vulkanmod.render.chunk.buffer.DrawParametersBuffer.getIndexCount(ptr);
            }
            Initializer.LOGGER.info("SMOKE terrain {}: {}", type, java.util.Arrays.toString(counts));
        }
        Initializer.LOGGER.info("SMOKE camera player={} yaw={} pitch={} camera={}", mc.player.position(), mc.player.getYRot(), mc.player.getXRot(), mc.gameRenderer.getMainCamera().getPosition());
    }

    private static final class FontProbe extends net.minecraft.client.gui.screens.Screen {
        private FontProbe() { super(Component.literal("Font probe")); }
        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF101010);
            graphics.drawString(font, "Vulkan plain", 16, 16, 0xFFFFFFFF, false);
            graphics.drawString(font, "Vulkan shadow", 16, 40, 0xFFFFFFFF, true);
            graphics.drawString(font, "Проверка шрифта", 16, 64, 0xFFFFFFFF, true);
        }
    }

    private static void checkFont(Minecraft mc) throws Exception {
        capture(mc, "font-probe", false);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            double scale = mc.getWindow().getGuiScale();
            for (int row = 0; row < 3; row++) {
                int bright = 0;
                int top = 16 + row * 24;
                for (int y = (int) (top * scale); y < (top + 12) * scale; y++)
                    for (int x = (int) (16 * scale); x < 170 * scale; x++) {
                        int c = image.getPixelRGBA(x, y);
                        if (Math.min(c & 255, Math.min(c >> 8 & 255, c >> 16 & 255)) >= 220) bright++;
                    }
                if (bright < 20) throw new AssertionError("White font pixels missing in row " + row + ": " + bright);
            }
        }
        Initializer.LOGGER.info("SMOKE font PASS: plain, shadow and Cyrillic foreground pixels");
    }

    private static void capture(Minecraft mc, String name, boolean requireContent) throws Exception {
        var directory = mc.gameDirectory.toPath().resolve("verification");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var colors = new HashSet<Integer>();
            for (int y = 0; y < image.getHeight(); y += 8)
                for (int x = 0; x < image.getWidth(); x += 8) colors.add(image.getPixelRGBA(x, y) & 0xFFFFFF);
            if (requireContent && colors.size() < 30) throw new AssertionError("Empty or uniform frame: " + name + ", colors=" + colors.size());
            image.writeToFile(directory.resolve(name + ".png"));
            Initializer.LOGGER.info("SMOKE screenshot {}: {} colors", name, colors.size());
        }
    }
}
