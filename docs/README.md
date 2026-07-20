# MTR Rail Works — docs

MTR Rail Works is a [Minecraft Transit Railway](https://modrinth.com/mod/mtr) (MTR) addon for
Minecraft 1.20.1, shipping for both Fabric and Forge from one shared codebase.

- [setup-and-building.md](setup-and-building.md) — getting the project running locally, the
  multi-loader/multi-version Gradle build, and how to produce a jar.
- [architecture.md](architecture.md) — how the codebase is organized, the mapping shim that makes
  one source file compile for both loaders, and how the major systems (items, packets, rail
  building, screens) fit together.
- [testing.md](testing.md) — how to verify a change, since there is no automated test suite: what
  `compileJava` does and doesn't catch, the in-game checklist, and the debug command.
