package net.vulkanmod.render.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.shader.PipelineManager;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;

public class CloudRenderer {
   private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
   private static final int DIR_NEG_Y_BIT = 1;
   private static final int DIR_POS_Y_BIT = 2;
   private static final int DIR_NEG_X_BIT = 4;
   private static final int DIR_POS_X_BIT = 8;
   private static final int DIR_NEG_Z_BIT = 16;
   private static final int DIR_POS_Z_BIT = 32;
   private static final byte Y_BELOW_CLOUDS = 0;
   private static final byte Y_ABOVE_CLOUDS = 1;
   private static final byte Y_INSIDE_CLOUDS = 2;
   private static final int CELL_WIDTH = 12;
   private static final int CELL_HEIGHT = 4;
   private CloudRenderer.CloudGrid cloudGrid;
   private int prevCloudX;
   private int prevCloudZ;
   private byte prevCloudY;
   private CloudStatus prevCloudsType;
   private boolean generateClouds;
   private VBO cloudBuffer;

   public CloudRenderer() {
      this.loadTexture();
   }

   public void loadTexture() {
      this.cloudGrid = createCloudGrid(TEXTURE_LOCATION);
   }

   public void renderClouds(
      ClientLevel level, PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float ticks, float partialTicks, double camX, double camY, double camZ
   ) {
      float cloudHeight = level.effects().getCloudHeight();
      if (!Float.isNaN(cloudHeight)) {
         Minecraft minecraft = Minecraft.getInstance();
         double timeOffset = (ticks + partialTicks) * 0.03F;
         double centerX = camX + timeOffset;
         double centerZ = camZ + 3.96F;
         double centerY = cloudHeight - (float)camY + 0.33F;
         int centerCellX = (int)Math.floor(centerX / 12.0);
         int centerCellZ = (int)Math.floor(centerZ / 12.0);
         byte yState;
         if (centerY < -4.0) {
            yState = 0;
         } else if (centerY > 0.0) {
            yState = 1;
         } else {
            yState = 2;
         }

         if (centerCellX != this.prevCloudX
            || centerCellZ != this.prevCloudZ
            || minecraft.options.getCloudsType() != this.prevCloudsType
            || this.prevCloudY != yState
            || this.cloudBuffer == null) {
            this.prevCloudX = centerCellX;
            this.prevCloudZ = centerCellZ;
            this.prevCloudsType = minecraft.options.getCloudsType();
            this.prevCloudY = yState;
            this.generateClouds = true;
         }

         if (this.generateClouds) {
            this.generateClouds = false;
            if (this.cloudBuffer != null) {
               this.cloudBuffer.close();
            }

            this.resetBuffer();
            MeshData cloudsMesh = this.buildClouds(Tesselator.getInstance(), centerCellX, centerCellZ, centerY);
            if (cloudsMesh == null) {
               return;
            }

            this.cloudBuffer = new VBO(true);
            this.cloudBuffer.upload(cloudsMesh);
         }

         if (this.cloudBuffer != null) {
            FogRenderer.levelFogColor();
            float xTranslation = (float)(centerX - centerCellX * 12);
            float yTranslation = (float)centerY;
            float zTranslation = (float)(centerZ - centerCellZ * 12);
            poseStack.pushPose();
            poseStack.mulPose(modelView);
            poseStack.translate(-xTranslation, yTranslation, -zTranslation);
            VRenderSystem.setModelOffset(-xTranslation, 0.0F, -zTranslation);
            Vec3 cloudColor = level.getCloudColor(partialTicks);
            RenderSystem.setShaderColor((float)cloudColor.x, (float)cloudColor.y, (float)cloudColor.z, 0.8F);
            GraphicsPipeline pipeline = PipelineManager.getCloudsPipeline();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            boolean fastClouds = this.prevCloudsType == CloudStatus.FAST;
            boolean insideClouds = yState == 2;
            boolean disableCull = insideClouds || fastClouds && centerY <= 0.0;
            if (disableCull) {
               RenderSystem.disableCull();
            }

            if (!fastClouds) {
               RenderSystem.colorMask(false, false, false, false);
               this.cloudBuffer.drawWithShader(poseStack.last().pose(), projection, pipeline);
               RenderSystem.colorMask(true, true, true, true);
            }

            this.cloudBuffer.drawWithShader(poseStack.last().pose(), projection, pipeline);
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            VRenderSystem.setModelOffset(0.0F, 0.0F, 0.0F);
            poseStack.popPose();
         }
      }
   }

   public void resetBuffer() {
      if (this.cloudBuffer != null) {
         this.cloudBuffer.close();
         this.cloudBuffer = null;
      }
   }

   private MeshData buildClouds(Tesselator tesselator, int centerCellX, int centerCellZ, double cloudY) {
      float upFaceBrightness = 1.0F;
      float xDirBrightness = 0.9F;
      float downFaceBrightness = 0.7F;
      float zDirBrightness = 0.8F;
      BufferBuilder bufferBuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      int renderDistance = 32;
      boolean insideClouds = this.prevCloudY == 2;
      if (this.prevCloudsType == CloudStatus.FANCY) {
         for (int cellX = -renderDistance; cellX < renderDistance; cellX++) {
            for (int cellZ = -renderDistance; cellZ < renderDistance; cellZ++) {
               int cellIdx = this.cloudGrid.getWrappedIdx(centerCellX + cellX, centerCellZ + cellZ);
               byte renderFaces = this.cloudGrid.renderFaces[cellIdx];
               int baseColor = this.cloudGrid.pixels[cellIdx];
               float x = cellX * 12;
               float z = cellZ * 12;
               if ((renderFaces & 2) != 0 && cloudY <= 0.0) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 1.0F);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 12.0F, color);
               }

               if ((renderFaces & 1) != 0 && cloudY >= -4.0) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 0.7F);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 12.0F, color);
               }

               if ((renderFaces & 8) != 0 && (x < 1.0F || insideClouds)) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 0.9F);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 0.0F, color);
               }

               if ((renderFaces & 4) != 0 && (x > -1.0F || insideClouds)) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 0.9F);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 12.0F, color);
               }

               if ((renderFaces & 32) != 0 && (z < 1.0F || insideClouds)) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 0.8F);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 12.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 12.0F, color);
               }

               if ((renderFaces & 16) != 0 && (z > -1.0F || insideClouds)) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColor, 0.8F);
                  putVertex(bufferBuilder, x + 12.0F, 4.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 12.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 0.0F, z + 0.0F, color);
                  putVertex(bufferBuilder, x + 0.0F, 4.0F, z + 0.0F, color);
               }
            }
         }
      } else {
         for (int cellX = -renderDistance; cellX < renderDistance; cellX++) {
            for (int cellZ = -renderDistance; cellZ < renderDistance; cellZ++) {
               int cellIdxx = this.cloudGrid.getWrappedIdx(centerCellX + cellX, centerCellZ + cellZ);
               byte renderFacesx = this.cloudGrid.renderFaces[cellIdxx];
               int baseColorx = this.cloudGrid.pixels[cellIdxx];
               float xx = cellX * 12;
               float zx = cellZ * 12;
               if ((renderFacesx & 1) != 0) {
                  int color = ColorUtil.ARGB.multiplyRGB(baseColorx, 1.0F);
                  putVertex(bufferBuilder, xx + 0.0F, 0.0F, zx + 12.0F, color);
                  putVertex(bufferBuilder, xx + 0.0F, 0.0F, zx + 0.0F, color);
                  putVertex(bufferBuilder, xx + 12.0F, 0.0F, zx + 0.0F, color);
                  putVertex(bufferBuilder, xx + 12.0F, 0.0F, zx + 12.0F, color);
               }
            }
         }
      }

      return bufferBuilder.build();
   }

   private static void putVertex(BufferBuilder bufferBuilder, float x, float y, float z, int color) {
      bufferBuilder.addVertex(x, y, z).setColor(color);
   }

   private static CloudRenderer.CloudGrid createCloudGrid(ResourceLocation textureLocation) {
      ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

      try {
         Resource resource = resourceManager.getResourceOrThrow(textureLocation);

         CloudRenderer.CloudGrid var8;
         try (InputStream inputStream = resource.open()) {
            NativeImage image = NativeImage.read(inputStream);
            int width = image.getWidth();
            int height = image.getHeight();
            Validate.isTrue(width == height, "Image width and height must be the same", new Object[0]);
            int[] pixels = image.getPixelsRGBA();
            var8 = new CloudRenderer.CloudGrid(pixels, width);
         }

         return var8;
      } catch (IOException var11) {
         throw new RuntimeException(var11);
      }
   }

   static class CloudGrid {
      final int width;
      final int[] pixels;
      final byte[] renderFaces;

      CloudGrid(int[] pixels, int width) {
         this.pixels = pixels;
         this.width = width;
         this.renderFaces = this.computeRenderFaces();
      }

      byte[] computeRenderFaces() {
         byte[] renderFaces = new byte[this.pixels.length];

         for (int z = 0; z < this.width; z++) {
            for (int x = 0; x < this.width; x++) {
               int idx = this.getIdx(x, z);
               int pixel = this.pixels[idx];
               if (hasColor(pixel)) {
                  byte faces = 3;
                  int adjPixel = this.getTexelWrapped(x - 1, z);
                  if (pixel != adjPixel) {
                     faces = (byte)(faces | 4);
                  }

                  adjPixel = this.getTexelWrapped(x + 1, z);
                  if (pixel != adjPixel) {
                     faces = (byte)(faces | 8);
                  }

                  adjPixel = this.getTexelWrapped(x, z - 1);
                  if (pixel != adjPixel) {
                     faces = (byte)(faces | 16);
                  }

                  adjPixel = this.getTexelWrapped(x, z + 1);
                  if (pixel != adjPixel) {
                     faces = (byte)(faces | 32);
                  }

                  renderFaces[idx] = faces;
               }
            }
         }

         return renderFaces;
      }

      int getTexelWrapped(int x, int z) {
         if (x < 0) {
            x = this.width - 1;
         }

         if (x > this.width - 1) {
            x = 0;
         }

         if (z < 0) {
            z = this.width - 1;
         }

         if (z > this.width - 1) {
            z = 0;
         }

         return this.pixels[this.getIdx(x, z)];
      }

      int getWrappedIdx(int x, int z) {
         x = Math.floorMod(x, this.width);
         z = Math.floorMod(z, this.width);
         return this.getIdx(x, z);
      }

      int getIdx(int x, int z) {
         return z * this.width + x;
      }

      private static boolean hasColor(int pixel) {
         return (pixel >> 24 & 0xFF) > 1;
      }
   }
}
