[![Version](https://img.shields.io/github/v/release/x341dev/MTR-Rail-Works?include_prereleases&sort=semver&label=version)](https://github.com/x341dev/MTR-Rail-Works/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/x341dev/MTR-Rail-Works/nightly.yml?label=build)](https://github.com/x341dev/MTR-Rail-Works/actions/workflows/nightly.yml)

# MTR Rail Works

[![Supports Fabric](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/fabric_vector.svg)](https://fabricmc.net/)
[![Supports Forge](https://raw.githubusercontent.com/intergrav/devins-badges/v3/assets/cozy/supported/forge_vector.svg)](https://files.minecraftforge.net/)

## Introduction

MTR Rail Works is an addon for [Minecraft Transit Railway](https://modrinth.com/mod/mtr) (MTR)
that adds extra rail construction tools MTR doesn't ship on its own, for Minecraft 1.20.1 (with
older-version support going back to 1.17.1) on both Fabric and Forge from a single shared codebase.

## Features

- **Bridge Wall Creator** — MTR ships a Tunnel Wall Creator but no bridge equivalent; this adds
  one. Builds walls along a bridge's edges, one block below rail level, in 12 height/width
  variants, with a toggleable both/left/right side mode.
- **Rail Worker** — a single configurable tool that replaces reaching for several different
  items: it builds bridge floors, tunnel excavation, and walls/ceiling together, all from one
  item. Mode, width, height, and floor/wall materials are set through its own screen (Ctrl+scroll
  while holding it switches which material slot a sneak-click saves into); node selection supports
  an optional second, mirrored pair so two parallel rail segments can be built in one batched
  operation.
- **Rail Works creative tab** — every tool this addon adds is grouped in its own tab instead of
  being folded into MTR's.
- **Multi-loader, multi-version build** — one Java source tree compiles unmodified against both
  Fabric and Forge, and against any of six supported Minecraft versions (1.17.1–1.20.4), via the
  `org.mtr.mapping.*` shim and a Manifold-preprocessor-driven build; see
  [docs/architecture.md](docs/architecture.md) for how.
- **`/mrw debug` command** — an op-only toggle that writes extra diagnostics (wall-side
  resolution, Rail Worker build parameters) to the server log when tracking down a build issue.

## Contributing

Start with [docs/](docs/) — [setup-and-building.md](docs/setup-and-building.md) covers getting a
build running locally, and [architecture.md](docs/architecture.md) covers how the codebase fits
together, including how this addon works against MTR's own (open source) code. MTR's upstream
repository and its [development wiki](https://wiki.minecrafttransitrailway.com/mtr:development)
are the right place for questions about MTR itself rather than this addon.

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`,
`fix:`, `docs:`, `refactor:`, `chore:`, ...) — this repo uses
[release-please](https://github.com/googleapis/release-please) to cut releases from them, so the
type you pick affects versioning. Open an issue or pull request on
[GitHub](https://github.com/x341dev/MTR-Rail-Works).

## License

[MIT](LICENSE.txt).
