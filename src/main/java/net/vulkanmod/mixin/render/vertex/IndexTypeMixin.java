package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexFormat.IndexType.class)
public class IndexTypeMixin {

    /**
     * @author
     */
    @Overwrite
    public static VertexFormat.IndexType least(int number) {
        return number <= 65536 ? VertexFormat.IndexType.SHORT : VertexFormat.IndexType.INT;
    }
}