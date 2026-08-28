# Architecture

This document explains how ProspectingCraft: NTNH scans, stores, limits, transfers, and renders survey data. It is intended for maintainers changing the Surveyor, saved-data formats, network protocol, or Navigator integration.

## System overview

The feature is split into five stages:

1. `SeismicRecorderTE` scans one X/Z row per server tick and records exact ore counts.
2. `PlayerSurveyData` stores each owner's surveys in server-side world saved data.
3. `SurveyViewResolver` converts exact stored data into the accuracy allowed by the best Surveyor tier at a coordinate.
4. The survey network sends owner-filtered 16-by-16 chunk snapshots to the client.
5. The Navigator layer renders those snapshots on Xaero's full world map.

The server is authoritative. Clients never receive the complete raw survey database.

## Survey lifecycle

`SeismicRecorderTE` owns the machine inventory, readiness checks, scan progress, and owner UUID.

A scan can start from the GUI button or a new redstone signal. The Surveyor must have an owner, at least three adequately separated Geophones, paper, gunpowder, and all four correct ink bottles. Starting a scan:

- creates a 128-by-128 `SurveyArea` centered around the Surveyor;
- records the placed Surveyor's `SurveyTier`;
- consumes one paper and one gunpowder;
- damages each ink bottle once;
- stores the active survey UUID in the tile entity; and
- performs the harmless explosion effect.

The server scans one row per tick for 128 ticks. Each column includes blocks from Y=0 up to, but not including, the Surveyor's Y coordinate. Unloaded chunks are not forced to load; their columns remain unsurveyed.

For every scanned block, `Block.damageDropped` converts placed-block metadata to a valid item metadata value before ore-dictionary lookup. This boundary is important for blocks such as Ex Nihilo infested leaves, whose placed metadata is not a valid `ItemBlock` variant.

When scanning completes, the server invalidates the affected client chunk snapshots for the owner. The map requests fresh snapshots as needed.

## Ore identification

`OreDictionaryResolver` accepts only block ore-dictionary entries and passes their names through `OreNameNormalizer`.

Normalization:

- removes the leading `ore` prefix;
- merges Nether-prefixed variants into the base material name;
- combines HBM Thorium aliases into `Thorium`; and
- excludes known non-prospecting entries such as `Volcanic`.

Each physical block contributes once to every distinct canonical ore name assigned to it. The exact per-column counts are stored even when a lower Surveyor tier will later display a coarse result. This allows rendering rules and tier settings to change without rescanning the world.

## Ownership and persistence

Survey data belongs to the player who placed or first claimed the Surveyor. GUI access is owner-only. The ownership predicate is intentionally isolated so team access can be added later without changing the saved-data model.

`PlayerSurveyData` is server-only `WorldSavedData` stored under a name derived from the owner's UUID. One player database is shared across dimensions because each `SurveyArea` records its own dimension ID.

The important persistent objects are:

- `SeismicRecorderTE`: inventory, owner, active scan UUID, progress, and readiness state.
- `PlayerSurveyData`: owner UUID, survey areas, and the player's ore-display selection.
- `SurveyArea`: tier, dimension, bounds, surveyed-column bitset, and exact ore counts.

When surveys overlap, `PlayerSurveyData.getSurveyAreaAt` selects the highest-tier surveyed area. Among overlapping areas of the same tier, the newest area wins. A lower-tier scan therefore cannot reduce an existing higher-tier result.

### Saved-data versions

`PlayerSurveyData` currently uses format 1. `SurveyAreaNbt` writes area format 4 and can read formats 2, 3, and 4:

- Format 2 predates stored tiers and is treated as Desh/exact data.
- Format 3 stores the Surveyor tier.
- Format 4 canonicalizes legacy HBM Thorium names.

The Thorium migration uses the maximum of duplicate legacy alias counts rather than summing them, because the aliases can describe the same physical block.

Never change a format number's meaning. Add a new version and migration coverage when the serialized representation changes.

## Tier-limited view

`SurveyViewResolver` produces the result that is safe to send to the owner.

For each requested map coordinate:

1. Find the highest-tier surveyed area covering that coordinate.
2. Align a cell to global X/Z coordinates using floor division.
3. Find the cell center.
4. Sum selected ores inside an inclusive square reading window centered there.
5. Sort ores by total mass and then stable name ordering.
6. Limit hover names and convert dominant mass to opacity using that tier's settings.

The reading window spans `2 * readingRadius + 1` blocks on each axis. It does not sample data outside the selected `SurveyArea`.

Default tier behavior is documented in [CONFIGURATION.md](CONFIGURATION.md). Base and Steel use density bands; Desh uses one-block cells, radius zero, and continuous opacity.

Ore selection is applied while building the server snapshot. It affects display only; it does not delete exact stored scan data.

## Network protocol and threading

`SurveyChunkNetworkProtocol` defines seven message names for chunk requests/responses, invalidation/reset, and ore-selection state.

Map data is transferred as `SurveyChunkSnapshot` objects:

- one snapshot covers a 16-by-16 Minecraft chunk;
- two bit masks distinguish unsurveyed, surveyed-empty, and ore-bearing columns; and
- ore-bearing columns contain only opacity and the tier-limited ore names.

Raw ore counts are never sent. `SurveyChunkSnapshotCodec` currently uses wire format 2 and limits each column to five ore names.

Network callbacks may run away from the main game thread. Both handlers therefore enqueue work:

- `SurveyChunkServerHandler` validates requests, limits queue size, and processes at most 16 chunk and 16 selection requests per server tick.
- `SurveyChunkClientHandler` queues received events and applies them during the client tick.

Chunk requests are restricted to the player's current dimension and bounded Minecraft chunk coordinates. Selection updates are canonicalized on the server before being saved or used.

Changing message payloads requires a new codec format, validation updates, matching client/server changes, and regression tests. The same mod build should be installed on both sides.

## Client cache and map rendering

`OreSurveyLayerManager` registers only for `SupportedMods.XaeroWorldMap`. Navigator requests an `OreSurveyChunkLocation` for visible chunks, and `SurveyChunkClientHandler` supplies or requests its snapshot.

`OreSurveyRenderStep` renders each column as follows:

- Unsurveyed: configured neutral overlay.
- Surveyed with no selected ore: transparent, revealing the normal map.
- Ore-bearing: configured dominant-ore color with server-provided density opacity and client opacity scaling.

The render step exits immediately on the minimap. The overlay is intentionally full-map only.

`OreSurveyLayerRenderer` supplies passive hover names. Normal-click toggles the layer through Navigator, while shift-click opens `OreSurveySelectionScreen`.

## Configuration boundaries

The single Forge config has server-authoritative accuracy values and client-side appearance values:

- `surveyAccuracy`: cell size, reading radius, tooltip limit, and strength calculation.
- `surveymapstyle`: unsurveyed appearance and global ore opacity.
- `surveymapcolors`: canonical ore colors and fallback colors.

See [CONFIGURATION.md](CONFIGURATION.md) for exact keys, defaults, and supported ranges.

## Compatibility constraints

The following identifiers are compatibility contracts:

- Forge mod ID: `prospectingcraft`
- Original base Surveyor registry ID: `prospectingcraft:seismic_recorder`
- Original Geophone registry ID: `prospectingcraft:geophone`
- Original hidden item and GSA registry IDs

Retired original objects remain registered, hidden from NEI, and without normal acquisition paths so older saves and quest data can still deserialize. Do not delete or rename them as routine cleanup.

Steel and Desh use the new registry IDs `seismic_recorder_steel` and `seismic_recorder_desh`.

## Common extension points

When adding or changing a Surveyor tier:

1. Preserve existing `SurveyTier.storageId` values.
2. Add a new storage ID rather than reusing one.
3. Register a distinct block and texture set.
4. Update configuration defaults and bounds.
5. Update resolution and persistence tests.
6. Validate overlapping-tier precedence in game.

When changing ore normalization:

1. Decide whether the change affects only future scans or requires saved-data migration.
2. Keep one physical block from being counted multiple times through aliases.
3. Update selection, persistence, and map-hover tests.
4. Review the default color palette and configuration documentation.
