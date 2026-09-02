#!/usr/bin/env python3
"""Generate Better Ladders' 3D ladder models: vanilla's, and the rope ladder's.

Vanilla's ladder is a single plane with the ladder drawn on it, which is why a
ladder seen edge-on disappears. This is the same picture given depth: two rails
standing off the wall and four rungs between them, each face taking the part of
the vanilla texture that already shows that part - the rails from the texture's
rail columns, the rungs from its rung rows. Nothing new is drawn, so a resource
pack that retextures ladders still retextures these.

The numbers below are read off block/ladder.png rather than guessed: rails at
columns 2-3 and 12-13, rungs at rows 1-2, 5-6, 9-10 and 13-14.

The rope ladder is the same geometry wearing its own boards, so the two hang the
same way and read as the same kind of thing built out of different stuff.

Usage: python3 generate_models.py
"""

import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
LADDER = os.path.join(HERE, "src/main/resources/assets/minecraft/models/block/ladder.json")
ROPE = os.path.join(HERE, "src/main/resources/assets/better-ladders-justfatlard/models/block/rope_ladder.json")
ROPE_STATE = os.path.join(HERE, "src/main/resources/assets/better-ladders-justfatlard/blockstates/rope_ladder.json")

# Where the ladder sits in its block. Vanilla's plane is at z=15.2, hard against the wall;
# standing the rails a pixel off it is what makes the rungs readable from the side.
BACK = 16.0
RAIL_FRONT, RAIL_BACK = 13.0, 15.0
RUNG_FRONT, RUNG_BACK = 13.5, 14.5

# Rails: texture columns 2-3 and 12-13, full height.
RAILS = ((2.0, 4.0), (12.0, 14.0))

# Rungs: texture rows, converted to model y. Row r covers y from 15-r to 16-r, so a
# rung spanning rows a..b covers 15-b to 16-a.
RUNG_ROWS = ((1, 2), (5, 6), (9, 10), (13, 14))


def rung_y(rows):
    top, bottom = rows
    return 15.0 - bottom, 16.0 - top


def face(u1, v1, u2, v2):
    return {"uv": [u1, v1, u2, v2], "texture": "#texture"}


def rail(x1, x2):
    """A rail, taking its colour from the same columns of the texture all the way round."""
    return {
        "from": [x1, 0.0, RAIL_FRONT],
        "to": [x2, 16.0, RAIL_BACK],
        "shade_direction_override": "up",
        "faces": {
            # Front and back read the rail head-on, which is what the texture columns are.
            "south": face(x1, 0, x2, 16),
            "north": face(x2, 0, x1, 16),
            # The narrow sides borrow the same columns: a rail is the same wood all round,
            # and there is nowhere else in a 16x16 ladder texture to take side grain from.
            "west": face(x1, 0, x2, 16),
            "east": face(x2, 0, x1, 16),
            "up": face(x1, 0, x2, 2),
            "down": face(x1, 14, x2, 16),
        },
    }


def rung(rows):
    y1, y2 = rung_y(rows)
    v1, v2 = rows[0], rows[1] + 1

    return {
        "from": [RAILS[0][1], y1, RUNG_FRONT],
        "to": [RAILS[1][0], y2, RUNG_BACK],
        "shade_direction_override": "up",
        "faces": {
            "south": face(4, v1, 12, v2),
            "north": face(12, v1, 4, v2),
            # Seen from above and below a rung is its own depth, so the up and down faces
            # take a strip of the same rows rather than pretending to be the front.
            "up": face(4, v1, 12, v1 + 1),
            "down": face(4, v2 - 1, 12, v2),
        },
    }


def build(texture):
    """The same rails and rungs whichever ladder it is; only the boards differ."""
    elements = [rail(*bounds) for bounds in RAILS]
    elements += [rung(rows) for rows in RUNG_ROWS]

    return {
        "ambientocclusion": False,
        "textures": {"particle": texture, "texture": texture},
        "elements": elements,
    }


def blockstate(model):
    """Vanilla's own ladder blockstate, turned the same four ways."""
    turns = {"north": None, "east": 90, "south": 180, "west": 270}
    variants = {}

    for facing, y in turns.items():
        variant = {"model": model}
        if y is not None:
            variant["y"] = y
        variants["facing=" + facing] = variant

    return {"variants": variants}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=4)
        f.write("\n")
    print("wrote", path)


if __name__ == "__main__":
    write(LADDER, build("block/ladder"))
    write(ROPE, build("better-ladders-justfatlard:block/rope_ladder"))
    write(ROPE_STATE, blockstate("better-ladders-justfatlard:block/rope_ladder"))
