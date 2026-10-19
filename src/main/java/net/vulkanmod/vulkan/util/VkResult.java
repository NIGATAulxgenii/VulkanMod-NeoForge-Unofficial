package net.vulkanmod.vulkan.util;

public class VkResult {
   public static final int VK_SUCCESS = 0;
   public static final int VK_NOT_READY = 1;
   public static final int VK_TIMEOUT = 2;
   public static final int VK_EVENT_SET = 3;
   public static final int VK_EVENT_RESET = 4;
   public static final int VK_INCOMPLETE = 5;
   public static final int VK_ERROR_OUT_OF_HOST_MEMORY = -1;
   public static final int VK_ERROR_OUT_OF_DEVICE_MEMORY = -2;
   public static final int VK_ERROR_INITIALIZATION_FAILED = -3;
   public static final int VK_ERROR_DEVICE_LOST = -4;
   public static final int VK_ERROR_MEMORY_MAP_FAILED = -5;
   public static final int VK_ERROR_LAYER_NOT_PRESENT = -6;
   public static final int VK_ERROR_EXTENSION_NOT_PRESENT = -7;
   public static final int VK_ERROR_FEATURE_NOT_PRESENT = -8;
   public static final int VK_ERROR_INCOMPATIBLE_DRIVER = -9;
   public static final int VK_ERROR_TOO_MANY_OBJECTS = -10;
   public static final int VK_ERROR_FORMAT_NOT_SUPPORTED = -11;
   public static final int VK_ERROR_FRAGMENTED_POOL = -12;
   public static final int VK_ERROR_UNKNOWN = -13;

   public static String decode(int result) {
      return switch (result) {
         case -13 -> "VK_ERROR_UNKNOWN";
         case -12 -> "VK_ERROR_FRAGMENTED_POOL";
         case -11 -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
         case -10 -> "VK_ERROR_TOO_MANY_OBJECTS";
         case -9 -> "VK_ERROR_INCOMPATIBLE_DRIVER";
         case -8 -> "VK_ERROR_FEATURE_NOT_PRESENT";
         case -7 -> "VK_ERROR_EXTENSION_NOT_PRESENT";
         case -6 -> "VK_ERROR_LAYER_NOT_PRESENT";
         case -5 -> "VK_ERROR_MEMORY_MAP_FAILED";
         case -4 -> "VK_ERROR_DEVICE_LOST";
         case -3 -> "VK_ERROR_INITIALIZATION_FAILED";
         case -2 -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
         case -1 -> "VK_ERROR_OUT_OF_HOST_MEMORY";
         case 0 -> "VK_SUCCESS";
         case 1 -> "VK_NOT_READY";
         case 2 -> "VK_TIMEOUT";
         case 3 -> "VK_EVENT_SET";
         case 4 -> "VK_EVENT_RESET";
         case 5 -> "VK_INCOMPLETE";
         default -> Integer.toString(result);
      };
   }
}
