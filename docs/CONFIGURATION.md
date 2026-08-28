# Configuration

ProspectingCraft: NTNH creates `config/prospectingcraft.cfg` the first time the game starts with the mod installed. Stop Minecraft before editing the file, and restart the game to apply changes.

To restore every default, make a backup and then remove `prospectingcraft.cfg`. Forge will generate a fresh copy on the next start.

## Multiplayer behavior

- The server's `surveyaccuracy` settings determine how much survey detail players receive.
- Each player's `surveymapstyle` and `surveymapcolors` settings control how that data looks on their full world map.
- A single-player world uses the same local file for both.

Changing accuracy settings also changes how existing saved surveys are presented. Players do not need to scan those areas again.

## Survey accuracy

The default tier settings are:

| Tier | Cell size | Reading radius | Hover ores | Strength bands | Blocks per band |
| --- | ---: | ---: | ---: | ---: | ---: |
| Base | 24 | 16 | 1 | 3 | 8 |
| Steel | 12 | 6 | 3 | 5 | 4 |
| Desh | 1 | 0 | 5 | Continuous | Not applicable |

The corresponding keys are stored in the `surveyaccuracy` section:

- `<tier>CellSize` sets the width and length of each aligned square map cell. Valid range: `1`–`64`.
- `<tier>ReadingRadius` sets how far the reading extends from the center of a cell along X and Z. A radius of `16`, for example, samples a 33-by-33-block square. Valid range: `0`–`64`.
- `<tier>TooltipLimit` controls how many of the most abundant selected ores may appear while hovering. Valid range: `1`–`5`.
- `<tier>StrengthBands` controls the number of distinct opacity levels used by Base and Steel. Valid range: `1`–`10`.
- `<tier>BlocksPerStrengthBand` controls how much of the dominant ore is needed to advance one opacity level. Valid range: `1`–`65535`.

Desh uses one-block cells and continuous opacity by default, so it does not generate strength-band settings. Values outside the supported ranges are clamped when the game loads.

## Map appearance

The `surveymapstyle` section controls client-side presentation:

| Setting | Default | Supported range | Purpose |
| --- | ---: | ---: | --- |
| `oreOpacityPercent` | `100` | `10`–`200` | Multiplies the opacity calculated by the server. High values stop at fully opaque. |
| `unsurveyedColor` | `#777777` | Hex color | Color used for terrain that has not been surveyed. |
| `unsurveyedOpacity` | `110` | `0`–`255` | Opacity of the unsurveyed overlay. Zero hides it. |
| `unknownOreColor` | `#B06DD6` | Hex color | Fallback for a discovered ore without its own configured color. |

Client-side opacity changes cannot reveal more precise survey data than the server provides.

## Ore colors

The `surveymapcolors` section maps canonical Ore Dictionary names to colors:

```text
surveymapcolors {
    S:Coal=#171618
    S:Copper=#BA633C
    S:Iron=#999999
}
```

Colors may use `#RRGGBB` or `0xRRGGBB` notation. Entries are matched without regard to capitalization, and additional canonical ore names may be added. Invalid values are ignored, logged as warnings, and replaced by the appropriate default or fallback color.

Ore selection is separate from color configuration. On Xaero's full world map, shift-click the **Ore Survey** button to choose which ores are shown.

## Legacy debug setting

The generated `gsakit` section belongs to a hidden compatibility machine from the original mod. Leave `B:debug=false` during normal play.
