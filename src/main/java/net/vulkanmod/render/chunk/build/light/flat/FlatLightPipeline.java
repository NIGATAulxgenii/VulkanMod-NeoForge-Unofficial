package net.vulkanmod.render.chunk.build.light.flat;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.LightDataAccess;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.util.SimpleDirection;
import net.vulkanmod.render.model.quad.ModelQuadView;

public class FlatLightPipeline implements LightPipeline {
   private final LightDataAccess lightCache;

   public FlatLightPipeline(LightDataAccess lightCache) {
      this.lightCache = lightCache;
   }

   @Override
   public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
      int lightmap;
      if (cullFace != null) {
         lightmap = this.getLightmap(pos, cullFace);
      } else {
         int flags = quad.getFlags();
         if ((flags & 4) == 0 && ((flags & 2) == 0 || !LightDataAccess.unpackFC(this.lightCache.get(pos)))) {
            lightmap = LightDataAccess.getEmissiveLightmap(this.lightCache.get(pos));
         } else {
            lightmap = this.getLightmap(pos, lightFace);
         }
      }

      Arrays.fill(out.lm, lightmap);
      Arrays.fill(out.br, this.lightCache.getRegion().getShade(lightFace, shade));
   }

   private int getLightmap(BlockPos pos, Direction face) {
      int word = this.lightCache.get(pos);
      if (LightDataAccess.unpackEM(word)) {
         return 15728880;
      } else {
         int adjWord = this.lightCache.get(pos, SimpleDirection.of(face));
         return LightDataAccess.getLightmap(adjWord);
      }
   }
}
