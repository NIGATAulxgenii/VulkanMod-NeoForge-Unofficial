package net.vulkanmod.config.gui.util;

import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.option.CyclingOption;

public class SearchHelper {
   public static <T> boolean matchesAnyValue(CyclingOption<T> cycling, String searchTerm) {
      Function<T, Component> translator = cycling.getTranslator();

      for (T value : cycling.getValues()) {
         String translated = translator.apply(value).getString().toLowerCase();
         if (translated.contains(searchTerm)) {
            return true;
         }
      }

      return false;
   }
}
