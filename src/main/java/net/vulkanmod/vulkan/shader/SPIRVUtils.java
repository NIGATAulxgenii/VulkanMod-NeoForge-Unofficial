package net.vulkanmod.vulkan.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.ShadercIncludeResolveI;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class SPIRVUtils {
    private static final boolean DEBUG = false;
    private static final boolean OPTIMIZATIONS = true;

    private static long compiler;
    private static long options;

    //The dedicated Includer and Releaser Inner Classes used to Initialise #include Support for ShaderC
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();
    private static final long pUserData = 0;

    private static ObjectArrayList<String> includePaths;

    private static float time = 0.0f;

    static {
        initCompiler();
    }

    private static void initCompiler() {
        compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

        if(OPTIMIZATIONS)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        if(DEBUG)
            shaderc_compile_options_set_generate_debug_info(options);

        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2);
        shaderc_compile_options_set_include_callbacks(options, SHADER_INCLUDER, SHADER_RELEASER, pUserData);

        includePaths = new ObjectArrayList<>();
        addIncludePath("/assets/vulkanmod/shaders/include/");
    }

    public static void addIncludePath(String path) {
        includePaths.add(path.endsWith("/") ? path : path + "/");
    }

    public static SPIRV compileShaderResource(String path, ShaderKind shaderKind) {
        try (InputStream stream = SPIRVUtils.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Shader resource missing: " + path);
            return compileShader(path, new String(stream.readAllBytes(), StandardCharsets.UTF_8), shaderKind);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read shader " + path, e);
        }
    }

    public static synchronized SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
        long startTime = System.nanoTime();

        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            String error = shaderc_result_get_error_message(result);
            shaderc_result_release(result);
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + error);
        }

        time += (System.nanoTime() - startTime) / 1000000.0f;

        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader),
        COMPUTE_SHADER(shaderc_glsl_compute_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    private static class ShaderIncluder implements ShadercIncludeResolveI {

        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            var requesting = memASCII(requesting_source);
            var requested = memASCII(requested_source);

            try {
                for (String includePath : includePaths) {
                    try (InputStream stream = SPIRVUtils.class.getResourceAsStream(includePath + requested)) {
                        if (stream != null) {
                            return includeResult(includePath + requested, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            } catch (IOException e) {
                return includeResult("", "Cannot read include " + requested + ": " + e.getMessage());
            }
            return includeResult("", requesting + ": include missing: " + requested);
        }

        private long includeResult(String name, String source) {
            // shaderc retains these pointers after the callback returns. A popped
            // MemoryStack frame is invalid here; the release callback owns them.
            return ShadercIncludeResult.calloc()
                    .source_name(MemoryUtil.memUTF8(name, false))
                    .content(MemoryUtil.memUTF8(source, false))
                    .user_data(NULL).address();
        }
    }

    private static class ShaderReleaser implements ShadercIncludeResultReleaseI {

        @Override
        public void invoke(long user_data, long include_result) {
            ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
            MemoryUtil.memFree(result.source_name());
            MemoryUtil.memFree(result.content());
            result.free();
        }
    }

    public static final class SPIRV implements NativeResource {

        private long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
            if (handle != NULL) {
                shaderc_result_release(handle);
                handle = NULL;
            }
            bytecode = null; // Help the GC
        }
    }

}
