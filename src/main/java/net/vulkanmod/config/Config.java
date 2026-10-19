package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {

    public int frameQueueSize = 2;
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public boolean windowedFullscreen = false;
    public int windowMode = -1;
    public int cloudDistance = 128;
    public InactivityLimit inactivityLimit = InactivityLimit.AFK;

    public int advCulling = 2;
    public boolean indirectDraw = false;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public int device = -1;

    public int ambientOcclusion = 1;
    public int builderThreads = 0;
    public boolean backFaceCulling = true;
    public boolean textureAnimations = true;

    public void write() {
        if (this.windowMode == -1) this.windowMode = this.windowedFullscreen ? 1 : 0;
        this.windowedFullscreen = this.windowMode == 1;

        if(!Files.exists(CONFIG_PATH.getParent())) {
            try {
                Files.createDirectories(CONFIG_PATH.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path CONFIG_PATH;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static Config load(Path path) {
        Config config;
        Config.CONFIG_PATH = path;

        if (Files.exists(path)) {
            try (FileReader fileReader = new FileReader(path.toFile())) {
                config = GSON.fromJson(fileReader, Config.class);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
        else {
            config = null;
        }

        if (config != null) {
            if (config.windowMode == -1) config.windowMode = config.windowedFullscreen ? 1 : 0;
            if (config.windowMode < 0 || config.windowMode > 2) config.windowMode = 0;
            config.cloudDistance = Math.clamp(config.cloudDistance, 2, 128);
            if (config.inactivityLimit == null) config.inactivityLimit = InactivityLimit.AFK;
        }
        return config;
    }
}
