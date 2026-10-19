# Changelog

## 0.4.8-neoforge.7-dev - 2026-10-19 (Experimental)

- Add a unified window-mode selector, independent cloud distance with its own
  fog range, and the newer Minimized/AFK frame-rate limit setting. Migrate
  existing borderless-window settings and preserve configured FPS caps.
- Backport semaphore-backed texture uploads and deferred command-buffer reuse.
  Keep explicit synchronous drains for readback, resizing and resource retirement;
  wait for a frame slot before reusing its staging memory.
- Reuse circular lists and iterators while repositioning section/area grids.
- Skip intermediate sprite interpolation while advancing the animation timeline.
- Support strided pixel uploads, mip-level transfers and readback, PBO contents,
  independent framebuffer bindings and rectangular framebuffer copies through
  Minecraft's native APIs. Preserve existing mip pixels when storage grows.
- Reuse matching prepared textures, retain native image formats, convert RGB
  storage safely and retire deleted renderbuffer images.
- Restore Minecraft's emergency-save path; release main passes and indirect
  buffers on shutdown. Keep scissor commands inside active frame recording.
- Check compiled shader UBO member offsets against pipeline metadata. Extend
  packaged checks to window transitions, native formats, upload synchronization,
  vanilla post effects and emergency saving. Add optional bounded frame timing.

See `UPSTREAM_PARITY.md` for the upstream scope, platform adaptations and limits.

## 0.4.8-neoforge.6-dev - Included in .7-dev

- Backport CPU terrain backface culling with facing buckets, contiguous draw
  parameters and grouped direct/indirect draws from upstream VulkanMod 0.6.0.
- Reduce terrain vertices to 16 bytes while retaining NeoForge model data,
  fluid extensions and additional section geometry. Preserve unclassified
  and out-of-section geometry without CPU face culling.
- Backport cached cloud geometry with fancy/fast modes and resource lifecycle,
  retaining NeoForge's custom dimension cloud hook on Minecraft 1.21.1.
- Add builder thread and texture animation controls, legacy configuration
  compatibility and bounded asynchronous checks for this port's releases.
- Correct biome tint cache layers, custom resolvers and spatial blending;
  capture biome data before handing render regions to workers.
- Preserve independent minification/magnification filters and all four mipmap
  filter modes, lazy texture IDs and full sampler cache keys.
- Correct beacon fog and the position/color/normal shader uniform layout;
  remove global block entities when sections change or entities disappear.
- Support large automatic and sorted index buffers and retain shared VBO
  index handles across reallocations. Bound initial per-facing buffer budgets.
- Extend packaged Minecraft checks for renderer modes, cloud caching and reload,
  native samplers, large indices, beacon removal and leashed-entity fixtures.

## 0.4.8-neoforge.5-dev - Included in .7-dev

- Release temporary upload buffers when their chunk area has been dismissed.
- Preserve supplied index order, index type and draw count in immediate rendering.
- Retain streamed index buffers per frame, align mixed index types and defer
  destruction of old allocations when buffers grow.

## 0.4.8-neoforge.4 - 2026-10-16

- Identify the mod as an unofficial NeoForge port maintained by Aviello.
- Preserve original VulkanMod credits and LGPL-3.0-only; include the incorporated
  GPLv3 text and bundled-library license notices in binary and source JARs.
- Direct port support requests to its own repository.
- Correct the Breeze wind vertex-position constructor and its lightmap binding.
- Match the blit-screen sampler to its pipeline metadata.
- Check compilation and active descriptor bindings for bundled shaders using
  the renderer's shaderc compiler during `build`.
- Correct misleading comments without changing renderer behavior in those files.
- Document experimental compatibility limits and hosting disclosures.

## 0.4.8-neoforge.3

- Route native Unihex glyph uploads through Vulkan and retain exactly-once pixel release.
- Add Cyrillic/Unicode rendering and native texture upload/readback smoke checks.

## 0.4.8-neoforge.2

- Correct Vulkan projection depth and shadowed GUI text on NeoForge.

## 0.4.8-neoforge.1

- Initial native NeoForge 1.21.1 port. See PORTING.md for details.
