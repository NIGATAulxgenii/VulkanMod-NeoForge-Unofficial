package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IndexType.class)
public class IndexTypeMixin {
   @Overwrite
   public static IndexType least(int number) {
      return IndexType.SHORT;
   }
}
