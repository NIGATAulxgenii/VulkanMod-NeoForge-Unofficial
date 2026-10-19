package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo.Buffer;

public class GraphicsPipeline extends Pipeline {
   private final Object2LongMap<PipelineState> graphicsPipelines = new Object2LongOpenHashMap();
   private final VertexFormat vertexFormat;
   private final GraphicsPipeline.VertexInputDescription vertexInputDescription;
   private long vertShaderModule = 0L;
   private long fragShaderModule = 0L;

   GraphicsPipeline(Pipeline.Builder builder) {
      super(builder.name);
      this.buffers = builder.UBOs;
      this.imageDescriptors = builder.imageDescriptors;
      this.pushConstants = builder.pushConstants;
      this.vertexFormat = builder.vertexFormat;
      this.vertexInputDescription = new GraphicsPipeline.VertexInputDescription(this.vertexFormat);
      this.createDescriptorSetLayout();
      this.createPipelineLayout();
      this.createShaderModules(builder);
      if (builder.renderPass != null) {
         this.graphicsPipelines.computeIfAbsent(PipelineState.DEFAULT, this::createGraphicsPipeline);
      }

      this.createDescriptorSets(Renderer.getFramesNum());
      PIPELINES.add(this);
   }

   public long getHandle(PipelineState state) {
      return this.graphicsPipelines.computeIfAbsent(state, this::createGraphicsPipeline);
   }

   private long createGraphicsPipeline(PipelineState state) {
      MemoryStack stack = MemoryStack.stackPush();

      long var21;
      try {
         ByteBuffer entryPoint = stack.UTF8("main");
         Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
         VkPipelineShaderStageCreateInfo vertShaderStageInfo = (VkPipelineShaderStageCreateInfo)shaderStages.get(0);
         vertShaderStageInfo.sType(18);
         vertShaderStageInfo.stage(1);
         vertShaderStageInfo.module(this.vertShaderModule);
         vertShaderStageInfo.pName(entryPoint);
         VkPipelineShaderStageCreateInfo fragShaderStageInfo = (VkPipelineShaderStageCreateInfo)shaderStages.get(1);
         fragShaderStageInfo.sType(18);
         fragShaderStageInfo.stage(16);
         fragShaderStageInfo.module(this.fragShaderModule);
         fragShaderStageInfo.pName(entryPoint);
         VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
         vertexInputInfo.sType(19);
         if (this.vertexInputDescription != null) {
            vertexInputInfo.pVertexBindingDescriptions(this.vertexInputDescription.bindingDescriptions);
            vertexInputInfo.pVertexAttributeDescriptions(this.vertexInputDescription.attributeDescriptions);
         }

         int topology = PipelineState.AssemblyRasterState.decodeTopology(state.assemblyRasterState);
         VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
         inputAssembly.sType(20);
         inputAssembly.topology(topology);
         inputAssembly.primitiveRestartEnable(false);
         VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
         viewportState.sType(22);
         viewportState.viewportCount(1);
         viewportState.scissorCount(1);
         int polygonMode = PipelineState.AssemblyRasterState.decodePolygonMode(state.assemblyRasterState);
         int cullMode = PipelineState.AssemblyRasterState.decodeCullMode(state.assemblyRasterState);
         VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
         rasterizer.sType(23);
         rasterizer.depthClampEnable(false);
         rasterizer.rasterizerDiscardEnable(false);
         rasterizer.polygonMode(polygonMode);
         rasterizer.lineWidth(1.0F);
         rasterizer.cullMode(cullMode);
         rasterizer.frontFace(0);
         rasterizer.depthBiasEnable(true);
         VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
         multisampling.sType(24);
         multisampling.sampleShadingEnable(false);
         multisampling.rasterizationSamples(1);
         VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
         depthStencil.sType(25);
         depthStencil.depthTestEnable(PipelineState.DepthState.depthTest(state.depthState_i));
         depthStencil.depthWriteEnable(PipelineState.DepthState.depthMask(state.depthState_i));
         depthStencil.depthCompareOp(PipelineState.DepthState.decodeDepthFun(state.depthState_i));
         depthStencil.depthBoundsTestEnable(false);
         depthStencil.minDepthBounds(0.0F);
         depthStencil.maxDepthBounds(1.0F);
         depthStencil.stencilTestEnable(false);
         org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
         colorBlendAttachment.colorWriteMask(state.colorMask_i);
         if (PipelineState.BlendState.enable(state.blendState_i)) {
            colorBlendAttachment.blendEnable(true);
            colorBlendAttachment.srcColorBlendFactor(PipelineState.BlendState.getSrcRgbFactor(state.blendState_i));
            colorBlendAttachment.dstColorBlendFactor(PipelineState.BlendState.getDstRgbFactor(state.blendState_i));
            colorBlendAttachment.colorBlendOp(PipelineState.BlendState.blendOp(state.blendState_i));
            colorBlendAttachment.srcAlphaBlendFactor(PipelineState.BlendState.getSrcAlphaFactor(state.blendState_i));
            colorBlendAttachment.dstAlphaBlendFactor(PipelineState.BlendState.getDstAlphaFactor(state.blendState_i));
            colorBlendAttachment.alphaBlendOp(PipelineState.BlendState.blendOp(state.blendState_i));
         } else {
            colorBlendAttachment.blendEnable(false);
         }

         VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
         colorBlending.sType(26);
         colorBlending.logicOpEnable(PipelineState.LogicOpState.enable(state.logicOp_i));
         colorBlending.logicOp(PipelineState.LogicOpState.decodeFun(state.logicOp_i));
         colorBlending.pAttachments(colorBlendAttachment);
         colorBlending.blendConstants(stack.floats(0.0F, 0.0F, 0.0F, 0.0F));
         VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
         dynamicStates.sType(27);
         dynamicStates.pDynamicStates(stack.ints(3, 0, 1, 2));
         org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
         pipelineInfo.sType(28);
         pipelineInfo.pStages(shaderStages);
         pipelineInfo.pVertexInputState(vertexInputInfo);
         pipelineInfo.pInputAssemblyState(inputAssembly);
         pipelineInfo.pViewportState(viewportState);
         pipelineInfo.pRasterizationState(rasterizer);
         pipelineInfo.pMultisampleState(multisampling);
         pipelineInfo.pDepthStencilState(depthStencil);
         pipelineInfo.pColorBlendState(colorBlending);
         pipelineInfo.pDynamicState(dynamicStates);
         pipelineInfo.layout(this.pipelineLayout);
         pipelineInfo.basePipelineHandle(0L);
         pipelineInfo.basePipelineIndex(-1);
         pipelineInfo.renderPass(state.renderPass.getId());
         pipelineInfo.subpass(0);
         LongBuffer pGraphicsPipeline = stack.mallocLong(1);
         Vulkan.checkResult(
            VK10.vkCreateGraphicsPipelines(DeviceManager.vkDevice, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline),
            "Failed to create graphics pipeline " + this.name
         );
         var21 = pGraphicsPipeline.get(0);
      } catch (Throwable var24) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var23) {
               var24.addSuppressed(var23);
            }
         }

         throw var24;
      }

      if (stack != null) {
         stack.close();
      }

      return var21;
   }

   private void createShaderModules(Pipeline.Builder builder) {
      String vsh = builder.shadersSrc.get(SPIRVUtils.ShaderKind.VERTEX_SHADER);
      SPIRVUtils.SPIRV vertShaderSPIRV = SPIRVUtils.compileShader(String.format("%s.vsh", this.name), vsh, SPIRVUtils.ShaderKind.VERTEX_SHADER);
      String fsh = builder.shadersSrc.get(SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
      SPIRVUtils.SPIRV fragShaderSPIRV = SPIRVUtils.compileShader(String.format("%s.fsh", this.name), fsh, SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
      this.vertShaderModule = createShaderModule(vertShaderSPIRV.bytecode());
      this.fragShaderModule = createShaderModule(fragShaderSPIRV.bytecode());
   }

   @Override
   public void cleanUp() {
      VK10.vkDestroyShaderModule(DeviceManager.vkDevice, this.vertShaderModule, null);
      VK10.vkDestroyShaderModule(DeviceManager.vkDevice, this.fragShaderModule, null);
      this.vertexInputDescription.cleanUp();
      this.destroyDescriptorSets();
      this.graphicsPipelines.forEach((state, pipeline) -> VK10.vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null));
      this.graphicsPipelines.clear();
      VK10.vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, this.descriptorSetLayout, null);
      VK10.vkDestroyPipelineLayout(DeviceManager.vkDevice, this.pipelineLayout, null);
      PIPELINES.remove(this);
      Renderer.getInstance().removeUsedPipeline(this);
   }

   private static org.lwjgl.vulkan.VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat) {
      org.lwjgl.vulkan.VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1);
      bindingDescription.binding(0);
      bindingDescription.stride(vertexFormat.getVertexSize());
      bindingDescription.inputRate(0);
      return bindingDescription;
   }

   private static org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(VertexFormat vertexFormat) {
      List<VertexFormatElement> elements = vertexFormat.getElements();
      int size = elements.size();
      org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(size);
      int offset = 0;

      for (int i = 0; i < size; i++) {
         VkVertexInputAttributeDescription posDescription;
         posDescription = (VkVertexInputAttributeDescription)attributeDescriptions.get(i);
         posDescription.binding(0);
         posDescription.location(i);
         VertexFormatElement formatElement = elements.get(i);
         Usage usage = formatElement.usage();
         Type type = formatElement.type();
         int elementCount = formatElement.count();
         label58:
         switch (usage) {
            case POSITION:
               switch (type) {
                  case FLOAT:
                     posDescription.format(106);
                     posDescription.offset(offset);
                     offset += 12;
                     break label58;
                  case SHORT:
                     posDescription.format(96);
                     posDescription.offset(offset);
                     offset += 8;
                     break label58;
                  case BYTE:
                     posDescription.format(42);
                     posDescription.offset(offset);
                     offset += 4;
                  default:
                     break label58;
               }
            case COLOR:
               switch (type) {
                  case UBYTE:
                     posDescription.format(37);
                     posDescription.offset(offset);
                     offset += 4;
                     break label58;
                  case UINT:
                     posDescription.format(98);
                     posDescription.offset(offset);
                     offset += 4;
                  default:
                     break label58;
               }
            case UV:
               switch (type) {
                  case FLOAT:
                     posDescription.format(103);
                     posDescription.offset(offset);
                     offset += 8;
                     break label58;
                  case SHORT:
                     posDescription.format(82);
                     posDescription.offset(offset);
                     offset += 4;
                  case BYTE:
                  case UBYTE:
                  default:
                     break label58;
                  case UINT:
                     posDescription.format(98);
                     posDescription.offset(offset);
                     offset += 4;
                     break label58;
                  case USHORT:
                     posDescription.format(81);
                     posDescription.offset(offset);
                     offset += 4;
                     break label58;
               }
            case NORMAL:
               posDescription.format(38);
               posDescription.offset(offset);
               offset += 4;
               break;
            case GENERIC:
               if (type == Type.SHORT && elementCount == 1) {
                  posDescription.format(75);
                  posDescription.offset(offset);
                  offset += 2;
               } else {
                  if (type == Type.INT && elementCount == 1) {
                     posDescription.format(99);
                     posDescription.offset(offset);
                     offset += 4;
                     break;
                  }

                  if (type != Type.FLOAT || elementCount != 1) {
                     throw new RuntimeException(String.format("Unknown type: %s", type));
                  }

                  posDescription.format(100);
                  posDescription.offset(offset);
                  offset += 4;
               }
               break;
            default:
               throw new RuntimeException(String.format("Unknown format: %s", usage));
         }

         posDescription.offset(((VertexFormatMixed)vertexFormat).getOffset(i));
      }

      return (org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer)attributeDescriptions.rewind();
   }

   static class VertexInputDescription {
      final org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer attributeDescriptions;
      final org.lwjgl.vulkan.VkVertexInputBindingDescription.Buffer bindingDescriptions;

      VertexInputDescription(VertexFormat vertexFormat) {
         if (vertexFormat != CustomVertexFormat.NONE) {
            this.bindingDescriptions = GraphicsPipeline.getBindingDescription(vertexFormat);
            this.attributeDescriptions = GraphicsPipeline.getAttributeDescriptions(vertexFormat);
         } else {
            this.bindingDescriptions = null;
            this.attributeDescriptions = null;
         }
      }

      void cleanUp() {
         if (this.bindingDescriptions != null) {
            MemoryUtil.memFree(this.bindingDescriptions);
            MemoryUtil.memFree(this.attributeDescriptions);
         }
      }
   }
}
