#!/usr/bin/env python3
"""Builds the small Korean font the web version needs.

The browser has no Korean font of its own inside the game's canvas, so the web app ships a subset of Noto Sans KR
(SIL Open Font License) that holds only the Korean characters used in the game. Run this again after adding Korean text:

    pip install fonttools
    curl -O "https://raw.githubusercontent.com/google/fonts/main/ofl/notosanskr/NotoSansKR%5Bwght%5D.ttf"
    python3 tools/make_korean_font.py "NotoSansKR[wght].ttf"
"""
import glob
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

OUT = "web/src/wasmJsMain/composeResources/files/NotoSansKR-subset.ttf"


def korean_characters() -> set[str]:
    chars = set()
    for path in glob.glob("shared/src/**/*.kt", recursive=True):
        for ch in open(path, encoding="utf-8").read():
            if "가" <= ch <= "힣" or "ᄀ" <= ch <= "ᇿ" or "㄰" <= ch <= "㆏":
                chars.add(ch)
    return chars


def main(source: str) -> None:
    chars = korean_characters()
    font = TTFont(source)
    font = instancer.instantiateVariableFont(font, {"wght": 400})  # one regular weight is enough for a fallback
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(text="".join(sorted(chars)) + " .,·()[]-/:;!?'\"")
    subsetter.subset(font)
    font.save(OUT)
    print(f"{len(chars)} Korean characters -> {OUT}")


if __name__ == "__main__":
    main(sys.argv[1])
