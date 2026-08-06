package com.bobby.bobbychests.client.chest.screen.widget;

import com.bobby.bobbychests.client.fluid.ClientFluidSprites;
import com.bobby.bobbycore.client.gui.draw.UiDraw;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A vertical tank readout drawn with the fluid's real still texture.
 *
 * <p>The sprite is tiled at its natural 16x16 rather than stretched, and clipped to the fill line
 * with a scissor, so a half-full tank looks like the same fluid as a full one instead of a squashed
 * copy of it. Animated fluids animate, because the sprite the atlas hands back is the live one.
 */
public final class TankGauge {

    private static final int SPRITE_SIZE = 16;
    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public TankGauge(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public void draw(
            GuiGraphicsExtractor graphics,
            UiTheme theme,
            FluidResource fluid,
            int amountMb,
            int capacityMb) {
        UiDraw.contentWell(graphics, theme, this.x - 1, this.y - 1, this.width + 2, this.height + 2);

        int filled = fillPixels(amountMb, capacityMb);
        if (filled > 0 && !fluid.isEmpty()) {
            TextureAtlasSprite sprite = ClientFluidSprites.stillSprite(fluid);
            if (sprite != null) {
                this.drawFluid(graphics, sprite, ClientFluidSprites.tint(fluid), filled);
            }
        }

        UiDraw.border(graphics, theme, this.x - 1, this.y - 1, this.width + 2, this.height + 2);
    }

    private int fillPixels(int amountMb, int capacityMb) {
        if (amountMb <= 0 || capacityMb <= 0) {
            return 0;
        }
        // Anything present shows at least one pixel, so a nearly-empty tank does not read as empty.
        int pixels = (int) ((long) this.height * amountMb / capacityMb);
        return Math.max(1, Math.min(this.height, pixels));
    }

    private void drawFluid(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int tint, int filled) {
        int top = this.y + this.height - filled;
        graphics.enableScissor(this.x, top, this.x + this.width, this.y + this.height);
        // Tile upward from the bottom so the fill line always cuts a tile rather than the seam
        // between two of them.
        for (int drawnY = this.y + this.height - SPRITE_SIZE; drawnY > top - SPRITE_SIZE; drawnY -= SPRITE_SIZE) {
            for (int drawnX = this.x; drawnX < this.x + this.width; drawnX += SPRITE_SIZE) {
                graphics.blitSprite(PIPELINE, sprite, drawnX, drawnY, SPRITE_SIZE, SPRITE_SIZE, tint);
            }
        }
        graphics.disableScissor();
    }

    /** Tooltip lines: the fluid's own name, then the amount against capacity. */
    public List<Component> tooltip(FluidResource fluid, int amountMb, int capacityMb) {
        if (fluid.isEmpty() || amountMb <= 0) {
            return List.of(Component.translatable("gui.bobbychests.tank.empty"));
        }
        return List.of(
                fluid.getHoverName(),
                Component.translatable(
                        "gui.bobbychests.tank.amount",
                        format(amountMb),
                        format(capacityMb)));
    }

    public static String format(int amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    public @Nullable Component overlayLabel(int amountMb, int capacityMb) {
        if (capacityMb <= 0) {
            return null;
        }
        return Component.literal(Math.round(100.0F * amountMb / capacityMb) + "%");
    }

    public int centerX() {
        return this.x + this.width / 2;
    }

    public int bottomY() {
        return this.y + this.height;
    }
}
