"""Generates the mod's newsreel textures and colour-grade post effects.

    python tools/make_assets.py

Standard library only (Pillow isn't installed here). Everything it writes is
committed, so this only needs running again to change how they look.
"""
import json
import math
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "lawrun"
TEXTURES = ASSETS / "textures" / "gui"
POST = ASSETS / "post_effect"

# The weights vanilla's color_convolve shader uses for luma.
LUMA = (0.3, 0.59, 0.11)
# color_convolve multiplies chroma by this after the matrix; we divide it back out.
SHADER_SATURATION = 1.8

SEPIA = (
    (0.393, 0.769, 0.189),
    (0.349, 0.686, 0.168),
    (0.272, 0.534, 0.131),
)
GREY = (LUMA, LUMA, LUMA)
IDENTITY = ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0))


def write_png(path, width, height, pixel):
    """pixel(x, y) -> (r, g, b, a), each 0-255."""
    rows = bytearray()
    for y in range(height):
        rows.append(0)  # filter: none
        for x in range(width):
            rows.extend(pixel(x, y))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(bytes(rows), 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def vignette(x, y, size=256):
    """White, transparent in the middle and opaque in the corners; tinted when drawn."""
    dx = (x + 0.5) / size * 2 - 1
    dy = (y + 0.5) / size * 2 - 1
    d = math.sqrt(dx * dx + dy * dy) / math.sqrt(2)
    t = min(1.0, max(0.0, (d - 0.35) / 0.65))
    return 255, 255, 255, round(255 * t * t * (3 - 2 * t))


def grain_texture():
    rng = random.Random(1940)
    cells = [[rng.random() for _ in range(256)] for _ in range(256)]

    def pixel(x, y):
        v = cells[y][x]
        if v > 0.985:
            return 255, 255, 255, 170
        if v < 0.02:
            return 0, 0, 0, 150
        return 0, 0, 0, 0

    return pixel


def paper_texture():
    rng = random.Random(1941)
    noise = [[rng.random() for _ in range(128)] for _ in range(128)]

    def pixel(x, y):
        fibre = 6 * math.sin((x * 0.9 + y * 0.15) * 0.8) * noise[y][(x * 3) % 128]
        speck = -22 if noise[y][x] > 0.992 else 0
        base = 226 + fibre + speck + (noise[y][x] - 0.5) * 10
        v = max(0, min(255, round(base)))
        return v, round(v * 0.95), round(v * 0.84), 255

    return pixel


def blend(a, b, t):
    return tuple(tuple(a[r][c] * (1 - t) + b[r][c] * t for c in range(3)) for r in range(3))


def compensate(matrix):
    """Pre-shrink chroma so the shader's saturation boost lands on the intended colour."""
    luma_row = tuple(sum(LUMA[r] * matrix[r][c] for r in range(3)) for c in range(3))
    return tuple(
        tuple(round(luma_row[c] + (matrix[r][c] - luma_row[c]) / SHADER_SATURATION, 4) for c in range(3))
        for r in range(3)
    )


def post_effect(matrix, brightness):
    red, green, blue = compensate(matrix)
    return {
        "targets": {"swap": {}},
        "passes": [
            {
                "vertex_shader": "minecraft:core/screenquad",
                "fragment_shader": "minecraft:post/color_convolve",
                "inputs": [{"sampler_name": "In", "target": "minecraft:main"}],
                "output": "swap",
                "uniforms": {
                    "ColorConfig": [
                        {"name": "RedMatrix", "type": "vec3", "value": list(red)},
                        {"name": "GreenMatrix", "type": "vec3", "value": list(green)},
                        {"name": "BlueMatrix", "type": "vec3", "value": list(blue)},
                    ]
                },
            },
            {
                "vertex_shader": "minecraft:core/screenquad",
                "fragment_shader": "minecraft:post/blit",
                "inputs": [{"sampler_name": "In", "target": "swap"}],
                "uniforms": {
                    "BlitConfig": [
                        {"name": "ColorModulate", "type": "vec4", "value": [brightness, brightness, brightness, 1.0]}
                    ]
                },
                "output": "minecraft:main",
            },
        ],
    }


def main():
    write_png(TEXTURES / "vignette.png", 256, 256, vignette)
    write_png(TEXTURES / "grain.png", 256, 256, grain_texture())
    write_png(TEXTURES / "paper.png", 128, 128, paper_texture())

    POST.mkdir(parents=True, exist_ok=True)
    for name, target in (("sepia", SEPIA), ("grey", GREY)):
        for step in (25, 50, 75, 100):
            t = step / 100
            # A touch darker as the grade deepens, like old film stock.
            effect = post_effect(blend(IDENTITY, target, t), round(1 - 0.12 * t, 3))
            (POST / f"{name}_{step}.json").write_text(json.dumps(effect, indent=4) + "\n", encoding="utf-8")
    print("wrote", sorted(p.name for p in TEXTURES.iterdir()), sorted(p.name for p in POST.iterdir()))


if __name__ == "__main__":
    main()
