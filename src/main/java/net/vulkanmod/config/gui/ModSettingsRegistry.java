package net.vulkanmod.config.gui;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.option.Options;

public class ModSettingsRegistry {
   public static final ModSettingsRegistry INSTANCE = new ModSettingsRegistry();
   private final Set<ModSettingsEntry> modEntries = new ObjectArraySet();

   ModSettingsRegistry() {
      ModSettingsEntry vulkanModSettings = new ModSettingsEntry(
         Component.literal("VulkanMod").withStyle(ChatFormatting.DARK_RED),
         () -> ResourceLocation.fromNamespaceAndPath("vulkanmod", "vlogo_transparent.png"),
         Options::getOptionPages,
         () -> Initializer.CONFIG.write()
      );
      this.addModEntry(vulkanModSettings);
   }

   public void addModEntry(ModSettingsEntry entry) {
      this.modEntries.add(entry);
   }

   public Set<ModSettingsEntry> getModEntries() {
      return this.modEntries;
   }
}
