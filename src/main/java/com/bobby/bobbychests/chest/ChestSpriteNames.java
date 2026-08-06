package com.bobby.bobbychests.chest;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import net.minecraft.resources.Identifier;

/**
 * Naming for the chest sprites on the chests atlas.
 *
 * <p>Shared by the datagen that declares the composited variants and the renderer that asks for
 * them, so the two cannot drift. The item-mode names are the ones that already existed:
 * {@code copper_chest} for a networked chest and {@code copper_chest_no_id} for a local one.
 */
public final class ChestSpriteNames {

    /**
     * Overlay stamped onto a tier's chest texture to cut its fluid or energy window.
     *
     * <p>Deliberately outside {@code entity/chest}: the vanilla chests atlas stitches that whole
     * directory across every namespace, and an overlay is a compositing input, not a sprite that
     * anything should be able to render on its own.
     */
    public static final String OVERLAY_PATH = "overlay/chest/";

    /**
     * Sprite-source type id for the composited fluid and energy variants.
     *
     * <p>Lives here rather than beside the client-only source class so datagen, which declares the
     * entries, and the client, which registers the codec, cannot disagree about the name.
     */
    public static final Identifier CHEST_COMPOSITE_SOURCE =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "chest_composite");

    private ChestSpriteNames() {
    }

    /** Texture base name for a tier, e.g. {@code copper_chest}. */
    public static String baseName(ChestTier tier) {
        return switch (tier) {
            case WOOD -> "wooden_chest";
            default -> tier.id() + "_chest";
        };
    }

    /**
     * Sprite path for one cell of the mode-by-storage matrix.
     *
     * <p>Takes the base name rather than a tier because the block entity renderers are constructed
     * with the texture name directly. Both callers route through here so the names the datagen
     * declares and the names the renderer asks for cannot drift.
     *
     * @param global whether the chest is on a shared channel, which already had its own texture
     */
    public static String spritePath(String baseName, ChestResourceMode mode, boolean global) {
        StringBuilder name = new StringBuilder("entity/chest/").append(baseName);
        if (mode != ChestResourceMode.ITEM) {
            name.append('_').append(mode.id());
        }
        if (!global) {
            name.append("_no_id");
        }
        return name.toString();
    }

    public static Identifier spriteId(String baseName, ChestResourceMode mode, boolean global) {
        return Identifier.fromNamespaceAndPath(BobbyChests.MODID, spritePath(baseName, mode, global));
    }

    public static Identifier spriteId(ChestTier tier, ChestResourceMode mode, boolean global) {
        return spriteId(baseName(tier), mode, global);
    }

    /** The already-authored item-mode texture a fluid or energy variant is composited from. */
    public static Identifier baseSpriteId(ChestTier tier, boolean global) {
        return spriteId(tier, ChestResourceMode.ITEM, global);
    }

    public static Identifier overlayId(ChestResourceMode mode) {
        return Identifier.fromNamespaceAndPath(BobbyChests.MODID, OVERLAY_PATH + mode.id() + "_window");
    }

    /** Erase mask that opens the port; see {@code ChestCompositeSource.Entry#cutouts}. */
    public static Identifier cutoutId(ChestResourceMode mode) {
        return Identifier.fromNamespaceAndPath(BobbyChests.MODID, OVERLAY_PATH + mode.id() + "_window_cutout");
    }
}
