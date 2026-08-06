package com.bobby.bobbychests.client.chest.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;

/**
 * Draws the contents of a fluid or energy chest.
 *
 * <p>The two modes are deliberately different. A fluid chest has real holes cut in its walls, so it
 * gets a liquid cube behind them plus the chest's own inner walls to look at; an energy chest keeps
 * its meter painted on the surface, since a charge bar has no substance to see into.
 *
 * <p>Backface culling is load-bearing for the fluid path: the inner walls are a box wound
 * <em>inward</em>, so the wall nearest the viewer is culled and the eye carries through the port to
 * the far side. {@code entityTranslucent} is built {@code withCull(false)} and would draw that near
 * wall, so {@link RenderTypes#entityCutoutCull} is used instead. The cost is that a fluid's alpha is
 * thresholded rather than blended, so water reads solid, which suits a tank anyway.
 */
final class ChestWindowRenderer {

    /** One block, in the sixteenths {@link ChestWindow} is expressed in. */
    private static final float PX = 1.0F / 16.0F;

    private ChestWindowRenderer() {
    }

    /**
     * Fluid: the chest's inner walls, then the liquid cube standing in them.
     *
     * @param fluidSprite the still texture, or null when the tank is empty
     * @param chestSprite this chest's own sheet sprite, used for the inner walls
     */
    static void submitFluid(
            TieredChestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            @Nullable TextureAtlasSprite fluidSprite,
            @Nullable TextureAtlasSprite chestSprite) {

        if (chestSprite != null) {
            // Its own atlas and therefore its own draw call: the chest texture is on the chest
            // sheet, the fluid on the block sheet.
            RenderType chestType = RenderTypes.entityCutoutCull(Sheets.CHEST_SHEET);
            collector.submitCustomGeometry(poseStack, chestType, (pose, buffer) -> interiorBox(
                    buffer, pose, chestSprite, state.lightCoords));
        }

        if (fluidSprite == null || state.fill <= 0.0F) {
            return;
        }
        RenderType blockType = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
        float top = ChestWindow.contentTop(state.fill);
        collector.submitCustomGeometry(poseStack, blockType, (pose, buffer) -> box(
                buffer, pose, fluidSprite, state.tint, state.lightCoords,
                ChestWindow.CONTENT_MIN, ChestWindow.CONTENT_FLOOR, ChestWindow.CONTENT_MIN,
                ChestWindow.CONTENT_MAX, top, ChestWindow.CONTENT_MAX));
    }

    /**
     * Energy: a lit bar sitting a hair proud of the meter slot on each of the four sides.
     *
     * <p>No hole and no cube. The chest wall is intact here; only the painted slot from the overlay
     * is covered up, from the bottom to the charge line.
     */
    static void submitEnergy(
            TieredChestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            @Nullable TextureAtlasSprite cellSprite) {

        if (cellSprite == null || state.fill <= 0.0F) {
            return;
        }
        RenderType renderType = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
        float top = ChestWindow.meterTop(state.fill);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float lo = ChestWindow.METER_MIN_X * PX;
            float hi = ChestWindow.METER_MAX_X * PX;
            float y0 = ChestWindow.METER_MIN_Y * PX;
            float y1 = top * PX;
            float near = (ChestWindow.WALL_MIN - ChestWindow.SURFACE_OFFSET) * PX;
            float far = (ChestWindow.WALL_MAX + ChestWindow.SURFACE_OFFSET) * PX;

            float u0 = cellSprite.getU0();
            float u1 = cellSprite.getU(span(ChestWindow.METER_MAX_X - ChestWindow.METER_MIN_X));
            float v0 = cellSprite.getV0();
            float v1 = cellSprite.getV(span(top - ChestWindow.METER_MIN_Y));

            int color = state.tint;
            int light = state.lightCoords;

            // -Z
            face(buffer, pose, color, light, 0.0F, 0.0F, -1.0F, u0, u1, v0, v1,
                    lo, y0, near, lo, y1, near, hi, y1, near, hi, y0, near);
            // +Z
            face(buffer, pose, color, light, 0.0F, 0.0F, 1.0F, u0, u1, v0, v1,
                    lo, y0, far, hi, y0, far, hi, y1, far, lo, y1, far);
            // -X
            face(buffer, pose, color, light, -1.0F, 0.0F, 0.0F, u0, u1, v0, v1,
                    near, y0, lo, near, y0, hi, near, y1, hi, near, y1, lo);
            // +X
            face(buffer, pose, color, light, 1.0F, 0.0F, 0.0F, u0, u1, v0, v1,
                    far, y0, lo, far, y1, lo, far, y1, hi, far, y0, hi);
        });
    }

    /**
     * The chest's inner walls, floor and ceiling, wound inward so only the far side is drawn.
     *
     * <p>Textured from a cut side-face region on purpose, so the inner walls carry the same ports
     * the outer ones do and the chest reads as hollow all the way through.
     */
    private static void interiorBox(
            VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int light) {

        float lo = ChestWindow.INTERIOR_MIN * PX;
        float hi = ChestWindow.INTERIOR_MAX * PX;
        float y0 = ChestWindow.INTERIOR_FLOOR * PX;
        float y1 = ChestWindow.INTERIOR_CEILING * PX;

        float u0 = sprite.getU(ChestWindow.INTERIOR_U0);
        float u1 = sprite.getU(ChestWindow.INTERIOR_U1);
        float v0 = sprite.getV(ChestWindow.INTERIOR_V0);
        float v1 = sprite.getV(ChestWindow.INTERIOR_V1);

        // Slightly darkened: these are interior surfaces and should not read as brightly as the
        // outside of the chest sitting right next to them.
        int color = 0xFFAAAAAA;

        // Each face is wound as the outward face of the opposite wall, which is exactly the inward
        // face of this one — so the near side culls and the far side shows.
        // -Z wall, seen from +Z
        face(buffer, pose, color, light, 0.0F, 0.0F, 1.0F, u0, u1, v0, v1,
                lo, y0, lo, hi, y0, lo, hi, y1, lo, lo, y1, lo);
        // +Z wall, seen from -Z
        face(buffer, pose, color, light, 0.0F, 0.0F, -1.0F, u0, u1, v0, v1,
                lo, y0, hi, lo, y1, hi, hi, y1, hi, hi, y0, hi);
        // -X wall, seen from +X
        face(buffer, pose, color, light, 1.0F, 0.0F, 0.0F, u0, u1, v0, v1,
                lo, y0, lo, lo, y1, lo, lo, y1, hi, lo, y0, hi);
        // +X wall, seen from -X
        face(buffer, pose, color, light, -1.0F, 0.0F, 0.0F, u0, u1, v0, v1,
                hi, y0, lo, hi, y0, hi, hi, y1, hi, hi, y1, lo);
        // Floor, seen from above
        face(buffer, pose, color, light, 0.0F, 1.0F, 0.0F, u0, u1, v0, v1,
                lo, y0, lo, lo, y0, hi, hi, y0, hi, hi, y0, lo);
        // Ceiling, seen from below
        face(buffer, pose, color, light, 0.0F, -1.0F, 0.0F, u0, u1, v0, v1,
                lo, y1, lo, hi, y1, lo, hi, y1, hi, lo, y1, hi);
    }

    /**
     * Emits an axis-aligned solid box, every face wound counter-clockwise as seen from outside,
     * which is what the culling pipeline treats as the front.
     */
    private static void box(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int color,
            int light,
            float x0, float y0, float z0,
            float x1, float y1, float z1) {

        float ax0 = x0 * PX;
        float ay0 = y0 * PX;
        float az0 = z0 * PX;
        float ax1 = x1 * PX;
        float ay1 = y1 * PX;
        float az1 = z1 * PX;

        // Sprite extents sized to each face so the texture keeps its natural one-texel-per-pixel
        // scale, rather than a whole tile being squashed onto every face whatever its size.
        float u0 = sprite.getU0();
        float uW = sprite.getU(span(x1 - x0));
        float uD = sprite.getU(span(z1 - z0));
        float v0 = sprite.getV0();
        float vH = sprite.getV(span(y1 - y0));
        float vD = sprite.getV(span(z1 - z0));

        // -Z
        face(buffer, pose, color, light, 0.0F, 0.0F, -1.0F, u0, uW, v0, vH,
                ax0, ay0, az0, ax0, ay1, az0, ax1, ay1, az0, ax1, ay0, az0);
        // +Z
        face(buffer, pose, color, light, 0.0F, 0.0F, 1.0F, u0, uW, v0, vH,
                ax0, ay0, az1, ax1, ay0, az1, ax1, ay1, az1, ax0, ay1, az1);
        // -X
        face(buffer, pose, color, light, -1.0F, 0.0F, 0.0F, u0, uD, v0, vH,
                ax0, ay0, az0, ax0, ay0, az1, ax0, ay1, az1, ax0, ay1, az0);
        // +X
        face(buffer, pose, color, light, 1.0F, 0.0F, 0.0F, u0, uD, v0, vH,
                ax1, ay0, az0, ax1, ay1, az0, ax1, ay1, az1, ax1, ay0, az1);
        // -Y
        face(buffer, pose, color, light, 0.0F, -1.0F, 0.0F, u0, uW, v0, vD,
                ax0, ay0, az0, ax1, ay0, az0, ax1, ay0, az1, ax0, ay0, az1);
        // +Y, the visible surface of the liquid
        face(buffer, pose, color, light, 0.0F, 1.0F, 0.0F, u0, uW, v0, vD,
                ax0, ay1, az0, ax0, ay1, az1, ax1, ay1, az1, ax1, ay1, az0);
    }

    /** Emits one quad from four corners given in counter-clockwise order as seen from its front. */
    private static void face(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int color,
            int light,
            float nx, float ny, float nz,
            float u0, float u1, float v0, float v1,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {

        float[][] corners = {{x0, y0, z0}, {x1, y1, z1}, {x2, y2, z2}, {x3, y3, z3}};
        float[][] uvs = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};

        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(color)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    /**
     * Converts a span in sixteenths into a sprite offset.
     *
     * <p>{@link TextureAtlasSprite#getU(float)} takes 0..1 across the sprite and a block texture is
     * 16 texels wide, so a ten-sixteenth face samples five eighths of it.
     */
    private static float span(float sixteenths) {
        return Math.max(0.0F, Math.min(1.0F, sixteenths / 16.0F));
    }
}
