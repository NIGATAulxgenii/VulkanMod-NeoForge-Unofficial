package net.vulkanmod.mixin.render.clouds;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.render.sky.CloudRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererM {
    @Shadow private int ticks;
    @Shadow private ClientLevel level;
    @Unique private CloudRenderer vulkanmod$cloudRenderer;

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void renderClouds(PoseStack poses, Matrix4f view, Matrix4f projection, float partialTicks,
                              double x, double y, double z, CallbackInfo ci) {
        if (this.level == null) return;
        if (!this.level.effects().renderClouds(this.level, this.ticks, partialTicks, poses, x, y, z, view, projection)) {
            if (this.vulkanmod$cloudRenderer == null) this.vulkanmod$cloudRenderer = new CloudRenderer();
            this.vulkanmod$cloudRenderer.renderClouds(this.level, this.ticks, partialTicks, x, y, z, poses, view, projection);
        }
        ci.cancel();
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void resetClouds(CallbackInfo ci) {
        if (this.vulkanmod$cloudRenderer != null) this.vulkanmod$cloudRenderer.resetBuffer();
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void reloadClouds(ResourceManager resources, CallbackInfo ci) {
        if (this.vulkanmod$cloudRenderer != null) this.vulkanmod$cloudRenderer.loadTexture();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void changeLevel(ClientLevel level, CallbackInfo ci) {
        if (this.vulkanmod$cloudRenderer != null) this.vulkanmod$cloudRenderer.resetBuffer();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeClouds(CallbackInfo ci) {
        if (this.vulkanmod$cloudRenderer != null) this.vulkanmod$cloudRenderer.resetBuffer();
    }
}
