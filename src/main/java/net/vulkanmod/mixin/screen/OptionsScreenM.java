package net.vulkanmod.mixin.screen;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.VOptionScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public class OptionsScreenM extends Screen {

    @Shadow @Final private Screen lastScreen;

    @Shadow @Final private Options options;

    protected OptionsScreenM(Component title) {
        super(title);
    }

    @Inject(method = "openScreenButton", at = @At("HEAD"), cancellable = true)
    private void injectVideoOptionScreen(Component caption, Supplier<Screen> factory, CallbackInfoReturnable<Button> cir) {
        if (caption.equals(Component.translatable("options.video"))) {
            cir.setReturnValue(Button.builder(caption,
                    button -> this.minecraft.setScreen(new VOptionScreen(caption, this))).build());
        }
    }
}
