# Changelog

## 0.6.8-neoforge.1-dev (Experimental)

- Rebuild the port on the official VulkanMod 0.6.8 source (`dev` checkpoint
  `d3db079`) instead of the 0.4.8 base. Renderer, buffer slices, descriptor sets,
  staging pool, upload synchronization and terrain renderer are the newer upstream
  implementations.
- Replace Minecraft 1.21.11's GPU API with adapters for the 1.21.1 shader and
  render-target classes (shader instances, post chains, uniforms, GL state).
- Native NeoForge startup: early window handoff, Vulkan `NO_API` window,
  module-safe surface creation, `neoforge.mods.toml` and a settings registration event.
- Port the 1.21.1 shader set and add a shader compile check.
- Add a packaged smoke test (separate test client, fresh world, screenshot readback,
  Cyrillic text) that requires a `SMOKE PASS` marker.
- Add `SOURCE_PROVENANCE.md`, `NOTICE.md`, `COPYING` and bundled library licenses.

Upstream VulkanMod `dev` has moved on (spirv-cross shader conversion, instance/device
refactors); those commits are not included.

## Earlier releases

0.4.8-neoforge.4 through .7-dev were based on the 0.4.8 source with 0.6.0 feature
backports. See the releases page of the repository.
