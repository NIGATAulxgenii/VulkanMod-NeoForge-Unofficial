# Native NeoForge port: Minecraft 1.21.1

Original source: VulkanMod 0.4.8 development branch for Fabric/Minecraft 1.21.
Rollback: restore `../VulkanMod-1.21-original.zip` into a separate directory.
Target: Minecraft 1.21.1, Java 21, NeoForge 21.1.252. No Connector or Fabric runtime.

## Ownership and migration

* Gradle/Loom and Fabric metadata: replaced by ModDevGradle, native `@Mod`,
  `neoforge.mods.toml`, access transformers, and NeoForge Jar-in-Jar libraries.
* Startup: settings/platform must initialize before Window, because NeoForge
  constructs mods after renderer startup. Retire the loader's early GL window
  on its owning thread and create a separate `GLFW_NO_API` Vulkan window.
  The loader's service layer cannot be targeted by ordinary game mixins;
  reflection is restricted to the pinned loader's early-window handoff.
* Shader registration: replace Fabric callback with `RegisterShadersEvent`;
  respect NeoForge's namespaced ShaderInstance constructor and resources.
* Fluids: use `IClientFluidTypeExtensions`, `FluidSpriteCache`, and native
  block overlay decisions, including absent optional overlay sprites.
* Block models: snapshot ModelData on the client thread, query all native
  model render layers, use the extended getQuads/AO API in workers.
  Capture neighboring block entity maps and auxiliary light managers before
  handing the region to workers.
* Additional section geometry: collect `AddSectionGeometryEvent` on the client
  thread, run its callbacks in the chunk worker through a `VertexConsumer`
  adapter, and preserve geometry even when the block section contains only air.
* Mixin targets: adapt changed constructor, particle rendering, effect
  initialization, and settings targets to actual NeoForge-patched bytecode.

## Reliability improvements found during the port

* Config writer created a directory at the JSON filename; create its parent.
* Shader resource paths assumed an unpacked filesystem and directory URL;
  use UTF-8 resource streams for JARs and development resource directories.
* shaderc include callbacks returned popped stack memory; allocate retained
  native buffers and free them in the release callback.
* Free shaderc compilation results after Vulkan copies SPIR-V; release failed
  results too, and fail clearly instead of leaving null shader pipelines.
* Scope Vulkan instance scratch memory to the driver extension count, preserving
  LWJGL's caller stack. Use heap buffers for large extension lists in mod code.
  This fixes startup with RTX 5070 drivers exposing over 64 KiB of extensions.
* Reset block origin for each render layer; do not accumulate model offsets.
* Route framebuffer destruction through Minecraft's Vulkan API. NeoForge's
  bootstrap LWJGL classes cannot rely on Fabric's ordinary GL mixin behavior.
* Make shader cleanup idempotent, deduplicate shader aliases on failure/retirement,
  and expose settings through NeoForge's Mods configuration button.
* Release effect pipelines after their in-flight frames retire. Destroy image
  views before their owning images/swapchain, and free the actual mip-level
  views instead of repeatedly destroying the main view.
* Use native handles at the GLFW/Vulkan boundary: NeoForge's bootstrap GLFW
  cannot resolve Vulkan Java classes loaded in the child game module layer.
* Keep native classifier configurations non-transitive: embedding LWJGL core
  again introduces conflicting Java module packages in the release JAR.
* Convert OpenGL projection depth to Vulkan's [0, w] range at the renderer
  boundary, including legacy shader and post-processing uniforms. JOML is a
  bootstrap library on NeoForge; its old projection mixin did not execute.
  With NeoForge's GUI projection, the white foreground of shadowed text was
  clipped while the dark shadow survived. Preserve Minecraft's matrices and
  projection/restore vertex-sorting state; remove the ineffective JOML mixin.

## Verification

`gradlew.bat build` produces the installable JAR and separate source JAR.
`gradlew.bat runClientSmoke` loads a separate development-only test mod. It checks
config persistence, shader include success/failure, NO_API window, Vulkan device,
world section compilation, screenshot pixels, native additional geometry
including an empty section, a namespaced shader through `RegisterShadersEvent`,
resource reload, white text both with and without shadows, repeated UI sounds,
and the Minecraft video-settings button. The test creates
worlds only in its separate test directory and stops its client when finished.

`gradlew.bat runClientPackagedSmoke` stages the release JAR under
`run-packaged-smoke/mods`, loads only the smoke mod from development source sets,
and obtains Vulkan/VMA/shaderc and their native resources from the release JAR.
The Gradle task requires a fresh `verification/PASS.txt` marker after the client
exits; a clean Java exit alone cannot hide a failed visual assertion. Staging
syncs the test mods directory so older versions do not remain alongside the JAR.
For a world already copied into the test saves directory, use
`gradlew.bat runClientPackagedSmoke -PsmokeWorld=CopiedWorldName`. This mode opens
that copy without constructing the synthetic test scene.

Runtime validation: Windows 10, Java 21.0.9, NVIDIA RTX 5070. Build, packaged
client startup, world rendering, transparency/fluids, a chest, native additional
geometry, shader registration/reload, window resize, settings UI and graceful shutdown passed.
Final command: `gradlew.bat build runClientPackagedSmoke --console=plain`.
Evidence is stored under `run-packaged-smoke/verification` and in
`verification-final.log` (`BUILD SUCCESSFUL`, process exit 0). The test observed
10 additional geometry callbacks and 2 namespaced shader registrations.
Linux/macOS, multiplayer, arbitrary resource packs and other mods
replacing the renderer have not been runtime-tested. No FPS improvement claim
is made without a controlled comparison.

Update `0.4.8-neoforge.2`: `build runClientPackagedSmoke` passed on Temurin
21.0.9+10, the same JVM as the reported Prism instance. The shadowed-text probe
changed from zero white pixels (maximum brightness 63) to 904 white pixels
(maximum 255); unshadowed text remained white. The copied Prism world also
opened, rendered, reloaded resources and exited normally with the instance's
settings and its fifine SC1 output device. Logs: `verification-neoforge2.log`,
`prism-world-copy.log`, `openal-world-copy.log` (local, ignored by Git).

The separately reported native exit was recorded by Windows in OpenAL.dll
1.23.1 with exception 0xc0000409. Minecraft's debug log had no Java exception,
and no retained native dump was available. Sound playback and world entry did
not reproduce that failure in the tests; this update does not claim to fix it
or replace the audio library without evidence.

## Sources used

* https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/
* https://github.com/neoforged/ModDevGradle
* NeoForge 21.1.252 generated source/bytecode in `build/moddev/artifacts`
* LWJGL 3.3.3 MemoryStack/VkInstance source (https://github.com/LWJGL/lwjgl3)
