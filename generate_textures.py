#!/usr/bin/env python3
"""Generate Better Ladders' rope ladder art: the block it becomes, and the coil you carry.

Rungs in the ladder's own wood, rails in string's own pale twist, both read out
of the vanilla textures rather than written down: the item is made of a ladder
and some string, and it should look like it is.

Pure stdlib PNG reader and writer (zlib + struct) so it runs without Pillow, the
same script generated art approach as the rest of the suite. Deterministic:
re-running produces identical bytes.

Usage: python3 generate_textures.py [path/to/minecraft.jar]
"""

import glob
import os
import struct
import sys
import zipfile
import zlib
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ITEM = os.path.join(HERE, "src/main/resources/assets/better-ladders-justfatlard/textures/item/rope_ladder.png")
BLOCK = os.path.join(HERE, "src/main/resources/assets/better-ladders-justfatlard/textures/block/rope_ladder.png")

CLEAR = (0, 0, 0, 0)
_JAR = None


def minecraft_version():
    """The version this mod targets, so the sprite is cut from the same jar the
    mod is built against rather than whatever happens to be cached."""
    path = os.path.join(HERE, "gradle.properties")
    if not os.path.exists(path):
        return None
    for line in open(path):
        key, sep, value = line.partition("=")
        if sep and key.strip() == "minecraft_version":
            return value.strip()
    return None


def find_jar():
    """Loom caches the remapped Minecraft jars after a build; that is where the
    vanilla art comes from. Override with an argument or $MINECRAFT_JAR."""
    global _JAR
    if _JAR:
        return _JAR
    if len(sys.argv) > 1:
        _JAR = sys.argv[1]
        return _JAR
    if os.environ.get("MINECRAFT_JAR"):
        _JAR = os.environ["MINECRAFT_JAR"]
        return _JAR
    cache = os.path.expanduser("~/.gradle/caches/fabric-loom")
    names = ("minecraft-merged.jar", "minecraft-client.jar")
    found = []
    version = minecraft_version()
    if version:
        for name in names:
            found += glob.glob(os.path.join(cache, version, name))
    if not found:
        for name in names:
            found += glob.glob(os.path.join(cache, "*", name))
    if not found:
        sys.exit("no cached Minecraft jar found: build the mod once, "
                 "or pass a jar path as the first argument")
    _JAR = max(found, key=os.path.getmtime)
    return _JAR


def vanilla(name):
    """Read assets/minecraft/textures/<name> out of the vanilla jar."""
    with zipfile.ZipFile(find_jar()) as jar:
        return decode_png(jar.read("assets/minecraft/textures/" + name))


def decode_png(data):
    """Minimal PNG reader: no interlacing, every colour type and bit depth
    vanilla actually ships. Returns rows of RGBA tuples."""
    pos = 8
    idat = b""
    width = height = depth = ctype = None
    palette = trns = None
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if tag == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert interlace == 0, "interlaced PNG not supported"
        elif tag == b"PLTE":
            palette = body
        elif tag == b"tRNS":
            trns = body
        elif tag == b"IDAT":
            idat += body
        elif tag == b"IEND":
            break

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    stride = (width * channels * depth + 7) // 8
    step = max(1, (channels * depth) // 8)
    raw = zlib.decompress(idat)
    out = bytearray(stride * height)
    prev = bytearray(stride)
    p = 0
    for y in range(height):
        filt = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if filt == 1:
            for i in range(step, stride):
                line[i] = (line[i] + line[i - step]) & 0xFF
        elif filt == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif filt == 3:
            for i in range(stride):
                a = line[i - step] if i >= step else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif filt == 4:
            for i in range(stride):
                a = line[i - step] if i >= step else 0
                b = prev[i]
                c = prev[i - step] if i >= step else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[y * stride:(y + 1) * stride] = line
        prev = line

    pixels = []
    if depth < 8:
        per = 8 // depth
        mask = (1 << depth) - 1
        for y in range(height):
            base = y * stride
            row = []
            for x in range(width):
                i = x * channels
                value = (out[base + i // per] >> (8 - depth * (i % per + 1))) & mask
                if ctype == 3:
                    r, g, b = palette[value * 3:value * 3 + 3]
                    a = trns[value] if trns and value < len(trns) else 255
                    row.append((r, g, b, a))
                else:
                    v = value * 255 // mask
                    row.append((v, v, v, 255))
            pixels.append(row)
        return pixels

    for y in range(height):
        base = y * stride
        row = []
        for x in range(width):
            i = base + x * channels
            if ctype == 6:
                row.append(tuple(out[i:i + 4]))
            elif ctype == 2:
                row.append((out[i], out[i + 1], out[i + 2], 255))
            elif ctype == 4:
                row.append((out[i], out[i], out[i], out[i + 1]))
            elif ctype == 0:
                row.append((out[i], out[i], out[i], 255))
            else:
                r, g, b = palette[out[i] * 3:out[i] * 3 + 3]
                a = trns[out[i]] if trns and out[i] < len(trns) else 255
                row.append((r, g, b, a))
        pixels.append(row)
    return pixels


def write_png(path, pixels):
    """pixels: rows of RGBA tuples."""
    height = len(pixels)
    width = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in pixels)

    def chunk(tag, body):
        c = tag + body
        return struct.pack(">I", len(body)) + c + struct.pack(">I", zlib.crc32(c))

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
           + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("wrote %s (%dx%d)" % (path, width, height))




def tones(texture, keep=5):
    """A texture's own colours, darkest first."""
    rows = vanilla(texture)[:16]
    counts = Counter(px for row in rows for px in row if px[3])
    return sorted((px for px, _ in counts.most_common(keep)),
                  key=lambda p: 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2])


def wood():
    """Ladder timber: shadow and body."""
    shades = tones("block/ladder.png")
    return shades[0], shades[-2]


def rope():
    """String's two pale tones. Deliberately not its darkest, which is the item outline: a
    black-edged rope reads as wrought iron, and string has no dark in it at all."""
    shades = tones("item/string.png")
    return shades[-2], shades[-1]


# The rails hang, so they sway: one pixel in and back over the drop rather than ruled straight.
SWAY = (0, 0, 1, 1, 1, 0, 0, -1, -1, -1, 0, 0, 1, 1, 1, 0)

LEFT_RAIL, RIGHT_RAIL = 4, 11
RUNG_ROWS = (2, 5, 8, 11, 14)


def build_item():
    wood_shadow, wood_body = wood()
    rope_shadow, rope_light = rope()
    sprite = [[CLEAR] * 16 for _ in range(16)]

    # Rungs first, rails over them: the rope runs down the outside of the rungs it carries,
    # and drawing it second is the cheapest way to say so.
    for y in RUNG_ROWS:
        drift = SWAY[y]
        for x in range(LEFT_RAIL + drift + 1, RIGHT_RAIL + drift):
            if not (0 <= x < 16):
                continue
            sprite[y][x] = wood_body
            if y + 1 < 16:
                sprite[y + 1][x] = wood_shadow

    for y in range(16):
        drift = SWAY[y]
        for rail in (LEFT_RAIL, RIGHT_RAIL):
            x = rail + drift
            if 0 <= x < 16:
                sprite[y][x] = rope_light if y % 2 == 0 else rope_shadow

    return sprite


# The block texture is laid out exactly like vanilla's ladder, because the 3D model reads its
# rails and rungs off those same columns and rows. Rope where the timber rails were, slats where
# the rungs were: same frame, different stuff.
RAIL_COLUMNS = (2, 3, 12, 13)
RUNG_ROWS_TEX = ((1, 2), (5, 6), (9, 10), (13, 14))


def build_block():
    wood_shadow, wood_body = wood()
    rope_shadow, rope_light = rope()
    face = [[CLEAR] * 16 for _ in range(16)]

    # Rungs first, full width, so the rope drawn after runs down the outside of them
    for top, bottom in RUNG_ROWS_TEX:
        for y in range(top, bottom + 1):
            for x in range(1, 15):
                face[y][x] = wood_body if y == top else wood_shadow

    # Rope rails, twisted: the two tones alternate down the drop rather than running flat
    for y in range(16):
        for x in RAIL_COLUMNS:
            face[y][x] = rope_light if (y + x) % 2 == 0 else rope_shadow

    return face


if __name__ == "__main__":
    sprite = build_item()
    assert len(sprite) == 16 and len(sprite[0]) == 16, "item sprites are 16x16"
    write_png(ITEM, sprite)

    block = build_block()
    assert len(block) == 16 and len(block[0]) == 16, "block textures are 16x16"
    write_png(BLOCK, block)
