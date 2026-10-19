package net.vulkanmod.config.gui.widget;

import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.KeyEventHelper;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class VTextInputWidget extends VAbstractWidget {
   public boolean selected = false;
   Consumer<VTextInputWidget> onSearch;
   private String text;
   private final Component placeholder;
   private int cursorPos = 0;
   private int selectionEnd = 0;
   private long lastBlinkTime = 0L;
   private boolean showCursor = true;
   private static final int CURSOR_BLINK_INTERVAL = 500;

   public VTextInputWidget(int x, int y, int width, int height, Component placeholder, Consumer<VTextInputWidget> onSearch) {
      this.setPosition(x, y, width, height);
      this.placeholder = placeholder;
      this.onSearch = onSearch;
      this.text = "";
   }

   @Override
   public void renderWidget(double mouseX, double mouseY) {
      if (this.isVisible()) {
         boolean hasText = !this.text.isEmpty();
         boolean isFocused = this.focused || this.selected;
         int backgroundColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.45F);
         int textColor = hasText ? VGuiConstants.COLOR_WHITE : VGuiConstants.COLOR_GRAY;
         GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
         this.renderHovering(0, 0);
         if (isFocused && this.cursorPos != this.selectionEnd) {
            int start = Math.min(this.cursorPos, this.selectionEnd);
            int end = Math.max(this.cursorPos, this.selectionEnd);
            String before = this.text.substring(0, start);
            String selected = this.text.substring(start, end);
            int xBefore = this.x + 8 + Minecraft.getInstance().font.width(before);
            int xSelected = Minecraft.getInstance().font.width(selected);
            int selColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.55F);
            GuiRenderer.fill(xBefore, this.y + 4, xBefore + xSelected, this.y + this.height - 4, selColor);
         }

         Component displayText = (Component)(hasText ? Component.literal(this.text) : this.placeholder);
         int var10002 = this.x + 8;
         int var10003 = this.y + (this.height - 8) / 2;
         GuiRenderer.drawString(Minecraft.getInstance().font, displayText, var10002, var10003, textColor | 0xFF000000);
         if (isFocused && this.showCursor) {
            String beforeCursor = this.text.substring(0, this.cursorPos);
            int cursorX = this.x + 8 + Minecraft.getInstance().font.width(beforeCursor);
            GuiRenderer.fill(cursorX, this.y + 6, cursorX + 1, this.y + this.height - 6, VGuiConstants.COLOR_WHITE);
         }

         if (isFocused) {
            int borderColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.8F);
            GuiRenderer.renderBorder(this.x, this.y, this.x + this.width, this.y + this.height, 1, borderColor);
         }

         if (isFocused) {
            long time = Util.getMillis();
            if (time - this.lastBlinkTime > 500L) {
               this.showCursor = !this.showCursor;
               this.lastBlinkTime = time;
            }
         } else {
            this.showCursor = true;
         }
      }
   }

   @Override
   public boolean keyPressed(int key, int scancode, int modifiers) {
      if (!this.focused && !this.selected) {
         return false;
      } else {
         boolean shift = KeyEventHelper.hasShiftDown(modifiers);
         boolean ctrl = KeyEventHelper.hasControlDown(modifiers);
         if (key != 257 && key != 335) {
            if (this.cursorPos != this.selectionEnd) {
               int start = Math.min(this.cursorPos, this.selectionEnd);
               int end = Math.max(this.cursorPos, this.selectionEnd);
               if (key == 259 || key == 261) {
                  this.text = this.text.substring(0, start) + this.text.substring(end);
                  this.cursorPos = start;
                  this.selectionEnd = start;
                  this.onSearch.accept(this);
                  return true;
               }
            }

            if (key == 259) {
               if (this.cursorPos > 0) {
                  this.text = this.text.substring(0, this.cursorPos - 1) + this.text.substring(this.cursorPos);
                  this.cursorPos--;
                  this.selectionEnd = this.cursorPos;
                  this.onSearch.accept(this);
               }

               return true;
            } else if (key == 261) {
               if (this.cursorPos < this.text.length()) {
                  this.text = this.text.substring(0, this.cursorPos) + this.text.substring(this.cursorPos + 1);
                  this.onSearch.accept(this);
               }

               return true;
            } else if (ctrl && key == 65) {
               this.cursorPos = this.text.length();
               this.selectionEnd = 0;
               return true;
            } else if (key == 263) {
               if (this.cursorPos > 0) {
                  this.cursorPos--;
               }

               if (!shift) {
                  this.selectionEnd = this.cursorPos;
               }

               return true;
            } else if (key == 262) {
               if (this.cursorPos < this.text.length()) {
                  this.cursorPos++;
               }

               if (!shift) {
                  this.selectionEnd = this.cursorPos;
               }

               return true;
            } else {
               String keyName = GLFW.glfwGetKeyName(key, scancode);
               if (keyName != null && keyName.length() == 1) {
                  char c = shift ? keyName.toUpperCase().charAt(0) : keyName.charAt(0);
                  if (this.cursorPos != this.selectionEnd) {
                     int start = Math.min(this.cursorPos, this.selectionEnd);
                     int end = Math.max(this.cursorPos, this.selectionEnd);
                     this.text = this.text.substring(0, start) + c + this.text.substring(end);
                     this.cursorPos = start + 1;
                  } else {
                     this.text = this.text.substring(0, this.cursorPos) + c + this.text.substring(this.cursorPos);
                     this.cursorPos++;
                  }

                  this.selectionEnd = this.cursorPos;
                  this.onSearch.accept(this);
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            this.onSearch.accept(this);
            return true;
         }
      }
   }

   public String getInput() {
      return this.text;
   }

   public void setInput(String input) {
      this.text = input != null ? input : "";
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public boolean isActive() {
      return this.active;
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent event) {
      return this.active && this.visible ? super.nextFocusPath(event) : null;
   }

   @Override
   public boolean mouseClicked(double mX, double mY, int button) {
      if (this.active && this.visible) {
         boolean clicked = this.clicked(mX, mY);
         if (!clicked) {
            this.setFocused(false);
            this.selected = false;
            return false;
         } else {
            this.setFocused(true);
            this.selected = true;
            int relX = (int)mX - (this.x + 8);
            int pos = 0;

            for (int i = 0; i < this.text.length() && Minecraft.getInstance().font.width(this.text.substring(0, i + 1)) <= relX; i++) {
               pos = i + 1;
            }

            this.cursorPos = pos;
            this.selectionEnd = pos;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public void setFocused(boolean focused) {
      super.setFocused(focused);
      if (!focused) {
         this.selected = false;
      }
   }
}
