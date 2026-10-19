package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlTexture {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlTexture> map = new Int2ReferenceOpenHashMap<>();
    private static int boundTextureId = 0;
    private static GlTexture boundTexture;
    private static int activeTexture = 0;
    private static int unpackRowLength, unpackSkipRows, unpackSkipPixels;
    private static int unpackAlignment = 4;

    public static void bindIdToImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = map.get(id);
        texture.vulkanImage = vulkanImage;
    }

    public static int genTextureId() {
        int id = ID_COUNTER;
        map.put(id, new GlTexture(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindTexture(int id) {
        boundTextureId = id;
        boundTexture = map.get(id);

        if (id <= 0)
            return;

        if (boundTexture == null)
            throw new NullPointerException("bound texture is null");

        VulkanImage vulkanImage = boundTexture.vulkanImage;
        if (vulkanImage != null)
            VTextureSelector.bindTexture(activeTexture, vulkanImage);
    }

    public static void glDeleteTextures(IntBuffer intBuffer) {
        for (int i = intBuffer.position(); i < intBuffer.limit(); i++) {
            glDeleteTextures(intBuffer.get(i));
        }
    }

    public static void glDeleteTextures(int i) {
        GlTexture glTexture = map.remove(i);
        VulkanImage image = glTexture != null ? glTexture.vulkanImage : null;
        if (image != null)
            MemoryManager.getInstance().addToFreeable(image);
    }

    public static GlTexture getTexture(int id) {
        if (id == 0)
            return null;

        return map.get(id);
    }

    public static void activeTexture(int i) {
        activeTexture = i - GL30.GL_TEXTURE0;
        VTextureSelector.setActiveTexture(activeTexture);
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, long pixels) {
        if (checkParams(level, width, height))
            return;

        boundTexture.allocateIfNeeded(level, width, height, internalFormat, type);
        VTextureSelector.bindTexture(activeTexture, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        if (checkParams(level, width, height))
            return;

        boundTexture.allocateIfNeeded(level, width, height, internalFormat, type);
        VTextureSelector.bindTexture(activeTexture, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    private static boolean checkParams(int level, int width, int height) {
        if (width == 0 || height == 0)
            return true;

        if (level < 0 || level > 30 || width < 0 || height < 0)
            throw new IllegalArgumentException("Invalid texture level or dimensions");
        return false;
    }

    public static void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long pixels) {
        if (width == 0 || height == 0) return;
        GlBuffer buffer = GlBuffer.getPixelUnpackBufferBound();
        ByteBuffer source;
        if (buffer != null) {
            source = buffer.data.duplicate();
            source.position(Math.toIntExact(pixels));
        } else if (pixels != 0L) {
            int size = PixelTransfer.requiredBytes(width, height, PixelTransfer.components(format),
                    unpackRowLength, unpackSkipRows, unpackSkipPixels, unpackAlignment);
            source = MemoryUtil.memByteBuffer(pixels, size);
        } else return;
        boundTexture.uploadSubImage(level, xOffset, yOffset, width, height, format, type, source);
    }

    public static void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, @Nullable ByteBuffer pixels) {
        if (width == 0 || height == 0) return;
        GlBuffer buffer = GlBuffer.getPixelUnpackBufferBound();
        if (buffer != null && pixels != null)
            throw new IllegalStateException("Pixel buffer supplied while a Pixel Unpack Buffer is bound");
        ByteBuffer source = buffer != null ? buffer.data.duplicate().clear() : pixels;
        if (source != null) boundTexture.uploadSubImage(level, xOffset, yOffset, width, height, format, type, source);
    }

    public static void pixelStoreI(int name, int value) {
        if (value < 0) throw new IllegalArgumentException("Negative pixel store value");
        switch (name) {
            case GL11.GL_UNPACK_ROW_LENGTH -> unpackRowLength = value;
            case GL11.GL_UNPACK_SKIP_ROWS -> unpackSkipRows = value;
            case GL11.GL_UNPACK_SKIP_PIXELS -> unpackSkipPixels = value;
            case GL11.GL_UNPACK_ALIGNMENT -> {
                if (value != 1 && value != 2 && value != 4 && value != 8)
                    throw new IllegalArgumentException("Invalid unpack alignment: " + value);
                unpackAlignment = value;
            }
        }
    }

    public static void texParameteri(int target, int pName, int param) {
        if (target != GL11.GL_TEXTURE_2D)
            throw new UnsupportedOperationException("target != GL_TEXTURE_2D not supported");

        switch (pName) {
            case GL30.GL_TEXTURE_MAX_LEVEL -> boundTexture.setMaxLevel(param);
            case GL30.GL_TEXTURE_MAX_LOD -> boundTexture.setMaxLod(param);
            case GL30.GL_TEXTURE_MIN_LOD -> {}
            case GL30.GL_TEXTURE_LOD_BIAS -> {}

            case GL11.GL_TEXTURE_MAG_FILTER -> boundTexture.setMagFilter(param);
            case GL11.GL_TEXTURE_MIN_FILTER -> boundTexture.setMinFilter(param);

            case GL11.GL_TEXTURE_WRAP_S, GL11.GL_TEXTURE_WRAP_T -> boundTexture.setClamp(param);

            default -> {}
        }

        //TODO
    }

    public static int getTexLevelParameter(int target, int level, int pName) {
        if (target != GL11.GL_TEXTURE_2D)
            throw new UnsupportedOperationException("target != GL_TEXTURE_2D not supported");

        if (boundTexture == null)
            return -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> GlUtil.getGlFormat(boundTexture.vulkanImage.format);
            case GL11.GL_TEXTURE_WIDTH -> level < boundTexture.vulkanImage.mipLevels ? Math.max(1, boundTexture.vulkanImage.width >> level) : 0;
            case GL11.GL_TEXTURE_HEIGHT -> level < boundTexture.vulkanImage.mipLevels ? Math.max(1, boundTexture.vulkanImage.height >> level) : 0;

            default -> -1;
        };
    }

    public static int getTexParameteri(int target, int name) {
        if (target != GL11.GL_TEXTURE_2D) throw new UnsupportedOperationException("Only 2D textures are supported");
        if (boundTexture == null) return -1;
        return switch (name) {
            case GL30.GL_TEXTURE_MAX_LEVEL -> boundTexture.maxLevel;
            case GL30.GL_TEXTURE_MAX_LOD -> boundTexture.maxLod;
            case GL11.GL_TEXTURE_MIN_FILTER -> boundTexture.minFilter;
            case GL11.GL_TEXTURE_MAG_FILTER -> boundTexture.magFilter;
            default -> getTexLevelParameter(target, 0, name);
        };
    }

    public static void generateMipmap(int target) {
        if (target != GL11.GL_TEXTURE_2D)
            throw new UnsupportedOperationException("target != GL_TEXTURE_2D not supported");

        // TODO: crashing
//        boundTexture.generateMipmaps();
    }

    public static void getTexImage(int tex, int level, int format, int type, long pixels) {
        VulkanImage image = boundTexture.vulkanImage;

        GlBuffer buffer = GlBuffer.getPixelPackBufferBound();
        long ptr;
        if (buffer != null) {
            int size = Math.multiplyExact(Math.multiplyExact(Math.max(1, image.width >> level), Math.max(1, image.height >> level)), image.formatSize);
            int offset = Math.toIntExact(pixels);
            if (offset < 0 || offset > buffer.data.capacity() - size) throw new IllegalArgumentException("Pixel Pack Buffer is too small");
            ptr = MemoryUtil.memAddress(buffer.data, offset);
        } else {
            ptr = pixels;
        }


        ImageUtil.downloadTexture(image, level, ptr);
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = map.get(id);

        texture.vulkanImage = vulkanImage;
    }

    public static GlTexture getBoundTexture() {
        return boundTexture;
    }

    final int id;
    VulkanImage vulkanImage;
    int internalFormat;

    boolean needsUpdate = false;
    int maxLevel = 0;
    int maxLod = 0;
    int minFilter = GL11.GL_NEAREST_MIPMAP_LINEAR, magFilter = GL11.GL_LINEAR;

    boolean clamp = true;

    public GlTexture(int id) {
        this.id = id;
    }

    void allocateIfNeeded(int level, int width, int height, int internalFormat, int type) {
        int vkFormat = GlUtil.vulkanFormat(internalFormat, type);
        int baseWidth = level == 0 || vulkanImage == null ? Math.multiplyExact(width, 1 << level) : vulkanImage.width;
        int baseHeight = level == 0 || vulkanImage == null ? Math.multiplyExact(height, 1 << level) : vulkanImage.height;
        int availableLevels = 32 - Integer.numberOfLeadingZeros(Math.max(baseWidth, baseHeight));
        if (level >= availableLevels || width != Math.max(1, baseWidth >> level) || height != Math.max(1, baseHeight >> level))
            throw new IllegalArgumentException("Texture mip dimensions do not match the base image");
        maxLevel = Math.max(maxLevel, level);
        int levels = Math.min(maxLevel, availableLevels - 1) + 1;
        if (vulkanImage == null || vulkanImage.width != baseWidth || vulkanImage.height != baseHeight
                || vulkanImage.format != vkFormat || vulkanImage.mipLevels != levels) {
            VulkanImage previous = vulkanImage;
            if (VulkanImage.isDepthFormat(vkFormat)) {
                vulkanImage = VulkanImage.createDepthImage(vkFormat, baseWidth, baseHeight,
                        VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                        false, true);
            } else {
                vulkanImage = new VulkanImage.Builder(baseWidth, baseHeight)
                        .setMipLevels(levels).setFormat(vkFormat).addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT).createVulkanImage();
            }
            if (previous != null) {
                if (previous.width == baseWidth && previous.height == baseHeight && previous.format == vkFormat
                        && !VulkanImage.isDepthFormat(vkFormat) && previous.getCurrentLayout() != VK_IMAGE_LAYOUT_UNDEFINED)
                    ImageUtil.copyMipLevels(previous, vulkanImage);
                previous.free();
            }
            updateSampler();
        }
        needsUpdate = false;
        this.internalFormat = internalFormat;
    }

    public void setFilter(boolean blur, boolean mipmap) {
        this.magFilter = blur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        this.minFilter = mipmap ? (blur ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_NEAREST_MIPMAP_LINEAR) : this.magFilter;
        updateSampler();
    }

    void updateSampler() {
        if (vulkanImage == null)
            return;

        byte samplerFlags = SamplerManager.SEPARATE_MAG_FILTER_BIT;
        samplerFlags |= clamp ? SamplerManager.CLAMP_BIT : 0;
        samplerFlags |= magFilter == GL11.GL_LINEAR ? SamplerManager.MAG_LINEAR_FILTERING_BIT : 0;
        if (minFilter == GL11.GL_LINEAR || minFilter == GL11.GL_LINEAR_MIPMAP_NEAREST || minFilter == GL11.GL_LINEAR_MIPMAP_LINEAR)
            samplerFlags |= SamplerManager.LINEAR_FILTERING_BIT;
        if (minFilter == GL11.GL_NEAREST_MIPMAP_NEAREST || minFilter == GL11.GL_NEAREST_MIPMAP_LINEAR
                || minFilter == GL11.GL_LINEAR_MIPMAP_NEAREST || minFilter == GL11.GL_LINEAR_MIPMAP_LINEAR)
            samplerFlags |= SamplerManager.USE_MIPMAPS_BIT;
        if (minFilter == GL11.GL_NEAREST_MIPMAP_LINEAR || minFilter == GL11.GL_LINEAR_MIPMAP_LINEAR)
            samplerFlags |= SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;

        vulkanImage.updateTextureSampler(maxLod, samplerFlags);
    }

    private void uploadSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, ByteBuffer pixels) {
        if (level < 0 || level >= vulkanImage.mipLevels || xOffset < 0 || yOffset < 0
                || width < 0 || height < 0 || width > Math.max(1, vulkanImage.width >> level) - xOffset
                || height > Math.max(1, vulkanImage.height >> level) - yOffset)
            throw new IllegalArgumentException("Texture upload is outside its mip level");
        ByteBuffer packed = PixelTransfer.unpack(pixels, width, height, format, type, vulkanImage.formatSize,
                unpackRowLength, unpackSkipRows, unpackSkipPixels, unpackAlignment);
        try {
            if (vulkanImage.format == VK_FORMAT_B8G8R8A8_UNORM) {
                for (int pixel = 0; pixel < packed.limit(); pixel += 4) {
                    byte red = packed.get(pixel);
                    packed.put(pixel, packed.get(pixel + 2));
                    packed.put(pixel + 2, red);
                }
            }
            this.vulkanImage.uploadSubTextureAsync(level, width, height, xOffset, yOffset, 0, 0, 0, packed);
        } finally {
            MemoryUtil.memFree(packed);
        }
    }

    void generateMipmaps() {
        //TODO test
        ImageUtil.generateMipmaps(vulkanImage);
    }

    void setMaxLevel(int l) {
        if (l < 0)
            throw new IllegalStateException("max level cannot be < 0.");

        if (maxLevel != l) {
            maxLevel = l;
            needsUpdate = true;
        }
    }

    void setMaxLod(int l) {
        if (l < 0)
            throw new IllegalStateException("max level cannot be < 0.");

        if (maxLod != l) {
            maxLod = l;
            updateSampler();
        }
    }

    void setMagFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST -> {
            }

            default -> throw new IllegalArgumentException("illegal mag filter value: " + v);
        }

        this.magFilter = v;
        updateSampler();
    }

    void setMinFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST,
                 GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_NEAREST_MIPMAP_LINEAR,
                 GL11.GL_LINEAR_MIPMAP_NEAREST, GL11.GL_NEAREST_MIPMAP_NEAREST -> {
            }

            default -> throw new IllegalArgumentException("illegal min filter value: " + v);
        }

        this.minFilter = v;
        updateSampler();
    }

    void setClamp(int v) {
        if (v == GL30.GL_CLAMP_TO_EDGE) {
            this.clamp = true;
        } else {
            this.clamp = false;
        }

        updateSampler();
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage vulkanImage) {
        this.vulkanImage = vulkanImage;
        this.maxLevel = vulkanImage.mipLevels - 1;
        this.maxLod = this.maxLevel;
    }

}
