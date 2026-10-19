package net.vulkanmod.config.video;

import org.jetbrains.annotations.NotNull;

public record VideoMode(int width, int height, int bitDepth, int refreshRate) {
   @NotNull
   @Override
   public String toString() {
      return this.width + "×" + this.height + (this.refreshRate > 0 ? " @ " + this.refreshRate + "Hz" : "");
   }

   public VideoMode withRefreshRate(int newRate) {
      return new VideoMode(this.width, this.height, this.bitDepth, newRate);
   }
}
