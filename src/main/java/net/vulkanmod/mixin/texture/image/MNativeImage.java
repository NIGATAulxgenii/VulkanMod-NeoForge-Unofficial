package net.vulkanmod.mixin.texture.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlUtil;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.Locale;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;

@Mixin(NativeImage.class)
public abstract class MNativeImage {

    @Shadow private long pixels;
    @Shadow private long size;

    @Shadow public abstract void close();


    @Shadow @Final private NativeImage.Format format;

    @Shadow public abstract int getWidth();

    @Shadow @Final private int width;
    @Shadow @Final private int height;

    @Shadow public abstract int getHeight();

    @Shadow public abstract void setPixelRGBA(int i, int j, int k);

    @Shadow public abstract int getPixelRGBA(int i, int j);

    @Shadow protected abstract void checkAllocated();

    private ByteBuffer buffer;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, CallbackInfo ci) {
        if(this.pixels != 0) {
            buffer = MemoryUtil.memByteBuffer(this.pixels, (int)this.size);
        }
    }

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, long pixels, CallbackInfo ci) {
        if(this.pixels != 0) {
            buffer = MemoryUtil.memByteBuffer(this.pixels, (int)this.size);
        }
    }

    /**
     * @author
     */
    @Overwrite
    private void _upload(int level, int xOffset, int yOffset, int unpackSkipPixels, int unpackSkipRows, int widthIn, int heightIn, boolean blur, boolean clamp, boolean mipmap, boolean autoClose) {
        RenderSystem.assertOnRenderThreadOrInit();

        ByteBuffer source = this.buffer;
        if (this.format == NativeImage.Format.RGB && VTextureSelector.getBoundTexture().formatSize == 4)
            source = GlUtil.RGBtoRGBA_buffer(source);
        try {
            VTextureSelector.uploadSubTexture(level, widthIn, heightIn, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, this.getWidth(), source);
        } finally {
            if (source != this.buffer) MemoryUtil.memFree(source);
            if (autoClose) this.close();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void downloadTexture(int level, boolean removeAlpha) {
        RenderSystem.assertOnRenderThread();

        VulkanImage image = VTextureSelector.getBoundTexture();
        if (this.format == NativeImage.Format.RGB && image.formatSize == 4) {
            ByteBuffer rgba = MemoryUtil.memAlloc(Math.multiplyExact(Math.multiplyExact(width, height), 4));
            try {
                ImageUtil.downloadTexture(image, level, MemoryUtil.memAddress(rgba));
                for (int pixel = 0; pixel < width * height; pixel++)
                    MemoryUtil.memCopy(MemoryUtil.memAddress(rgba) + pixel * 4L, this.pixels + pixel * 3L, 3);
            } finally { MemoryUtil.memFree(rgba); }
        } else ImageUtil.downloadTexture(image, level, this.pixels);

        if (removeAlpha && this.format.hasAlpha()) {
            if (this.format != NativeImage.Format.RGBA) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
            }

            for (long l = 0; l < this.width * this.height * 4L; l+=4) {
                int v =  MemoryUtil.memGetInt(this.pixels + l);

                if (image.format == VK_FORMAT_B8G8R8A8_UNORM || image.format == VK_FORMAT_B8G8R8A8_SRGB)
                    v = ColorUtil.BGRAtoRGBA(v);

                v = v | 255 << this.format.alphaOffset();
                MemoryUtil.memPutInt(this.pixels + l, v);
            }
        }

    }

}
