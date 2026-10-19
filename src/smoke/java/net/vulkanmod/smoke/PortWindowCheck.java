package net.vulkanmod.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.video.WindowMode;
import org.lwjgl.glfw.GLFW;

public final class PortWindowCheck {
    private final WindowMode original;
    private final int width, height;
    private int step, ticks;

    public PortWindowCheck(Minecraft mc) {
        original = mc.options.fullscreen().get() ? WindowMode.EXCLUSIVE_FULLSCREEN
                : Initializer.CONFIG.windowMode == 1 ? WindowMode.WINDOWED_FULLSCREEN : WindowMode.WINDOWED;
        width = mc.getWindow().getWidth();
        height = mc.getWindow().getHeight();
        apply(WindowMode.WINDOWED_FULLSCREEN);
    }

    public boolean tick(Minecraft mc) {
        if (++ticks < 20 || Options.fullscreenDirty) return false;
        ticks = 0;
        WindowMode mode = step == 0 ? WindowMode.WINDOWED_FULLSCREEN : step == 1 ? WindowMode.EXCLUSIVE_FULLSCREEN
                : step == 2 ? WindowMode.WINDOWED : original;
        long window = mc.getWindow().getWindow();
        boolean exclusive = GLFW.glfwGetWindowMonitor(window) != 0;
        boolean decorated = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
        if (exclusive != (mode == WindowMode.EXCLUSIVE_FULLSCREEN)
                || mode != WindowMode.EXCLUSIVE_FULLSCREEN && decorated != (mode == WindowMode.WINDOWED))
            throw new IllegalStateException("Window mode did not apply: " + mode);
        if (++step == 1) apply(WindowMode.EXCLUSIVE_FULLSCREEN);
        else if (step == 2) apply(WindowMode.WINDOWED);
        else if (step == 3) apply(original);
        else {
            if (original == WindowMode.WINDOWED) GLFW.glfwSetWindowSize(window, width, height);
            Initializer.LOGGER.info("SMOKE window modes PASS: borderless, exclusive, windowed, restoration through real option callbacks");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void apply(WindowMode mode) {
        for (var block : Options.getVideoOpts()) for (var option : block.options()) {
            if (option.getName().getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("vulkanmod.options.windowMode")) {
                var windowMode = (Option<WindowMode>) option;
                windowMode.setNewValue(mode);
                windowMode.apply();
                return;
            }
        }
        throw new IllegalStateException("Window mode option missing");
    }
}
