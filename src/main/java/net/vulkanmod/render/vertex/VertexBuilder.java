package net.vulkanmod.render.vertex;

import org.lwjgl.system.MemoryUtil;

public interface VertexBuilder {
   void vertex(long var1, float var3, float var4, float var5, int var6, float var7, float var8, int var9, int var10);

   void position(long var1, float var3, float var4, float var5);

   void color(long var1, int var3);

   void uv(long var1, float var3, float var4);

   void light(long var1, int var3);

   void normal(long var1, int var3);

   int getStride();

   public static class CompressedVertexBuilder implements VertexBuilder {
      private static final int VERTEX_SIZE = 16;
      public static final float POS_CONV_MUL = 2048.0F;
      public static final float POS_OFFSET = -4.0F;
      public static final float POS_OFFSET_CONV = -8192.0F;
      public static final float UV_CONV_MUL = 32768.0F;

      @Override
      public void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
         short sX = (short)(x * 2048.0F + -8192.0F);
         short sY = (short)(y * 2048.0F + -8192.0F);
         short sZ = (short)(z * 2048.0F + -8192.0F);
         MemoryUtil.memPutShort(ptr + 0L, sX);
         MemoryUtil.memPutShort(ptr + 2L, sY);
         MemoryUtil.memPutShort(ptr + 4L, sZ);
         short l = (short)(light >>> 8 & 0xFF00 | light & 0xFF);
         MemoryUtil.memPutShort(ptr + 6L, l);
         MemoryUtil.memPutShort(ptr + 8L, (short)(u * 32768.0F));
         MemoryUtil.memPutShort(ptr + 10L, (short)(v * 32768.0F));
         MemoryUtil.memPutInt(ptr + 12L, color);
      }

      @Override
      public void position(long ptr, float x, float y, float z) {
         short sX = (short)(x * 2048.0F + -8192.0F);
         short sY = (short)(y * 2048.0F + -8192.0F);
         short sZ = (short)(z * 2048.0F + -8192.0F);
         MemoryUtil.memPutShort(ptr + 0L, sX);
         MemoryUtil.memPutShort(ptr + 2L, sY);
         MemoryUtil.memPutShort(ptr + 4L, sZ);
      }

      @Override
      public void color(long ptr, int color) {
         MemoryUtil.memPutInt(ptr + 12L, color);
      }

      @Override
      public void uv(long ptr, float u, float v) {
         MemoryUtil.memPutShort(ptr + 8L, (short)(u * 32768.0F));
         MemoryUtil.memPutShort(ptr + 10L, (short)(v * 32768.0F));
      }

      @Override
      public void light(long ptr, int light) {
         short l = (short)(light >>> 8 & 0xFF00 | light & 0xFF);
         MemoryUtil.memPutShort(ptr + 6L, l);
      }

      @Override
      public void normal(long ptr, int normal) {
      }

      @Override
      public int getStride() {
         return 16;
      }
   }

   public static class DefaultVertexBuilder implements VertexBuilder {
      private static final int VERTEX_SIZE = 32;

      @Override
      public void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
         MemoryUtil.memPutFloat(ptr + 0L, x);
         MemoryUtil.memPutFloat(ptr + 4L, y);
         MemoryUtil.memPutFloat(ptr + 8L, z);
         MemoryUtil.memPutInt(ptr + 12L, color);
         MemoryUtil.memPutFloat(ptr + 16L, u);
         MemoryUtil.memPutFloat(ptr + 20L, v);
         MemoryUtil.memPutShort(ptr + 24L, (short)(light & 65535));
         MemoryUtil.memPutShort(ptr + 26L, (short)(light >> 16 & 65535));
         MemoryUtil.memPutInt(ptr + 28L, packedNormal);
      }

      @Override
      public void position(long ptr, float x, float y, float z) {
      }

      @Override
      public void color(long ptr, int color) {
      }

      @Override
      public void uv(long ptr, float u, float v) {
      }

      @Override
      public void light(long ptr, int light) {
      }

      @Override
      public void normal(long ptr, int normal) {
      }

      @Override
      public int getStride() {
         return 32;
      }
   }
}
