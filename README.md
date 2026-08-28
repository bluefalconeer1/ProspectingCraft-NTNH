# ProspectingCraft: NTNH

ProspectingCraft: NTNH is a direct replacement for the original ProspectingCraft mod, focused on making geological surveying useful in Nuclear Tech: New Horizons.

The Geological Surveyor scans underground ore data and displays the results as a private layer on Xaero's World Map. Survey results indicate promising regions without affecting the minimap.

> This project is under active development and is not yet a finished release.

## Features

Three Surveyor tiers provide increasing accuracy:

| Surveyor | Map detail | Hover information |
| --- | --- | --- |
| Base | Broad regional readings | Dominant ore |
| Steel | More precise regional readings | Up to three ores |
| Desh | Block-accurate X/Z columns | Up to five ores |

- A selectable ore overlay on Xaero's full world map.
- Configurable tier accuracy, reading radius, colors, and opacity.

## Using the Surveyor

1. Place a Geological Surveyor and three Geophones around the survey area.
2. Supply the Surveyor with paper, gunpowder, and all four required ink colors.
3. When the machine reports `Ready`, press **Start** in its GUI or provide a new redstone signal.
4. Open Xaero's full world map and click the **Ore Survey** button to toggle the layer.
5. Shift-click the button to choose which discovered ores are displayed.

Each completed scan consumes one paper and one gunpowder. The ink containers take durability damage and can be reused. Only the player who placed or first claimed a Surveyor can open its GUI.

Unsurveyed terrain appears gray while the layer is enabled.

## Configuration

Settings are generated in `config/prospectingcraft.cfg` after the game starts once.

- `surveyAccuracy` controls cell size, reading radius, strength bands, and tooltip limits for each tier.
- `surveymapstyle` controls the unsurveyed-area color and global ore opacity.
- `surveymapcolors` assigns a hexadecimal color to each ore.

## Compatibility

- Minecraft 1.7.10
- Minecraft Forge 10.13.4.1614
- Nuclear Tech: New Horizons 2.13
- Navigator 1.1.6 or newer
- Xaero's World Map

The mod is designed for the NT:NH pack rather than as a general-purpose standalone mod. HBM content is used by the Steel and Desh progression recipes when available.

Do not install this replacement alongside the original ProspectingCraft jar.

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md), [the architecture guide](docs/ARCHITECTURE.md), [configuration reference](docs/CONFIGURATION.md), and [NT:NH pack integration guide](docs/PACK_INTEGRATION.md).
