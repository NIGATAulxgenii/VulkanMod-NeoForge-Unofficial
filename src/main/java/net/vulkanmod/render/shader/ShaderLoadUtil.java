package net.vulkanmod.render.shader;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public abstract class ShaderLoadUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String RESOURCES_PATH = SPIRVUtils.class.getResource("/assets/vulkanmod").toExternalForm();
   public static final String SHADERS_PATH = "%s/shaders/".formatted(RESOURCES_PATH);
   public static final Set<String> REMAPPED_SHADERS = Sets.newHashSet(
      new String[]{"core/screenquad.vsh", "core/rendertype_entity_translucent_cull", "core/rendertype_item_entity_translucent_cull.vsh"}
   );

   public static String resolveShaderPath(String path) {
      return resolveShaderPath(SHADERS_PATH, path);
   }

   public static String resolveShaderPath(String shaderPath, String path) {
      return "%s%s".formatted(shaderPath, path);
   }

   public static String loadShader(String path, String shaderName) {
      return loadShader(path, shaderName, null);
   }

   public static String loadShader(String path, String shaderName, List<String> defines) {
      String source = getShaderSource(path, shaderName);
      if (defines != null && !defines.isEmpty()) {
         source = injectDefines(source, defines);
      }

      return source;
   }

   public static String getConfigFilePath(String path, String rendertype) {
      String basePath = "%s/shaders/%s".formatted(RESOURCES_PATH, path);
      String configPath = "%s/%s/%s.json".formatted(basePath, rendertype, rendertype);

      Path filePath;
      try {
         filePath = FileSystems.getDefault().getPath(configPath);
         if (!Files.exists(filePath)) {
            configPath = "%s/%s.json".formatted(basePath, rendertype);
            filePath = FileSystems.getDefault().getPath(configPath);
         }

         if (!Files.exists(filePath)) {
            return null;
         }
      } catch (Throwable var6) {
         throw new RuntimeException(var6);
      }

      return filePath.toString();
   }

   public static JsonObject getJsonConfig(String path, String rendertype) {
      if (rendertype.contains(String.valueOf(':'))) {
         return null;
      } else {
         String basePath = path;
         String configPath = "%s/%s/%s.json".formatted(path, rendertype, rendertype);

         try {
            InputStream stream = getInputStream(configPath);
            if (stream == null) {
               configPath = "%s/%s.json".formatted(basePath, rendertype);
               stream = getInputStream(configPath);
            }

            if (stream == null) {
               return null;
            } else {
               JsonElement jsonElement = JsonParser.parseReader(new BufferedReader(new InputStreamReader(stream)));
               stream.close();
               return (JsonObject)jsonElement;
            }
         } catch (Throwable var6) {
            throw new RuntimeException(var6);
         }
      }
   }

   public static String getShaderSource(String path, String shaderName) {
      String shaderFile = "%s/%s".formatted(path, shaderName);

      try {
         InputStream stream = getInputStream(shaderFile);
         if (stream == null) {
            return null;
         } else {
            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();
            return source;
         }
      } catch (Throwable var5) {
         throw new RuntimeException(var5);
      }
   }

   public static String getShaderSource(String path, String configName, String shaderName, SPIRVUtils.ShaderKind type) {
      String shaderExtension = switch (type) {
         case VERTEX_SHADER -> ".vsh";
         case FRAGMENT_SHADER -> ".fsh";
         case COMPUTE_SHADER -> ".comp";
         default -> throw new UnsupportedOperationException("shader type %s unsupported");
      };
      String basePath = path;
      String shaderPath = "/%s/%s".formatted(configName, configName);
      String shaderFile = "%s%s%s".formatted(path, shaderPath, shaderExtension);

      try {
         InputStream stream = getInputStream(shaderFile);
         if (stream == null) {
            shaderPath = "/%s".formatted(shaderName);
            shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
            stream = getInputStream(shaderFile);
         }

         if (stream == null) {
            shaderPath = "/%s/%s".formatted(configName, shaderName);
            shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
            stream = getInputStream(shaderFile);
         }

         if (stream == null) {
            shaderPath = "/%s/%s".formatted(shaderName, shaderName);
            shaderFile = "%s%s%s".formatted(basePath, shaderPath, shaderExtension);
            stream = getInputStream(shaderFile);
         }

         if (stream == null) {
            return null;
         } else {
            String source = IOUtils.toString(new BufferedReader(new InputStreamReader(stream)));
            stream.close();
            return source;
         }
      } catch (Throwable var10) {
         throw new RuntimeException(var10);
      }
   }

   public static String getFileName(String path) {
      int idx = path.lastIndexOf(47);
      return idx > -1 ? path.substring(idx + 1) : path;
   }

   public static String removeNameSpace(String path) {
      int idx = path.indexOf(58);
      return idx > -1 ? path.substring(idx + 1) : path;
   }

   public static String[] splitPath(String path) {
      int idx = path.lastIndexOf(47);
      return new String[]{path.substring(0, idx), path.substring(idx + 1)};
   }

   public static InputStream getInputStream(String path) {
      try {
         Path path1 = Paths.get(new URI(path));
         return !Files.exists(path1) ? null : Files.newInputStream(path1);
      } catch (IOException | URISyntaxException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static String injectDefines(String shaderSrc, List<String> defines) {
      StringBuilder stringBuilder = new StringBuilder();

      for (String define : defines) {
         stringBuilder.append("#define ").append(define).append('\n');
      }

      int i = shaderSrc.indexOf(10);
      String out = shaderSrc.substring(0, i + 1) + "\n" + stringBuilder + "\n" + shaderSrc.substring(i + 1);
      return out;
   }

   public static GlslPreprocessor createPreprocessor(ResourceProvider resourceProvider, ResourceLocation resourceLocation, String string2) {
      final ResourceLocation resourceLocation2 = resourceLocation.withPath(FileUtil::getFullResourcePath);
      final String string3 = FileUtil.getFullResourcePath(string2);
      return new GlslPreprocessor() {
         private final Set<ResourceLocation> importedLocations = new ObjectArraySet();

         public String applyImport(boolean bl, String string) {
            string = FileUtil.normalizeResourcePath((bl ? string3 : "shaders/include/") + string);

            ResourceLocation resourceLocationx;
            try {
               if (bl) {
                  resourceLocationx = resourceLocation2.withPath(FileUtil.normalizeResourcePath(resourceLocation2.getPath() + string));
               } else {
                  resourceLocationx = ResourceLocation.parse(string);
               }
            } catch (ResourceLocationException var15) {
               ShaderLoadUtil.LOGGER.error("Malformed GLSL import {}: {}", string, var15.getMessage());
               return "#error " + var15.getMessage();
            }

            if (!this.importedLocations.add(resourceLocationx)) {
               return null;
            } else {
               try {
                  Reader reader = resourceProvider.openAsReader(resourceLocationx);

                  String includeSrc;
                  try {
                     includeSrc = IOUtils.toString(reader);
                  } catch (Exception var12) {
                     throw var12;
                  } finally {
                     reader.close();
                  }

                  return includeSrc;
               } catch (IOException var14) {
                  ShaderLoadUtil.LOGGER.error("Could not open GLSL import {}: {}", resourceLocationx, var14.getMessage());
                  return "#error " + var14.getMessage();
               }
            }
         }
      };
   }
}
