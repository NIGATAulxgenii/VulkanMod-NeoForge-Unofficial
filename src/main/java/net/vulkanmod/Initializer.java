package net.vulkanmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.VOptionScreen;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(value = "vulkanmod", dist = Dist.CLIENT)
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION = "0.4.8-neoforge.1";
	public static Config CONFIG;

	public Initializer(ModContainer container) {
		VERSION = container.getModInfo().getVersion().toString();
		UpdateChecker.checkForUpdates(VERSION);
		LOGGER.info("== VulkanMod {} / NeoForge ==", VERSION);
		container.registerExtensionPoint(IConfigScreenFactory.class,
				(IConfigScreenFactory) (mod, parent) -> new VOptionScreen(Component.translatable("options.video"), parent));
	}

	// NeoForge constructs mods after Window. GLFW and settings must be ready earlier,
	// on the render thread, before Minecraft creates its window and renderer.
	public static synchronized void initWindow() {
		if (CONFIG != null) return;

		LOGGER.info("== VulkanMod ==");

		Platform.init();
		VideoModeManager.init();

		var configPath = FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json");

		CONFIG = loadConfig(configPath);
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);

		if(config == null) {
			config = new Config();
			config.write();
		}

		return config;
	}

	public static String getVersion() {
		return VERSION;
	}
}
