package net.vulkanmod.config;

public enum InactivityLimit {
    MINIMIZED,
    AFK;

    private static long lastInput = System.nanoTime();

    public static void recordInput() {
        lastInput = System.nanoTime();
    }

    public int limit(int configuredLimit, boolean minimized) {
        return limit(configuredLimit, minimized, System.nanoTime() - lastInput);
    }

    public int limit(int configuredLimit, boolean minimized, long inactiveNanos) {
        if (minimized || this == AFK && inactiveNanos > 600_000_000_000L) return Math.min(configuredLimit, 10);
        if (this == AFK && inactiveNanos > 60_000_000_000L) return Math.min(configuredLimit, 30);
        return configuredLimit;
    }
}
