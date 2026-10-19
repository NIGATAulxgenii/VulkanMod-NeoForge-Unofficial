package net.vulkanmod.config.video;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

public class VideoModeSet {
   public static VideoModeSet DUMMY;
   public final int width;
   public final int height;
   public final int bitDepth;
   List<Integer> refreshRates = new ObjectArrayList();

   public static VideoModeSet getDummy() {
      if (DUMMY == null) {
         VideoModeSet set = new VideoModeSet(-1, -1, -1);
         set.addRefreshRate(-1);
         DUMMY = set;
      }

      return DUMMY;
   }

   public VideoModeSet(int width, int height, int bitDepth) {
      this.width = width;
      this.height = height;
      this.bitDepth = bitDepth;
   }

   public int getRefreshRate() {
      return this.refreshRates.get(0);
   }

   public boolean hasRefreshRate(int r) {
      return this.refreshRates.contains(r);
   }

   public List<Integer> getRefreshRates() {
      return this.refreshRates;
   }

   void addRefreshRate(int rr) {
      this.refreshRates.add(rr);
   }

   @Override
   public String toString() {
      return this.width + " x " + this.height;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         VideoModeSet that = (VideoModeSet)o;
         return this.width == that.width && this.height == that.height && this.bitDepth == that.bitDepth && this.refreshRates.equals(that.refreshRates);
      } else {
         return false;
      }
   }

   public VideoModeSet.VideoMode getVideoMode(int refresh) {
      int idx = this.refreshRates.indexOf(refresh);
      if (idx == -1) {
         idx = 0;
      }

      return new VideoModeSet.VideoMode(this.width, this.height, this.bitDepth, this.refreshRates.get(idx));
   }

   public VideoModeSet.VideoMode getVideoMode() {
      int refreshRate = this.refreshRates.get(this.refreshRates.size() - 1);
      return new VideoModeSet.VideoMode(this.width, this.height, this.bitDepth, refreshRate);
   }

   public static final class VideoMode {
      public int width;
      public int height;
      public int bitDepth;
      public int refreshRate;

      public VideoMode(int width, int height, int bitDepth, int refreshRate) {
         this.width = width;
         this.height = height;
         this.bitDepth = bitDepth;
         this.refreshRate = refreshRate;
      }

      @Override
      public String toString() {
         return "VideoMode[width=" + this.width + ", height=" + this.height + ", bitDepth=" + this.bitDepth + ", refreshRate=" + this.refreshRate + "]";
      }
   }
}
