#!/usr/bin/env python3
"""Generate a local, interactive Ore Survey palette worksheet.

The report embeds representative textures from an existing NT:NH installation,
but writes them only beneath the ignored build directory. No third-party texture
assets are copied into the project source tree.
"""

from __future__ import annotations

import argparse
import base64
import glob
import json
import math
import pathlib
import re
import subprocess
import zipfile
from dataclasses import dataclass
from typing import Dict, List, Tuple


@dataclass(frozen=True)
class TextureReference:
    name: str
    source_mod: str
    archive_pattern: str
    texture_path: str


HBM = "HBM-NTM-*.jar"
HEE = "HardcoreEnderExpansion-*.jar"
AE2 = "appliedenergistics2-*.jar"
ETFUTURUM = "etfuturum-*.jar"
IMC = "Improving Minecraft-*.jar"


TEXTURES: Tuple[TextureReference, ...] = (
    TextureReference("Adamantium", "Improving Minecraft", IMC, "assets/imc/textures/items/item_adamantium_ingot.png"),
    TextureReference("Aluminum", "HBM", HBM, "assets/hbm/textures/items/ingot_aluminium.png"),
    TextureReference("Asbestos", "HBM", HBM, "assets/hbm/textures/items/gem_asbestos.png"),
    TextureReference("Australium", "HBM", HBM, "assets/hbm/textures/items/ingot_australium.png"),
    TextureReference("Beryllium", "HBM", HBM, "assets/hbm/textures/items/ingot_beryllium.png"),
    TextureReference("Borax", "HBM", HBM, "assets/hbm/textures/items/powder_borax.png"),
    TextureReference(
        "CertusQuartz",
        "Applied Energistics 2",
        AE2,
        "assets/appliedenergistics2/textures/items/ItemMaterial.CertusQuartzCrystal.png",
    ),
    TextureReference(
        "ChargedCertusQuartz",
        "Applied Energistics 2",
        AE2,
        "assets/appliedenergistics2/textures/items/ItemMaterial.CertusQuartzCrystalCharged.png",
    ),
    TextureReference("Cinnabar", "HBM", HBM, "assets/hbm/textures/items/crystal_cinnebar.png"),
    TextureReference("Coal", "HBM", HBM, "assets/hbm/textures/items/powder_coal.png"),
    TextureReference("Cobalt", "HBM", HBM, "assets/hbm/textures/items/ingot_cobalt.png"),
    TextureReference("Coltan", "HBM", HBM, "assets/hbm/textures/items/powder_coltan.png"),
    TextureReference(
        "Conglomerate",
        "HBM",
        HBM,
        "assets/hbm/textures/blocks/stone_resource.conglomerate.png",
    ),
    TextureReference("Copper", "HBM", HBM, "assets/hbm/textures/items/ingot_copper.png"),
    TextureReference(
        "Debris",
        "Et Futurum Requiem",
        ETFUTURUM,
        "assets/minecraft/textures/blocks/ancient_debris_side.png",
    ),
    TextureReference("Diamond", "HBM", HBM, "assets/hbm/textures/items/crystal_diamond.png"),
    TextureReference("Emerald", "HBM", HBM, "assets/hbm/textures/items/powder_emerald.png"),
    TextureReference("Fluorite", "HBM", HBM, "assets/hbm/textures/items/crystal_fluorite.png"),
    TextureReference("Gold", "HBM", HBM, "assets/hbm/textures/items/powder_gold.png"),
    TextureReference(
        "HeeEndium",
        "Hardcore Ender Expansion",
        HEE,
        "assets/hardcoreenderexpansion/textures/items/endium_ingot.png",
    ),
    TextureReference(
        "HeeEndPowder",
        "Hardcore Ender Expansion",
        HEE,
        "assets/hardcoreenderexpansion/textures/items/end_powder.png",
    ),
    TextureReference(
        "HeeIgneousRock",
        "Hardcore Ender Expansion",
        HEE,
        "assets/hardcoreenderexpansion/textures/items/igneous_rock.png",
    ),
    TextureReference(
        "HeeInstabilityOrb",
        "Hardcore Ender Expansion",
        HEE,
        "assets/hardcoreenderexpansion/textures/items/instability_orb.png",
    ),
    TextureReference(
        "HeeStardust",
        "Hardcore Ender Expansion",
        HEE,
        "assets/hardcoreenderexpansion/textures/items/stardust.png",
    ),
    TextureReference("Hematite", "HBM", HBM, "assets/hbm/textures/blocks/stone_resource.hematite.png"),
    TextureReference("Iodine", "HBM", HBM, "assets/hbm/textures/items/powder_iodine.png"),
    TextureReference("Iron", "HBM", HBM, "assets/hbm/textures/items/ingot_iron.png"),
    TextureReference("Lanthanum", "HBM", HBM, "assets/hbm/textures/items/ingot_lanthanium.png"),
    TextureReference("Lapis", "HBM", HBM, "assets/hbm/textures/items/powder_lapis.png"),
    TextureReference("Lead", "HBM", HBM, "assets/hbm/textures/items/ingot_lead.png"),
    TextureReference("Lignite", "HBM", HBM, "assets/hbm/textures/items/powder_lignite.png"),
    TextureReference("Limestone", "HBM", HBM, "assets/hbm/textures/items/powder_limestone.png"),
    TextureReference("Lithium", "HBM", HBM, "assets/hbm/textures/items/powder_lithium.png"),
    TextureReference("Malachite", "HBM", HBM, "assets/hbm/textures/items/chunk_ore.malachite.png"),
    TextureReference("Molysite", "HBM", HBM, "assets/hbm/textures/items/powder_molysite.png"),
    TextureReference("Neodymium", "HBM", HBM, "assets/hbm/textures/items/ingot_neodymium.png"),
    TextureReference("Nickel", "HBM", HBM, "assets/hbm/textures/items/ingot_nickel.png"),
    TextureReference("Niobium", "HBM", HBM, "assets/hbm/textures/items/ingot_niobium.png"),
    TextureReference("Plutonium", "HBM", HBM, "assets/hbm/textures/items/ingot_plutonium.png"),
    TextureReference("Quartz", "HBM", HBM, "assets/hbm/textures/items/powder_quartz.png"),
    TextureReference("RareEarth", "HBM", HBM, "assets/hbm/textures/items/crystal_rare.png"),
    TextureReference("RedPhosphorus", "HBM", HBM, "assets/hbm/textures/items/powder_red_phosphorus.png"),
    TextureReference("Redstone", "HBM", HBM, "assets/hbm/textures/items/crystal_redstone.png"),
    TextureReference("Saltpeter", "HBM", HBM, "assets/hbm/textures/items/crystal_niter.png"),
    TextureReference("Schrabidium", "HBM", HBM, "assets/hbm/textures/items/ingot_schrabidium.png"),
    TextureReference("Sulfur", "HBM", HBM, "assets/hbm/textures/items/crystal_sulfur.png"),
    TextureReference("Thorium", "HBM", HBM, "assets/hbm/textures/items/ingot_thorium.png"),
    TextureReference("Titanium", "HBM", HBM, "assets/hbm/textures/items/ingot_titanium.png"),
    TextureReference("Tungsten", "HBM", HBM, "assets/hbm/textures/items/ingot_tungsten.png"),
    TextureReference("Uranium", "HBM", HBM, "assets/hbm/textures/items/ingot_uranium.png"),
    TextureReference("Zinc", "HBM", HBM, "assets/hbm/textures/items/ingot_zinc.png"),
    TextureReference("Zirconium", "HBM", HBM, "assets/hbm/textures/items/ingot_zirconium.png"),
)


UNKNOWN_COLOR = "#B06DD6"


def find_archive(mods_dir: pathlib.Path, pattern: str) -> pathlib.Path:
    matches = sorted(pathlib.Path(path) for path in glob.glob(str(mods_dir / pattern)))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one archive matching {pattern!r}, found {len(matches)}: {matches}")
    return matches[0]


def read_texture(archive: pathlib.Path, texture_path: str) -> bytes:
    with zipfile.ZipFile(str(archive)) as source:
        try:
            return source.read(texture_path)
        except KeyError as exception:
            raise RuntimeError(f"Missing {texture_path!r} in {archive}") from exception


def average_color(texture: bytes) -> str:
    command = (
        "magick",
        "png:-",
        "-background",
        "none",
        "-alpha",
        "on",
        "-scale",
        "1x1!",
        "-alpha",
        "off",
        "-format",
        "#%[hex:p{0,0}]",
        "info:",
    )
    result = subprocess.run(command, input=texture, capture_output=True, check=True)
    color = result.stdout.decode("ascii").strip().upper()
    if len(color) != 7 or not color.startswith("#"):
        raise RuntimeError(f"ImageMagick returned an unexpected color: {color!r}")
    return color


def rgb(color: str) -> Tuple[int, int, int]:
    return tuple(int(color[index : index + 2], 16) for index in (1, 3, 5))  # type: ignore[return-value]


def relative_luminance(color: str) -> float:
    channels = []
    for component in rgb(color):
        value = component / 255.0
        channels.append(value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4)
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def lab(color: str) -> Tuple[float, float, float]:
    linear = []
    for component in rgb(color):
        value = component / 255.0
        linear.append(value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4)
    x = (0.4124 * linear[0] + 0.3576 * linear[1] + 0.1805 * linear[2]) / 0.95047
    y = 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
    z = (0.0193 * linear[0] + 0.1192 * linear[1] + 0.9505 * linear[2]) / 1.08883

    def pivot(value: float) -> float:
        return value ** (1.0 / 3.0) if value > 0.008856 else 7.787 * value + 16.0 / 116.0

    px, py, pz = pivot(x), pivot(y), pivot(z)
    return 116.0 * py - 16.0, 500.0 * (px - py), 200.0 * (py - pz)


def color_distance(first: str, second: str) -> float:
    first_lab, second_lab = lab(first), lab(second)
    return math.sqrt(sum((left - right) ** 2 for left, right in zip(first_lab, second_lab)))


def similarity_warnings(rows: List[Dict[str, object]], threshold: float = 12.0) -> None:
    for row in rows:
        row["similar"] = []
    for index, first in enumerate(rows):
        for second in rows[index + 1 :]:
            distance = color_distance(str(first["suggested"]), str(second["suggested"]))
            if distance < threshold:
                first["similar"].append({"name": second["name"], "distance": round(distance, 1)})  # type: ignore[union-attr]
                second["similar"].append({"name": first["name"], "distance": round(distance, 1)})  # type: ignore[union-attr]


def load_current_colors() -> Dict[str, str]:
    source_file = (
        pathlib.Path(__file__).resolve().parents[1]
        / "src/main/java/gcewing/prospecting/survey/SurveyMapStyleConfig.java"
    )
    pattern = re.compile(r'colors[.]put[(]"([^"]+)", 0x([0-9A-Fa-f]{6})[)][;]')
    colors = {name: "#" + value.upper() for name, value in pattern.findall(source_file.read_text(encoding="utf-8"))}
    if not colors:
        raise RuntimeError(f"Could not read built-in colors from {source_file}")
    return colors


def build_rows(minecraft_dir: pathlib.Path) -> List[Dict[str, object]]:
    mods_dir = minecraft_dir / "mods"
    archive_cache: Dict[str, pathlib.Path] = {}
    current_colors = load_current_colors()
    rows: List[Dict[str, object]] = []
    for reference in TEXTURES:
        archive = archive_cache.setdefault(
            reference.archive_pattern,
            find_archive(mods_dir, reference.archive_pattern),
        )
        texture = read_texture(archive, reference.texture_path)
        suggested = average_color(texture)
        current = current_colors.get(reference.name)
        if current is None:
            raise RuntimeError(f"No built-in color exists for {reference.name}")
        rows.append(
            {
                "name": reference.name,
                "sourceMod": reference.source_mod,
                "texturePath": reference.texture_path,
                "icon": "data:image/png;base64," + base64.b64encode(texture).decode("ascii"),
                "suggested": suggested,
                "current": current,
                "currentDisplay": current,
                "textColor": "#111111" if relative_luminance(suggested) > 0.45 else "#FFFFFF",
            }
        )
    similarity_warnings(rows)
    return rows


def render_report(rows: List[Dict[str, object]]) -> str:
    data = json.dumps(rows, separators=(",", ":")).replace("</", "<\\/")
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>ProspectingCraft Ore Palette Worksheet</title>
<style>
:root {{ color-scheme: dark; font-family: Inter, system-ui, sans-serif; background:#11151b; color:#e9eef6; }}
* {{ box-sizing:border-box; }}
body {{ margin:0; }}
header {{ position:sticky; top:0; z-index:5; padding:18px 24px; background:#151b24ee; backdrop-filter:blur(10px); border-bottom:1px solid #303a49; }}
h1 {{ margin:0 0 5px; font-size:22px; }}
p {{ margin:5px 0; color:#b9c4d3; }}
.toolbar {{ display:flex; flex-wrap:wrap; gap:10px; margin-top:14px; }}
button,input[type=search] {{ border:1px solid #3c4859; border-radius:8px; background:#202936; color:#f4f7fb; padding:9px 12px; font:inherit; }}
button {{ cursor:pointer; }} button:hover {{ background:#2b3747; }}
input[type=search] {{ min-width:260px; }}
#count {{ align-self:center; color:#9eadc0; }}
main {{ padding:22px; }}
.legend {{ max-width:1000px; margin:0 0 18px; padding:13px 15px; border:1px solid #364253; border-radius:10px; background:#19212c; }}
.grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(310px,1fr)); gap:14px; }}
.card {{ border:1px solid #303b4b; border-radius:12px; padding:14px; background:#181f29; box-shadow:0 5px 18px #0004; }}
.card-head {{ display:grid; grid-template-columns:74px 1fr; gap:13px; align-items:center; }}
.icon-box {{ width:74px; height:74px; display:grid; place-items:center; border-radius:9px; background:#242d38; }}
.icon {{ width:64px; height:64px; object-fit:cover; object-position:top; image-rendering:pixelated; }}
h2 {{ margin:0; font-size:18px; overflow-wrap:anywhere; }}
.source {{ margin-top:4px; color:#96a6ba; font-size:13px; }}
.path {{ margin-top:5px; color:#738297; font:11px ui-monospace,monospace; overflow-wrap:anywhere; }}
.comparison {{ display:grid; grid-template-columns:1fr 1fr; gap:9px; margin-top:13px; }}
.color-box {{ min-height:58px; border-radius:8px; padding:8px; border:1px solid #ffffff24; }}
.label {{ font-size:11px; text-transform:uppercase; letter-spacing:.07em; opacity:.8; }}
.hex {{ margin-top:5px; font:600 14px ui-monospace,monospace; }}
.editor {{ display:grid; grid-template-columns:48px 1fr; gap:8px; margin-top:10px; }}
input[type=color] {{ width:48px; height:40px; padding:2px; border:1px solid #48566a; border-radius:7px; background:#202936; }}
input[type=text] {{ width:100%; border:1px solid #48566a; border-radius:7px; padding:8px; color:#f5f7fa; background:#10151c; font:14px ui-monospace,monospace; text-transform:uppercase; }}
.preview-label {{ margin:12px 0 6px; color:#9fadc0; font-size:12px; }}
.previews {{ display:grid; grid-template-columns:repeat(3,1fr); gap:5px; }}
.preview {{ height:36px; display:grid; place-items:center; border-radius:6px; color:#fff; text-shadow:0 1px 2px #000; font-size:11px; border:1px solid #ffffff26; }}
.warning {{ margin-top:10px; padding:8px; border-radius:7px; color:#ffdca3; background:#4a351b; font-size:12px; }}
.hidden {{ display:none; }}
dialog {{ max-width:min(760px,90vw); border:1px solid #475469; border-radius:12px; background:#151c25; color:#e9eef6; }}
textarea {{ width:100%; min-height:420px; color:#e9eef6; background:#0e1319; border:1px solid #3b4657; padding:12px; font:13px ui-monospace,monospace; }}
</style>
</head>
<body>
<header>
  <h1>ProspectingCraft Ore Palette Worksheet</h1>
  <p>52 canonical block ores from the live NT:NH Ore Dictionary. Suggested colors are transparency-aware texture averages and require human review.</p>
  <div class="toolbar">
    <input id="search" type="search" placeholder="Filter ore or source mod…">
    <button id="reset">Reset suggested colors</button>
    <button id="config">View config block</button>
    <button id="download">Download config snippet</button>
    <span id="count"></span>
  </div>
</header>
<main>
  <div class="legend">
    <p><strong>Current</strong> shows the mod's present built-in color; “fallback” means it currently uses purple. <strong>Suggested</strong> is calculated from the displayed texture.</p>
    <p>The three previews simulate weak, medium, and strong overlays over a green terrain color. Similarity warnings use approximate perceptual distance and are prompts for review, not automatic failures.</p>
  </div>
  <section id="grid" class="grid"></section>
</main>
<dialog id="dialog">
  <h2>Forge config block</h2>
  <p>Copy these values into the <code>surveymapcolors</code> section of <code>prospectingcraft.cfg</code>.</p>
  <textarea id="configText" spellcheck="false"></textarea>
  <div class="toolbar"><button id="copy">Copy</button><button id="close">Close</button></div>
</dialog>
<script>
const rows={data};
const chosen=Object.fromEntries(rows.map(row=>[row.name,row.suggested]));
const grid=document.getElementById('grid');
const search=document.getElementById('search');
const count=document.getElementById('count');
const escapeHtml=value=>value.replace(/[&<>\"']/g,ch=>({{'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}}[ch]));
function rgba(hex,alpha){{const n=parseInt(hex.slice(1),16);return `rgba(${{n>>16}},${{(n>>8)&255}},${{n&255}},${{alpha}})`;}}
function card(row){{
  const warning=row.similar.length?`<div class="warning">Near suggested colors: ${{row.similar.map(item=>`${{escapeHtml(item.name)}} (Δ ${{item.distance}})`).join(', ')}}</div>`:'';
  const currentLabel=row.current?'built-in':'fallback';
  return `<article class="card" data-name="${{row.name.toLowerCase()}}" data-source="${{row.sourceMod.toLowerCase()}}">
    <div class="card-head"><div class="icon-box"><img class="icon" src="${{row.icon}}" alt=""></div><div>
      <h2>${{escapeHtml(row.name)}}</h2><div class="source">${{escapeHtml(row.sourceMod)}}</div><div class="path">${{escapeHtml(row.texturePath)}}</div>
    </div></div>
    <div class="comparison">
      <div class="color-box" style="background:${{row.currentDisplay}};color:${{textFor(row.currentDisplay)}}"><div class="label">Current (${{currentLabel}})</div><div class="hex">${{row.currentDisplay}}</div></div>
      <div class="color-box suggested" style="background:${{chosen[row.name]}};color:${{textFor(chosen[row.name])}}"><div class="label">Suggested</div><div class="hex suggested-hex">${{chosen[row.name]}}</div></div>
    </div>
    <div class="editor"><input class="picker" type="color" value="${{chosen[row.name]}}" aria-label="${{row.name}} color"><input class="hex-input" type="text" value="${{chosen[row.name]}}" maxlength="7" aria-label="${{row.name}} hex color"></div>
    <div class="preview-label">Overlay preview</div><div class="previews">
      <div class="preview weak">Weak</div><div class="preview medium">Medium</div><div class="preview strong">Strong</div>
    </div>${{warning}}</article>`;
}}
function textFor(hex){{const n=parseInt(hex.slice(1),16),r=(n>>16)/255,g=((n>>8)&255)/255,b=(n&255)/255;const f=x=>x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);return .2126*f(r)+.7152*f(g)+.0722*f(b)>.45?'#111':'#fff';}}
function updateCard(element,name,color){{
  chosen[name]=color.toUpperCase();
  const box=element.querySelector('.suggested');box.style.background=chosen[name];box.style.color=textFor(chosen[name]);
  element.querySelector('.suggested-hex').textContent=chosen[name];element.querySelector('.picker').value=chosen[name];element.querySelector('.hex-input').value=chosen[name];
  element.querySelector('.weak').style.background=`linear-gradient(${{rgba(chosen[name],72/255)}},${{rgba(chosen[name],72/255)}}),#627e48`;
  element.querySelector('.medium').style.background=`linear-gradient(${{rgba(chosen[name],144/255)}},${{rgba(chosen[name],144/255)}}),#627e48`;
  element.querySelector('.strong').style.background=`linear-gradient(${{rgba(chosen[name],216/255)}},${{rgba(chosen[name],216/255)}}),#627e48`;
}}
function render(){{grid.innerHTML=rows.map(card).join('');grid.querySelectorAll('.card').forEach((element,index)=>{{const row=rows[index];updateCard(element,row.name,chosen[row.name]);
  element.querySelector('.picker').addEventListener('input',event=>updateCard(element,row.name,event.target.value));
  element.querySelector('.hex-input').addEventListener('change',event=>{{const value=event.target.value.trim().toUpperCase();if(/^#[0-9A-F]{{6}}$/.test(value))updateCard(element,row.name,value);else event.target.value=chosen[row.name];}});
}});filter();}}
function filter(){{const query=search.value.trim().toLowerCase();let visible=0;grid.querySelectorAll('.card').forEach(card=>{{const show=!query||card.dataset.name.includes(query)||card.dataset.source.includes(query);card.classList.toggle('hidden',!show);if(show)visible++;}});count.textContent=`${{visible}} of ${{rows.length}} ores`;}}
function configText(){{return 'surveymapcolors {{\\n'+rows.map(row=>`    S:${{row.name}}=${{chosen[row.name]}}`).join('\\n')+'\\n}}\\n';}}
search.addEventListener('input',filter);
document.getElementById('reset').addEventListener('click',()=>{{rows.forEach(row=>chosen[row.name]=row.suggested);render();}});
const dialog=document.getElementById('dialog'),text=document.getElementById('configText');
document.getElementById('config').addEventListener('click',()=>{{text.value=configText();dialog.showModal();}});
document.getElementById('close').addEventListener('click',()=>dialog.close());
document.getElementById('copy').addEventListener('click',()=>{{text.select();document.execCommand('copy');}});
document.getElementById('download').addEventListener('click',()=>{{const link=document.createElement('a');link.href=URL.createObjectURL(new Blob([configText()],{{type:'text/plain'}}));link.download='prospectingcraft-surveymapcolors.cfg';link.click();URL.revokeObjectURL(link.href);}});
render();
</script>
</body>
</html>
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-dir", type=pathlib.Path, required=True)
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=pathlib.Path("build/reports/ore-palette/index.html"),
    )
    return parser.parse_args()


def main() -> None:
    arguments = parse_args()
    rows = build_rows(arguments.minecraft_dir.resolve())
    if len(rows) != 52:
        raise RuntimeError(f"Expected 52 canonical palette rows, found {len(rows)}")
    report = render_report(rows)
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(report, encoding="utf-8")
    print(f"Wrote {len(rows)} palette rows to {arguments.output.resolve()}")


if __name__ == "__main__":
    main()
