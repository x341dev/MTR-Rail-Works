# Architecture

## The mapping shim

MTR ships its own cross-loader compatibility layer, `org.mtr.mapping.*`, and this addon only ever
references that layer — never raw `net.minecraft.*` (Yarn-mapped on Fabric, Mojmap-named on
Forge) and never a loader API directly, with two narrow, deliberate exceptions (see
[Loader-specific code](#loader-specific-code) below). That's what lets one Java source file
compile unmodified against both the Fabric-flavored and Forge-flavored MTR jars.

Two package families matter:

- **`org.mtr.mapping.holder.*`** — thin wrapper types around the real Minecraft classes. Every
  wrapper follows the `HolderBase<T>` pattern: a public `data` field holding the raw
  loader-specific object, and a constructor taking that raw type. You unwrap with `.data` (e.g. to
  check `instanceof` against a raw MTR class like `BlockNode`) and wrap by constructing the holder
  around a raw instance.
- **`org.mtr.mapping.mapper.*`** — base classes you *extend* to hook into vanilla behavior
  (`ItemExtension`, `ScreenExtension`, `CheckboxWidgetExtension`, ...). Method names here are
  suffixed `2` (`useOnBlock2`, `onClose2`, `render(...)`) because the mapper class also carries a
  `final` bridge method under the loader's real (obfuscated/intermediary) name that forwards into
  your override — you never call or override the bridge method itself.

MTR itself is open source, but the jars this addon depends on (pulled from Modrinth's Maven — see
[setup-and-building.md](setup-and-building.md)) don't ship a sources jar. When you need an exact
method signature or constructor argument order that isn't already used elsewhere in this codebase,
see [Working against MTR's own source](#working-against-mtrs-own-source).

## The Fabric / Forge split

`fabric/src/main/java/dev/x341/mrw/mod/**` is the single source of truth for all shared logic —
items, packets, screens, the rail-building system, registries. The Forge subproject's
`setupFiles` Gradle task deletes and re-copies that entire `mod` package (plus the `assets` and
`data` resource trees) into `forge/src/main/java/dev/x341/mrw/mod/**` before compiling. Those
Forge-side copies are gitignored and must never be hand-edited — edit the Fabric originals.

Two things live *outside* the `mod` package specifically so `setupFiles` never touches them:

- **Loader bootstrap classes**: `fabric/.../MRWFabric.java`, `fabric/.../MRWFabricClient.java`,
  `forge/.../MRWForge.java`. These just call into the shared `Init.init()` /
  `InitClient.init()` and register any loader-specific glue.
- **Loader-specific code** (see below).

### Loader-specific code

The "never touch a loader API directly" rule has two exceptions, both isolated in their own files
outside the `mod` package:

- **`fabric/.../mixin/MouseScrollMixin.java`** — Fabric API has no scroll event, so intercepting
  scroll-while-holding-Ctrl (Rail Worker's floor/wall target-slot cycling) requires a genuine
  mixin into vanilla `net.minecraft.client.Mouse#onMouseScroll`, Yarn-mapped, with `remap`
  defaulting to `true` (every other mixin in this repo targets MTR's own unobfuscated classes and
  sets `remap = false`).
- **`forge/.../RailWorkerClientHandler.java`** — Forge's equivalent. Forge already exposes both
  scrolling (`InputEvent.MouseScrollingEvent`) and client ticking (`TickEvent.ClientTickEvent`) as
  ordinary events, so no mixin is needed there at all — just an `@SubscribeEvent`-annotated class
  registered on `MinecraftForge.EVENT_BUS` from `MRWForge`'s client-only branch.

Every other mixin in the repo (under `dev.x341.mrw.mod.mixin`, copied to both loaders) targets
MTR's own decompiled classes with `remap = false`, since MTR ships unobfuscated.

### Manifold preprocessor

Root `build.gradle` wires in the Manifold preprocessor so source can carry version/loader
directives inline, e.g.:

```java
#if MC_VERSION >= "11903"
    // 1.19.3+ only
#endif
#if LOADER == "forge"
    // Forge only
#endif
```

`MC_VERSION` is a fixed-width merged version string (`1.20.1` → `"12001"`) so plain string
comparison matches numeric ordering. The IntelliJ "Manifold" plugin is needed for the IDE to
understand these directives; without it they still compile correctly, just without inline
graying-out of inactive branches.

## Core systems

- **`Init` / `InitClient`** — the shared entry points, called once from each loader's bootstrap.
  `Init.init()` registers items, packets, the debug command, and the end-of-tick hook that drives
  `RailActionModuleMrw`. `InitClient.init()` registers client-only things: item model predicates,
  packet setup.
- **Packets** — MTR's own `org.mtr.mapping.registry.Registry` / `PacketHandler` abstraction, not
  vanilla `CustomPacketPayload`. Every packet class is registered once in `Init.init()` via
  `REGISTRY.registerPacket(Class, ConstructorRef)`. See `PacketApplyRailWorkerBuild` for the
  fullest example (client → server, with an optional/nullable second payload).
- **Rail building** — two parallel systems:
  - MTR's own native `org.mtr.mod.data.RailAction` / `RailActionModule` covers bridge floors
    (`markRailForBridge`) and tunnel excavation (`markRailForTunnel`, air only, no floor material
    placed).
  - This mod's own `RailActionMrw` / `RailActionModuleMrw` / `RailActionType` /
    `RailActionBatchTracker` reimplements wall-building, since MTR's native version can't do
    bridge walls, single-sided walls, or Rail Worker's walls/ceiling mode.
    `RailActionBatchTracker` unifies the "(n/total)" progress action-bar message across *both*
    systems (MTR's native actions via `RailActionMixin`/`RailActionModuleMixin`, and this mod's
    own via direct calls), so a batched multi-segment operation shows one consistent counter
    regardless of which system is doing the placing.
  - **Wall side**: `RailActionMrw` decides which single edge to build (when not `WallSide.BOTH`)
    from a reference direction dotted against the rail's tangent — either the player's facing
    (`isRailAgainstPlayerFacing`, used by the manually-toggled Bridge/Tunnel Wall Creators) or,
    for Rail Worker, the raw coordinate offset between the two nodes clicked for that pair
    (`isRailAgainstNodeOffset`, since Rail Worker's pairs can be built away from where the player
    is standing). **Passing `WallSide.BOTH` disables this entirely** — the "which edge" check in
    `RailActionMrw.create()` short-circuits true for `BOTH`, so every edge gets built regardless of
    the computed direction. Anything that wants single-sided walls must pass `LEFT` or `RIGHT` as
    the nominal side and let `invertWallSide` flip which physical side that resolves to.
- **Rail Worker** (`ItemRailWorker`) — the fullest worked example of most of the above at once:
  extends this mod's own `ItemNodeModifierSelectableBlockBase` (not MTR's — that one assumes a
  single saved block and a single 2-click connect flow, neither of which fits Rail Worker), which
  guarantees a plain click only ever reaches `onNodeClick` when it lands on an actual rail node.
  All state — mode bitmask, width/height, floor/wall block, target slot, *and* the 4-click
  dual-pair click-progress state machine — lives in this specific item stack's own NBT, exactly
  like MTR's own node-clicking items store their `TAG_POS`, so two Rail Worker stacks never bleed
  progress into each other. Node clicks resolve BFS rail paths (`RailPathFinder`) between clicked
  positions; a single batched packet (`PacketApplyRailWorkerBuild`) that reads mode/size/blocks
  from the item's own already-synced NBT rather than trusting anything the packet claims; and a
  3-state item model override (none / pair 1 / pair 2 selected) computed from that same NBT.
- **Debug logging** — `MrwDebug.isEnabled()`, toggled by the `/mrw debug on|off` command
  (registered in `Init.init()` via `REGISTRY.registerCommand`, op-only). When enabled, extra
  detail (wall-side resolution inputs/outputs, Rail Worker build parameters) is written to
  `Init.LOGGER` — check the server log/console, not chat.

## Working against MTR's own source

MTR is open source: [Minecraft-Transit-Railway/Minecraft-Transit-Railway](https://github.com/Minecraft-Transit-Railway/Minecraft-Transit-Railway/tree/master)
on GitHub, with its own developer documentation on the
[MTR wiki](https://wiki.minecrafttransitrailway.com/mtr:development). That's the first place to
look when you need to understand *why* something behaves a certain way, not just its signature —
reading the real source beats guessing from bytecode every time it's available. Match the branch
or tag to the `mtrVersion` this addon is pinned to (see `gradle.properties`) before trusting what
you read, since `master` can be ahead of the version actually on the classpath.

The one thing GitHub source can't give you is a guarantee that what you're reading matches the
*exact* jar this build resolves — Modrinth's Maven publishes jars without a sources jar attached,
so for anything version-sensitive (an exact method signature, a constructor argument order, which
overload actually exists on the jar in `~/.gradle`), decompile the real cached jar with `javap`
rather than trusting the GitHub source alone:

```
# Find the cached jar (both loader flavors exist per Minecraft version)
find ~/.gradle/caches/modules-2/files-2.1/maven.modrinth/minecraft-transit-railway -iname "*FABRIC*1.20.1*.jar"

# Inspect a class's public/protected/package-private API
mkdir -p /tmp/mtr-inspect && cd /tmp/mtr-inspect
jar xf <path-to-jar> org/mtr/mod/item/ItemNodeModifierBase.class
javap -p -classpath . org.mtr.mod.item.ItemNodeModifierBase

# See method bodies (bytecode) when the signature alone doesn't answer "what does this do"
javap -c -p -classpath . org.mtr.mod.item.ItemNodeModifierBase
```

Do this for both the Fabric and Forge flavored jars when a type's raw constructor argument differs
by loader (it sometimes does — e.g. `ClientPlayerEntity`'s wrapped type is `net.minecraft.class_746`
on Fabric/Yarn but `net.minecraft.client.player.LocalPlayer` on Forge/Mojmap). Also check the
Yarn-mapped vanilla client jar under `~/.gradle/caches/fabric-loom/minecraftMaven/...` the same way
when a mixin needs to target real vanilla code.
