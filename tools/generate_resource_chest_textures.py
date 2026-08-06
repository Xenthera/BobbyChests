#!/usr/bin/env python3
"""Generates the source art for fluid and energy chests.

Nothing here is baked into a per-tier chest PNG. The two window overlays are stamped onto
whatever the tier chest textures happen to be at atlas-stitch time by ChestCompositeSource,
so re-drawing a tier's chest art updates its fluid and energy variants for free, and this
script only ever needs to be re-run if the window shape itself changes.

Geometry note. The chest base is a 14x10x14 box at texture offset (0,19), which puts its
four side faces at v 33..43 and u 0/14/28/42. The window is stamped into all four, which
also sidesteps having to work out which of them is the front. Keep WINDOWS in step with
ChestWindow.java, which converts the same numbers into model-space quad bounds.

Usage:  python3 tools/generate_resource_chest_textures.py
Needs:  pip install Pillow
"""

from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parent.parent
ASSETS = REPO / "src/main/resources/assets/bobbychests/textures"

# Base side faces of the chest bottom box, as (u, v) origins of 14x10 rects.
FACE_ORIGINS = [(0, 33), (14, 33), (28, 33), (42, 33)]

# Window rect within a face, as (u_offset, v_offset, width, height).
# Fluid gets a wide port to show the liquid; energy gets a narrow meter slot.
WINDOWS = {
    "fluid": (3, 2, 8, 6),
    "energy": (4, 2, 6, 6),
}

FRAME_DARK = (24, 25, 30, 255)
FRAME_LIGHT = (104, 110, 122, 255)
MASK = (255, 255, 255, 255)
# Unlit meter track, painted straight onto the chest for the energy variant.
SLOT_DARK = (22, 24, 30, 255)

# Fluid gets a real hole cut through the wall; energy keeps its wall intact and just has a
# meter painted on it, since a charge bar has no depth worth looking into.
CUT_THROUGH = {"fluid": True, "energy": False}


def window_overlay(kind: str) -> Image.Image:
    """A 64x64 overlay: a frame around each port, and for energy the unlit meter track too.

    For fluid the inside of the frame is left untouched here and punched out by the separate
    mask below. An overlay is alpha-blended onto the chest, so a transparent pixel in it leaves
    the chest opaque and could never open a hole; erasing has to be its own step.
    """
    u_off, v_off, w, h = WINDOWS[kind]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    for face_u, face_v in FACE_ORIGINS:
        x0 = face_u + u_off
        y0 = face_v + v_off
        for dy in range(h):
            for dx in range(w):
                on_edge = dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1
                if on_edge:
                    # Lit on the top and left, dark on the bottom and right, so the frame reads
                    # as cut into the chest rather than stuck onto it.
                    lit = dy == 0 or dx == 0
                    px[x0 + dx, y0 + dy] = FRAME_LIGHT if lit else FRAME_DARK
                elif not CUT_THROUGH[kind]:
                    px[x0 + dx, y0 + dy] = SLOT_DARK
    return img


def window_cutout(kind: str) -> Image.Image:
    """Erase mask: opaque wherever the chest wall should become a real hole."""
    u_off, v_off, w, h = WINDOWS[kind]
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for face_u, face_v in FACE_ORIGINS:
        # Inset by one so the frame ring drawn by the overlay survives.
        for dy in range(1, h - 1):
            for dx in range(1, w - 1):
                px[face_u + u_off + dx, face_v + v_off + dy] = MASK
    return img


def energy_cell() -> Image.Image:
    """White 16x16 for the block-side charge bar; the colour comes from the render tint."""
    img = Image.new("RGBA", (16, 16), (255, 255, 255, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # Faint horizontal banding so a lit bar has some texture instead of reading flat.
            shade = 255 if (y % 4) else 226
            px[x, y] = (shade, shade, shade, 255)
    return img


DROPLET = [
    "...##...",
    "...##...",
    "..####..",
    "..####..",
    ".######.",
    "########",
    "########",
    "########",
    ".######.",
    "..####..",
]

BOLT = [
    "....###.",
    "...###..",
    "..###...",
    ".###....",
    ".######.",
    "...###..",
    "..###...",
    ".###....",
    "##......",
    "#.......",
]


def card(mask, fill, shadow):
    """Stamps a pictogram onto the blank upgrade card, matching the other cards' look."""
    img = Image.open(ASSETS / "item/blank_upgrade_card.png").convert("RGBA")
    px = img.load()
    origin_x = (16 - len(mask[0])) // 2
    origin_y = (16 - len(mask)) // 2
    for dy, row in enumerate(mask):
        for dx, ch in enumerate(row):
            if ch != "#":
                continue
            x, y = origin_x + dx, origin_y + dy
            # One-pixel drop shadow below-right, as the existing cards have.
            if y + 1 < 16 and px[x, y + 1][3] > 0:
                px[x, y + 1] = shadow
            px[x, y] = fill
    return img


def main():
    overlay_dir = ASSETS / "overlay/chest"
    overlay_dir.mkdir(parents=True, exist_ok=True)

    written = []
    for kind in WINDOWS:
        path = overlay_dir / f"{kind}_window.png"
        window_overlay(kind).save(path)
        written.append(path)

        if CUT_THROUGH[kind]:
            path = overlay_dir / f"{kind}_window_cutout.png"
            window_cutout(kind).save(path)
            written.append(path)

    block_dir = ASSETS / "block"
    block_dir.mkdir(parents=True, exist_ok=True)
    path = block_dir / "energy_cell.png"
    energy_cell().save(path)
    written.append(path)

    path = ASSETS / "item/fluid_upgrade_card.png"
    card(DROPLET, (58, 142, 230, 255), (26, 78, 140, 255)).save(path)
    written.append(path)

    path = ASSETS / "item/energy_upgrade_card.png"
    card(BOLT, (240, 196, 48, 255), (150, 112, 20, 255)).save(path)
    written.append(path)

    for path in written:
        print("wrote", path.relative_to(REPO))


if __name__ == "__main__":
    main()
