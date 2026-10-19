package net.vulkanmod.config.api;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.vulkanmod.config.gui.ModSettingsRegistry;

public final class RegisterVulkanSettingsEvent extends Event implements IModBusEvent {
    private final ModSettingsRegistry registry;

    public RegisterVulkanSettingsEvent(ModSettingsRegistry registry) {
        this.registry = registry;
    }

    public void register(VkModSettingsFactory factory) {
        registry.addModEntry(factory.build(new VkModSettingsEntryBuilder()));
    }
}
