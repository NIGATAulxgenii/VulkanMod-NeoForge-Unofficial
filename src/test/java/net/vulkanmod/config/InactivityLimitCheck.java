package net.vulkanmod.config;

public final class InactivityLimitCheck {
    public static void main(String[] args) {
        require(InactivityLimit.AFK.limit(260, false, 60_000_000_000L) == 260);
        require(InactivityLimit.AFK.limit(260, false, 60_000_000_001L) == 30);
        require(InactivityLimit.AFK.limit(260, false, 600_000_000_001L) == 10);
        require(InactivityLimit.AFK.limit(20, false, 120_000_000_000L) == 20);
        require(InactivityLimit.MINIMIZED.limit(260, false, Long.MAX_VALUE) == 260);
        require(InactivityLimit.MINIMIZED.limit(260, true, 0) == 10);
        InactivityLimit.recordInput();
        require(InactivityLimit.AFK.limit(260, false) == 260);
        System.out.println("Inactivity limit PASS: minute/ten-minute thresholds, minimized, configured cap, input reset");
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("Inactivity FPS limit mismatch");
    }
}
