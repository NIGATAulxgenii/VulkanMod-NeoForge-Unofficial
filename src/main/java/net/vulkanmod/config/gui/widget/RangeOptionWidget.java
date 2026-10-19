package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.RangeOption;
import net.vulkanmod.vulkan.util.ColorUtil;

public class RangeOptionWidget extends OptionWidget<RangeOption> {
   private boolean focused;

   public RangeOptionWidget(RangeOption option, Component name) {
      super(option, name);
      this.setValue(option.getScaledValue());
   }

   @Override
   protected void renderControls(double mouseX, double mouseY) {
      float scaledValue = this.option.getScaledNewValue();
      int valueX = this.controlX + (int)(scaledValue * this.controlWidth);
      if (this.controlHovered && this.active) {
         int halfWidth = 2;
         int halfHeight = 4;
         int y0 = (int)(this.y + this.height * 0.5F - 1.0F);
         int y1 = (int)(y0 + 2.0F);
         GuiRenderer.fill(this.controlX, y0, this.controlX + this.controlWidth, y1, ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.1F));
         GuiRenderer.fill(this.controlX, y0, valueX - halfWidth, y1, ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.3F));
         int color = ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.3F);
         GuiRenderer.renderBorder(valueX - halfWidth, y0 - halfHeight, valueX + halfWidth, y1 + halfHeight, 1, color);
      } else {
         int y0 = (int)(this.y + this.height - 5.0F);
         int y1 = (int)(y0 + 1.5F);
         GuiRenderer.fill(this.controlX, y0, this.controlX + this.controlWidth, y1, ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, 0.3F));
         float alpha = this.active ? 0.8F : 0.3F;
         GuiRenderer.fill(this.controlX, y0, valueX, y1, ColorUtil.ARGB.pack(1.0F, 1.0F, 1.0F, alpha));
      }

      int color = this.active ? -1 : -6250336;
      Font font = Minecraft.getInstance().font;
      Component text = this.getDisplayedValue();
      int width = font.width(text);
      int x = this.controlX + this.controlWidth / 2 - width / 2;
      int y = this.y + (this.height - 9) / 2;
      GuiRenderer.drawString(font, text.getVisualOrderText(), x, y, color);
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      this.setValueFromMouse(mouseX);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean isLeft = keyCode == 263;
      boolean isRight = keyCode == 262;
      if (isLeft || isRight) {
         float direction = isLeft ? -1.0F : 1.0F;
         double currentValue = this.option.getScaledValue();
         this.setValue(currentValue + direction / (this.width - 8));
      }

      return false;
   }

   @Override
   public void setFocused(boolean bl) {
      this.focused = bl;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   private void setValueFromMouse(double mouseX) {
      this.setValue((mouseX - (this.controlX + 4)) / (this.controlWidth - 8));
   }

   private void setValue(double value) {
      double currentValue = this.option.getScaledValue();
      value = Mth.clamp(value, 0.0, 1.0);
      if (currentValue != value) {
         this.applyNewValue((float)value);
      }

      this.updateDisplayedValue();
   }

   @Override
   protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
      this.setValueFromMouse(mouseX);
   }

   private void applyNewValue(float value) {
      this.option.setNewValueFromScaledFloat(value);
   }

   @Override
   public void playDownSound(SoundManager soundManager) {
   }

   @Override
   public void onRelease(double mouseX, double mouseY) {
      if (this.controlHovered) {
         super.playDownSound(Minecraft.getInstance().getSoundManager());
      }
   }
}
