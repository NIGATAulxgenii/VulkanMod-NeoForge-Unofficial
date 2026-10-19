package net.vulkanmod.smoke;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;

public final class ImmediateIndexScreen extends Screen {
    public ImmediateIndexScreen() {
        super(Component.literal("Immediate index regression probe"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF000000);
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int panel = 0; panel < 5; panel++) {
            try (var vertices = new ByteBufferBuilder(256); var indices = new ByteBufferBuilder(256)) {
                var builder = new BufferBuilder(vertices, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                if (panel >= 3) {
                    for (int i = 0; i < 65536; i++) builder.addVertex(-100, -100, 0).setColor(0xFF000000);
                }
                for (int color : new int[] {0xFFFF0000, 0xFF0000FF}) {
                    int x = 16 + panel * 48;
                    builder.addVertex(x, 48, 0).setColor(color);
                    builder.addVertex(x + 32, 48, 0).setColor(color);
                    builder.addVertex(x + 32, 16, 0).setColor(color);
                    builder.addVertex(x, 16, 0).setColor(color);
                }
                var mesh = builder.buildOrThrow();
                if (panel == 1 || panel == 2 || panel == 4) mesh.sortQuads(indices, centers -> {
                    int[] order = java.util.stream.IntStream.range(0, centers.length).toArray();
                    order[order.length - 2] = order.length - 1;
                    order[order.length - 1] = order.length - 2;
                    return order;
                });
                if (panel == 2) BufferUploader.draw(mesh);
                else BufferUploader.drawWithShader(mesh);
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
    }

    public static void check(Minecraft mc) throws Exception {
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            var directory = mc.gameDirectory.toPath().resolve("verification");
            Files.createDirectories(directory);
            image.writeToFile(directory.resolve("immediate-indices.png"));
            double scale = mc.getWindow().getGuiScale();
            for (int panel = 0; panel < 5; panel++) {
                int pixel = image.getPixelRGBA((int) ((32 + panel * 48) * scale), (int) (32 * scale));
                int red = pixel & 255, blue = (pixel >>> 16) & 255;
                boolean expected = panel == 0 || panel == 3 ? blue > 220 && red < 30 : red > 220 && blue < 30;
                if (!expected) throw new IllegalStateException("Immediate index order failed in panel " + panel + ": " + Integer.toHexString(pixel));
            }
        }
    }
}
