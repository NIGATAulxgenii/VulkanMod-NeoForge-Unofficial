package net.vulkanmod.render.chunk.util;

import java.util.Arrays;
import java.util.Iterator;
import net.vulkanmod.render.chunk.ChunkArea;

public record AreaSetQueue(int size, int[] set, StaticQueue<ChunkArea> queue) {
   public AreaSetQueue(int size) {
      this(size, new int[(int)Math.ceil(size / 32.0F)], new StaticQueue<>(size));
   }

   public void add(ChunkArea chunkArea) {
      int i = chunkArea.index >> 5;
      int mask = 1 << (chunkArea.index & 31);
      if ((this.set[i] & mask) == 0) {
         this.queue.add(chunkArea);
         this.set[i] = this.set[i] | mask;
      }
   }

   public void clear() {
      Arrays.fill(this.set, 0);
      this.queue.clear();
   }

   public Iterator<ChunkArea> iterator(boolean reverseOrder) {
      return this.queue.iterator(reverseOrder);
   }

   public Iterator<ChunkArea> iterator() {
      return this.iterator(false);
   }
}
