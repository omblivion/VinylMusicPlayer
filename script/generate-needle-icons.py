#!/usr/bin/env python3
"""Regenerate launcher assets from the supplied Needle artwork (requires Pillow >= 12.1)."""

from pathlib import Path
from PIL import Image


def main():
    root = Path(__file__).resolve().parent.parent
    source = Image.open(root / "art" / "Vinyl Turntable App Icon.png").convert("RGBA")
    # The supplied artwork has a nearly black matte, not transparency.
    source.putdata([
        (r, g, b, 0 if max(r, g, b) < 20 else a)
        for r, g, b, a in source.get_flattened_data()
    ])
    monochrome = source.copy()
    # The golden needle is recognizable on its own in themed launcher icons.
    monochrome.putdata([
        (255, 255, 255, a if r > 180 and g > 85 and b < 100 else 0)
        for r, g, b, a in source.get_flattened_data()
    ])
    for density, scale in {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
        directory = root / "app" / "src" / "main" / "res" / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        size = round(48 * scale)
        legacy = source.resize((size, size), Image.Resampling.LANCZOS)
        legacy.save(directory / "ic_launcher.png")
        legacy.save(directory / "ic_launcher_round.png")
        for name, artwork in [("foreground", source), ("monochrome", monochrome)]:
            canvas_size, content_size = round(108 * scale), round(72 * scale)
            canvas = Image.new("RGBA", (canvas_size, canvas_size))
            offset = (canvas_size - content_size) // 2
            canvas.alpha_composite(artwork.resize((content_size, content_size), Image.Resampling.LANCZOS), (offset, offset))
            canvas.save(directory / f"ic_launcher_{name}.png")


if __name__ == "__main__":
    main()
