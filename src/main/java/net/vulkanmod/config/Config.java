package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import net.vulkanmod.config.video.VideoModeSet;

public class Config {
   public VideoModeSet.VideoMode videoMode = VideoModeSet.getDummy().getVideoMode();
   public int windowMode = 0;
   public int advCulling = 2;
   public boolean indirectDraw = true;
   public boolean uniqueOpaqueLayer = true;
   public boolean entityCulling = true;
   public int device = -1;
   public boolean useWayland = false;
   public int ambientOcclusion = 1;
   public int frameQueueSize = 2;
   public int builderThreads = 0;
   public boolean backFaceCulling = true;
   public boolean textureAnimations = true;
   private static Path CONFIG_PATH;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(new int[]{2}).create();

   public void write() {
      if (!Files.exists(CONFIG_PATH.getParent())) {
         try {
            Files.createDirectories(CONFIG_PATH);
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

      try {
         Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
      } catch (IOException var2) {
         var2.printStackTrace();
      }
   }

   public static Config load(Path path) {
      CONFIG_PATH = path;
      Config config;
      if (Files.exists(path)) {
         try (FileReader fileReader = new FileReader(path.toFile())) {
            config = (Config)GSON.fromJson(fileReader, Config.class);
         } catch (IOException var7) {
            throw new RuntimeException(var7.getMessage());
         }
      } else {
         config = new Config();
         config.write();
      }

      return config;
   }
}
