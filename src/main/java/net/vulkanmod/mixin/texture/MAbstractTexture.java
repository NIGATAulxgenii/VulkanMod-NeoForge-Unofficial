package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture {
    @Shadow protected boolean blur;
    @Shadow protected boolean mipmap;

    @Shadow protected int id;
    @Shadow public abstract int getId();

    /**
     * @author
     */
    @Overwrite
    public void bind() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::bindTexture);
        } else {
            this.bindTexture();
        }
    }

    /**
     * @author
     */
    @Overwrite
    public void setFilter(boolean blur, boolean mipmap) {
        this.blur = blur;
        this.mipmap = mipmap;
        GlTexture.getTexture(this.getId()).setFilter(blur, mipmap);
    }

    private void bindTexture() {
        GlTexture.bindTexture(this.getId());
    }
}
