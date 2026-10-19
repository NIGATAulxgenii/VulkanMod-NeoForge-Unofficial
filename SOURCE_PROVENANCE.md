# Source provenance for the NeoForge / Minecraft 1.21.1 port

This work starts from the official VulkanMod repository, rather than the old
0.4.8 NeoForge checkout. It is an experimental derivative maintained by Aviello,
not an official upstream release.

- Official source checkpoint: `d3db07925f60257433233409125a08d7c08bf023`,
  the 0.6.8 version bump on the author's `dev` history. That source targets
  Minecraft 1.21.11 / Fabric. The branch head includes later development and
  is retained separately as a reference.
- Game-version reference: the author's published **0.6.7+1.21.1** Fabric JAR,
  [Modrinth version 2eqroDKD](https://modrinth.com/mod/vulkanmod/version/2eqroDKD).
  Its SHA-512 was checked against the official API before use:
  `2de36869bbc765637e874300af691cfa5208983e79485dcbfc3c5f8d2490d502065082a97dd7933334ed4d0e37f18234d0c9c5bf27b1217a3e2c032c52154ff0`.
- The public upstream `1.21` source branch is still 0.4.8. Where the updated
  1.21.1 adapters are absent from public source, the official 0.6.7 JAR was
  remapped from Fabric intermediary names to Mojang names with matching
  Minecraft mappings and inspected with Vineflower. Recovered Java is not
  represented as the author's original source text. Compiler/decompiler and
  NeoForge compatibility corrections are part of this port.
- Shared implementations come directly from the official source checkpoint;
  the existing buffer slices, long buffer sizes, descriptor sets, staging
  buffer pool, upload synchronization and terrain renderer remain the newer
  upstream implementations. Minecraft's newer GPU API is replaced by the
  actual 1.21.1 shader/render-target adapters.
- The old NeoForge port supplies bounded loader integration and verified fixes:
  early window handoff, module-safe surface creation, a driver-extension scratch
  allocation workaround, shader include lifetime handling, native model data,
  native fluids/section geometry and OpenGL-to-Vulkan projection conversion.
  These integrations do not replace the newer upstream renderer with 0.4.8.

Original VulkanMod authorship and LGPL-3.0-only remain in force. `LICENSE`,
`COPYING`, `NOTICE.md` and bundled dependency notices accompany distribution.
Development and adaptation use AI assistance; upstream authorship is separate.

This source tree is still undergoing port verification. A successful compilation
alone does not establish matching behavior or compatibility with arbitrary mods.
