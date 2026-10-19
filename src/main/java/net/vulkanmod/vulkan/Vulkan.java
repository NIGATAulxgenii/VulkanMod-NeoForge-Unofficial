package net.vulkanmod.vulkan;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffers;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.util.VkResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkLayerProperties.Buffer;

public class Vulkan {
   public static final boolean ENABLE_VALIDATION_LAYERS = false;
   public static final boolean DYNAMIC_RENDERING = false;
   public static final Set<String> VALIDATION_LAYERS = null;
   public static final Set<String> REQUIRED_EXTENSION = getRequiredExtensionSet();
   public static long window;
   private static VkInstance instance;
   private static long debugMessenger;
   private static long surface;
   private static long commandPool;
   private static VkCommandBuffer immediateCmdBuffer;
   private static long immediateFence;
   private static long allocator;
   private static final StagingBuffers stagingBuffers = new StagingBuffers();
   public static boolean use24BitsDepthFormat = true;
   private static int DEFAULT_DEPTH_FORMAT = 0;

   private static Set<String> getRequiredExtensionSet() {
      ArrayList<String> extensions = new ArrayList<>(List.of("VK_KHR_swapchain"));
      return new HashSet<>(extensions);
   }

   private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
      VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
      String s;
      if ((messageSeverity & 4096) != 0) {
         s = "\u001b[31m" + callbackData.pMessageString();
      } else {
         s = callbackData.pMessageString();
      }

      System.err.println(s);
      if ((messageSeverity & 4096) != 0) {
         System.nanoTime();
      }

      return 0;
   }

   private static int createDebugUtilsMessengerEXT(
      VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo, VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger
   ) {
      return VK10.vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != 0L
         ? EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger)
         : -7;
   }

   private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks) {
      if (VK10.vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != 0L) {
         EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
      }
   }

   public static VkDevice getVkDevice() {
      return DeviceManager.vkDevice;
   }

   public static long getAllocator() {
      return allocator;
   }

   public static void initVulkan(long window) {
      createInstance();
      setupDebugMessenger();
      createSurface(window);
      DeviceManager.init(instance);
      createVma();
      MemoryTypes.createMemoryTypes();
      createCommandPool();
      setupDepthFormat();
   }

   static void createStagingBuffers() {
      stagingBuffers.updateFrameCount(Renderer.getFramesNum());
   }

   static void setupDepthFormat() {
      DEFAULT_DEPTH_FORMAT = DeviceManager.findDepthFormat(use24BitsDepthFormat);
   }

   public static void waitIdle() {
      VK10.vkDeviceWaitIdle(DeviceManager.vkDevice);
   }

   public static void cleanUp() {
      VK10.vkDeviceWaitIdle(DeviceManager.vkDevice);
      VK10.vkDestroyCommandPool(DeviceManager.vkDevice, commandPool, null);
      VK10.vkDestroyFence(DeviceManager.vkDevice, immediateFence, null);
      Pipeline.destroyPipelineCache();
      Renderer.getInstance().cleanUpResources();
      freeStagingBuffers();

      try {
         MemoryManager.getInstance().freeAllBuffers();
      } catch (Exception var1) {
         var1.printStackTrace();
      }

      Vma.vmaDestroyAllocator(allocator);
      SamplerManager.cleanUp();
      DeviceManager.destroy();
      destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
      KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
      VK10.vkDestroyInstance(instance, null);
   }

   private static void freeStagingBuffers() {
      stagingBuffers.free();
   }

   private static void createInstance() {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
         appInfo.sType(0);
         appInfo.pApplicationName(stack.UTF8Safe("VulkanMod"));
         appInfo.applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
         appInfo.pEngineName(stack.UTF8Safe("VulkanMod Engine"));
         appInfo.engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
         appInfo.apiVersion(VK12.VK_API_VERSION_1_2);
         VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
         createInfo.sType(1);
         createInfo.pApplicationInfo(appInfo);
         createInfo.ppEnabledExtensionNames(getRequiredInstanceExtensions());
         PointerBuffer instancePtr = stack.mallocPointer(1);
         int result = VK10.vkCreateInstance(createInfo, null, instancePtr);
         checkResult(result, "Failed to create instance");
         instance = net.vulkanmod.vulkan.util.VulkanInstanceFactory.create(instancePtr.get(0), createInfo);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }
   }

   static boolean checkValidationLayerSupport() {
      MemoryStack stack = MemoryStack.stackPush();

      boolean var4;
      try {
         IntBuffer layerCount = stack.ints(0);
         VK10.vkEnumerateInstanceLayerProperties(layerCount, null);
         Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
         VK10.vkEnumerateInstanceLayerProperties(layerCount, availableLayers);
         Set<String> availableLayerNames = availableLayers.stream().<String>map(VkLayerProperties::layerNameString).collect(Collectors.toSet());
         var4 = availableLayerNames.containsAll(VALIDATION_LAYERS);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      return var4;
   }

   private static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
      debugCreateInfo.sType(1000128004);
      debugCreateInfo.messageSeverity(4352);
      debugCreateInfo.messageType(7);
      debugCreateInfo.pfnUserCallback(Vulkan::debugCallback);
   }

   private static void setupDebugMessenger() {
   }

   public static void setDebugLabel(MemoryStack stack, int objectType, long handle, String label) {
   }

   private static void createSurface(long handle) {
      window = handle;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer target = stack.longs(0L);
         // GLFW is in NeoForge's bootstrap layer; pass handles across the module boundary.
         checkResult(GLFWVulkan.nglfwCreateWindowSurface(instance.address(), window, 0L, org.lwjgl.system.MemoryUtil.memAddress(target)), "Failed to create window surface");
         surface = target.get(0);
      }
   }

   private static void createVma() {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(stack);
         vulkanFunctions.set(instance, DeviceManager.vkDevice);
         VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack);
         allocatorCreateInfo.physicalDevice(DeviceManager.physicalDevice);
         allocatorCreateInfo.device(DeviceManager.vkDevice);
         allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);
         allocatorCreateInfo.instance(instance);
         allocatorCreateInfo.vulkanApiVersion(VK12.VK_API_VERSION_1_2);
         PointerBuffer pAllocator = stack.pointers(0L);
         checkResult(Vma.vmaCreateAllocator(allocatorCreateInfo, pAllocator), "Failed to create Allocator");
         allocator = pAllocator.get(0);
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
   }

   private static void createCommandPool() {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         Queue.QueueFamilyIndices queueFamilyIndices = Queue.getQueueFamilies();
         VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
         poolInfo.sType(39);
         poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);
         poolInfo.flags(2);
         LongBuffer pCommandPool = stack.mallocLong(1);
         checkResult(VK10.vkCreateCommandPool(DeviceManager.vkDevice, poolInfo, null, pCommandPool), "Failed to create command pool");
         commandPool = pCommandPool.get(0);
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
   }

   private static PointerBuffer getRequiredInstanceExtensions() {
      return GLFWVulkan.glfwGetRequiredInstanceExtensions();
   }

   public static void checkResult(int result, String errorMessage) {
      if (result != 0) {
         throw new RuntimeException(String.format("%s: %s", errorMessage, VkResult.decode(result)));
      }
   }

   public static void setVsync(boolean b) {
      SwapChain swapChain = Renderer.getInstance().getSwapChain();
      if (swapChain.isVsync() != b) {
         Renderer.scheduleSwapChainUpdate();
         swapChain.setVsync(b);
      }
   }

   public static int getDefaultDepthFormat() {
      return DEFAULT_DEPTH_FORMAT;
   }

   public static long getSurface() {
      return surface;
   }

   public static long getCommandPool() {
      return commandPool;
   }

   public static StagingBuffer getStagingBuffer() {
      return stagingBuffers.getStagingBuffer();
   }

   public static StagingBuffers getStagingBuffers() {
      return stagingBuffers;
   }

   public static Device getDevice() {
      return DeviceManager.device;
   }
}
