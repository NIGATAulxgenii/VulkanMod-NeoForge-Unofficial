package net.vulkanmod.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class UpdateCheckerCheck {
    public static void main(String[] args) {
        var releases = new JsonArray();
        releases.add(release("v0.4.8-neoforge.4", false, "VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.4.jar"));
        releases.add(release("v9.0.0", false, "VulkanMod-1.21.11-Fabric-9.0.0.jar"));
        releases.add(release("v9.0.0", false, "VulkanMod-1.21.1-NeoForge-9.0.0-sources.jar"));
        releases.add(release("v9.0.0", true, "VulkanMod-1.21.1-NeoForge-9.0.0.jar"));
        require(UpdateChecker.findUpdate(releases, "0.4.8-neoforge.6-dev") == null, "offered older/incompatible/draft/source release");
        releases.add(release("v0.4.8-neoforge.6", false, "VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.6.jar"));
        require("0.4.8-neoforge.6".equals(UpdateChecker.findUpdate(releases, "0.4.8-neoforge.6-dev")), "final release did not replace dev");
        releases.add(release("v0.4.8-neoforge.10", false, "VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.10.jar"));
        require("0.4.8-neoforge.10".equals(UpdateChecker.findUpdate(releases, "0.4.8-neoforge.9")), "versions compared lexically");
        require(UpdateChecker.findUpdate(releases, "0.4.8-neoforge.10") == null, "offered current version");
        System.out.println("Port update checks PASS: versions, dev/final, loader/game filtering, source assets, drafts");
    }

    private static JsonObject release(String tag, boolean draft, String assetName) {
        var release = new JsonObject();
        release.addProperty("tag_name", tag);
        release.addProperty("draft", draft);
        var assets = new JsonArray();
        var asset = new JsonObject();
        asset.addProperty("name", assetName);
        assets.add(asset);
        release.add("assets", assets);
        return release;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
