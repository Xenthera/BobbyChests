#!/usr/bin/env python3
"""Move tier chest types into subpackages; update package lines and cross-tier imports."""
from __future__ import annotations

import os

ROOT = os.path.join("src", "main", "java", "com", "bobby", "bobbychests")

MOVES: list[tuple[str, str, str]] = [
    ("block", "dirt", "DirtChestBlock"),
    ("block", "wooden", "WoodenChestBlock"),
    ("block", "copper", "CopperChestBlock"),
    ("block", "iron", "IronChestBlock"),
    ("block", "gold", "GoldChestBlock"),
    ("block", "diamond", "DiamondChestBlock"),
    ("block", "emerald", "EmeraldChestBlock"),
    ("blockentity", "dirt", "DirtChestBlockEntity"),
    ("blockentity", "wooden", "WoodenChestBlockEntity"),
    ("blockentity", "copper", "CopperChestBlockEntity"),
    ("blockentity", "iron", "IronChestBlockEntity"),
    ("blockentity", "gold", "GoldChestBlockEntity"),
    ("blockentity", "diamond", "DiamondChestBlockEntity"),
    ("blockentity", "emerald", "EmeraldChestBlockEntity"),
    ("menu", "dirt", "DirtChestMenu"),
    ("menu", "dirt", "DirtChestMenuProvider"),
    ("menu", "wooden", "WoodenChestMenu"),
    ("menu", "wooden", "WoodenChestMenuProvider"),
    ("menu", "copper", "CopperChestMenu"),
    ("menu", "copper", "CopperChestMenuProvider"),
    ("menu", "iron", "IronChestMenu"),
    ("menu", "iron", "IronChestMenuProvider"),
    ("menu", "gold", "GoldChestMenu"),
    ("menu", "gold", "GoldChestMenuProvider"),
    ("menu", "diamond", "DiamondChestMenu"),
    ("menu", "diamond", "DiamondChestMenuProvider"),
    ("menu", "emerald", "EmeraldChestMenu"),
    ("menu", "emerald", "EmeraldChestMenuProvider"),
    ("client/screen", "dirt", "DirtChestScreen"),
    ("client/screen", "wooden", "WoodenChestScreen"),
    ("client/screen", "copper", "CopperChestScreen"),
    ("client/screen", "iron", "IronChestScreen"),
    ("client/screen", "gold", "GoldChestScreen"),
    ("client/screen", "diamond", "DiamondChestScreen"),
    ("client/screen", "emerald", "EmeraldChestScreen"),
    ("client/render", "dirt", "DirtChestRenderer"),
    ("client/render", "wooden", "WoodenChestRenderer"),
    ("client/render", "copper", "CopperChestRenderer"),
    ("client/render", "iron", "IronChestRenderer"),
    ("client/render", "gold", "GoldChestRenderer"),
    ("client/render", "diamond", "DiamondChestRenderer"),
    ("client/render", "emerald", "EmeraldChestRenderer"),
]

ROWS = [
    ("dirt", "Dirt"),
    ("wooden", "Wooden"),
    ("copper", "Copper"),
    ("iron", "Iron"),
    ("gold", "Gold"),
    ("diamond", "Diamond"),
    ("emerald", "Emerald"),
]


def fix_tier_imports(txt: str) -> str:
    for tier_folder, prefix in ROWS:
        for layer, cls in [
            ("block", f"{prefix}ChestBlock"),
            ("blockentity", f"{prefix}ChestBlockEntity"),
            ("menu", f"{prefix}ChestMenu"),
            ("menu", f"{prefix}ChestMenuProvider"),
            ("client.screen", f"{prefix}ChestScreen"),
            ("client.render", f"{prefix}ChestRenderer"),
        ]:
            old = f"import com.bobby.bobbychests.{layer}.{cls};"
            new = f"import com.bobby.bobbychests.{layer}.{tier_folder}.{cls};"
            txt = txt.replace(old, new)
    return txt


def insert_after_package(txt: str, lines: list[str]) -> str:
    lines = [ln for ln in lines if ln and ln not in txt]
    if not lines:
        return txt
    first_nl = txt.find("\n")
    if first_nl == -1:
        return txt
    return txt[: first_nl + 1] + "\n" + "\n".join(lines) + "\n" + txt[first_nl + 1 :]


def ensure_parent_imports(txt: str, sub: str, stem: str) -> str:
    need: list[str] = []
    if sub == "block":
        need.append("import com.bobby.bobbychests.block.AbstractTieredChestBlock;")
    elif sub == "blockentity":
        need.append("import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;")
        need.append("import com.bobby.bobbychests.blockentity.ModBlockEntities;")
        need.append("import com.bobby.bobbychests.tier.ChestTier;")
    elif sub == "menu":
        need.append("import com.bobby.bobbychests.menu.ModMenus;")
        if stem.endswith("MenuProvider"):
            need.append("import com.bobby.bobbychests.menu.AbstractTieredChestMenuProvider;")
            need.append("import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;")
        elif stem == "EmeraldChestMenu":
            need.append("import com.bobby.bobbychests.menu.AbstractScrollableChestMenu;")
        else:
            need.append("import com.bobby.bobbychests.menu.AbstractChestMenu;")
    elif sub == "client/screen":
        need.append("import com.bobby.bobbychests.client.screen.AbstractChestScreen;")
        if stem == "EmeraldChestScreen":
            need.append("import com.bobby.bobbychests.client.screen.AbstractScrollableChestScreen;")
    elif sub == "client/render":
        need.append("import com.bobby.bobbychests.client.render.AbstractChestRenderer;")
    return insert_after_package(txt, need)


def pkg_for(sub: str, tier: str | None) -> str:
    base = "com.bobby.bobbychests." + sub.replace("/", ".")
    return f"{base}.{tier}" if tier else base


def main() -> None:
    repo = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(repo)
    for sub, tier, stem in MOVES:
        src = os.path.join(ROOT, sub, f"{stem}.java")
        dst_dir = os.path.join(ROOT, sub, tier)
        os.makedirs(dst_dir, exist_ok=True)
        dst = os.path.join(dst_dir, f"{stem}.java")
        if not os.path.isfile(src):
            raise SystemExit(f"Missing: {src}")
        txt = open(src, encoding="utf-8").read()
        old_pkg = "package " + pkg_for(sub, None) + ";"
        new_pkg = "package " + pkg_for(sub, tier) + ";"
        if old_pkg not in txt:
            raise SystemExit(f"Bad package in {src}")
        txt = txt.replace(old_pkg, new_pkg, 1)
        txt = fix_tier_imports(txt)
        txt = ensure_parent_imports(txt, sub, stem)
        open(dst, "w", encoding="utf-8", newline="\n").write(txt)
        os.remove(src)
        print("moved", stem)


if __name__ == "__main__":
    main()
