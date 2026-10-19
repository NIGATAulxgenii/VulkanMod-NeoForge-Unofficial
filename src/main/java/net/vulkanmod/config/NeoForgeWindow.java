package net.vulkanmod.config;

import net.neoforged.fml.loading.ImmediateWindowHandler;
import org.lwjgl.opengl.GL;

import java.lang.reflect.Field;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

/** Hands off NeoForge's early OpenGL window before creating the Vulkan surface. */
public final class NeoForgeWindow {
    private NeoForgeWindow() {}

    public static long create(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        // Finish the loader's asynchronous window handoff first. Never destroy a
        // window while its render thread still owns the context.
        long earlyWindow = ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
        try {
            Field providerField = ImmediateWindowHandler.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(null);
            if (provider.getClass().getName().equals("net.neoforged.fml.earlydisplay.DisplayWindow")) {
                GL.createCapabilities();
                // The loader lives in the service layer, outside Mixin's transformer.
                // Keep reflection confined to this 1.21.1 lifecycle adapter.
                Field repaint = provider.getClass().getDeclaredField("repaintTick");
                repaint.setAccessible(true);
                repaint.set(provider, (Runnable) () -> {});
                provider.getClass().getMethod("close").invoke(provider);
            } else if (!provider.getClass().getSimpleName().equals("DummyProvider")) {
                throw new IllegalStateException("Unsupported early window provider: " + provider.getClass().getName());
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot release NeoForge's early OpenGL window", e);
        }
        glfwMakeContextCurrent(0L);
        GL.setCapabilities(null);
        glfwDestroyWindow(earlyWindow);
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        boolean undecorated = Platform.isWayLand() && (Platform.isGnome() || Platform.isWeston() || Platform.isGeneric());
        glfwWindowHint(GLFW_DECORATED, undecorated ? GLFW_FALSE : GLFW_TRUE);
        long window = glfwCreateWindow(width.getAsInt(), height.getAsInt(), title.get(), monitor.getAsLong(), 0L);
        if (window == 0L) throw new IllegalStateException("Cannot create the Vulkan GLFW window");
        return window;
    }
}
