# NT:NH pack integration

ProspectingCraft: NTNH is designed as a direct jar replacement for the original ProspectingCraft mod, but an official pack update requires coordinated pack changes.

## Drop-in compatibility

The replacement preserves:

- the Forge mod ID `prospectingcraft`;
- the original Base Surveyor and Geophone registry IDs;
- hidden legacy block and item IDs needed to deserialize old saves and quest data; and
- migration support for existing survey data formats.

Existing NT:NH 2.13 test worlds load with the replacement, and the original jar must not be installed beside it.

The replacement adds a client/server map protocol. Install the same released jar on both sides.

## Required pack changes

### Mod artifact

Replace the original ProspectingCraft artifact in the pack manifest with a clean, tagged release of this project on both client and server. Do not distribute a `dirty` development jar.

Runtime requirements are Minecraft 1.7.10, Forge 10.13.4.1614, Navigator 1.1.6 or newer, and Xaero's World Map. NT:NH's HBM content supplies the Steel and Desh recipe ingredients.

### Default configuration

Ship an agreed `config/prospectingcraft.cfg` so server operators and clients begin with the pack-approved tier accuracy, opacity, and ore palette. Code defaults work without a distributed file, but packaging the config pins the balance choices for the pack.

Server-authoritative and client-appearance settings are described in [CONFIGURATION.md](CONFIGURATION.md).

### BetterQuesting

The NT:NH 2.13 quest database contains two relevant quests:

- **Geological Samples** references the retired Eyeglass, GSA Kit, Prospecting Pick, and Litmus Paper path. Remove this quest when adopting the replacement.
- **Geological Survey** still describes monochrome/color modes, redstone-only activation, a printed map item, and an output slot. Rewrite it for the GUI Start button, optional redstone activation, all four inks, three tiers, and the Xaero full-map layer. Replace its hidden `seismic_survey` icon with an active Surveyor where practical.

The Geological Survey retrieval task for a Base Surveyor and Geophone can remain if it fits the revised quest progression.

### Recipes and balance

The mod currently registers the Geophone and three-tier recipe chain directly. No ProspectingCraft CraftTweaker recipe references were found in the tested NT:NH 2.13 instance, but pack maintainers should repeat that search against the integration branch.

Pack maintainers should approve:

- Geophone: redstone torch over leather.
- Base Surveyor: Geophone, iron, feather, and wood.
- Steel Surveyor: Base Surveyor, steel, HBM Analog Circuit Board, and wood.
- Desh Surveyor: Steel Surveyor, Desh, HBM Integrated Circuit Board, and wood.

### Other pack configuration

Applied Energistics facade exclusions should cover:

- `geophone`
- `gsa_kit`
- `seismic_recorder`
- `seismic_recorder_steel`
- `seismic_recorder_desh`

The tested local pack generated all five exclusions.

## Integration validation

Before merging the pack update:

1. Confirm only the replacement jar is present.
2. Start a client and dedicated or integrated server with matching jars.
3. Load an existing world containing original Surveyor data.
4. Confirm Base, Steel, and Desh recipes in NEI.
5. Complete a scan and verify consumption, persistence, and the full-map overlay.
6. Confirm retired items are hidden and no Sow's Ear drops remain.
7. Confirm the rewritten quests contain no retired acquisition requirements or printed-map instructions.
8. Save, restart, and verify the world and survey data again.
