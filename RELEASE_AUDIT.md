# Unofficial-port release audit

Date: 2026-10-16. Baseline: `bdf04b0` in the VulkanMod NeoForge repository.
Prepared version: `0.4.8-neoforge.4`, Minecraft 1.21.1 / NeoForge 21.1.252.

## Result

Prepared as an experimental Beta, with clear upstream credit and port ownership.
The audit used `awesome-slop-audit`, followed by a bounded
`awesome-code-cleanup` pass and a closing review of the actual changes.
It does not certify human authorship, legal compliance in every jurisdiction,
security, arbitrary modpack compatibility or hosting-platform acceptance.

The maintainer states that Aviello personally wrote a substantial part of the
port and used AI assistance. This preparation also uses AI-assisted code and
writing. Public disclosure must distinguish those contributions from the
original VulkanMod implementation.

## Scope and findings

The review covered all 232 tracked production Java files, the development smoke
test, 119 standalone vertex/fragment shaders, four GLSL includes, 59 shader JSONs,
the access transformer, metadata, README, port notes, build files and CI templates.
Generated outputs, Gradle wrapper implementation, binary images and locale data
were excluded from stylistic review. Shader JSONs were parsed in full.

The source contains old commented-out alternatives, empty Mixin Javadoc fields,
some unused internal methods and isolated stale comments. These are maintenance
findings, not evidence of AI authorship. They already existed in the baseline;
mass deletion would provide little release value and could hide useful history.

Fixed publication findings:

- README and bug-report instructions sent port users to upstream support.
  They now identify the port and route its issues to its own repository.
- Mod metadata did not identify the unofficial port or Aviello's role.
- The distribution included LGPL text without the incorporated GPLv3 text.
  Binary/source JARs now include both, project notices and library license texts.
- Upstream logo metadata was removed; upstream resource names remain unchanged.
- The old rollback path pointed to a local archive outside the repository.

Fixed misleading comments, with no Java behavior changes:

- `config/Platform.java`: removed an incorrect non-Linux/null-platform claim.
- `mixin/render/GlStateManagerM.java`: describe removing the renderbuffer
  registration, without promising GPU resource retirement.
- `render/chunk/build/light/smooth/NewSmoothLightPipeline.java`: describe
  self-occlusion in the parallel-face path instead of claiming algorithmic identity.
- `vulkan/texture/ImageUtil.java`: removed four unconditional tightly-packed
  claims from operations that accept caller-supplied strides.
- `smoke/VulkanSmoke.java`: correct which tasks can compile the test mod.
- CI: remove template narration while preserving job behavior and action versions.

Separate, regression-backed shader fixes:

- Breeze wind: shaderc reproduced an invalid matrix-to-vector assignment caused
  by a missing `vec4` constructor. The vertex shader now constructs the vector.
- Breeze wind: compiled binding 4 was absent from the JSON-derived pipeline.
  Its active lightmap sampler now uses binding 3.
- Blit screen: the compiled sampler had no matching JSON descriptor. The shader
  and metadata now declare the texture at binding 0.

Before the fixes, shaderc rejected one of 119 shaders. After correcting that
syntax, compiled-descriptor checks independently rejected the Breeze and blit
bindings. After the narrow fixes, all 119 shaders pass. `build` now runs the
development-only `shaderCompileCheck`; it is excluded from the installable JAR.

## Verification

- Baseline `gradlew.bat build --offline --console=plain`: passed.
- Prepared `gradlew.bat build --offline --console=plain`: passed, including all
  119 shader compilation/descriptor checks. No unit-test source set existed.
- The complete source ZIP was extracted to a separate directory and built
  successfully with the same command, including the 119-shader check. Existing
  Gradle dependencies and matching compilation-cache entries were reused.
- `gradlew.bat runClientPackagedSmoke --offline --console=plain`: passed with a
  fresh PASS marker, clean process exit and completed world saving.
- Hardware: Windows, NVIDIA RTX 5080, driver 617.14, Temurin 21.0.10.
- Game checks: NO_API Vulkan window/device, rendered fresh scene with fluids,
  glass, chest and additional geometry; resource reload; settings; sound;
  white foreground/shadow text and Cyrillic/Unicode glyph upload/readback.
- White text probe: plain 500, shadowed 904, Unicode 721 bright pixels;
  maximum brightness 255 for each. These are regression observations, not FPS tests.

The source contains no configured standalone linter/formatter. Compilation and
the existing packaged integration test are the relevant gates. CI configuration
was reviewed locally; hosted GitHub Actions was not executed.

## Remaining compatibility work

These are disclosed limitations or source-level leads, not silently fixed behavior:

- Public OpenGL mipmap generation is disabled in the inherited compatibility
  layer. Its unused internal helper also mixes usage flags and image-layout enums.
  Do not enable it without a focused Vulkan validation/readback test.
- Beacon-beam fragment uniforms and the position/color/normal fog UBO have
  source-layout discrepancies with their JSON metadata. Their visible effects
  were not exercised by this game scene. The shader check validates compilation
  and active binding numbers, not UBO member offsets or rendered correctness.
- Renderbuffer deletion removes a map entry without proving GPU retirement;
  texture ByteBuffer upload and framebuffer-status compatibility are incomplete.
- Unused no-op methods and a cause-dropping error path in `CircularIntList`
  remain unchanged. Removing or fixing them is separate behavior work.
- Linux/macOS, multiplayer, arbitrary resource packs, shader packs and other
  renderer replacements have not passed runtime compatibility gates.

Retained guards include native memory ownership, GLFW/loader-layer handoff,
main-thread model snapshots, Vanilla/Minecraft edge-case rationale, attribution,
and test assertions. No public identifier or resource namespace was renamed.

## Distribution

The prepared folder contains the installable JAR, source JAR, complete buildable
source ZIP, LGPL/GPL/project/dependency notices, publishing copy and SHA-256 sums.
The full-source ZIP excludes `.git`, caches, game worlds, local logs and binaries
from previous builds; it includes the Gradle wrapper, smoke checks and build files.

Full source must remain available alongside the binary. Publish the matching
repository changes before treating its public URL as the source of this release.
The original repository, history and attribution remain intact. No commit, push,
project upload or message to upstream was performed by this preparation.

Modrinth's derivative-content and AI-code/text disclosures are described in
`PUBLISHING.md`. A non-AI project icon must be supplied if a platform requires one.
The release has not been submitted to moderation.

## Repository address update - 2026-10-17

The publication repository is https://github.com/NIGATAulxgenii/VulkanMod-NeoForge-Unofficial.
Source/support links were updated after the runtime gate. The renderer and shaders
remain unchanged; the rebuilt JAR is compared entry by entry with the tested JAR,
allowing only the mod metadata and project notice to differ. Runtime evidence above
continues to refer to the 2026-10-16 test, rather than a new game test.
