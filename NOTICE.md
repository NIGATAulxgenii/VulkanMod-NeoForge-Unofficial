# Copyright and distribution notices

VulkanMod NeoForge (Unofficial) is a modified distribution of VulkanMod.

- Original project: xCollateral and the VulkanMod contributors,
  https://github.com/xCollateral/VulkanMod.
- NeoForge port and maintenance: Aviello,
  https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial.
- Target: Minecraft 1.21.1 / NeoForge 21.1.252. Local port history starts on
  2026-10-04; release-preparation changes are dated 2026-10-16.

The fork retains LGPL-3.0-only. `LICENSE` contains LGPLv3; `COPYING` contains
the GPLv3 terms incorporated by LGPLv3. Neither renaming nor porting transfers
copyright in the original code. Existing source attributions, including Sodium
references retained by upstream VulkanMod, are preserved.

The original team does not endorse this port and does not provide its support.
Original logos and resource namespaces retained inside the inherited source tree
do not indicate endorsement. The release metadata does not reuse the upstream logo.

## Bundled libraries

LWJGL 3.3.3 Vulkan, VMA and shaderc Java modules and their native resources are
redistributed with this mod. License texts are copied from the official LWJGL
3.3.3 tag without modification:

- `licenses/LICENSE-LWJGL.md`: LWJGL BSD-style license.
- `licenses/vma_license.txt`: Vulkan Memory Allocator, MIT.
- `licenses/shaderc_license.txt`: shaderc, Apache-2.0.
- `licenses/khronos_license.txt`: Khronos Vulkan material license.

Sources: https://github.com/LWJGL/lwjgl3/tree/3.3.3 and the individual module
license files under `modules/lwjgl/`. Additional third-party notices are retained
in `licenses/LICENSE-MoltenVK.txt`, `LICENSE-glslang.txt`,
`LICENSE-SPIRV-Tools.txt` and `LICENSE-SPIRV-Headers.txt`. The source URLs are
recorded in `licenses/SOURCES.md`. These references identify notice texts;
they do not establish the exact internal revisions of LWJGL's prebuilt natives.

Minecraft, NeoForge and the installed graphics driver are supplied separately
and retain their own terms. This project does not distribute the Minecraft client.
