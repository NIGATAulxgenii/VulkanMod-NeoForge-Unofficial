package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.config.option.PerformanceImpact;
import net.vulkanmod.vulkan.util.ColorUtil;

public abstract class OptionWidget<O extends Option<?>> extends VAbstractWidget implements NarratableEntry {
   public int controlX;
   public int controlWidth;
   private final Component name;
   protected Component displayedValue;
   protected boolean controlHovered;
   final O option;

   public OptionWidget(O option, Component name) {
      this.option = option;
      this.name = name;
      this.displayedValue = Component.literal("N/A");
   }

   @Override
   public void setDimensions(int x, int y, int width, int height) {
      super.setDimensions(x, y, width, height);
      this.controlWidth = Math.min((int)(width * 0.5F) - 8, 120);
      this.controlX = this.x + this.width - this.controlWidth - 8;
   }

   @Override
   public void render(double mouseX, double mouseY) {
      if (this.visible) {
         this.updateDisplayedValue();
         this.controlHovered = mouseX >= this.controlX && mouseY >= this.y && mouseX < this.controlX + this.controlWidth && mouseY < this.y + this.height;
         this.renderWidget(mouseX, mouseY);
      }
   }

   @Override
   public void renderWidget(double mouseX, double mouseY) {
      Minecraft minecraftClient = Minecraft.getInstance();
      int xPadding = 0;
      int yPadding = 0;
      int color = ColorUtil.ARGB.pack(0.0F, 0.0F, 0.0F, 0.45F);
      GuiRenderer.fill(this.x - xPadding, this.y - yPadding, this.x + this.width + xPadding, this.y + this.height + yPadding, color);
      this.renderHovering(0, 0);
      color = this.active ? -1 : -6250336;
      Font textRenderer = minecraftClient.font;
      Component nameComp = this.getName();
      if (this.option.isChanged()) {
         nameComp = nameComp.copy().withStyle(style -> style.withItalic(true));
      }

      GuiRenderer.drawString(textRenderer, nameComp.getVisualOrderText(), this.x + 8, this.y + (this.height - 8) / 2, color);
      this.renderControls(mouseX, mouseY);
   }

   protected abstract void renderControls(double var1, double var3);

   @Override
   public abstract void onClick(double var1, double var3);

   @Override
   public abstract void onRelease(double var1, double var3);

   @Override
   protected abstract void onDrag(double var1, double var3, double var5, double var7);

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.isValidClickButton(button)) {
         this.onDrag(mouseX, mouseY, deltaX, deltaY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (!this.active || !this.visible) {
         return false;
      } else if (this.isValidClickButton(button) && this.clicked(mouseX, mouseY)) {
         this.playDownSound(Minecraft.getInstance().getSoundManager());
         this.onClick(mouseX, mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.isValidClickButton(button)) {
         this.onRelease(mouseX, mouseY);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.active && this.visible && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
   }

   @Override
   public void setFocused(boolean bl) {
      this.focused = bl;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   @Override
   protected boolean clicked(double mouseX, double mouseY) {
      return this.active && this.visible && mouseX >= this.controlX && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
   }

   public Component getName() {
      return this.name;
   }

   public Component getDisplayedValue() {
      return this.displayedValue;
   }

   protected void updateDisplayedValue() {
      this.displayedValue = this.option.getDisplayedValue();
   }

   @Override
   public Component getTooltip() {
      return this.option.getTooltip();
   }

   @Override
   public PerformanceImpact getImpact() {
      return this.option.getImpact();
   }

   @Override
   public NarrationPriority narrationPriority() {
      if (this.focused) {
         return NarrationPriority.FOCUSED;
      } else {
         return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
      }
   }

   @Override
   public final void updateNarration(NarrationElementOutput narrationElementOutput) {
   }
}
