package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.vulkanmod.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public class SpriteTickerMixin {
    @Shadow int subFrame;
    @Shadow int frame;
    @Shadow @Final SpriteContents.AnimatedTexture animationInfo;

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void skipIntermediateUpload(int x, int y, CallbackInfo ci) {
        if (SpriteUtil.shouldUpload()) return;
        if (++subFrame >= animationInfo.frames.get(frame).time) {
            frame = (frame + 1) % animationInfo.frames.size();
            subFrame = 0;
        }
        ci.cancel();
    }
}
