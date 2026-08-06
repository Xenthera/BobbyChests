package com.bobby.bobbychests.client.chest.screen.widget;

import com.bobby.bobbycore.client.gui.draw.UiDraw;
import com.bobby.bobbycore.client.gui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import com.bobby.bobbycore.client.gui.theme.UiColor;

import java.util.List;

/**
 * A vertical FE charge bar.
 *
 * <p>Energy has no texture to show the way a fluid does, so the bar carries the information the
 * fluid's identity would have: it runs red when nearly flat through amber to green when full, which
 * reads at a glance from across a room in the block's window as well as here in the GUI.
 */
public final class EnergyGauge {

    private static final int LOW_COLOR = 0xFFE04A2F;
    private static final int MID_COLOR = 0xFFE8B23A;
    private static final int FULL_COLOR = 0xFF54D44A;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public EnergyGauge(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /** Charge colour at {@code fraction} of capacity, shared with the block renderer's meter. */
    public static int colorFor(float fraction) {
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        if (clamped < 0.5F) {
            return UiColor.blend(LOW_COLOR, MID_COLOR, clamped / 0.5F);
        }
        return UiColor.blend(MID_COLOR, FULL_COLOR, (clamped - 0.5F) / 0.5F);
    }

    public void draw(GuiGraphicsExtractor graphics, UiTheme theme, int amountFe, int capacityFe) {
        UiDraw.contentWell(graphics, theme, this.x - 1, this.y - 1, this.width + 2, this.height + 2);

        if (amountFe > 0 && capacityFe > 0) {
            float fraction = Math.min(1.0F, (float) amountFe / capacityFe);
            int filled = Math.max(1, (int) (this.height * fraction));
            int top = this.y + this.height - filled;
            UiDraw.fill(graphics, this.x, top, this.width, filled, colorFor(fraction));
        }

        UiDraw.border(graphics, theme, this.x - 1, this.y - 1, this.width + 2, this.height + 2);
    }

    public List<Component> tooltip(int amountFe, int capacityFe) {
        return List.of(Component.translatable(
                "gui.bobbychests.energy.amount",
                TankGauge.format(amountFe),
                TankGauge.format(capacityFe)));
    }

    public int centerX() {
        return this.x + this.width / 2;
    }

    public int bottomY() {
        return this.y + this.height;
    }
}
