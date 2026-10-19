package net.vulkanmod.config.option;

import java.util.ArrayList;
import java.util.List;
import net.vulkanmod.config.gui.OptionBlock;

public class Page {
   private final String name;
   private final List<Page.Block> blocks = new ArrayList<>();

   private Page(String name) {
      this.name = name;
   }

   public static Page of(String name) {
      return new Page(name);
   }

   public Page.Block block(String title) {
      Page.Block block = new Page.Block(title, this);
      this.blocks.add(block);
      return block;
   }

   public static class Block {
      private final String title;
      private final List<Option<?>> options = new ArrayList<>();
      private final Page parent;

      private Block(String title, Page parent) {
         this.title = title;
         this.parent = parent;
      }

      public Page.Block add(Option<?> option) {
         this.options.add(option);
         return this;
      }

      public Page done() {
         return this.parent;
      }

      private OptionBlock build() {
         return new OptionBlock(this.title, this.options.toArray(new Option[0]));
      }
   }
}
