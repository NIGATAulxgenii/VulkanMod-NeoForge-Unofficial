package net.vulkanmod.smoke;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ShaderCompileCheck {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        List<Path> shaders;
        try (var files = Files.walk(root)) {
            shaders = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".vsh") || path.toString().endsWith(".fsh"))
                    .sorted().toList();
        }
        if (shaders.isEmpty()) throw new IllegalStateException("No shaders found in " + root);
        int failures = 0;
        for (Path shader : shaders) {
            String resource = "/assets/vulkanmod/shaders/" + root.relativize(shader).toString().replace('\\', '/');
            var kind = resource.endsWith(".vsh") ? SPIRVUtils.ShaderKind.VERTEX_SHADER : SPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            try (var spirv = SPIRVUtils.compileShaderResource(resource, kind)) {
                if (spirv.bytecode().remaining() < 20) throw new IllegalStateException("Invalid SPIR-V: " + resource);
                checkBindings(shader, spirv.bytecode());
            } catch (RuntimeException error) {
                failures++;
                System.err.println(error.getMessage());
            }
        }
        if (failures != 0) throw new IllegalStateException(failures + " of " + shaders.size() + " shaders failed compilation");
        System.out.println("Shader compilation PASS: " + shaders.size() + " shaders");
    }

    private static void checkBindings(Path shader, ByteBuffer bytecode) throws Exception {
        String name = shader.getFileName().toString();
        Path metadata = shader.resolveSibling(name.substring(0, name.lastIndexOf('.')) + ".json");
        if (!Files.exists(metadata)) return;
        Set<Integer> bindings = new HashSet<>();
        int nextBinding = 0;
        try (var reader = Files.newBufferedReader(metadata)) {
            var pipeline = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : List.of("UBOs", "ManualUBO")) {
                var uniforms = pipeline.getAsJsonArray(key);
                if (uniforms == null) continue;
                for (var uniform : uniforms) {
                    int binding = uniform.getAsJsonObject().get("binding").getAsInt();
                    bindings.add(binding);
                    nextBinding = Math.max(nextBinding, binding + 1);
                }
            }
            var samplers = pipeline.getAsJsonArray("samplers");
            if (samplers != null) {
                for (var sampler : samplers) bindings.add(nextBinding++);
            }
        }
        ByteBuffer words = bytecode.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        // SPIR-V OpDecorate (71), Binding (33): descriptors retained by shaderc.
        for (int offset = 20; offset < words.limit();) {
            int instruction = words.getInt(offset);
            int count = instruction >>> 16;
            if ((instruction & 0xffff) == 71 && words.getInt(offset + 8) == 33) {
                int binding = words.getInt(offset + 12);
                if (!bindings.contains(binding)) {
                    throw new IllegalStateException(shader + ": compiled binding " + binding + " absent from " + metadata);
                }
            }
            offset += count * 4;
        }
    }
}
