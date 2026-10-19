package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.task.TaskDispatcher;
import net.vulkanmod.render.chunk.graph.SectionGraph;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.profiling.BuildTimeProfiler;
import net.vulkanmod.render.profiling.Profiler;
import net.vulkanmod.render.shader.PipelineManager;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.IndirectBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class WorldRenderer {
   private static WorldRenderer INSTANCE;
   private final Minecraft minecraft;
   private ClientLevel level;
   private int renderDistance;
   private final RenderBuffers renderBuffers;
   private final Vector3d cameraPos = new Vector3d();
   private float partialTick;
   private int lastCameraSectionX;
   private int lastCameraSectionY;
   private int lastCameraSectionZ;
   private float lastCameraX;
   private float lastCameraY;
   private float lastCameraZ;
   private float lastCamRotX;
   private float lastCamRotY;
   private SectionGrid sectionGrid;
   private SectionGraph sectionGraph;
   private boolean graphNeedsUpdate;
   private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
   private final TaskDispatcher taskDispatcher;
   private double xTransparentOld;
   private double yTransparentOld;
   private double zTransparentOld;
   IndirectBuffer[] indirectBuffers;
   public RenderRegionBuilder renderRegionCache;
   private final List<Runnable> onAllChangedCallbacks = new ObjectArrayList();

   public static WorldRenderer init(RenderBuffers renderBuffers) {
      return INSTANCE != null ? INSTANCE : (INSTANCE = new WorldRenderer(renderBuffers));
   }

   private WorldRenderer(RenderBuffers renderBuffers) {
      this.minecraft = Minecraft.getInstance();
      this.renderBuffers = renderBuffers;
      this.renderRegionCache = new RenderRegionBuilder();
      this.taskDispatcher = new TaskDispatcher();
      ChunkTask.setTaskDispatcher(this.taskDispatcher);
      this.allocateIndirectBuffers();
      TerrainRenderType.updateMapping();
      Renderer.getInstance().addOnResizeCallback(() -> {
         if (this.indirectBuffers.length != Renderer.getFramesNum()) {
            this.allocateIndirectBuffers();
         }
      });
   }

   private void allocateIndirectBuffers() {
      if (this.indirectBuffers != null) {
         Arrays.stream(this.indirectBuffers).forEach(Buffer::scheduleFree);
      }

      this.indirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];

      for (int i = 0; i < this.indirectBuffers.length; i++) {
         this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);
      }
   }

   private void benchCallback() {
      BuildTimeProfiler.runBench(this.graphNeedsUpdate || !this.taskDispatcher.isIdle());
   }

   public void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
      Profiler profiler = Profiler.getMainProfiler();
      profiler.push("Setup_Renderer");
      this.benchCallback();
      Vec3 camPos = camera.getPosition();
      this.cameraPos.set(camPos.x(), camPos.y(), camPos.z());
      if (this.minecraft.options.getEffectiveRenderDistance() != this.renderDistance) {
         this.allChanged();
      }

      this.level.getProfiler().push("camera");
      float cameraX = (float)this.cameraPos.x();
      float cameraY = (float)this.cameraPos.y();
      float cameraZ = (float)this.cameraPos.z();
      int sectionX = SectionPos.posToSectionCoord(cameraX);
      int sectionY = SectionPos.posToSectionCoord(cameraY);
      int sectionZ = SectionPos.posToSectionCoord(cameraZ);
      profiler.push("reposition");
      if (this.lastCameraSectionX != sectionX || this.lastCameraSectionY != sectionY || this.lastCameraSectionZ != sectionZ) {
         this.lastCameraSectionX = sectionX;
         this.lastCameraSectionY = sectionY;
         this.lastCameraSectionZ = sectionZ;
         this.sectionGrid.repositionCamera(cameraX, cameraZ);
      }

      profiler.pop();
      double entityDistanceScaling = (Double)this.minecraft.options.entityDistanceScaling().get();
      Entity.setViewScale(Mth.clamp(this.renderDistance / 8.0, 1.0, 2.5) * entityDistanceScaling);
      this.level.getProfiler().popPush("cull");
      this.minecraft.getProfiler().popPush("culling");
      this.minecraft.getProfiler().popPush("update");
      boolean cameraMoved = false;
      float d_xRot = Math.abs(camera.getXRot() - this.lastCamRotX);
      float d_yRot = Math.abs(camera.getYRot() - this.lastCamRotY);
      cameraMoved |= d_xRot > 2.0F || d_yRot > 2.0F;
      cameraMoved |= cameraX != this.lastCameraX || cameraY != this.lastCameraY || cameraZ != this.lastCameraZ;
      this.graphNeedsUpdate |= cameraMoved;
      if (!isCapturedFrustum && this.graphNeedsUpdate()) {
         this.graphNeedsUpdate = false;
         this.lastCameraX = cameraX;
         this.lastCameraY = cameraY;
         this.lastCameraZ = cameraZ;
         this.lastCamRotX = camera.getXRot();
         this.lastCamRotY = camera.getYRot();
         this.sectionGraph.update(camera, frustum, spectator);
      }

      this.indirectBuffers[Renderer.getCurrentFrame()].reset();
      this.minecraft.getProfiler().pop();
      profiler.pop();
   }

   public void uploadSections() {
      this.minecraft.getProfiler().push("upload");
      Profiler profiler = Profiler.getMainProfiler();
      profiler.push("Uploads");

      try {
         if (this.taskDispatcher.updateSections()) {
            this.graphNeedsUpdate = true;
         }
      } catch (Exception var3) {
         Initializer.LOGGER.error(var3.getMessage());
         this.allChanged();
      }

      profiler.pop();
      this.minecraft.getProfiler().pop();
   }

   public boolean isSectionCompiled(BlockPos blockPos) {
      RenderSection renderSection = this.sectionGrid.getSectionAtBlockPos(blockPos);
      return renderSection != null && renderSection.isCompiled();
   }

   public void allChanged() {
      if (this.level != null) {
         this.level.clearTintCaches();
         this.renderRegionCache.clear();
         this.taskDispatcher.createThreads(Initializer.CONFIG.builderThreads);
         this.graphNeedsUpdate = true;
         this.renderDistance = this.minecraft.options.getEffectiveRenderDistance();
         if (this.sectionGrid != null) {
            this.sectionGrid.freeAllBuffers();
         }

         this.taskDispatcher.clearBatchQueue();
         synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.clear();
         }

         this.sectionGrid = new SectionGrid(this.level, this.renderDistance);
         this.sectionGraph = new SectionGraph(this.level, this.sectionGrid, this.taskDispatcher);
         this.onAllChangedCallbacks.forEach(Runnable::run);
         Entity entity = this.minecraft.getCameraEntity();
         if (entity != null) {
            this.sectionGrid.repositionCamera(entity.getX(), entity.getZ());
         }
      }
   }

   public void setLevel(@Nullable ClientLevel level) {
      this.lastCameraX = Float.MIN_VALUE;
      this.lastCameraY = Float.MIN_VALUE;
      this.lastCameraZ = Float.MIN_VALUE;
      this.lastCameraSectionX = Integer.MIN_VALUE;
      this.lastCameraSectionY = Integer.MIN_VALUE;
      this.lastCameraSectionZ = Integer.MIN_VALUE;
      this.level = level;
      ChunkStatusMap.createInstance(this.renderDistance);
      if (level != null) {
         this.allChanged();
      } else {
         if (this.sectionGrid != null) {
            this.sectionGrid.freeAllBuffers();
            this.sectionGrid = null;
         }

         this.taskDispatcher.stopThreads();
         this.graphNeedsUpdate = true;
      }
   }

   public void addOnAllChangedCallback(Runnable runnable) {
      this.onAllChangedCallbacks.add(runnable);
   }

   public void clearOnAllChangedCallbacks() {
      this.onAllChangedCallbacks.clear();
   }

   public void renderSectionLayer(TerrainRenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
      Renderer.getInstance().getMainPass().rebindMainTarget();
      this.sortTranslucentSections(camX, camY, camZ);
      this.minecraft.getProfiler().push("filterempty");
      this.minecraft.getProfiler().popPush(() -> "render_" + renderType);
      boolean isTranslucent = renderType == TerrainRenderType.TRANSLUCENT;
      boolean indirectDraw = Initializer.CONFIG.indirectDraw;
      if (!isTranslucent) {
         GlStateManager._disableBlend();
      } else {
         GlStateManager._enableBlend();
         VRenderSystem.blendFuncSeparate(770, 771, 1, 771);
      }

      VRenderSystem.disableCull();
      VRenderSystem.depthFunc(515);
      GlStateManager._enableDepthTest();
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(true, true, true, true);
      GlStateManager._disablePolygonOffset();
      VRenderSystem.setPolygonModeGL(6914);
      VRenderSystem.applyMVP(modelView, projection);
      VRenderSystem.setPrimitiveTopologyGL(4);
      Renderer renderer = Renderer.getInstance();
      GraphicsPipeline pipeline = PipelineManager.getTerrainShader(renderType);
      renderer.bindGraphicsPipeline(pipeline);
      TextureManager textureManager = Minecraft.getInstance().getTextureManager();
      AbstractTexture blockAtlasTexture = textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS);
      VkGlTexture glTexture = VkGlTexture.getTexture(blockAtlasTexture.getId());
      VulkanImage vulkanImage = glTexture.getVulkanImage();
      vulkanImage.setSampler(SamplerManager.getSampler(false, false, true, vulkanImage.mipLevels));
      RenderSystem.setShaderTexture(0, blockAtlasTexture.getId());
      Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
      VTextureSelector.bindShaderTextures(pipeline);
      IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
      Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer, indexBuffer.indexType.value);
      int currentFrame = Renderer.getCurrentFrame();
      Set<TerrainRenderType> allowedRenderTypes = Initializer.CONFIG.uniqueOpaqueLayer
         ? TerrainRenderType.COMPACT_RENDER_TYPES
         : TerrainRenderType.SEMI_COMPACT_RENDER_TYPES;
      if (allowedRenderTypes.contains(renderType)) {
         renderType.setCutoutUniform();

         for (var areas = this.sectionGraph.getChunkAreaQueue().iterator(); areas.hasNext();) {
            ChunkArea chunkArea = areas.next();
            StaticQueue<RenderSection> queue = chunkArea.sectionQueue;
            DrawBuffers drawBuffers = chunkArea.drawBuffers;
            renderer.uploadAndBindUBOs(pipeline);
            if (drawBuffers.getAreaBuffer(renderType) != null && queue.size() > 0) {
               drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, renderType, camX, camY, camZ);
               renderer.uploadAndBindUBOs(pipeline);
               if (indirectDraw) {
                  drawBuffers.buildDrawBatchesIndirect(this.cameraPos, this.indirectBuffers[currentFrame], queue, renderType);
               } else {
                  drawBuffers.buildDrawBatchesDirect(this.cameraPos, queue, renderType);
               }
            }
         }
      }

      if (renderType == TerrainRenderType.CUTOUT || renderType == TerrainRenderType.TRIPWIRE) {
         this.indirectBuffers[currentFrame].submitUploads();
      }

      if (!indirectDraw) {
         VRenderSystem.setModelOffset(0.0F, 0.0F, 0.0F);
         renderer.pushConstants(pipeline);
      }

      this.minecraft.getProfiler().pop();
   }

   private void sortTranslucentSections(double camX, double camY, double camZ) {
      this.minecraft.getProfiler().push("translucent_sort");
      double d0 = camX - this.xTransparentOld;
      double d1 = camY - this.yTransparentOld;
      double d2 = camZ - this.zTransparentOld;
      if (d0 * d0 + d1 * d1 + d2 * d2 > 2.0) {
         this.xTransparentOld = camX;
         this.yTransparentOld = camY;
         this.zTransparentOld = camZ;
         int j = 0;
         Iterator<RenderSection> iterator = this.sectionGraph.getSectionQueue().iterator(false);

         while (iterator.hasNext() && j < 200) {
            RenderSection section = iterator.next();
            section.resortTransparency(this.taskDispatcher, this.cameraPos);
            if (!section.isCompletelyEmpty()) {
               j++;
            }
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderBlockEntities(
      PoseStack poseStack, double camX, double camY, double camZ, Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, float gameTime
   ) {
      Profiler profiler = Profiler.getMainProfiler();
      profiler.pop();
      profiler.push("Block-entities");
      MultiBufferSource bufferSource = this.renderBuffers.bufferSource();

      for (RenderSection renderSection : this.sectionGraph.getBlockEntitiesSections()) {
         List<BlockEntity> list = renderSection.getCompiledSection().getBlockEntities();
         if (!list.isEmpty()) {
            for (BlockEntity blockEntity : list) {
               BlockPos blockPos = blockEntity.getBlockPos();
               MultiBufferSource bufferSource1 = bufferSource;
               poseStack.pushPose();
               poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
               SortedSet<BlockDestructionProgress> sortedset = (SortedSet<BlockDestructionProgress>)destructionProgress.get(blockPos.asLong());
               if (sortedset != null && !sortedset.isEmpty()) {
                  int j1 = sortedset.last().getProgress();
                  if (j1 >= 0) {
                     Pose pose = poseStack.last();
                     VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                        this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(j1)), pose, 1.0F
                     );
                     bufferSource1 = renderType -> {
                        VertexConsumer vertexConsumer2 = bufferSource.getBuffer(renderType);
                        return renderType.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexConsumer2) : vertexConsumer2;
                     };
                  }
               }

               this.minecraft.getBlockEntityRenderDispatcher().render(blockEntity, gameTime, poseStack, bufferSource1);
               poseStack.popPose();
            }
         }
      }
   }

   public void scheduleGraphUpdate() {
      this.graphNeedsUpdate = true;
   }

   public boolean graphNeedsUpdate() {
      return this.graphNeedsUpdate;
   }

   public int getVisibleSectionsCount() {
      return this.sectionGraph.getSectionQueue().size();
   }

   public void setSectionDirty(int x, int y, int z, boolean flag) {
      this.sectionGrid.setDirty(x, y, z, flag);
      this.renderRegionCache.remove(x, z);
   }

   public SectionGrid getSectionGrid() {
      return this.sectionGrid;
   }

   public ChunkAreaManager getChunkAreaManager() {
      return this.sectionGrid == null ? null : this.sectionGrid.chunkAreaManager;
   }

   public TaskDispatcher getTaskDispatcher() {
      return this.taskDispatcher;
   }

   public short getLastFrame() {
      return this.sectionGraph.getLastFrame();
   }

   public int getRenderDistance() {
      return this.renderDistance;
   }

   public String getChunkStatistics() {
      return this.sectionGraph == null ? null : this.sectionGraph.getStatistics();
   }

   public void cleanUp() {
      if (this.indirectBuffers != null) {
         Arrays.stream(this.indirectBuffers).forEach(Buffer::scheduleFree);
      }
   }

   public static WorldRenderer getInstance() {
      return INSTANCE;
   }

   public static ClientLevel getLevel() {
      return INSTANCE.level;
   }

   public static Vector3d getCameraPos() {
      return INSTANCE.cameraPos;
   }
}
