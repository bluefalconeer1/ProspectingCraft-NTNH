# Contributing

ProspectingCraft: NTNH targets Minecraft 1.7.10 and the Nuclear Tech: New Horizons 2.13 environment. Changes should preserve existing worlds, player-owned survey data, and the server-authoritative map model.

## Development setup

Requirements:

- A Git checkout of this repository
- A Java 8 SDK for the Minecraft/IntelliJ project
- Internet access for the first Gradle dependency download
- A separate NT:NH test instance for full-pack validation

Open the repository as a Gradle project in IntelliJ IDEA and select a Java 8 Project SDK. Let Gradle finish importing before running tasks.

The Gradle wrapper manages the build tooling. A normal verification build is:

```bash
./gradlew build
```

This checks formatting, compiles the mod, runs the automated tests, and assembles the reobfuscated jar under `build/libs/`.

To apply the configured source formatter deliberately, run:

```bash
./gradlew spotlessApply
```

The imported ProspectingCraft 1.2.2 framework retains its original wildcard imports, so Checkstyle is disabled at project level. Spotless and the automated test suite remain enabled.

## Code organization

- `gcewing.prospecting`: original framework, blocks, GUI, and Surveyor tile entity.
- `gcewing.prospecting.survey`: saved data, ore normalization, tier resolution, configuration, and snapshot models.
- `gcewing.prospecting.survey.network`: validated server requests and snapshot caching.
- `gcewing.prospecting.client.map`: Navigator/Xaero full-map integration.
- `src/test/java`: focused unit and regression tests.

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before changing persistence, networking, ownership, tier calculations, or rendering.

## Compatibility rules

- Do not change the `prospectingcraft` mod ID.
- Do not rename the original Surveyor, Geophone, or hidden compatibility registry IDs.
- Do not reuse saved-data, tier, or wire-format version numbers for new meanings.
- Keep exact survey data server-side and send only the tier-limited owner view.
- Keep the ore overlay off Xaero's minimap.
- Treat client and server jars as a matched version pair.
- Preserve the `Block.damageDropped` metadata normalization in the scanner.

If a persistent or network representation changes, add a new format version and tests for both the current format and every supported migration path.

## Testing

Run `./gradlew build` for every change. Add focused tests beside the affected subsystem.

High-risk changes also need manual testing in a copied NT:NH instance:

- existing-world load and clean shutdown;
- Surveyor readiness, consumption, and completion;
- save/reload persistence;
- overlapping Surveyor-tier precedence;
- full-map rendering, hover names, and minimap isolation;
- owner and non-owner access; and
- client/server connection when networking changes.

Never test development jars in a primary gameplay instance. Back up the world and the previously installed jar before replacement, and ensure only one ProspectingCraft jar is active.

## Pull requests

Keep commits focused and explain player-visible behavior, compatibility impact, and verification. Include configuration or pack-integration changes when behavior depends on them.

The mod repository and the NT:NH pack are separate integration surfaces. See [docs/PACK_INTEGRATION.md](docs/PACK_INTEGRATION.md) when preparing a pack update.
