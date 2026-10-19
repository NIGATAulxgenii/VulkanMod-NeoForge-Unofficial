package net.vulkanmod.render.chunk.cull;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.render.model.quad.QuadView;
import org.joml.Vector3f;

public enum QuadFacing {
    X_POS,
    Y_POS,
    Z_POS,
    X_NEG,
    Z_NEG,
    UNDEFINED,
    Y_NEG;

    public static final QuadFacing[] VALUES = QuadFacing.values();
    public static final int COUNT = VALUES.length;

    public static QuadFacing fromQuad(QuadView quad, Vector3f position) {
        for (int i = 0; i < 4; i++) {
            float x = position.x + quad.getX(i), y = position.y + quad.getY(i), z = position.z + quad.getZ(i);
            if (x < 0 || x > 16 || y < 0 || y > 16 || z < 0 || z > 16) return UNDEFINED;
        }
        float ax = quad.getX(1) - quad.getX(0), ay = quad.getY(1) - quad.getY(0), az = quad.getZ(1) - quad.getZ(0);
        float bx = quad.getX(2) - quad.getX(0), by = quad.getY(2) - quad.getY(0), bz = quad.getZ(2) - quad.getZ(0);
        float x = ay * bz - az * by, y = az * bx - ax * bz, z = ax * by - ay * bx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0) return UNDEFINED;
        QuadFacing facing = fromNormal(x / length, y / length, z / length);
        if (facing == X_POS || facing == X_NEG) {
            if (quad.getX(3) != quad.getX(0)) return UNDEFINED;
        } else if (facing == Y_POS || facing == Y_NEG) {
            if (quad.getY(3) != quad.getY(0)) return UNDEFINED;
        } else if (facing == Z_POS || facing == Z_NEG) {
            if (quad.getZ(3) != quad.getZ(0)) return UNDEFINED;
        }
        return facing;
    }

    public static QuadFacing fromDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> Y_NEG;
            case UP -> Y_POS;
            case NORTH -> Z_NEG;
            case SOUTH -> Z_POS;
            case WEST -> X_NEG;
            case EAST -> X_POS;
        };
    }

    public static QuadFacing fromNormal(int packedNormal) {
        final float x = VertexUtil.unpackN1(packedNormal);
        final float y = VertexUtil.unpackN2(packedNormal);
        final float z = VertexUtil.unpackN3(packedNormal);

        return fromNormal(x, y, z);
    }

    public static QuadFacing fromNormal(Vector3f normal) {
        return fromNormal(normal.x(), normal.y(), normal.z());
    }

    public static QuadFacing fromNormal(float x, float y, float z) {
        final float absX = Math.abs(x);
        final float absY = Math.abs(y);
        final float absZ = Math.abs(z);

        float sum = absX + absY + absZ;

        if (Mth.equal(sum, 1.0f)) {
            if (Mth.equal(absX, 1.0f)) {
                return x > 0.0f ? QuadFacing.X_POS : QuadFacing.X_NEG;
            }

            if (Mth.equal(absY, 1.0f)) {
                return y > 0.0f ? QuadFacing.Y_POS : QuadFacing.Y_NEG;
            }

            if (Mth.equal(absZ, 1.0f)) {
                return z > 0.0f ? QuadFacing.Z_POS : QuadFacing.Z_NEG;
            }
        }

        return QuadFacing.UNDEFINED;
    }
}
