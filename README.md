# VulkanMod NeoForge (Unofficial)
## [Скачать готовый JAR / Download](https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/releases/download/v0.4.8-neoforge.4/VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.4.jar)

**Beta 0.4.8-neoforge.4 · Minecraft 1.21.1 · NeoForge · Java 21**

[All release files, sources and checksums](https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/releases/tag/v0.4.8-neoforge.4)

An unofficial client-side port of [VulkanMod](https://github.com/xCollateral/VulkanMod)
to **Minecraft 1.21.1 / NeoForge**, maintained by **Aviello**. It replaces Minecraft's
OpenGL renderer with Vulkan. This project is not affiliated with or endorsed by the
original VulkanMod team.

## Requirements and installation

- Minecraft **1.21.1**, NeoForge **21.1.252** and Java **21**.
- A GPU and driver supporting **Vulkan 1.2**.
- Copy `VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.4.jar` into the client's `mods` folder.
- Install one renderer only. Do not combine it with the original VulkanMod,
  Avielix, Sodium, Embeddium, Iris or Oculus. The source JAR is not installable.

Fabric and Sinytra Connector are not required. Leave the mod off dedicated servers.
The existing `vulkanmod` ID and `config/vulkanmod_settings.json` path are retained.

## What this port changes

- Native NeoForge startup, metadata, settings integration and embedded Vulkan libraries.
- NeoForge model data, fluid rendering, extra section geometry and shader registration.
- Fixes for shader include memory ownership, resource retirement, projection depth
  and Cyrillic/Unicode glyph uploads.
- Version `0.4.8-neoforge.4` corrects the Breeze wind vertex shader and prepares the
  port's attribution, licensing and support information for a separate release.

See [PORTING.md](PORTING.md) for implementation details and historical test evidence,
and [CHANGELOG.md](CHANGELOG.md) for this release.

## Status and compatibility

**Experimental.** Historical game checks cover Windows with NVIDIA hardware.
Linux/macOS, multiplayer and arbitrary mod/resource-pack combinations are not
verified. Shader-pack compatibility and performance gains are not promised.
Some OpenGL compatibility entry points are incomplete, including generated mipmaps;
mods relying on them may render incorrectly. See the release audit before publishing.

## Build and verify

```text
gradlew.bat build
gradlew.bat shaderCompileCheck
gradlew.bat runClientPackagedSmoke
```

The installable and source JARs are written to `build/libs`. `build` checks bundled
shader compilation and active descriptor bindings; the separate shader command is
useful for a focused rerun. The packaged smoke test
uses a separate test client and creates its own world. It checks Vulkan startup,
world rendering, resource reload, settings and Unicode text; a fresh PASS marker is
required. The test mod is excluded from the installable JAR.

## Support and licensing

Report problems to [this port's issue tracker](https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/issues).
Include the mod version, NeoForge version, GPU/driver and `latest.log`.

Original VulkanMod code remains credited to xCollateral and contributors. This fork
is distributed under **LGPL-3.0-only**, with the incorporated GPLv3 text in `COPYING`.
Embedded dependencies retain their licenses; see [NOTICE.md](NOTICE.md) and `licenses/`.
This fork is separate from the independent Avielix renderer.

Publication materials are in [PUBLISHING.md](PUBLISHING.md). Aviello reports writing
a substantial part of the port, with AI assistance during development. This release
preparation also uses AI-assisted auditing, code changes and writing. This disclosure
refers to the port and its publication materials, not the original VulkanMod project.
