package net.vulkanmod.render.chunk.build.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.model.quad.ModelQuadView;

public interface LightPipeline {
   void calculate(ModelQuadView var1, BlockPos var2, QuadLightData var3, Direction var4, Direction var5, boolean var6);
}
