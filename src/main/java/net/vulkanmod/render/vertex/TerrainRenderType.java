package net.vulkanmod.render.vertex;

import java.util.EnumSet;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ExtendedRenderType;

public enum TerrainRenderType {
   SOLID(0.0F),
   CUTOUT_MIPPED(0.5F),
   CUTOUT(0.1F),
   TRANSLUCENT(0.0F),
   TRIPWIRE(0.1F);

   public static final TerrainRenderType[] VALUES = values();
   public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);
   public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, CUTOUT, TRANSLUCENT);
   private static Function<TerrainRenderType, TerrainRenderType> remapper;
   public final float alphaCutout;

   private TerrainRenderType(float alphaCutout) {
      this.alphaCutout = alphaCutout;
   }

   public void setCutoutUniform() {
      net.vulkanmod.vulkan.VRenderSystem.alphaCutout = this.alphaCutout;
   }

   public static TerrainRenderType get(RenderType renderType) {
      return ((ExtendedRenderType)renderType).getTerrainRenderType();
   }

   public static TerrainRenderType get(String name) {
      return switch (name) {
         case "solid" -> SOLID;
         case "cutout" -> CUTOUT;
         case "cutout_mipped" -> CUTOUT_MIPPED;
         case "translucent" -> TRANSLUCENT;
         case "tripwire" -> TRIPWIRE;
         default -> null;
      };
   }

   public static RenderType getRenderType(TerrainRenderType renderType) {
      return switch (renderType) {
         case SOLID -> RenderType.solid();
         case CUTOUT_MIPPED -> RenderType.cutoutMipped();
         case CUTOUT -> RenderType.cutout();
         case TRANSLUCENT -> RenderType.translucent();
         case TRIPWIRE -> RenderType.tripwire();
      };
   }

   public static void updateMapping() {
      if (Initializer.CONFIG.uniqueOpaqueLayer) {
         remapper = renderType -> {
            return switch (renderType) {
               case SOLID, CUTOUT_MIPPED, CUTOUT -> CUTOUT_MIPPED;
               case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT;
            };
         };
      } else {
         remapper = renderType -> {
            return switch (renderType) {
               case SOLID, CUTOUT_MIPPED -> CUTOUT_MIPPED;
               case CUTOUT -> CUTOUT;
               case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT;
            };
         };
      }
   }

   public static TerrainRenderType getRemapped(TerrainRenderType renderType) {
      return remapper.apply(renderType);
   }

   static {
      SEMI_COMPACT_RENDER_TYPES.add(CUTOUT);
      SEMI_COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
      SEMI_COMPACT_RENDER_TYPES.add(TRANSLUCENT);
      COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
      COMPACT_RENDER_TYPES.add(TRANSLUCENT);
   }
}
