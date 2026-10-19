package net.vulkanmod.mixin.texture.update;

import java.util.Set;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.texture.SpriteUpdateUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureManager.class)
public abstract class MTextureManager {
   @Shadow
   @Final
   private Set<Tickable> tickableTextures;

   @Overwrite
   public void tick() {
      if (Initializer.CONFIG.textureAnimations) {
         for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
         }

         SpriteUpdateUtil.transitionLayouts();
      }
   }
}
