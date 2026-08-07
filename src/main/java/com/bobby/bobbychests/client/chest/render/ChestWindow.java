package com.bobby.bobbychests.client.chest.render;

/**
 * Model-space geometry for a chest's window ports and what is drawn at them.
 *
 * <p>Derivation. The chest bottom is a 14x10x14 box at origin (1,0,1) with texture offset (0,19),
 * so each side face is a 14x10 rect whose V axis runs downward from the top of the box. A port at
 * face offset (u, v) sized (w, h) therefore spans {@code 1+u .. 1+u+w} horizontally and
 * {@code 10-v-h .. 10-v} vertically, all in sixteenths of a block. Keep these in step with the
 * {@code WINDOWS} table in {@code tools/generate_resource_chest_textures.py}.
 *
 * <p>Everything is on the bottom box, never the lid, so none of it swings open.
 *
 * <p>The two modes are drawn differently on purpose. Fluid gets a real hole with a liquid cube
 * behind it; energy keeps its meter painted on the surface, which suits a charge bar and avoids
 * opening the chest up for something that has no substance to show.
 */
public final class ChestWindow {

    private static final float BOX_ORIGIN = 1.0F;
    private static final float BOX_HEIGHT = 10.0F;

    private ChestWindow() {
    }

    // --- Fluid: a cut-through port with a liquid cube inside ------------------------------------

    /**
     * The liquid cube, barely smaller than the chest's own base box.
     *
     * <p>Half a pixel in from the walls on every side, which is just enough to avoid z-fighting the
     * shell while still reading as the chest being full of the stuff.
     */
    public static final float CONTENT_MIN = 1.5F;
    public static final float CONTENT_MAX = 14.5F;
    public static final float CONTENT_FLOOR = 0.5F;
    public static final float CONTENT_CEILING = 9.5F;

    /**
     * The chest's inner walls, drawn inward-facing behind the ports.
     *
     * <p>Without these a port looks into nothing: the chest's far wall is a backface and gets
     * culled, so an empty or part-full tank would show straight through the block. A hair inside
     * the shell so the two do not z-fight.
     */
    public static final float INTERIOR_MIN = 1.02F;
    public static final float INTERIOR_MAX = 14.98F;
    public static final float INTERIOR_FLOOR = 0.02F;
    public static final float INTERIOR_CEILING = 9.98F;

    /**
     * Sprite region used for the inner side walls: a base side face, at texture {@code u 0..14,
     * v 33..43} — the same region the port is punched out of.
     *
     * <p>Deliberately the cut region, so the inner walls carry the same holes the outer ones do.
     * Looking in through the near port you see the far port as an opening rather than a wall, and
     * the chest reads as genuinely hollow. All four side faces carry the same window, so any of
     * them serves.
     */
    public static final float INTERIOR_U0 = 0.0F;
    public static final float INTERIOR_U1 = 14.0F / 64.0F;
    public static final float INTERIOR_V0 = 33.0F / 64.0F;
    public static final float INTERIOR_V1 = 43.0F / 64.0F;

    /**
     * Sprite region for the interior floor and ceiling: the exterior bottom face of the base box,
     * at texture {@code u 28..42, v 19..33}.
     *
     * <p>That UV is never cut by the fluid window mask (ports live only on the side faces), so the
     * floor / ceiling stay solid wood when looking in through a port. Reusing the cut side-face
     * region here used to punch a matching hole into the bottom of the tank.
     */
    public static final float FLOOR_U0 = 28.0F / 64.0F;
    public static final float FLOOR_U1 = 42.0F / 64.0F;
    public static final float FLOOR_V0 = 19.0F / 64.0F;
    public static final float FLOOR_V1 = 33.0F / 64.0F;

    /** Top of the liquid cube at {@code fill} of capacity. */
    public static float contentTop(float fill) {
        float clamped = clamp(fill);
        return CONTENT_FLOOR + (CONTENT_CEILING - CONTENT_FLOOR) * clamped;
    }

    // --- Energy: a meter painted on the chest surface --------------------------------------------

    /**
     * Lit fill on each side face.
     *
     * <p>The energy overlay window is {@code (4, 2, 6, 6)} with a 1px frame around a dark track.
     * Horizontal bounds are inset by one so the fill stays inside that track and does not paint
     * over the frame borders.
     */
    public static final float METER_MIN_X = BOX_ORIGIN + 4.0F + 1.0F;
    public static final float METER_MAX_X = BOX_ORIGIN + 4.0F + 6.0F - 1.0F;
    public static final float METER_MIN_Y = BOX_HEIGHT - 2.0F - 6.0F;
    public static final float METER_MAX_Y = BOX_HEIGHT - 2.0F;

    /** Chest outer walls, and how far proud of them the meter sits. */
    public static final float WALL_MIN = 1.0F;
    public static final float WALL_MAX = 15.0F;
    public static final float SURFACE_OFFSET = 0.01F;

    /** Top of the lit part of the meter at {@code fill} of capacity. */
    public static float meterTop(float fill) {
        return METER_MIN_Y + (METER_MAX_Y - METER_MIN_Y) * clamp(fill);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
