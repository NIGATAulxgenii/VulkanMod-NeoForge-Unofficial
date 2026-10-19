package net.vulkanmod.mixin.render.entity.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import net.vulkanmod.render.vertex.format.I32_SNorm;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class ModelPartM {
   @Shadow
   @Final
   private List<Cube> cubes;
   Vector3f normal = new Vector3f();

   @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
   private void injCompile(Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
      this.renderCubes(pose, vertexConsumer, light, overlay, color);
      ci.cancel();
   }

   @Unique
   public void renderCubes(Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color) {
      Matrix4f matrix4f = pose.pose();
      Matrix3f matrix3f = pose.normal();
      ExtendedVertexBuilder vertexBuilder = ExtendedVertexBuilder.of(vertexConsumer);
      if (vertexBuilder != null && vertexBuilder.canUseFastVertex()) {
         color = ColorUtil.RGBA.fromArgb32(color);

         for (Cube cube : this.cubes) {
            ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)cube;
            CubeModel cubeModel = cubeMixed.getCubeModel();
            Polygon[] polygons = cubeModel.getPolygons();
            cubeModel.transformVertices(matrix4f);

            for (Polygon polygon : polygons) {
               matrix3f.transform(this.normal.set(polygon.normal));
               this.normal.normalize();
               int packedNormal = I32_SNorm.packNormal(this.normal.x(), this.normal.y(), this.normal.z());
               Vertex[] vertices = polygon.vertices;

               for (Vertex vertex : vertices) {
                  Vector3f pos = vertex.pos;
                  vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlay, light, packedNormal);
               }
            }
         }
      } else {
         for (Cube cube : this.cubes) {
            ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)cube;
            CubeModel cubeModel = cubeMixed.getCubeModel();
            Polygon[] polygons = cubeModel.getPolygons();
            cubeModel.transformVertices(matrix4f);

            for (Polygon polygon : polygons) {
               matrix3f.transform(this.normal.set(polygon.normal));
               this.normal.normalize();
               Vertex[] vertices = polygon.vertices;

               for (Vertex vertex : vertices) {
                  Vector3f pos = vertex.pos;
                  vertexConsumer.addVertex(
                     pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlay, light, this.normal.x(), this.normal.y(), this.normal.z()
                  );
               }
            }
         }
      }
   }
}
