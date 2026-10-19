package net.vulkanmod.render.chunk.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;

public final class TintCacheCheck {
    public static void main(String[] args) {
        var cache = new TintCache();
        int[] sampledY = {0};
        ColorResolver height = (biome, x, z) -> (sampledY[0] & 255) << 8;
        cache.init(position -> { sampledY[0] = position.getY(); return null; }, 0, -1, 0, -1);
        require(cache.getColor(new BlockPos(-1, 1, -1), height) == 0x100, "first height layer");
        require(cache.getColor(new BlockPos(-1, 2, -1), height) == 0x200, "height layers share storage");
        require(cache.getColor(new BlockPos(-1, 1, -1), height) == 0x100, "height layer cache overwritten");
        ColorResolver other = (biome, x, z) -> 0x654321;
        require(cache.getColor(new BlockPos(-1, 1, -1), other) == 0x654321, "custom resolver not independent");
        cache.init(position -> null, 1, -1, 0, -1);
        ColorResolver boundary = (biome, x, z) -> x < 0 ? 0xff0000 : 0x0000ff;
        int color = cache.getColor(new BlockPos(-1, 1, -1), boundary) & 0xffffff;
        require(color == 0xaa0055, "same-biome spatial tint not blended: " + Integer.toHexString(color));
        cache.init(position -> { sampledY[0] = 9; return null; }, 0, -1, 0, -1);
        require(cache.getColor(new BlockPos(-1, 1, -1), height) == 0x900, "reused region cache not invalidated");
        System.out.println("Biome tint PASS: independent Y layers/resolvers, negative coordinates, spatial blending, invalidation");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
