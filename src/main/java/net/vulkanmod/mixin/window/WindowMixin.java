package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;
import net.vulkanmod.config.NeoForgeWindow;
import java.util.Optional;
import java.util.function.*;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
   @Final
   @Shadow
   private long window;
   @Shadow
   private boolean vsync;
   @Shadow
   private boolean fullscreen;
   @Shadow
   @Final
   private static Logger LOGGER;
   @Shadow
   private int windowedX;
   @Shadow
   private int windowedY;
   @Shadow
   private int windowedWidth;
   @Shadow
   private int windowedHeight;
   @Shadow
   private int x;
   @Shadow
   private int y;
   @Shadow
   private int width;
   @Shadow
   private int height;
   @Shadow
   private int framebufferWidth;
   @Shadow
   private int framebufferHeight;
   @Unique
   private boolean wasOnFullscreen = false;

   @Shadow
   public abstract int getWidth();

   @Shadow
   public abstract int getHeight();

   @Shadow
   protected abstract void updateFullscreen(boolean var1);

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
   private void redirect(int hint, int value) {
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
   private void redirect2(long window) {
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
   private GLCapabilities redirect2() {
      return null;
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;maxSupportedTextureSize()I"))
   private int redirect3() {
      return 0;
   }

   @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowSizeLimits(JIIII)V"))
   private void redirect4(long window, int minwidth, int minheight, int maxwidth, int maxheight) {
   }

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void initVulkanPlatform(WindowEventHandler handler, ScreenManager manager, DisplayData data, String title, String videoMode, CallbackInfo ci) {
        Initializer.initWindow();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"), remap = false)
    private long createVulkanWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        return NeoForgeWindow.create(width, height, title, monitor);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;positionWindow(Ljava/util/Optional;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;)Z"), remap = false)
    private boolean positionVulkanWindow(Optional<Object> monitor, IntConsumer width, IntConsumer height, IntConsumer x, IntConsumer y) {
        return false;
    }

   @Inject(method = "<init>", at = @At("RETURN"))
   private void getHandle(
      WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci
   ) {
      VRenderSystem.setWindow(this.window);
   }

   @Overwrite
   public void updateVsync(boolean vsync) {
      this.vsync = vsync;
      Vulkan.setVsync(vsync);
   }

   @Overwrite
   public void toggleFullScreen() {
      this.fullscreen = !this.fullscreen;
      Options.fullscreenDirty = true;
      if (!this.fullscreen) {
         Config config = Initializer.CONFIG;
         config.windowMode = WindowMode.WINDOWED.mode;
      }
   }

   @Overwrite
   public void updateDisplay() {
      RenderSystem.flipFrame(this.window);
      if (Options.fullscreenDirty) {
         Options.fullscreenDirty = false;
         this.updateFullscreen(this.vsync);
      }
   }

   @Overwrite
   private void setMode() {
      Config config = Initializer.CONFIG;
      VideoModeManager.checkConfigVideoMode(config);
      if (this.fullscreen) {
         config.windowMode = WindowMode.EXCLUSIVE_FULLSCREEN.mode;
      }

      if (this.fullscreen) {
         VideoModeManager.selectBestMonitor((Window)(Object)this);
         long monitor = VideoModeManager.selectedMonitor;
         VideoModeSet.VideoMode videoMode = config.videoMode;
         VideoModeSet set = VideoModeManager.getVideoModeSet(videoMode);
         boolean supported;
         if (set != null) {
            supported = set.hasRefreshRate(videoMode.refreshRate);
         } else {
            supported = false;
         }

         if (!supported) {
            LOGGER.error("Resolution not supported, using first available as fallback");
            videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
         }

         if (!this.wasOnFullscreen) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;
         }

         this.x = 0;
         this.y = 0;
         this.width = videoMode.width;
         this.height = videoMode.height;
         GLFW.glfwSetWindowMonitor(this.window, monitor, this.x, this.y, this.width, this.height, videoMode.refreshRate);
         this.wasOnFullscreen = true;
      } else if (config.windowMode == WindowMode.WINDOWED_FULLSCREEN.mode) {
         VideoModeManager.selectBestMonitor((Window)(Object)this);
         VideoModeSet.VideoMode videoModex = VideoModeManager.getOsVideoMode();
         if (!this.wasOnFullscreen) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;
         }

         int width = videoModex.width;
         int height = videoModex.height;
         GLFW.glfwSetWindowAttrib(this.window, 131077, 0);
         GLFW.glfwSetWindowMonitor(this.window, 0L, 0, 0, width, height, -1);
         this.width = width;
         this.height = height;
         this.wasOnFullscreen = true;
      } else {
         this.x = this.windowedX;
         this.y = this.windowedY;
         this.width = this.windowedWidth;
         this.height = this.windowedHeight;
         GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
         GLFW.glfwSetWindowAttrib(this.window, 131077, 1);
         this.wasOnFullscreen = false;
      }
   }

   @Overwrite
   private void onFramebufferResize(long window, int width, int height) {
      if (window == this.window) {
         int prevWidth = this.getWidth();
         int prevHeight = this.getHeight();
         if (width > 0 && height > 0) {
            this.framebufferWidth = width;
            this.framebufferHeight = height;
            Renderer.scheduleSwapChainUpdate();
         }
      }
   }

   @Overwrite
   private void onResize(long window, int width, int height) {
      this.width = width;
      this.height = height;
      if (width > 0 && height > 0) {
         Renderer.scheduleSwapChainUpdate();
      }
   }
}
