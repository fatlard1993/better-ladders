# Better Ladders - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`); connecting clients
need only Pandorical. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API)
and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; the rope ladder, and shipping the ladder model |
| `RopeLadderItem.java` | A coil of ladder that unrolls itself |
| `RopeLadder.java` | Unrolling one down whatever is under it |
| `RopeLadderBlock.java` | A ladder that hangs, and what happens when it is cut |
| `mixin/LadderSupportMixin.java` | A ladder holds itself up |

## Art

`generate_models.py` cuts the 3D ladder model, and `generate_icon.py` and `generate_textures.py`
cut the icon and rope ladder sprite out of the vanilla jar. All three are deterministic; re-run
them after a Minecraft version bump.
