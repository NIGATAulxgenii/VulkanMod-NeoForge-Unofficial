package net.vulkanmod.vulkan.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.lang.reflect.Field;
import java.nio.IntBuffer;

import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

/** LWJGL 3.3.3 enumerates driver-sized extension arrays on a fixed 64 KiB stack. */
public final class VulkanInstanceFactory {
    private VulkanInstanceFactory() {}

    public static VkInstance create(long handle, VkInstanceCreateInfo info) {
        int required = extensionScratchBytes(handle);
        MemoryStack previous = MemoryStack.stackGet();
        if (previous.getPointer() >= required) return new VkInstance(handle, info);
        // Scope a larger scratch stack to this constructor only. Preserve all
        // caller frames and restore the original TLS even if driver probing fails.
        try {
            Field field = MemoryStack.class.getDeclaredField("TLS");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<MemoryStack> stacks = (ThreadLocal<MemoryStack>) field.get(null);
            stacks.set(MemoryStack.create(required));
            try {
                return new VkInstance(handle, info);
            } finally {
                stacks.set(previous);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot provide LWJGL 3.3.3 with extension scratch memory", e);
        }
    }

    private static int extensionScratchBytes(long instance) {
        int bytes = 64 * 1024;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long getProc = VK.getFunctionProvider().getFunctionAddress("vkGetInstanceProcAddr");
            long enumerateDevices = callPPP(instance, memAddress(stack.ASCII("vkEnumeratePhysicalDevices")), getProc);
            long enumerateExtensions = callPPP(instance, memAddress(stack.ASCII("vkEnumerateDeviceExtensionProperties")), getProc);
            IntBuffer count = stack.callocInt(1);
            if (callPPPI(instance, memAddress(count), NULL, enumerateDevices) != VK_SUCCESS) return bytes;
            PointerBuffer devices = stack.mallocPointer(count.get(0));
            if (callPPPI(instance, memAddress(count), memAddress(devices), enumerateDevices) != VK_SUCCESS) return bytes;
            int deviceCount = count.get(0);
            for (int i = 0; i < deviceCount; i++) {
                if (callPPPPI(devices.get(i), NULL, memAddress(count), NULL, enumerateExtensions) == VK_SUCCESS) {
                    bytes = Math.max(bytes, Math.addExact(Math.multiplyExact(count.get(0), VkExtensionProperties.SIZEOF), 64 * 1024));
                }
            }
        }
        return bytes;
    }
}
