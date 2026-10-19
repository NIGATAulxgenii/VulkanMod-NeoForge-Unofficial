package net.vulkanmod.config.video;

public enum WindowMode {
    WINDOWED,
    WINDOWED_FULLSCREEN,
    EXCLUSIVE_FULLSCREEN;

    public String translationKey() {
        return switch (this) {
            case WINDOWED -> "vulkanmod.options.windowMode.windowed";
            case WINDOWED_FULLSCREEN -> "vulkanmod.options.windowMode.windowedFullscreen";
            case EXCLUSIVE_FULLSCREEN -> "options.fullscreen";
        };
    }
}
