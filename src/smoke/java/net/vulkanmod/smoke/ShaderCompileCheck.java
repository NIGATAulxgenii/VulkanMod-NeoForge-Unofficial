package net.vulkanmod.smoke;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import com.google.gson.JsonParser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

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
        Map<Integer, List<Integer>> layouts = new HashMap<>();
        int nextBinding = 0;
        try (var reader = Files.newBufferedReader(metadata)) {
            var pipeline = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : List.of("UBOs", "ManualUBO")) {
                var uniforms = pipeline.getAsJsonArray(key);
                if (uniforms == null) continue;
                for (var uniform : uniforms) {
                    int binding = uniform.getAsJsonObject().get("binding").getAsInt();
                    bindings.add(binding);
                    var fields = uniform.getAsJsonObject().getAsJsonArray("fields");
                    if (fields != null) {
                        var builder = new AlignedStruct.Builder();
                        for (var field : fields) {
                            var node = field.getAsJsonObject();
                            builder.addUniformInfo(node.get("type").getAsString(), node.get("name").getAsString(), node.get("count").getAsInt());
                        }
                        layouts.put(binding, builder.buildUBO(binding, 0).getUniforms().stream().map(field -> field.getOffset() * 4).toList());
                    }
                    nextBinding = Math.max(nextBinding, binding + 1);
                }
            }
            var samplers = pipeline.getAsJsonArray("samplers");
            if (samplers != null) {
                for (var sampler : samplers) bindings.add(nextBinding++);
            }
        }
        ByteBuffer words = bytecode.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        Map<Integer, Integer> pointers = new HashMap<>(), variables = new HashMap<>(), variableBindings = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> memberOffsets = new HashMap<>();
        // SPIR-V OpDecorate (71), Binding (33): descriptors retained by shaderc.
        for (int offset = 20; offset < words.limit();) {
            int instruction = words.getInt(offset);
            int count = instruction >>> 16;
            int opcode = instruction & 0xffff;
            if (opcode == 32) pointers.put(words.getInt(offset + 4), words.getInt(offset + 12));
            if (opcode == 59) variables.put(words.getInt(offset + 8), words.getInt(offset + 4));
            if (opcode == 72 && count >= 5 && words.getInt(offset + 12) == 35)
                memberOffsets.computeIfAbsent(words.getInt(offset + 4), ignored -> new HashMap<>())
                        .put(words.getInt(offset + 8), words.getInt(offset + 16));
            if ((instruction & 0xffff) == 71 && words.getInt(offset + 8) == 33) {
                int binding = words.getInt(offset + 12);
                variableBindings.put(words.getInt(offset + 4), binding);
                if (!bindings.contains(binding)) {
                    throw new IllegalStateException(shader + ": compiled binding " + binding + " absent from " + metadata);
                }
            }
            offset += count * 4;
        }
        for (var descriptor : variableBindings.entrySet()) {
            List<Integer> expected = layouts.get(descriptor.getValue());
            var actual = memberOffsets.get(pointers.get(variables.get(descriptor.getKey())));
            if (expected == null || actual == null) continue;
            for (var member : actual.entrySet()) {
                if (member.getKey() >= expected.size() || !member.getValue().equals(expected.get(member.getKey())))
                    throw new IllegalStateException(shader + ": UBO binding " + descriptor.getValue() + " member " + member.getKey()
                            + " compiled offset " + member.getValue() + " differs from JSON layout " + expected);
            }
        }
    }
}
