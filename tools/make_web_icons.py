#!/usr/bin/env python3
"""Draws the web version's icons: a blue square with Wi-Fi waves, in the game's colors.

    pip install pillow
    python3 tools/make_web_icons.py
"""
from PIL import Image, ImageDraw

OUT = "web/src/wasmJsMain/resources/"
BLUE = (27, 44, 193)       # the game's Persian blue
LIGHT = (170, 211, 251)    # the sky blue of the town
WHITE = (255, 255, 255)


def draw(size: int) -> Image.Image:
    scale = 4  # draw big, then shrink, so the edges are smooth
    n = size * scale
    img = Image.new("RGB", (n, n), BLUE)
    d = ImageDraw.Draw(img)
    cx, cy = n * 0.5, n * 0.68
    for i, radius in enumerate((0.42, 0.30, 0.18)):
        r = n * radius
        d.arc((cx - r, cy - r, cx + r, cy + r), start=225, end=315, fill=WHITE if i != 0 else LIGHT, width=int(n * 0.07))
    d.ellipse((cx - n * 0.055, cy - n * 0.055, cx + n * 0.055, cy + n * 0.055), fill=WHITE)
    return img.resize((size, size), Image.LANCZOS)


for name, size in (("apple-touch-icon.png", 180), ("icon-192.png", 192), ("icon-512.png", 512), ("favicon-48.png", 48)):
    draw(size).save(OUT + name)
    print("wrote", OUT + name)
