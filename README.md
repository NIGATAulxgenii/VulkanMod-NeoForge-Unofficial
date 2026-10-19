# VulkanMod NeoForge (Unofficial)

**Experimental 0.6.8-neoforge.1-dev · Minecraft 1.21.1 · NeoForge 21.1.252 · Java 21**

An unofficial client-side port of [VulkanMod](https://github.com/xCollateral/VulkanMod)
to **Minecraft 1.21.1 / NeoForge**, maintained by **Aviello**. It replaces Minecraft's
OpenGL renderer with Vulkan. This project is not affiliated with or endorsed by the
original VulkanMod team.

This version is rebuilt on the official VulkanMod 0.6.8 source (the `dev` branch
checkpoint `d3db079`) instead of the old 0.4.8 base. The renderer, buffer, descriptor,
staging and terrain code come from upstream; Minecraft 1.21.11's GPU API is replaced by
adapters for the real 1.21.1 shader and render-target classes. See
[SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md) for where each part comes from.

## Requirements and installation

- Minecraft **1.21.1**, NeoForge **21.1.252** and Java **21**.
- A GPU and driver supporting **Vulkan 1.2**.
- Copy `VulkanMod-1.21.1-NeoForge-0.6.8-neoforge.1-dev.jar` into the client's `mods` folder.
- Install one renderer only. Do not combine it with the original VulkanMod,
  Sodium, Embeddium, Iris or Oculus. The `-sources` JAR is not installable.

Fabric and Sinytra Connector are not required. Leave the mod off dedicated servers.
The `vulkanmod` mod ID and `config/vulkanmod_settings.json` path are kept.

## Status and compatibility

**Experimental.** The packaged smoke test passes on Windows with an NVIDIA GPU. Linux,
macOS, multiplayer and arbitrary mod or resource-pack combinations are not verified.
Shader packs are not supported, and performance gains are not promised.

## Build and verify

```text
gradlew.bat build
gradlew.bat shaderCompileCheck
gradlew.bat runClientPackagedSmoke
```

The installable and source JARs are written to `build/libs`. `shaderCompileCheck`
compiles the bundled shaders and checks descriptor bindings. `runClientPackagedSmoke`
starts a separate test client, creates its own world and requires a fresh
`SMOKE PASS` marker in the log (Vulkan startup, visible sections, resource reload,
settings, screenshot readback, Cyrillic text). The test mod is not part of the JAR.

## Support and licensing

Report problems to [this port's issue tracker](https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/issues).
Include the mod version, NeoForge version, GPU/driver and `latest.log`.

Original VulkanMod code remains credited to xCollateral and contributors. This fork
is distributed under **LGPL-3.0-only**, with the incorporated GPLv3 text in `COPYING`.
Embedded dependencies keep their licenses; see [NOTICE.md](NOTICE.md) and `licenses/`.
