package com.bobby.bobbychests.client.screen;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.menu.EmeraldChestMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class EmeraldChestScreen extends AbstractScrollableChestScreen<EmeraldChestMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/bobby_base_chest_108.png");
    private static final int ID_BOX_W = 62;
    private static final int CHEST_GRID_LEFT = 8;
    private static final int CHEST_GRID_TOP = 18;
    private static final int CHEST_ROWS_VISIBLE = 6;
    private static final int SLOTS_PER_ROW = 18;
    private static final int SLOT_STEP = 18;
    /** Track sprite width (top / center / bottom are all 10px wide). */
    private static final int SCROLLBAR_TRACK_WIDTH = 10;
    /** Handle 3-slice sprites: 5×2 each (top / center / bottom). */
    private static final int SCROLLBAR_HANDLE_TEX_WIDTH = 5;
    private static final int SCROLLBAR_GAP_AFTER_GRID = 2;
    /** Fine-tune scrollbar placement after layout (track + handle + hitbox). */
    private static final int SCROLLBAR_OFFSET_X = -2;
    private static final int SCROLLBAR_OFFSET_Y = -2;
    /** Vertical 3-slice textures (top / tiled center / bottom). */
    private static final Identifier SCROLL_TRACK_TOP =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_track_top.png");
    private static final Identifier SCROLL_TRACK_CENTER =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_track_center.png");
    private static final Identifier SCROLL_TRACK_BOTTOM =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_track_bottom.png");
    private static final Identifier SCROLL_HANDLE_TOP =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_handle_top.png");
    private static final Identifier SCROLL_HANDLE_CENTER =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_handle_center.png");
    private static final Identifier SCROLL_HANDLE_BOTTOM =
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "textures/gui/scroll/emerald_scroll_handle_bottom.png");
    private static final int TRACK_TOP_SRC_H = 3;
    private static final int TRACK_CENTER_SRC_H = 1;
    private static final int TRACK_BOTTOM_SRC_H = 3;
    private static final int HANDLE_TOP_SRC_H = 2;
    private static final int HANDLE_CENTER_SRC_H = 2;
    private static final int HANDLE_BOTTOM_SRC_H = 2;
    /** Shaves pixels off the track at the bottom so it sits slightly above the slot grid edge. */
    private static final int SCROLLBAR_TRACK_BOTTOM_TRIM_PX = 1;

    private boolean emeraldScrollbarDragging;

    public EmeraldChestScreen(EmeraldChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected int idBoxX() {
        int rightPad = 15;
        return this.leftPos + this.imageWidth - ID_BOX_W - rightPad;
    }

    @Override
    protected int idBoxY() {
        return this.topPos + 5;
    }

    private boolean isMouseOverEmeraldChestGrid(double mouseX, double mouseY) {
        int x0 = this.leftPos + CHEST_GRID_LEFT;
        int y0 = this.topPos + CHEST_GRID_TOP;
        int x1 = x0 + SLOTS_PER_ROW * SLOT_STEP;
        int y1 = y0 + CHEST_ROWS_VISIBLE * SLOT_STEP;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int scrollViewportHeightPx() {
        return CHEST_ROWS_VISIBLE * SLOT_STEP;
    }

    private int scrollbarTrackHeightPx() {
        return this.scrollViewportHeightPx() - SCROLLBAR_TRACK_BOTTOM_TRIM_PX;
    }

    private int scrollContentHeightPx() {
        return EmeraldChestMenu.TOTAL_CHEST_ROWS * SLOT_STEP;
    }

    private int scrollbarTrackLeft() {
        return this.leftPos
                + CHEST_GRID_LEFT
                + SLOTS_PER_ROW * SLOT_STEP
                + SCROLLBAR_GAP_AFTER_GRID
                + SCROLLBAR_OFFSET_X;
    }

    private int scrollbarTrackTop() {
        return this.topPos + CHEST_GRID_TOP + SCROLLBAR_OFFSET_Y;
    }

    private int scrollbarHandleLeft() {
        return this.scrollbarTrackLeft() + (SCROLLBAR_TRACK_WIDTH - SCROLLBAR_HANDLE_TEX_WIDTH) / 2;
    }

    /** Y region where the handle moves: inside the track, below the top cap and above the bottom cap. */
    private int scrollbarTrackGrooveTop() {
        return this.scrollbarTrackTop() + TRACK_TOP_SRC_H;
    }

    private int scrollbarTrackGrooveHeight() {
        return this.scrollbarTrackHeightPx() - TRACK_TOP_SRC_H - TRACK_BOTTOM_SRC_H;
    }

    private boolean isMouseOverEmeraldScrollbarTrack(double mouseX, double mouseY) {
        int x0 = this.scrollbarTrackLeft();
        int y0 = this.scrollbarTrackTop();
        int x1 = x0 + SCROLLBAR_TRACK_WIDTH;
        int y1 = y0 + this.scrollbarTrackHeightPx();
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int scrollHandleHeightPx() {
        int grooveH = this.scrollbarTrackGrooveHeight();
        int ch = this.scrollContentHeightPx();
        return Mth.clamp(grooveH * grooveH / ch, 32, grooveH - 8);
    }

    private int scrollHandleTopPx() {
        int grooveTop = this.scrollbarTrackGrooveTop();
        int grooveH = this.scrollbarTrackGrooveHeight();
        int maxRow = this.menu.maxScrollRows();
        if (maxRow <= 0) {
            return grooveTop;
        }
        int handleH = this.scrollHandleHeightPx();
        return grooveTop + this.menu.getScrollRows() * (grooveH - handleH) / maxRow;
    }

    private void applyEmeraldScrollFromTrackMouseY(double mouseY) {
        int maxRow = this.menu.maxScrollRows();
        if (maxRow <= 0) {
            return;
        }
        int grooveTop = this.scrollbarTrackGrooveTop();
        int grooveH = this.scrollbarTrackGrooveHeight();
        int handleH = this.scrollHandleHeightPx();
        int row;
        if (mouseY <= grooveTop) {
            row = 0;
        } else if (mouseY >= grooveTop + grooveH) {
            row = maxRow;
        } else {
            double usable = Math.max(1.0D, grooveH - handleH);
            double centerY = mouseY - grooveTop - handleH / 2.0D;
            row = Mth.clamp((int) Math.round(centerY * maxRow / usable), 0, maxRow);
        }
        int before = this.menu.getScrollRows();
        this.menu.setScrollRows(row);
        this.sendScrollRowsToServerAfterMenuChange(before);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inBounds) {
        if (event.button() == 0 && this.isMouseOverEmeraldScrollbarTrack(event.x(), event.y())) {
            this.emeraldScrollbarDragging = true;
            this.applyEmeraldScrollFromTrackMouseY(event.y());
            return true;
        }
        return super.mouseClicked(event, inBounds);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.emeraldScrollbarDragging && event.button() == 0) {
            this.applyEmeraldScrollFromTrackMouseY(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.emeraldScrollbarDragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.delegateContainerMouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (scrollY != 0.0D
                && (this.isMouseOverEmeraldChestGrid(mouseX, mouseY) || this.isMouseOverEmeraldScrollbarTrack(mouseX, mouseY))) {
            int deltaRows = (int) -Math.signum(scrollY);
            if (this.menu.applyScrollDelta(deltaRows)) {
                this.sendScrollRowsToServer();
                return true;
            }
        }
        return this.tryScrollTallGuiOnMouseWheel(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BG,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                512, 512
        );
        int trackLeft = this.scrollbarTrackLeft();
        int trackTop = this.scrollbarTrackTop();
        int trackH = this.scrollbarTrackHeightPx();
        int trackW = SCROLLBAR_TRACK_WIDTH;
        blitVerticalThreeSlice(
                graphics,
                SCROLL_TRACK_TOP,
                SCROLL_TRACK_CENTER,
                SCROLL_TRACK_BOTTOM,
                trackW,
                TRACK_TOP_SRC_H,
                TRACK_CENTER_SRC_H,
                TRACK_BOTTOM_SRC_H,
                trackLeft,
                trackTop,
                trackW,
                trackH
        );
        if (this.menu.maxScrollRows() > 0) {
            int handleH = this.scrollHandleHeightPx();
            int handleTop = this.scrollHandleTopPx();
            int handleLeft = this.scrollbarHandleLeft();
            int handleW = SCROLLBAR_HANDLE_TEX_WIDTH;
            blitVerticalThreeSlice(
                    graphics,
                    SCROLL_HANDLE_TOP,
                    SCROLL_HANDLE_CENTER,
                    SCROLL_HANDLE_BOTTOM,
                    handleW,
                    HANDLE_TOP_SRC_H,
                    HANDLE_CENTER_SRC_H,
                    HANDLE_BOTTOM_SRC_H,
                    handleLeft,
                    handleTop,
                    handleW,
                    handleH
            );
        }
    }

    /** Draws a vertical 3-slice from three textures; the center is tiled in steps of {@code centerSrcH} pixels tall. */
    private static void blitVerticalThreeSlice(
            GuiGraphicsExtractor graphics,
            Identifier top,
            Identifier center,
            Identifier bottom,
            int texW,
            int topSrcH,
            int centerSrcH,
            int bottomSrcH,
            int destX,
            int destY,
            int destW,
            int totalDestH) {
        if (totalDestH <= 0) {
            return;
        }
        var pipeline = RenderPipelines.GUI_TEXTURED;
        graphics.blit(pipeline, top, destX, destY, 0, 0, destW, topSrcH, texW, topSrcH);
        int y = destY + topSrcH;
        int midEnd = destY + totalDestH - bottomSrcH;
        while (y < midEnd) {
            int h = Math.min(centerSrcH, midEnd - y);
            graphics.blit(pipeline, center, destX, y, 0, 0, destW, h, texW, centerSrcH);
            y += h;
        }
        graphics.blit(pipeline, bottom, destX, destY + totalDestH - bottomSrcH, 0, 0, destW, bottomSrcH, texW, bottomSrcH);
    }
}
