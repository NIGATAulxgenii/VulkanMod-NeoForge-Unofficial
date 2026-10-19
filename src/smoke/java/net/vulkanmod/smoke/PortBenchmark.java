package net.vulkanmod.smoke;

import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PortBenchmark {
    private final Minecraft mc;
    private final int originalLimit;
    private final boolean originalVsync;
    private final CloudStatus originalClouds;
    private final String originalInactivity;
    private final LongArrayList samples = new LongArrayList();
    private final List<Map<String, Object>> rounds = new ArrayList<>();
    private long roundStart, previous;
    private long idleSince, warmupStarted, measureStarted, compilationTime, collectionTime;
    private boolean warmed, done;

    public PortBenchmark(Minecraft mc) {
        this.mc = mc;
        mc.player.setPos(0.5, 202, 20.5);
        mc.player.setYRot(180);
        mc.player.setXRot(15);
        if (!Boolean.getBoolean("vulkanmod.smokeBaseline")) {
            originalInactivity = Initializer.CONFIG.inactivityLimit.name();
            Initializer.CONFIG.inactivityLimit = net.vulkanmod.config.InactivityLimit.MINIMIZED;
        } else originalInactivity = null;
        mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            level.setBlock(new net.minecraft.core.BlockPos(2, 201, 2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.setDayTime(6000);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, mc.getSingleplayerServer());
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, mc.getSingleplayerServer());
            for (var entity : level.getAllEntities()) if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            player.teleportTo(0.5, 202, 20.5);
        });
        originalLimit = mc.getWindow().getFramerateLimit();
        originalVsync = mc.options.enableVsync().get();
        originalClouds = mc.options.cloudStatus().get();
        mc.options.cloudStatus().set(CloudStatus.OFF);
        mc.getWindow().setFramerateLimit(260);
        mc.getWindow().updateVsync(false);
        roundStart = System.nanoTime();
        warmupStarted = roundStart;
    }

    public void frame() {
        if (done) return;
        long now = System.nanoTime();
        if (!warmed) {
            boolean idle = net.vulkanmod.render.chunk.WorldRenderer.getInstance().getTaskDispatcher().isIdle();
            if (!idle) idleSince = 0;
            else if (idleSince == 0) idleSince = now;
            if (now - roundStart >= 30_000_000_000L && idleSince != 0 && now - idleSince >= 2_000_000_000L) {
                warmed = true;
                roundStart = now;
                previous = now;
                measureStarted = now;
                compilationTime = compilationTime();
                collectionTime = collectionTime();
            }
            return;
        }
        samples.add(now - previous);
        previous = now;
        if (now - roundStart < 5_000_000_000L) return;
        long[] values = samples.toLongArray();
        Arrays.sort(values);
        long elapsed = now - roundStart;
        Map<String, Object> round = new LinkedHashMap<>();
        round.put("frames", values.length);
        round.put("fps", values.length * 1_000_000_000.0 / elapsed);
        round.put("median_ms", values[values.length / 2] / 1_000_000.0);
        round.put("p95_ms", values[(int) ((values.length - 1) * .95)] / 1_000_000.0);
        round.put("p99_ms", values[(int) ((values.length - 1) * .99)] / 1_000_000.0);
        long currentCompilation = compilationTime(), currentCollection = collectionTime();
        round.put("jit_compilation_ms", currentCompilation - compilationTime);
        round.put("gc_ms", currentCollection - collectionTime);
        round.put("chunk_workers_idle", net.vulkanmod.render.chunk.WorldRenderer.getInstance().getTaskDispatcher().isIdle());
        compilationTime = currentCompilation;
        collectionTime = currentCollection;
        rounds.add(round);
        samples.clear();
        roundStart = now;
        if (rounds.size() != 3) return;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("jar", Initializer.class.getProtectionDomain().getCodeSource().getLocation().toString());
        report.put("gpu", net.vulkanmod.vulkan.device.DeviceManager.deviceProperties.deviceNameString());
        report.put("width", mc.getWindow().getWidth());
        report.put("height", mc.getWindow().getHeight());
        report.put("scenario", "seed12345, synthetic platform, distance6, 960x540, clouds off, vsync off, unlimited, >=30s warmup and >=2s idle chunk workers, 3x5s wall-frame intervals");
        report.put("warmup_seconds", (measureStarted - warmupStarted) / 1_000_000_000.0);
        report.put("afk_throttling", "disabled; minimized protection retained (baseline has no AFK mode)");
        report.put("rounds", rounds);
        String label = System.getProperty("vulkanmod.benchmarkLabel", "current");
        if (!label.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid benchmark label");
        try {
            Files.writeString(mc.gameDirectory.toPath().resolve("verification/benchmark-" + label + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot save benchmark", error); }
        mc.getWindow().setFramerateLimit(originalLimit);
        mc.getWindow().updateVsync(originalVsync);
        mc.options.cloudStatus().set(originalClouds);
        if (originalInactivity != null) Initializer.CONFIG.inactivityLimit = net.vulkanmod.config.InactivityLimit.valueOf(originalInactivity);
        Initializer.LOGGER.info("SMOKE frame benchmark: {}", rounds);
        done = true;
    }

    public boolean isDone() { return done; }

    private static long compilationTime() {
        var compiler = java.lang.management.ManagementFactory.getCompilationMXBean();
        return compiler != null && compiler.isCompilationTimeMonitoringSupported() ? compiler.getTotalCompilationTime() : 0;
    }

    private static long collectionTime() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(collector -> Math.max(0, collector.getCollectionTime())).sum();
    }
}
