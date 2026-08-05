package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.copper.CopperChestMenu;
import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenu;
import com.bobby.bobbychests.chest.menu.dirt.DirtChestMenu;
import com.bobby.bobbychests.chest.menu.emerald.EmeraldChestMenu;
import com.bobby.bobbychests.chest.menu.gold.GoldChestMenu;
import com.bobby.bobbychests.chest.menu.iron.IronChestMenu;
import com.bobby.bobbychests.chest.menu.netherite.NetheriteChestMenu;
import com.bobby.bobbychests.chest.menu.wooden.WoodenChestMenu;
import com.bobby.bobbycore.client.gui.theme.BobbyThemes;
import com.bobby.bobbycore.client.gui.theme.UiTheme;

/**
 * Per-tier panel tints inferred from vanilla/mod tier colors.
 */
public final class ChestGuiThemes {
    public static final UiTheme DIRT = BobbyThemes.tinted(0x6B4B2A);
    public static final UiTheme WOOD = BobbyThemes.tinted(0x9A7A48);
    public static final UiTheme COPPER = BobbyThemes.tinted(0xB87333);
    public static final UiTheme IRON = BobbyThemes.tinted(0xA8A8B0);
    public static final UiTheme GOLD = BobbyThemes.tinted(0xD4AF37);
    public static final UiTheme DIAMOND = BobbyThemes.tinted(0x4AEDD9);
    public static final UiTheme EMERALD = BobbyThemes.tinted(0x17DD62);
    public static final UiTheme NETHERITE = BobbyThemes.tinted(0x5A4A5A);

    private ChestGuiThemes() {
    }

    public static UiTheme forMenu(AbstractChestMenu menu) {
        if (menu instanceof DirtChestMenu) {
            return DIRT;
        }
        if (menu instanceof WoodenChestMenu) {
            return WOOD;
        }
        if (menu instanceof CopperChestMenu) {
            return COPPER;
        }
        if (menu instanceof IronChestMenu) {
            return IRON;
        }
        if (menu instanceof GoldChestMenu) {
            return GOLD;
        }
        if (menu instanceof DiamondChestMenu) {
            return DIAMOND;
        }
        if (menu instanceof EmeraldChestMenu) {
            return EMERALD;
        }
        if (menu instanceof NetheriteChestMenu) {
            return NETHERITE;
        }
        return BobbyThemes.BOBBY_DARK;
    }
}
