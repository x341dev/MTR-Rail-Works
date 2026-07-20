# Testing

There is no automated test suite (no JUnit, no in-game test harness) — everything here is either a
compiler-enforced check or a manual in-game one.

## `compileJava` as a correctness gate

Because MTR is closed-source and this codebase leans on the `org.mtr.mapping.*` shim (see
[architecture.md](architecture.md)), the single most valuable automated check is simply making
sure both loaders compile after any change touching the `mod` package:

```
./gradlew :fabric:compileJava --console=plain
./gradlew :forge:compileJava --console=plain
```

Run **both**, not just Fabric — `forge:compileJava` depends on `forge:setupFiles`, which
regenerates the Forge copy of `dev.x341.mrw.mod.*` from the Fabric source first, so it's a real
second compilation against a *different* MTR jar (the Forge-flavored one) and, for anything that
constructs a raw loader type directly (rare, but see `ClientPlayerEntity` in
`RailWorkerClientHandler`), a different concrete type underneath the same mapping wrapper. A
change that only compiles on one loader is a real, shippable bug — this has caught real API
mismatches during development (e.g. a wrapper class expecting `MutableText` where a `Text` was
passed, a missing import causing a same-simple-name collision between MTR's `Init` and this mod's
own `Init`).

This does **not** verify runtime behavior — a screen that compiles can still render nothing, a
packet that compiles can still desync from what the server expects, and a rail-building offset
calculation that compiles can still be geometrically wrong. Compiling clean means the API usage is
type-correct against the actual jars, nothing more.

## In-game checklist

For any change touching an item, screen, packet, or the rail-building system, launch a client
(Fabric or Forge — ideally the one you changed) and check:

- **Creative tab** — Rail Works tab exists and contains the changed/added items.
- **Item tooltip** — renders, wraps sensibly, doesn't show a raw translation key (missing lang
  entry) or `null`.
- **Rail Worker specifically**:
  - Sneak + use in air opens the config screen; mode checkboxes, Replace, and the width/height
    sliders all render, are clickable, and Replace is visibly dimmed except under Walls Only.
  - Sneak-click a block saves it to whichever slot (floor/wall) is targeted; clicking the same
    block again clears it; Ctrl+scroll switches the targeted slot and only while Rail Worker is
    the held item.
  - Click a connected pair of nodes → builds immediately with the reported mode/size. Click a pair
    whose second node doesn't connect → the tool starts a second pair instead of erroring; walk to
    the far end and click both remaining nodes → both segments build in one action-bar progress
    counter.
  - The item's texture changes as you click (none → pair 1 → pair 2) and reverts to none once a
    build fires, the config screen is opened, or you switch away from the item mid-selection.
  - Walls actually build on **one side only** (unless Walls + Ceiling is selected with sufficient
    height) — this is the thing most likely to silently regress, since `WallSide.BOTH` compiles
    fine but builds every edge instead of one (see architecture.md's note on `isSelectedEdge`).
- **Multiplayer-shaped testing when touching packets** — a single-player world still round-trips
  packets through a local integrated server, which is enough to catch most desyncs (e.g. NBT the
  client assumes vs. what the server actually validates), but a real dedicated server is worth a
  spot-check for anything permission- or op-related (like the `/mrw debug` command).

## `/mrw debug on` / `/mrw debug off`

An op-only command (`REGISTRY.registerCommand`, permission level 2) that flips a runtime flag
(`MrwDebug.isEnabled()`) without needing a rebuild. When on, extra detail is written to the
**server log/console** (not chat) at the points most likely to need diagnosing:

- Wall-side resolution (`RailActionMrw.isRailAgainstNodeOffset`): the two clicked node positions,
  the rail's tangent direction, the computed offset, and the resulting `invertWallSide`.
- Rail Worker build packets (`PacketApplyRailWorkerBuild.runServer`): resolved mode/replace/
  width/height/sidesOnly/anyWalls and both pairs' endpoints and segment counts.

Reach for this before adding new one-off `System.out`/chat debug prints — it's already wired up
and gated, so add new detail there instead if you're chasing something in the same area
(wall-building geometry, Rail Worker's click resolution). Anything added should stay off by
default and cost nothing (or next to nothing) when disabled — guard it with
`MrwDebug.isEnabled()` the same way the existing call sites do, not with a separate flag.
