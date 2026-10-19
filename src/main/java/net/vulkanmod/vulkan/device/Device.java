package net.vulkanmod.vulkan.device;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan11Features;
import org.lwjgl.vulkan.VkExtensionProperties.Buffer;

public class Device {
   final VkPhysicalDevice physicalDevice;
   final VkPhysicalDeviceProperties properties;
   private final int vendorId;
   public final String vendorIdString;
   public final String deviceName;
   public final String driverVersion;
   public final String vkVersion;
   public final VkPhysicalDeviceFeatures2 availableFeatures;
   public final VkPhysicalDeviceVulkan11Features availableFeatures11;
   private boolean drawIndirectSupported;

   public Device(VkPhysicalDevice device) {
      this.physicalDevice = device;
      this.properties = VkPhysicalDeviceProperties.malloc();
      VK10.vkGetPhysicalDeviceProperties(this.physicalDevice, this.properties);
      this.vendorId = this.properties.vendorID();
      this.vendorIdString = decodeVendor(this.properties.vendorID());
      this.deviceName = this.properties.deviceNameString();
      this.driverVersion = decodeDvrVersion(this.properties.driverVersion(), this.properties.vendorID());
      this.vkVersion = decDefVersion(this.properties.apiVersion());
      this.availableFeatures = VkPhysicalDeviceFeatures2.calloc();
      this.availableFeatures.sType$Default();
      this.availableFeatures11 = VkPhysicalDeviceVulkan11Features.malloc();
      this.availableFeatures11.sType$Default();
      this.availableFeatures.pNext(this.availableFeatures11);
      VK11.vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.availableFeatures);
      if (this.availableFeatures.features().multiDrawIndirect() && this.availableFeatures11.shaderDrawParameters()) {
         this.drawIndirectSupported = true;
      }
   }

   private static String decodeVendor(int i) {
      return switch (i) {
         case 4098, 4130 -> "AMD";
         case 4112 -> "Imagination Technologies";
         case 4203 -> "Apple";
         case 4318 -> "Nvidia";
         case 5045 -> "ARM";
         case 5348 -> "Broadcom";
         case 6880 -> "Google";
         case 20803 -> "Qualcomm";
         case 32902 -> "Intel";
         case 65541 -> "Mesa";
         default -> "undef";
      };
   }

   static String decDefVersion(int v) {
      return VK10.VK_VERSION_MAJOR(v) + "." + VK10.VK_VERSION_MINOR(v) + "." + VK10.VK_VERSION_PATCH(v);
   }

   private static String decodeDvrVersion(int v, int i) {
      return switch (i) {
         case 4098, 4130 -> decDefVersion(v);
         case 4318 -> decodeNvidia(v);
         case 32902 -> decIntelVersion(v);
         default -> decDefVersion(v);
      };
   }

   private static String decIntelVersion(int v) {
      return GLFW.glfwGetPlatform() == 393217 ? (v >>> 14) + "." + (v & 16383) : decDefVersion(v);
   }

   private static String decodeNvidia(int v) {
      return (v >>> 22 & 1023) + "." + (v >>> 14 & 0xFF) + "." + (v >>> 6 & 0xFF) + "." + (v & 0xFF);
   }

   static int getVkVer() {
      MemoryStack stack = MemoryStack.stackPush();

      int var3;
      try {
         IntBuffer a = stack.mallocInt(1);
         VK11.vkEnumerateInstanceVersion(a);
         int vkVer1 = a.get(0);
         if (VK10.VK_VERSION_MINOR(vkVer1) < 2) {
            throw new RuntimeException("Vulkan 1.2 not supported: Only Has: %s".formatted(decDefVersion(vkVer1)));
         }

         var3 = vkVer1;
      } catch (Throwable var5) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (stack != null) {
         stack.close();
      }

      return var3;
   }

   public Set<String> getUnsupportedExtensions(Set<String> requiredExtensions) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         var count = stack.ints(0);
         VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, count, null);
         try (VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0))) {
            VK10.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, count, properties);
            Set<String> unsupported = new java.util.HashSet<>(requiredExtensions);
            properties.forEach(property -> unsupported.remove(property.extensionNameString()));
            return unsupported;
         }
      }
   }

   public boolean isDrawIndirectSupported() {
      return this.drawIndirectSupported;
   }

   public boolean isAMD() {
      return this.vendorId == 4130 || this.vendorId == 4098;
   }

   public boolean isNvidia() {
      return this.vendorId == 4318;
   }

   public boolean isIntel() {
      return this.vendorId == 32902;
   }
}
