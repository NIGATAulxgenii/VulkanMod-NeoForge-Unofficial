package net.vulkanmod.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.vulkanmod.Initializer;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public final class UpdateChecker {
    public static final String RELEASES_URL = "https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/releases";
    private static volatile String updateVersion;

    public static void checkForUpdates(String currentVersion) {
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) URI.create("https://api.github.com/repos/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/releases?per_page=20").toURL().openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "VulkanMod-NeoForge/" + currentVersion);
                if (connection.getResponseCode() != 200) return;
                try (var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    updateVersion = findUpdate(JsonParser.parseReader(reader).getAsJsonArray(), currentVersion);
                }
                if (updateVersion != null) Initializer.LOGGER.info("NeoForge port update available: {}", updateVersion);
            } catch (Exception error) {
                Initializer.LOGGER.debug("Unable to check NeoForge port updates", error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    static String findUpdate(JsonArray releases, String currentVersion) {
        String newest = currentVersion;
        for (var entry : releases) {
            JsonObject release = entry.getAsJsonObject();
            if (release.get("draft").getAsBoolean()) continue;
            String tag = release.get("tag_name").getAsString().replaceFirst("^v", "");
            boolean compatible = false;
            for (var asset : release.getAsJsonArray("assets")) {
                String name = asset.getAsJsonObject().get("name").getAsString();
                if (name.startsWith("VulkanMod-1.21.1-NeoForge-") && name.endsWith(".jar") && !name.endsWith("-sources.jar")) {
                    compatible = true;
                    break;
                }
            }
            if (compatible && compareVersions(tag, newest) > 0) newest = tag;
        }
        return newest.equals(currentVersion) ? null : newest;
    }

    private static int compareVersions(String first, String second) {
        return new DefaultArtifactVersion(first.replaceFirst("-dev$", "-SNAPSHOT"))
                .compareTo(new DefaultArtifactVersion(second.replaceFirst("-dev$", "-SNAPSHOT")));
    }

    public static boolean isUpdateAvailable() {
        return updateVersion != null;
    }
}
