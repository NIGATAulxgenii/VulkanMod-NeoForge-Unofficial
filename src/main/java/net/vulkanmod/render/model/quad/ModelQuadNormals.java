package net.vulkanmod.render.model.quad;
import net.vulkanmod.render.vertex.format.I32_SNorm;
public final class ModelQuadNormals {
   public static int computePackedNormal(ModelQuadView q) {
      float x0 = q.getX(0);
      float y0 = q.getY(0);
      float z0 = q.getZ(0);
      float x1 = q.getX(1);
      float y1 = q.getY(1);
      float z1 = q.getZ(1);
      float x2 = q.getX(2);
      float y2 = q.getY(2);
      float z2 = q.getZ(2);
      float x3 = q.getX(3);
      float y3 = q.getY(3);
      float z3 = q.getZ(3);
      float dx0 = x2 - x0;
      float dy0 = y2 - y0;
      float dz0 = z2 - z0;
      float dx1 = x3 - x1;
      float dy1 = y3 - y1;
      float dz1 = z3 - z1;
      float normX = dy0 * dz1 - dz0 * dy1;
      float normY = dz0 * dx1 - dx0 * dz1;
      float normZ = dx0 * dy1 - dy0 * dx1;
      float l = (float)Math.sqrt(normX * normX + normY * normY + normZ * normZ);
      if (l != 0.0F) {
         normX /= l;
         normY /= l;
         normZ /= l;
      }

      return I32_SNorm.packNormal(normX, normY, normZ);
   }

}
