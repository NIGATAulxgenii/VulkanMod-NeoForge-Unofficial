package net.vulkanmod.vulkan.util;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import sun.misc.Unsafe;

public class VUtil {
   public static final boolean CHECKS = true;
   public static final int UINT32_MAX = -1;
   public static final long UINT64_MAX = -1L;
   public static final Unsafe UNSAFE;

   public static PointerBuffer asPointerBuffer(Collection<String> collection) {
      MemoryStack stack = MemoryStack.stackGet();
      PointerBuffer buffer = stack.mallocPointer(collection.size());
      collection.stream().map(stack::UTF8).forEach(buffer::put);
      return (PointerBuffer)buffer.rewind();
   }

   public static void memcpy(ByteBuffer src, long dstPtr) {
      MemoryUtil.memCopy(MemoryUtil.memAddress0(src), dstPtr, src.capacity());
   }

   public static void memcpy(ByteBuffer src, Buffer dst, long size) {
      if (size > dst.getBufferSize() - dst.getUsedBytes()) {
         throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
      } else {
         long srcPtr = MemoryUtil.memAddress(src);
         long dstPtr = dst.getDataPtr() + dst.getUsedBytes();
         MemoryUtil.memCopy(srcPtr, dstPtr, size);
      }
   }

   public static void memcpy(Buffer src, ByteBuffer dst, long size) {
      if (size > dst.remaining()) {
         throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
      } else {
         long srcPtr = src.getDataPtr();
         long dstPtr = MemoryUtil.memAddress(dst);
         MemoryUtil.memCopy(srcPtr, dstPtr, size);
      }
   }

   public static void memcpy(ByteBuffer src, Buffer dst, long size, long srcOffset, long dstOffset) {
      if (size > dst.getBufferSize() - dstOffset) {
         throw new IllegalArgumentException("Upload size is greater than available dst buffer size");
      } else {
         long dstPtr = dst.getDataPtr() + dstOffset;
         long srcPtr = MemoryUtil.memAddress(src) + srcOffset;
         MemoryUtil.memCopy(srcPtr, dstPtr, size);
      }
   }

   public static int align(int x, int align) {
      int r = x % align;
      return r == 0 ? x : x + align - r;
   }

   static {
      Field f = null;

      try {
         f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         UNSAFE = (Unsafe)f.get(null);
      } catch (IllegalAccessException | NoSuchFieldException var2) {
         throw new RuntimeException(var2);
      }
   }
}
