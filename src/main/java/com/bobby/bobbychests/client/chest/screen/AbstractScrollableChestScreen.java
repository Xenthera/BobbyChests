package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.network.SetScrollableChestScrollPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Scrollable chest screen that applies row changes locally before sending them to the server.
 */
public abstract class AbstractScrollableChestScreen<M extends AbstractScrollableChestMenu> extends AbstractChestScreen<M> {
    private static final int SYNCED_CHANNEL_UNSET = Integer.MIN_VALUE;
    private static final int SCROLLBAR_TRACK_WIDTH = 10;
    private static final int SCROLLBAR_HANDLE_TEX_WIDTH = 5;
    private static final int SCROLLBAR_GAP_AFTER_GRID = 2;
    private static final int SCROLLBAR_OFFSET_X = -2;
    private static final int SCROLLBAR_OFFSET_Y = -2;
    private static final int TRACK_TOP_SRC_H = 3;
    private static final int TRACK_CENTER_SRC_H = 1;
    private static final int TRACK_BOTTOM_SRC_H = 3;
    private static final int HANDLE_TOP_SRC_H = 2;
    private static final int HANDLE_CENTER_SRC_H = 2;
    private static final int HANDLE_BOTTOM_SRC_H = 2;
    private static final int SCROLLBAR_TRACK_BOTTOM_TRIM_PX = 1;
    private static final int BACKGROUND_TEXTURE_SIZE = 512;

    private int syncedChannelId = SYNCED_CHANNEL_UNSET;
    private boolean syncedLocked;
    private @Nullable UUID syncedOwnerUuid;

    private final ScrollableChestGuiAssets guiAssets;
    private boolean scrollbarDragging;

    protected AbstractScrollableChestScreen(M menu, Inventory inv, Component title, ScrollableChestGuiAssets guiAssets) {
        super(menu, inv, title);
        this.guiAssets = guiAssets;
    }

    @Override
    protected void containerTick() {
        this.tickScrollFromSyncedBlockEntity();
        super.containerTick();
    }

    /**
     * Snap immediately so future {@link ScrollWindowSlot#set} calls write incoming server slot data to the same logical
     * slice the server is broadcasting.
     */
    @Override
    protected void resetScrollMenuOnStorageKeyChange() {
        this.menu.setScrollRows(0);
    }

    private void tickScrollFromSyncedBlockEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        if (!(this.minecraft.level.getBlockEntity(this.menu.getChestPos()) instanceof AbstractTieredChestBlockEntity be)) {
            return;
        }
        int channel = be.getGlobalStorageId();
        boolean locked = be.isLocked();
        UUID owner = be.getOwnerUuid();
        if (this.syncedChannelId == SYNCED_CHANNEL_UNSET) {
            this.syncedChannelId = channel;
            this.syncedLocked = locked;
            this.syncedOwnerUuid = owner;
            return;
        }
        if (channel != this.syncedChannelId
                || locked != this.syncedLocked
                || !Objects.equals(owner, this.syncedOwnerUuid)) {
            this.menu.onGlobalStorageContextChanged();
            this.syncedChannelId = channel;
            this.syncedLocked = locked;
            this.syncedOwnerUuid = owner;
        }
    }

    private void sendScrollRowsToServer() {
        if (this.minecraft == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(
                new SetScrollableChestScrollPayload(this.menu.getChestPos(), this.menu.getScrollRows())
        );
    }

    private void sendScrollRowsToServerAfterMenuChange(int previousScrollRows) {
        if (this.menu.getScrollRows() != previousScrollRows) {
            this.sendScrollRowsToServer();
        }
    }

    private int chestRowsVisible() {
        return this.menu.getVisibleChestRows();
    }

    private int slotsPerRow() {
        return this.menu.getSlotsPerRow();
    }

    private boolean isMouseOverChestGrid(double mouseX, double mouseY) {
        int step = this.menu.chestSlotStep();
        int x0 = this.leftPos + this.menu.chestSlotGridLeft();
        int y0 = this.topPos + this.menu.chestSlotGridTop();
        int x1 = x0 + this.slotsPerRow() * step;
        int y1 = y0 + this.chestRowsVisible() * step;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    private int scrollViewportHeightPx() {
        return this.chestRowsVisible() * this.menu.chestSlotStep();
    }

    private int scrollbarTrackHeightPx() {
        return this.scrollViewportHeightPx() - SCROLLBAR_TRACK_BOTTOM_TRIM_PX;
    }

    private int scrollContentHeightPx() {
        return this.menu.getTotalChestRows() * this.menu.chestSlotStep();
    }

    private int scrollbarTrackLeft() {
        int step = this.menu.chestSlotStep();
        return this.leftPos
                + this.menu.chestSlotGridLeft()
                + this.slotsPerRow() * step
                + SCROLLBAR_GAP_AFTER_GRID
                + SCROLLBAR_OFFSET_X;
    }

    private int scrollbarTrackTop() {
        return this.topPos + this.menu.chestSlotGridTop() + SCROLLBAR_OFFSET_Y;
    }

    private int scrollbarHandleLeft() {
        return this.scrollbarTrackLeft() + (SCROLLBAR_TRACK_WIDTH - SCROLLBAR_HANDLE_TEX_WIDTH) / 2;
    }

    private int scrollbarTrackGrooveTop() {
        return this.scrollbarTrackTop() + TRACK_TOP_SRC_H;
    }

    private int scrollbarTrackGrooveHeight() {
        return this.scrollbarTrackHeightPx() - TRACK_TOP_SRC_H - TRACK_BOTTOM_SRC_H;
    }

    private boolean isMouseOverScrollbarTrack(double mouseX, double mouseY) {
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

    private void applyScrollFromTrackMouseY(double mouseY) {
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
        if (row == this.menu.getScrollRows()) {
            return;
        }
        int before = this.menu.getScrollRows();
        this.menu.setScrollRows(row);
        this.sendScrollRowsToServerAfterMenuChange(before);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inBounds) {
        if (event.button() == 0 && this.isMouseOverScrollbarTrack(event.x(), event.y())) {
            this.scrollbarDragging = true;
            this.applyScrollFromTrackMouseY(event.y());
            return true;
        }
        return super.mouseClicked(event, inBounds);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrollbarDragging && event.button() == 0) {
            this.applyScrollFromTrackMouseY(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.scrollbarDragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D
                && (this.isMouseOverChestGrid(mouseX, mouseY) || this.isMouseOverScrollbarTrack(mouseX, mouseY))) {
            int max = this.menu.maxScrollRows();
            if (max <= 0) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            int deltaRows = (int) -Math.signum(scrollY);
            if (this.menu.applyScrollDelta(deltaRows)) {
                this.sendScrollRowsToServer();
                return true;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        Identifier bg = this.usingGlobalStorage() ? this.guiAssets.background() : this.guiAssets.backgroundNoId();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                bg,
                this.leftPos, this.topPos,
                0, 0,
                this.menu.getChestPanelWidthPx(), this.imageHeight,
                BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE
        );
        this.renderUpgradeSlotPlaceholders(graphics);
        int trackLeft = this.scrollbarTrackLeft();
        int trackTop = this.scrollbarTrackTop();
        int trackH = this.scrollbarTrackHeightPx();
        int trackW = SCROLLBAR_TRACK_WIDTH;
        blitVerticalThreeSlice(
                graphics,
                this.guiAssets.scrollTrackTop(),
                this.guiAssets.scrollTrackCenter(),
                this.guiAssets.scrollTrackBottom(),
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
                    this.guiAssets.scrollHandleTop(),
                    this.guiAssets.scrollHandleCenter(),
                    this.guiAssets.scrollHandleBottom(),
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
