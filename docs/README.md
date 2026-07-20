# MTR Rail Works — docs

MTR Rail Works is a [Minecraft Transit Railway](https://modrinth.com/mod/mtr) (MTR) addon for
Minecraft 1.20.1, shipping for both Fabric and Forge from one shared codebase.

- [setup-and-building.md](setup-and-building.md) — getting the project running locally, the
  multi-loader/multi-version Gradle build, and how to produce a jar.
- [architecture.md](architecture.md) — how the codebase is organized, the mapping shim that makes
  one source file compile for both loaders, and how the major systems (items, packets, rail
  building, screens) fit together.

## MTR itself

MTR is open source: [Minecraft-Transit-Railway/Minecraft-Transit-Railway](https://github.com/Minecraft-Transit-Railway/Minecraft-Transit-Railway/tree/master)
on GitHub, with its own development documentation on the
[MTR wiki](https://wiki.minecrafttransitrailway.com/mtr:development). Reach for those first when
you need to understand *why* MTR behaves a certain way, not just *what* its API looks like — see
[architecture.md](architecture.md#working-against-mtrs-own-source) for how the two fit together
with this addon's own decompile-based verification workflow.
