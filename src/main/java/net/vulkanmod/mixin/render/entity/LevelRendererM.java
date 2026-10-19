package net.vulkanmod.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererM {
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private Object2ReferenceOpenHashMap<MultiBufferSource, Map<Class<? extends Entity>, ObjectArrayList<Entity>>> bufferSourceMap = new Object2ReferenceOpenHashMap();
   @Unique
   boolean managed;

   @Inject(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
         shift = Shift.AFTER
      )
   )
   private void clearMap(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      ObjectIterator var9 = this.bufferSourceMap.keySet().iterator();

      while (var9.hasNext()) {
         MultiBufferSource bufferSource = (MultiBufferSource)var9.next();
         Map<Class<? extends Entity>, ObjectArrayList<Entity>> entityMap = (Map<Class<? extends Entity>, ObjectArrayList<Entity>>)this.bufferSourceMap
            .get(bufferSource);
         entityMap.clear();
      }

      this.managed = true;
   }

   @Overwrite
   private void renderEntity(Entity entity, double d, double e, double f, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource) {
      if (Initializer.CONFIG.entityCulling && this.managed) {
         Class<? extends Entity> entityClass = (Class<? extends Entity>)entity.getClass();
         Map<Class<? extends Entity>, ObjectArrayList<Entity>> entityMap = (Map<Class<? extends Entity>, ObjectArrayList<Entity>>)this.bufferSourceMap
            .computeIfAbsent(multiBufferSource, bufferSource -> new Object2ReferenceOpenHashMap());
         ObjectArrayList<Entity> list = entityMap.computeIfAbsent(entityClass, kx -> new ObjectArrayList());
         list.add(entity);
      } else {
         double h = Mth.lerp(partialTicks, entity.xOld, entity.getX());
         double i = Mth.lerp(partialTicks, entity.yOld, entity.getY());
         double j = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
         float k = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
         this.entityRenderDispatcher
            .render(entity, h - d, i - e, j - f, k, partialTicks, poseStack, multiBufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks));
      }
   }

   @Inject(
      method = "renderLevel",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V", shift = Shift.AFTER, ordinal = 0)
   )
   private void renderEntities(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      if (Initializer.CONFIG.entityCulling) {
         Vector3d cameraPos = WorldRenderer.getCameraPos();
         TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
         PoseStack poseStack = new PoseStack();
         ObjectIterator var12 = this.bufferSourceMap.keySet().iterator();

         while (var12.hasNext()) {
            MultiBufferSource bufferSource = (MultiBufferSource)var12.next();
            Map<Class<? extends Entity>, ObjectArrayList<Entity>> entityMap = (Map<Class<? extends Entity>, ObjectArrayList<Entity>>)this.bufferSourceMap
               .get(bufferSource);

            for (ObjectArrayList<Entity> list : entityMap.values()) {
               ObjectListIterator var17 = list.iterator();

               while (var17.hasNext()) {
                  Entity entity = (Entity)var17.next();
                  float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
                  double h = Mth.lerp(partialTicks, entity.xOld, entity.getX());
                  double i = Mth.lerp(partialTicks, entity.yOld, entity.getY());
                  double j = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
                  float k = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
                  this.entityRenderDispatcher
                     .render(
                        entity,
                        h - cameraPos.x,
                        i - cameraPos.y,
                        j - cameraPos.z,
                        k,
                        partialTicks,
                        poseStack,
                        bufferSource,
                        this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks)
                     );
               }
            }
         }

         this.managed = false;
      }
   }
}
