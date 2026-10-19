# Publishing the unofficial port

## Project fields

- Name: **VulkanMod NeoForge (Unofficial)**
- Summary: **Unofficial Vulkan renderer port for Minecraft 1.21.1 on NeoForge.**
- Maintainer: **Aviello**
- License: **LGPL-3.0-only**
- Minecraft: **1.21.1**; loader: **NeoForge**; environment: **client required, server unsupported**
- Initial release channel: **Beta**
- Source: https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial
- Issues: https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial/issues
- Original: https://github.com/xCollateral/VulkanMod

## English description

VulkanMod NeoForge (Unofficial) brings VulkanMod's Vulkan renderer to Minecraft
1.21.1 on NeoForge. The original project is by xCollateral and contributors;
this unofficial port is maintained by Aviello and is not endorsed by the original team.

The port uses native NeoForge startup and rendering integration. Fabric and
Sinytra Connector are not required. It includes NeoForge model-data, fluid,
additional section-geometry and shader-registration integration, plus fixes for
projection depth, native resource handling and Cyrillic/Unicode text uploads.

Install Java 21 and NeoForge 21.1.252, then place the main mod JAR in the client's
mods folder. A Vulkan 1.2-capable GPU and driver are required. Do not install
the source JAR or combine this port with another renderer replacement such as
the original VulkanMod, Avielix, Sodium, Embeddium, Iris or Oculus.

This is an experimental port. Historical game verification covers Windows on
NVIDIA hardware. Other operating systems, multiplayer, resource packs and mod
combinations need separate testing. Some OpenGL compatibility functions are
incomplete; no shader-pack compatibility or FPS improvement is promised.

Report port bugs through this project's issue tracker. The original VulkanMod
team is not responsible for support. The fork remains LGPL-3.0-only; matching
source code and build files accompany each release.

## Disclosures and assets

- Modrinth: enable **Contains Derivative Content** and credit/link upstream.
- Enable **AI-generated text** for these AI-assisted publication materials and
  **AI-generated code** to disclose development assistance transparently.
- Maintainer statement (2026-10-16): Aviello personally wrote a substantial part
  of the port; AI tools assisted. This is a statement of provenance from the
  maintainer, not a conclusion drawn from code style. Attribute the original
  VulkanMod implementation separately to its authors.
- Modrinth judges AI use in a fork's new contributions, not the human-written
  upstream code. A primarily AI-generated port may be ineligible for public
  publication even when its license permits distribution. Disclosure alone
  does not override that restriction; describe the actual human and AI roles
  accurately if moderation asks for clarification.
- Use actual game screenshots. Supply your own non-AI project icon if required;
  do not reuse the upstream logo as the port's identity or upload an AI-generated icon.
- Keep the independent Avielix implementation on its own project page.

## Release files and checks

Upload the main `VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.7-dev.jar` as the mod.
The `-sources.jar` is an optional source attachment, not an installable mod.
Keep the full `-source.zip` available alongside the binary: it includes the
Gradle wrapper, build configuration, source, resources and license notices.
The full-source ZIP is the buildable source delivery; the source JAR alone is
not a replacement for it. Publish the matching repository changes before
claiming that the source link contains this version.

Before submission, inspect `SHA256SUMS.txt`, the release audit and the fresh
build/packaged-smoke results. A successful local build does not guarantee
platform acceptance or compatibility with arbitrary modpacks.

Policies checked during preparation on 2026-10-16:

- https://modrinth.com/legal/rules
- https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai
- https://support.modrinth.com/en/articles/16567675-content-disclosures
- https://support.curseforge.com/support/solutions/articles/9000197279

Publishing this repository does not submit the mod to Modrinth or CurseForge, or contact upstream.
