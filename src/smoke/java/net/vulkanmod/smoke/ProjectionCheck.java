package net.vulkanmod.smoke;

import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class ProjectionCheck {
    static void run() {
        var source = new Matrix4f().perspective((float) Math.PI / 2, 1, 0.1f, 100);
        var saved = new Matrix4f(source);
        var converted = VRenderSystem.toVulkanProjection(source);
        var near = new Vector4f(0, 0, -0.1f, 1).mul(converted);
        var far = new Vector4f(0, 0, -100, 1).mul(converted);
        if (Math.abs(near.z / near.w) > 0.0001f || Math.abs(far.z / far.w - 1) > 0.0001f || !source.equals(saved))
            throw new AssertionError("Vulkan depth range or source-matrix preservation failed");
        System.out.println("Projection PASS: near/far clip planes and immutable Minecraft matrix");
    }
}
