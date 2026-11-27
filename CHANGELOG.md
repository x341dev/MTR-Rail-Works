# Changelog

## [0.3.0-beta.2](https://github.com/x341dev/MTR-Rail-Works/compare/v0.3.0-beta.1...v0.3.0-beta.2) (2026-11-27)


### Features

* auto slab placement in Shape Designer and debug-stick style property editing ([3aa15b5](https://github.com/x341dev/MTR-Rail-Works/commit/3aa15b516c9a7497934134061d93f168f93e1e00))
* integrate WorldEdit support for shape importing and add movement controls in Shape Designer ([bd172ca](https://github.com/x341dev/MTR-Rail-Works/commit/bd172caa958f303e5678292bab42efabdf53ca57))


### Bug Fixes

* improve block state selection logic for better matching ([05f7ad5](https://github.com/x341dev/MTR-Rail-Works/commit/05f7ad5f2f508b8e92bccfafd2fe135a5cb0065d))
* removed @NotNull giving 1.17.1 forge error ([0bde338](https://github.com/x341dev/MTR-Rail-Works/commit/0bde338b30b89db180b5f38f45cf18f4a939b06d))
* update block entry handling to correctly identify air blocks ([76d8f4e](https://github.com/x341dev/MTR-Rail-Works/commit/76d8f4e41df9641deda24d041d751da6f78da234))

## [0.3.0-beta.1](https://github.com/x341dev/MTR-Rail-Works/compare/v0.3.0-beta...v0.3.0-beta.1) (2026-11-26)


### Bug Fixes

* Forge build error in BlockShapeDesigner and prerelease suffix ([5af4a38](https://github.com/x341dev/MTR-Rail-Works/commit/5af4a38e3dd2f69a85e88e38caff3fa8b4233f6f))
* remove NotNull from BlockShapeDesigner for Forge and start prereleases at .1 ([3dae05b](https://github.com/x341dev/MTR-Rail-Works/commit/3dae05b2eb962255ada012eee8ee0c0696d5d30f))

## [0.3.0-beta](https://github.com/x341dev/MTR-Rail-Works/compare/v0.2.3...v0.3.0-beta) (2026-11-26)


### Features

* add block list with icons and mod block support to shape designer ([0bce0f5](https://github.com/x341dev/MTR-Rail-Works/commit/0bce0f5f8dbedec45d808b537ee3a1f0d86bfa97))
* add blocks integration to Shape Designer ([368eaeb](https://github.com/x341dev/MTR-Rail-Works/commit/368eaeb862ced6634ed6a38fa5afa56f3b20d5aa))
* add drag painting, block rotation and names to shape designer ([8c7858a](https://github.com/x341dev/MTR-Rail-Works/commit/8c7858aeed608fb37a5b9f8ee51c7ebf42f9c105))
* add shape designer block and 9x9 profile storage ([e5f8f52](https://github.com/x341dev/MTR-Rail-Works/commit/e5f8f52a7e59e1121bb044147fb0f4d5415773bc))
* add shape designer screen and save packet ([1a56367](https://github.com/x341dev/MTR-Rail-Works/commit/1a563673c2fcb731b5b9b9e1fcb80d314d45b2dc))
* add UI classes for better UI ([4339209](https://github.com/x341dev/MTR-Rail-Works/commit/433920983ca1e66d3f10ee8102ea526f419af6e2))
* build custom shape profiles with rail worker ([35eb844](https://github.com/x341dev/MTR-Rail-Works/commit/35eb8445f3f18df0a9f0622133195d0df017f59b))
* copy shape designer profile into rail worker ([13edecd](https://github.com/x341dev/MTR-Rail-Works/commit/13edecdfbc944f95d8187de0a8dd0d107e6a599f))
* highlight rails and nodes while holding the rail worker ([9dfd66e](https://github.com/x341dev/MTR-Rail-Works/commit/9dfd66ea668cd608a6659c35e167adeb583fc59b))


### Bug Fixes

* assign each world block to one profile column when building shapes ([f750f68](https://github.com/x341dev/MTR-Rail-Works/commit/f750f6814422c1efa1a607287a0605a2dd5c94af))
* forge errors ([4d96d2a](https://github.com/x341dev/MTR-Rail-Works/commit/4d96d2a4d0e1bb51f49dea0aacef3d38a67fa384))
* only swing the arm when the rail worker does something ([acbcb2b](https://github.com/x341dev/MTR-Rail-Works/commit/acbcb2b94e3ec9a5f7f3858cab634eb6048fc70b))
* orient shape designer blocks along the rail and mark the rail cell ([339338d](https://github.com/x341dev/MTR-Rail-Works/commit/339338d314141db4e25337c43c2f3b0ba9f687be))

## [0.2.3](https://github.com/x341dev/MTR-Rail-Works/compare/v0.2.2...v0.2.3) (2026-09-22)


### Bug Fixes

* cap client-supplied path counts in rail worker packets ([ea25113](https://github.com/x341dev/MTR-Rail-Works/commit/ea251133b339dc681c04ca3d3140b13e2e8f2e35))
* change client functions to ClientHelper on Forge's side ([2d57a4d](https://github.com/x341dev/MTR-Rail-Works/commit/2d57a4d2117e21cdb78c48a188b2aa08f623342d))
* prevent shell injection via workflow tag interpolation ([8d597d9](https://github.com/x341dev/MTR-Rail-Works/commit/8d597d9cc35723e2b4292d3ca948513ce85e3581))
* resolve Rail Worker wall side by pair position, not selection order ([12e841a](https://github.com/x341dev/MTR-Rail-Works/commit/12e841af96d84aa36dd463e19d91871cf12c585c))
* separate Rail Worker client only logic to dedicated class ([3fd56b6](https://github.com/x341dev/MTR-Rail-Works/commit/3fd56b6a9f97be094b64a562822d5c34f08e6893))

## [0.2.2](https://github.com/x341dev/MTR-Rail-Works/compare/v0.2.1...v0.2.2) (2026-07-21)


### Bug Fixes

* adjust Rail Worker configuration screen layout and checkbox behavior ([c02ad6b](https://github.com/x341dev/MTR-Rail-Works/commit/c02ad6b598126a421a934cfd814bb9bc2e7d6c0f))
* make Rail Worker non-stackable ([43ff8bf](https://github.com/x341dev/MTR-Rail-Works/commit/43ff8bfab46d76aa6f367c13b17da86e97cd9334))
* stop Rail Worker walls from racing MTR's tunnel-clear queue ([49916b3](https://github.com/x341dev/MTR-Rail-Works/commit/49916b37c52ddf1cdba51a1c56d7f2dc51e8f51a))

## [0.2.1](https://github.com/x341dev/MTR-Rail-Works/compare/v0.2.0...v0.2.1) (2026-07-20)


### Bug Fixes

* use release-please block annotation for gradle.properties and resync version ([3104d63](https://github.com/x341dev/MTR-Rail-Works/commit/3104d639bfbef539a13a9826e59a30017ce42816))

## [0.2.0](https://github.com/x341dev/MTR-Rail-Works/compare/v0.1.1...v0.2.0) (2026-07-20)


### Features

* add Rail Worker item ([66d26fc](https://github.com/x341dev/MTR-Rail-Works/commit/66d26fc18f38ef324c79d622bdb9dae729218601))
* add Rail Worker wall building action ([8b68756](https://github.com/x341dev/MTR-Rail-Works/commit/8b68756936560efbbeebda26fdeae71dfeb608c5))
* first MRW version 0.1.0 ([eb762a8](https://github.com/x341dev/MTR-Rail-Works/commit/eb762a86cc3f2edaec82d25e275689891776f4c3))
* force Rail Worker's second pair with sneak+use on a node ([7bee61f](https://github.com/x341dev/MTR-Rail-Works/commit/7bee61fea836857e97272780052a3e5926b642cd))


### Bug Fixes

* double Rail Worker screen height and anchor it to the bottom ([cb2e9ca](https://github.com/x341dev/MTR-Rail-Works/commit/cb2e9ca653e85e6837f482ad6dda6d4f67b5e132))
* give Rail Worker a custom node-modifier base class and per-item click state ([04c1c13](https://github.com/x341dev/MTR-Rail-Works/commit/04c1c1369b7330359be8af57d3c21a46437e531d))
* inverted pov polarity ([96bd37f](https://github.com/x341dev/MTR-Rail-Works/commit/96bd37f702cd279ffc440e846de70ce9f78e64fc))
* support pre-1.19.2 Forge InputEvent.MouseScrollEven ([7754962](https://github.com/x341dev/MTR-Rail-Works/commit/7754962711fc9b2d6fc3637a50b99397e5bc30bd))
* support pre-1.19.2 Forge InputEvent.MouseScrollEven ([c0759f3](https://github.com/x341dev/MTR-Rail-Works/commit/c0759f37d6b9f5a1cc2abbdbacad6d443e705091))
* use of rail vector instead of player pov for selecting side ([6a46f0f](https://github.com/x341dev/MTR-Rail-Works/commit/6a46f0f174b7746010dad8d65d2d5df9e8a16844))
