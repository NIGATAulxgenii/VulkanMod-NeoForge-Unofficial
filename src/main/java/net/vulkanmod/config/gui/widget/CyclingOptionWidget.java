package net.vulkanmod.config.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.CyclingOption;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix4f;

public class CyclingOptionWidget extends OptionWidget<CyclingOption<?>> {
   private final CyclingOptionWidget.Button leftButton = new CyclingOptionWidget.Button(CyclingOptionWidget.Button.Direction.LEFT);
   private final CyclingOptionWidget.Button rightButton = new CyclingOptionWidget.Button(CyclingOptionWidget.Button.Direction.RIGHT);
   private boolean focused;

   public CyclingOptionWidget(CyclingOption<?> option, Component name) {
      super(option, name);
   }

   @Override
   public void setDimensions(int x, int y, int width, int height) {
      super.setDimensions(x, y, width, height);
      this.leftButton.setDimensions(this.controlX, 16);
      this.rightButton.setDimensions(this.controlX + this.controlWidth - 16, 16);
   }

   @Override
   public void renderControls(double mouseX, double mouseY) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderBars();
      this.leftButton.setStatus(this.option.index() > 0);
      this.rightButton.setStatus(this.option.index() < this.option.getValues().length - 1);
      int color = this.active ? -1 : -6250336;
      Font textRenderer = Minecraft.getInstance().font;
      int x = this.controlX + this.controlWidth / 2;
      int y = this.y + (this.height - 9) / 2;
      GuiRenderer.drawScrollingString(
         textRenderer, this.getDisplayedValue(), x, y, this.rightButton.x - (this.leftButton.x + this.leftButton.width) - 12, color
      );
      this.leftButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
      this.rightButton.renderButton(GuiRenderer.guiGraphics.pose(), mouseX, mouseY);
   }

   public void renderBars() {
      int count = this.option.getValues().length;
      int current = this.option.index();
      int margin = 30;
      int padding = 4;
      int barWidth = (this.controlWidth - 2 * margin - padding * count) / count;
      int color = ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.4F);
      int activeColor = ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 1.0F);
      if (barWidth > 0) {
         for (int i = 0; i < count; i++) {
            int x0 = this.controlX + margin + i * (barWidth + padding);
            int y0 = this.y + this.height - 5;
            int c = i == current ? activeColor : color;
            GuiRenderer.fill(x0, y0, x0 + barWidth, (int)(y0 + 1.5F), c);
         }
      }
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
      this.leftButton.active &= active;
      this.rightButton.active &= active;
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      if (this.leftButton.isHovered(mouseX, mouseY)) {
         this.option.prevValue();
      } else if (this.rightButton.isHovered(mouseX, mouseY)) {
         this.option.nextValue();
      }
   }

   @Override
   public void onRelease(double mouseX, double mouseY) {
   }

   @Override
   protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
   }

   @Override
   public void setFocused(boolean bl) {
      this.focused = bl;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   class Button {
      final int ACTIVE_COLOR = ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.8F);
      final int HOVERED_COLOR = ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 1.0F);
      final int INACTIVE_COLOR = ColorUtil.ARGB.pack(0.3F, 0.3F, 0.3F, 0.8F);
      int x;
      int width;
      boolean active = true;
      CyclingOptionWidget.Button.Direction direction;

      Button(CyclingOptionWidget.Button.Direction direction) {
         this.direction = direction;
      }

      public void setDimensions(int x, int width) {
         this.x = x;
         this.width = width;
      }

      boolean isHovered(double mouseX, double mouseY) {
         return mouseX >= this.x
            && mouseX <= this.x + this.width
            && mouseY >= CyclingOptionWidget.this.y
            && mouseY <= CyclingOptionWidget.this.y + CyclingOptionWidget.this.height;
      }

      void setStatus(boolean status) {
         this.active = status;
      }

      void renderButton(PoseStack matrices, double mouseX, double mouseY) {
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = tesselator.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         float f = this.isHovered(mouseX, mouseY) && this.active ? 5.0F : 4.5F;
         Matrix4f matrix4f = matrices.last().pose();
         RenderSystem.setShader(GameRenderer::getPositionShader);
         RenderSystem.enableBlend();
         int color;
         if (this.isHovered(mouseX, mouseY) && this.active) {
            color = this.HOVERED_COLOR;
         } else if (this.active) {
            color = this.ACTIVE_COLOR;
         } else {
            color = this.INACTIVE_COLOR;
         }

         VRenderSystem.setShaderColor(color);
         float w = f - 1.0F;
         float yC = CyclingOptionWidget.this.y + CyclingOptionWidget.this.height * 0.5F;
         float xC = this.x + this.width * 0.5F;
         if (this.direction == CyclingOptionWidget.Button.Direction.LEFT) {
            bufferBuilder.addVertex(matrix4f, xC - w, yC, 0.0F);
            bufferBuilder.addVertex(matrix4f, xC + w, yC + f, 0.0F);
            bufferBuilder.addVertex(matrix4f, xC + w, yC - f, 0.0F);
         } else {
            bufferBuilder.addVertex(matrix4f, xC + w, yC, 0.0F);
            bufferBuilder.addVertex(matrix4f, xC - w, yC - f, 0.0F);
            bufferBuilder.addVertex(matrix4f, xC - w, yC + f, 0.0F);
         }

         BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
      }

      static enum Direction {
         LEFT,
         RIGHT;
      }
   }
}
