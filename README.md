# <a href="https://github.com/xCollateral/VulkanMod"> <img src="./src/main/resources/assets/vulkanmod/Vlogo.png" width="30" height="30"/> </a> VulkanMod

This fork is a native **NeoForge** port of VulkanMod for **Minecraft 1.21.1**, using **Java 21** and **NeoForge 21.1.252**. It replaces Minecraft's OpenGL renderer with Vulkan. Fabric, Sinytra Connector and translation layers are not required.

See [PORTING.md](PORTING.md) for the migration, reliability fixes and runtime verification. The upstream project links below refer to the original VulkanMod project.

### Why?
- Highly experimental project that overhauls and modernizes the internal renderer for Minecraft. <br>
- Updates the renderer from OpenGL 3.2 to Vulkan 1.2.  <br>
- Provides a potential reference for a future-proof Vulkan codebase for Minecraft Java. <br>
- Utilizes the VulkanAPI to allow for capabilities not always possible with OpenGL. <br>
- Including reduced CPU Overhead and use of newer, modern hardware capabilities. <br>

### Demonstration Video:

[![Demostration Video](http://img.youtube.com/vi/sbr7UxcAmOE/0.jpg)](https://youtu.be/sbr7UxcAmOE)

## FAQ
- Remember to check the [Wiki](https://github.com/xCollateral/VulkanMod/wiki) we wrote before asking for support!

## Installation

### Original upstream links (not this NeoForge build):

- [![CurseForge](https://cf.way2muchnoise.eu/full_635429_downloads.svg?badge_style=flat)](https://www.curseforge.com/minecraft/mc-mods/vulkanmod)

- [![Modrinth Downloads](https://img.shields.io/modrinth/dt/JYQhtZtO?logo=modrinth&label=Modrinth%20Downloads)](https://modrinth.com/mod/vulkanmod/versions)

- [![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/xCollateral/VulkanMod/total?style=flat-square&logo=github&label=Github%20Downloads)](https://github.com/xCollateral/VulkanMod/releases)

### Install guide:
1. Install Minecraft **1.21.1** with **NeoForge 21.1.252** and Java **21**.
2. Build with `gradlew.bat build`, or use the supplied `VulkanMod-1.21.1-NeoForge-0.4.8-neoforge.2.jar`.
3. Put that JAR from `build/libs` into the client's `mods` folder. Do not install the `-sources.jar`.
4. Use a Vulkan 1.2 capable GPU and driver. This is a client renderer; other mods replacing the renderer require separate compatibility checks.

Development verification: `gradlew.bat runClientSmoke`. To test the packaged mod and its embedded libraries: `gradlew.bat runClientPackagedSmoke`. These runs use separate test game directories and a test-only mod that is absent from the release JAR.

## Useful links
<table>
    <tr>
      <th> Discord server</th>
      <th> Ko-Fi</th>
    </tr>
  <tr>
    <td style="text-align:center"> 
        <a href="https://discord.gg/FVXg7AYR2Q"> 
            <img alt="Discord" align="top" src="https://img.shields.io/discord/963180553547419670?style=flat-square&logo=discord&logoColor=%23FFFFFF&label=Vulkanmod%20official%20discord%20server&labelColor=%235865F2&color=%235865F2">
        </a>
     </td>
    <td>
        <a href="https://ko-fi.com/V7V7CHHJV">
            <img alt="Static Badge" align="top" src="https://img.shields.io/badge/KoFi-%23ff5e5b?logo=ko-fi&logoColor=%23FFFFFF&link=https%3A%2F%2Fko-fi.com%2FV7V7CHHJV">
        </a>
    </td>
  </tr>
</table>


## Features

### Optimizations:
>- [x] Multiple chunk culling algorithms
>- [x] Reduced CPU overhead
>- [x] Improved GPU performance
>- [x] Indirect Draw mode (reduces CPU overhead)
>- [x] Chunk rendering optimizations

### New changes:
>- [x] Native Wayland support
>- [x] GPU selector
>- [x] Windowed fullscreen mode
>- [x] Revamped graphic settings menu
>- [x] Resizable render frame queue
>- [ ] Shader support
>- [ ] Removed Herobrine


## Notes
- This mod is still in development, please report issues in the [issue tab](https://github.com/xCollateral/VulkanMod/issues) with logs attached!
- This mode isn't just "minecraft on vulkan" (e.g: [zink](https://docs.mesa3d.org/drivers/zink.html) ), it is a full rewrite of the minecraft renderer.
