package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class EntityRendererM<T extends Entity> {
   @Redirect(
      method = "shouldRender",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z")
   )
   private boolean isVisible(Frustum frustum, AABB aABB) {
      if (Initializer.CONFIG.entityCulling) {
         WorldRenderer worldRenderer = WorldRenderer.getInstance();
         Vec3 pos = aABB.getCenter();
         RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int)pos.x(), (int)pos.y(), (int)pos.z());
         return section == null ? frustum.isVisible(aABB) : worldRenderer.getLastFrame() == section.getLastFrame();
      } else {
         return frustum.isVisible(aABB);
      }
   }
}
