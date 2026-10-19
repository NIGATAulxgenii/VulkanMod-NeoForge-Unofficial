package net.vulkanmod.smoke;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;

final class LegacyShaderCheck {
    static void run() {
        var parser = new GLSLParser();
        parser.setVertexFormat(DefaultVertexFormat.POSITION);
        parser.parse(new Lexer("""
            #version 150
            uniform mat4 ProjMat;
            in vec3 Position;
            out vec3 sampleColor;
            void main() { sampleColor = Position; gl_Position = ProjMat * vec4(Position, 1.0); }
            """), GLSLParser.Stage.VERTEX);
        parser.parse(new Lexer("""
            #version 150
            uniform mat4 ProjMat;
            in vec3 sampleColor;
            out vec4 fragColor;
            void main() { fragColor = vec4(sampleColor * ProjMat[0][0], 1.0); }
            """), GLSLParser.Stage.FRAGMENT);
        var ubos = parser.createUBOs();
        if (ubos.length != 1 || ubos[0].getUniforms().size() != 1 || ubos[0].getSize() != 64)
            throw new AssertionError("Shared legacy uniform must have one matching UBO allocation");
        try (var vertex = SPIRVUtils.compileShader("legacy-check.vsh", parser.getOutput(GLSLParser.Stage.VERTEX), SPIRVUtils.ShaderKind.VERTEX_SHADER);
             var fragment = SPIRVUtils.compileShader("legacy-check.fsh", parser.getOutput(GLSLParser.Stage.FRAGMENT), SPIRVUtils.ShaderKind.FRAGMENT_SHADER)) {
            if (vertex.bytecode().remaining() < 20 || fragment.bytecode().remaining() < 20)
                throw new AssertionError("Invalid converted SPIR-V");
        }
        System.out.println("Legacy shader PASS: shared uniforms across stages, UBO allocation, converted SPIR-V");
    }
}
