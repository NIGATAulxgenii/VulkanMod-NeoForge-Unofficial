package net.vulkanmod.config.video;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;

public abstract class VideoModeManager {
   public static Long2ObjectMap<Monitor> monitors;
   public static Long2ObjectMap<VideoModeSet[]> monitorToVideoModeSets = new Long2ObjectOpenHashMap();
   public static Long2ObjectMap<VideoModeSet.VideoMode> osVideoModes = new Long2ObjectOpenHashMap();
   public static long selectedMonitor;
   public static VideoModeSet.VideoMode selectedVideoMode;

   public static void init(Long2ObjectMap<Monitor> monitors) {
      VideoModeManager.monitors = monitors;
      monitorToVideoModeSets.clear();
      LongIterator var1 = VideoModeManager.monitors.keySet().iterator();

      while (var1.hasNext()) {
         long monitor = (Long)var1.next();
         addMonitorVideoModes(monitor);
      }
   }

   public static void addMonitorVideoModes(long monitor) {
      monitorToVideoModeSets.put(monitor, getVideoResolutions(monitor));
      osVideoModes.put(monitor, getCurrentVideoMode(monitor));
   }

   public static void removeMonitor(long monitor) {
      monitorToVideoModeSets.remove(monitor);
      osVideoModes.remove(monitor);
   }

   public static void applySelectedVideoMode() {
      Initializer.CONFIG.videoMode = selectedVideoMode;
   }

   public static VideoModeSet[] getVideoResolutions() {
      return (VideoModeSet[])monitorToVideoModeSets.get(selectedMonitor);
   }

   public static VideoModeSet getFirstAvailable() {
      VideoModeSet[] videoModeSets = (VideoModeSet[])monitorToVideoModeSets.get(GLFW.glfwGetPrimaryMonitor());
      return videoModeSets != null ? videoModeSets[videoModeSets.length - 1] : null;
   }

   public static VideoModeSet.VideoMode getOsVideoMode() {
      return (VideoModeSet.VideoMode)osVideoModes.get(selectedMonitor);
   }

   public static VideoModeSet.VideoMode getCurrentVideoMode(long monitor) {
      GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
      if (vidMode == null) {
         throw new NullPointerException("Unable to get current video mode");
      } else {
         return new VideoModeSet.VideoMode(vidMode.width(), vidMode.height(), vidMode.redBits(), vidMode.refreshRate());
      }
   }

   public static VideoModeSet[] getVideoResolutions(long monitor) {
      Buffer buffer = GLFW.glfwGetVideoModes(monitor);
      List<VideoModeSet> videoModeSets = new ArrayList<>();
      int currWidth = 0;
      int currHeight = 0;
      int currBitDepth = 0;
      VideoModeSet videoModeSet = null;

      for (int i = 0; i < buffer.limit(); i++) {
         buffer.position(i);
         int bitDepth = buffer.redBits();
         if (buffer.redBits() >= 8 && buffer.greenBits() == bitDepth && buffer.blueBits() == bitDepth) {
            int width = buffer.width();
            int height = buffer.height();
            int refreshRate = buffer.refreshRate();
            if (currWidth != width || currHeight != height || currBitDepth != bitDepth) {
               currWidth = width;
               currHeight = height;
               currBitDepth = bitDepth;
               videoModeSet = new VideoModeSet(width, height, bitDepth);
               videoModeSets.add(videoModeSet);
            }

            videoModeSet.addRefreshRate(refreshRate);
         }
      }

      VideoModeSet[] arr = new VideoModeSet[videoModeSets.size()];
      videoModeSets.toArray(arr);
      return arr;
   }

   public static VideoModeSet getVideoModeSet(VideoModeSet.VideoMode videoMode) {
      VideoModeSet[] videoModeSets = (VideoModeSet[])monitorToVideoModeSets.get(selectedMonitor);

      for (VideoModeSet set : videoModeSets) {
         if (set.width == videoMode.width && set.height == videoMode.height) {
            return set;
         }
      }

      return null;
   }

   public static void selectBestMonitor(Window window) {
      selectedMonitor = findBestMonitor(window).getMonitor();
      if (selectedMonitor == 0L) {
         selectedMonitor = GLFW.glfwGetPrimaryMonitor();
      }
   }

   public static void checkConfigVideoMode(Config config) {
      VideoModeSet.VideoMode videoMode = config.videoMode;
      if (videoMode.width <= 0 || videoMode.height <= 0 || videoMode.refreshRate <= 0) {
         videoMode = getFirstAvailable().getVideoMode();
         config.videoMode = videoMode;
         config.write();
      }
   }

   public static Monitor findBestMonitor(Window window) {
      long windowMonitor = GLFW.glfwGetWindowMonitor(window.getWindow());
      if (windowMonitor != 0L) {
         return (Monitor)monitors.get(windowMonitor);
      } else {
         int winMinX = window.getX();
         int winMaxX = winMinX + window.getScreenWidth();
         int winMinY = window.getY();
         int winMaxY = winMinY + window.getScreenHeight();
         int maxArea = -1;
         Monitor result = null;
         long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
         Initializer.LOGGER.debug("Selecting monitor - primary: {}, current monitors: {}", primaryMonitor, monitors);
         ObjectIterator var11 = monitors.values().iterator();

         while (var11.hasNext()) {
            Monitor monitor = (Monitor)var11.next();
            int monMinX = monitor.getX();
            int monMaxX = monMinX + monitor.getCurrentMode().getWidth();
            int monMinY = monitor.getY();
            int monMaxY = monMinY + monitor.getCurrentMode().getHeight();
            int minX = Math.clamp((long)winMinX, monMinX, monMaxX);
            int maxX = Math.clamp((long)winMaxX, monMinX, monMaxX);
            int minY = Math.clamp((long)winMinY, monMinY, monMaxY);
            int maxY = Math.clamp((long)winMaxY, monMinY, monMaxY);
            int sx = Math.max(0, maxX - minX);
            int sy = Math.max(0, maxY - minY);
            int area = sx * sy;
            if (area > maxArea) {
               result = monitor;
               maxArea = area;
            } else if (area == maxArea && primaryMonitor == monitor.getMonitor()) {
               Initializer.LOGGER.debug("Primary monitor {} is preferred to monitor {}", monitor, result);
               result = monitor;
            }
         }

         Initializer.LOGGER.debug("Selected monitor: {}", result);
         return result;
      }
   }

   public static Long2ObjectMap<Monitor> getMonitors() {
      return monitors;
   }
}
