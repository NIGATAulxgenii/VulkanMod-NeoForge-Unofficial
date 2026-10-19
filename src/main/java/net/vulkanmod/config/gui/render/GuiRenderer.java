package net.vulkanmod.config.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public abstract class GuiRenderer {
   public static Minecraft minecraft;
   public static GuiGraphics guiGraphics;
   public static PoseStack pose;
   public static BufferBuilder bufferBuilder;
   private static boolean batching = false;
   private static boolean drawing = false;

   public static void enableScissor(int i, int j, int k, int l) {
      guiGraphics.enableScissor(i, j, k, l);
   }

   public static void disableScissor() {
      guiGraphics.disableScissor();
   }

   public static void fillBox(int x0, int y0, int width, int height, int color) {
      fill(x0, y0, x0 + width, y0 + height, 0, color);
   }

   public static void fill(int x0, int y0, int x1, int y1, int color) {
      fill(x0, y0, x1, y1, 0, color);
   }

   public static void fill(int x0, int y0, int x1, int y1, int z, int color) {
      guiGraphics.fill(x0, y0, x1, y1, color);
   }

   public static void fillGradient(int x0, int y0, int x1, int y1, int color1, int color2) {
      fillGradient(x0, y0, x1, y1, 0, color1, color2);
   }

   public static void fillGradient(int x0, int y0, int x1, int y1, int z, int color1, int color2) {
      guiGraphics.fillGradient(x0, y0, x1, y1, color1, color2);
   }

   public static void renderBoxBorder(int x0, int y0, int width, int height, int borderWidth, int color) {
      renderBorder(x0, y0, x0 + width, y0 + height, borderWidth, color);
   }

   public static void renderBorder(int x0, int y0, int x1, int y1, int width, int color) {
      fill(x0, y0, x1, y0 + width, color);
      fill(x0, y1 - width, x1, y1, color);
      fill(x0, y0 + width, x0 + width, y1 - width, color);
      fill(x1 - width, y0 + width, x1, y1 - width, color);
   }

   public static void drawString(Font font, Component component, int x, int y, int color) {
      drawString(font, component.getVisualOrderText(), x, y, color);
   }

   public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color) {
      guiGraphics.drawString(font, formattedCharSequence, x, y, color);
   }

   public static void drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
      drawString(font, component.getVisualOrderText(), x, y, color, shadow);
   }

   public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
      guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
   }

   public static void drawCenteredString(Font font, Component component, int x, int y, int color) {
      FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
      guiGraphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
   }

   public static void drawScrollingString(Font font, Component component, int x, int y, int maxWidth, int color) {
      int textWidth = font.width(component);
      if (textWidth <= maxWidth) {
         drawCenteredString(font, component, x, y, color);
      } else {
         int x0 = x - maxWidth / 2;
         int x1 = x + maxWidth / 2;
         int scrollAmount = textWidth - maxWidth;
         double currentTimeInSeconds = Util.getMillis() / 1000.0;
         double scrollSpeed = Math.max(scrollAmount * 0.5, 3.0);
         double scrollingOffset = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * currentTimeInSeconds / scrollSpeed)) / 2.0 + 0.5;
         double horizontalScroll = Mth.lerp(scrollingOffset, 0.0, scrollAmount);
         enableScissor(x0 - 1, 0, x1, Minecraft.getInstance().getWindow().getScreenHeight());
         drawString(font, component, (int)(x0 - horizontalScroll), y, color);
         disableScissor();
      }
   }

   public static int getMaxTextWidth(Font font, List<FormattedCharSequence> list) {
      int maxWidth = 0;

      for (FormattedCharSequence text : list) {
         int width = font.width(text);
         if (width > maxWidth) {
            maxWidth = width;
         }
      }

      return maxWidth;
   }

   public static void beginBatch() {
      setupBufferBuilder();
      batching = true;
   }

   public static void endBatch() {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      MeshData meshData = bufferBuilder.build();
      if (meshData != null) {
         BufferUploader.drawWithShader(meshData);
         meshData.close();
      }

      batching = false;
      drawing = false;
   }

   public static void flush() {
      guiGraphics.flush();
      if (batching) {
         endBatch();
      }
   }

   private static void setupBufferBuilder() {
      if (!batching || !drawing) {
         bufferBuilder = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         drawing = true;
      }
   }

   private static void submitIfNeeded() {
      if (!batching) {
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
         drawing = false;
      }
   }
}
