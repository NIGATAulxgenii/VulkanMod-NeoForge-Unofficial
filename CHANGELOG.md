# Changelog

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
