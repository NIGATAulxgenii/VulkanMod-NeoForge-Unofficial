# VulkanMod 0.6.0 features on native NeoForge 1.21.1

Target: `0.4.8-neoforge.7-dev`, Minecraft 1.21.1, NeoForge 21.1.252, Java 21.
Upstream reference: [tag 0.6.0](https://github.com/xCollateral/VulkanMod/releases/tag/0.6.0),
commit `8c76e02fe2458a92db5f53f8e10aa2ec6cb88782`.
This derivative retains the 0.4.8 source base and upstream LGPL attribution;
the later features are adapted backports. Aviello maintains the unofficial port.

## Feature map

| Upstream capability | Native 1.21.1 implementation | Evidence |
| --- | --- | --- |
| Vulkan rendering and frame queues | Existing NO_API renderer, native loader/window integration, frame-queue setting | Packaged startup/device/world/resize checks |
| CPU terrain backface culling and grouped draws | QuadFacing buckets, contiguous DrawParametersBuffer, merged direct/indirect batches | terrainGeometryCheck; four packaged render modes |
| Early depth tests for translucent terrain | Existing terrain_z fragment shader uses early_fragment_tests and is selected for the translucent layer | Pipeline selection, compiled shader and packaged direct/indirect scenes |
| 16-byte terrain vertices | Native builders, consumers, sorting, vertex format and shaders use 16-byte vertices | terrainLayoutCheck; shader and game checks |
| Section/entity visibility | Existing native section graph, advanced-culling and entity-culling paths retained | Graph/renderer inspection; fresh-world and negative-camera rendering |
| Custom block models and fluids | NeoForge ModelData, native model layers/AO, fluid extensions, auxiliary lights and additional section geometry | Packaged additional geometry, fluid/chest rendering; native caller inspection |
| Biome blending fixes | Main-thread biome snapshot, independent Y/resolver tint caches, spatial blending | tintCacheCheck; worker/region inspection |
| Cached fancy/fast clouds | Optimized grid faces, VBO cache/reload/close, custom dimension hook | cloudGridCheck; fancy/fast screenshots, cache/reset/reload |
| Cloud range | Separate 2-128 chunk control; cache invalidation and independent FogCloudsEnd | Packaged range/cache change and uniform supplier assertion |
| Builder threads | Live 1..CPU limit and auto selection; worker resource retirement | Packaged 1/2/auto restarts |
| Texture animation controls | Stop/resume texture ticks; skip intermediate interpolation while advancing frames | Packaged tick probe and real SpriteContents ticker |
| Release notifications | Bounded asynchronous checks for this NeoForge port's releases | updateCheckerCheck; settings link |
| Immediate sorted indices and large meshes | Streamed 16/32-bit indices, large automatic indices, safe shared-buffer replacement | streamedIndexCheck; terrainGeometryCheck; packaged sorted draws |
| Interrupted-upload memory release | Release dismissed layer/index uploads exactly once | Packaged CancelledUploadCheck |
| Global block entity destruction | Per-section ownership, clear on relocation/release and entity removal | Packaged beacon destruction |
| Shader/fog correctness | Beacon fog, position/color/normal UBO, independent cloud fog and native leash shader retained | 121 compiled shaders with descriptor/member-offset checks; beacon/leashed-entity scene |
| Texture filtering | Independent min/mag and all four mip filters; sampler keys retain all flags | Packaged real sampler-handle checks |
| Texture copies and compatibility APIs | Mip upload/readback, row lengths/skips/alignment, RGB/BGRA conversion, PBO copy/deletion, framebuffer bindings/status/rectangular copies | PixelTransferCheck and GPU pixel readback |
| Texture upload scheduling | Semaphore waits in frame submission; command buffers retire after the frame fence; explicit readback/resize drains | 360 GPU submissions with bounded pool reuse; packaged world/reload/resize |
| Grid movement allocation reduction | Reused circular lists/ranges in section and chunk-area managers | circularGridCheck; camera/grid movement |
| Window modes | Unified windowed, borderless and exclusive selector with legacy migration | Real option callbacks and GLFW monitor/decoration assertions |
| Inactivity FPS limits | Minimized/AFK, 30 FPS after one minute and 10 after ten; input resets and lower configured caps retained | inactivityLimitCheck; keyboard/mouse hooks loaded in game |
| Screenshots and post processing | Native screenshot/readback plus legacy ShaderInstance/EffectInstance conversion and Vulkan targets | Invert/creeper rendered screenshots; resource reload and cleanup |
| Resource lifetime and emergency save | Main passes, indirect/renderbuffer resources retire safely; vanilla emergency save remains enabled | Packaged shutdown and fresh-world emergency save |

## Loader and Minecraft API adaptation

The upstream tag targets Minecraft 1.21.10/Fabric. Its `render/engine` device,
command encoder, texture views, buffer slices, shader converter/manager and GUI
render-state mixins implement the newer Minecraft GPU API. On 1.21.1 the
corresponding entry points are RenderSystem, BufferUploader, VertexBuffer,
ShaderInstance, EffectInstance and RenderTarget. Their native Vulkan adapters
remain in use; newer Minecraft classes cannot be dependencies of a 1.21.1 mod.

Fabric's rendering API is replaced by NeoForge's model/fluid/section callbacks.
BiomeManager's native snapshot adapter replaces the upstream biome accessor
mixins. The native 1.21.1 model/particle/debug/screenshot interfaces remain
version-appropriate. Optimized shader compilation, retained include buffers,
module-layer window handling and other verified port fixes are preserved.
Upstream placeholder GL shader/program methods and disabled GL mipmap generation
are not treated as completed product features.

## Verification boundary

`gradlew.bat build runClientPackagedSmoke --offline --console=plain` checks the
distributable JAR in an isolated fresh world. The smoke classes/resources stay
outside the user JAR. The source archive includes all checks and the Gradle wrapper.

Optional timing: `runClientPackagedSmoke -PsmokeBenchmark=label -PsmokeBenchmarkOnly`.
`-PsmokeJar=<saved-jar>` selects an older binary for the same common timing harness.
Results identify the loaded JAR, hardware and scene under
`run-packaged-smoke/verification/benchmark-label.json`. These local wall-frame
measurements are a regression comparison, not a cross-version Fabric benchmark.
The harness waits at least 30 seconds and for two seconds of idle chunk workers,
records JIT/GC time, and disables AFK throttling during timing while retaining
minimized-window protection. It restores the original setting afterwards.

The renderer is experimental. Arbitrary modpacks, raw LWJGL calls from other
mods, shader packs, multiplayer and non-Windows platforms need their own runtime
checks. NeoForge's bootstrap LWJGL layer limits interception of direct GL calls;
the tested compatibility routes use Minecraft's native APIs. Public GL mipmap
generation remains disabled as in the reference source. This map establishes
feature coverage on the target API and records its evidence; it does not promise
identical FPS, visual output or stability on every driver/mod combination.
