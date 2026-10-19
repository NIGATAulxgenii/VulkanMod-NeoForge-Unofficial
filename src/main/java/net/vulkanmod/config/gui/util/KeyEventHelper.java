package net.vulkanmod.config.gui.util;

import net.minecraft.client.Minecraft;

public abstract class KeyEventHelper {
   public static final boolean REPLACE_CTRL_KEY_WITH_CMD_KEY = Minecraft.ON_OSX;
   public static final int EDIT_SHORTCUT_KEY_LEFT = REPLACE_CTRL_KEY_WITH_CMD_KEY ? 343 : 341;
   public static final int EDIT_SHORTCUT_KEY_RIGHT = REPLACE_CTRL_KEY_WITH_CMD_KEY ? 347 : 345;
   public static final int EDIT_SHORTCUT_KEY_MODIFIER = REPLACE_CTRL_KEY_WITH_CMD_KEY ? 8 : 2;

   public static boolean hasShiftDown(int modifiers) {
      return (modifiers & 1) != 0;
   }

   public static boolean hasControlDown(int modifiers) {
      return (modifiers & EDIT_SHORTCUT_KEY_MODIFIER) != 0;
   }
}
