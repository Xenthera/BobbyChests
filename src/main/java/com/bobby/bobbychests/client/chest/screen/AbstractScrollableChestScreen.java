package com.bobby.bobbychests.client.chest.screen;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.network.SetScrollableChestScrollPayload;
import com.bobby.bobbycore.client.gui.scroll.ScrollController;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
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
    private int syncedChannelId = SYNCED_CHANNEL_UNSET;
    private boolean syncedLocked;
    private @Nullable UUID syncedOwnerUuid;
    private ScrollController scrollController;

    protected AbstractScrollableChestScreen(M menu, Inventory inv, Component title, ScrollableChestGuiAssets guiAssets) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.scrollController = new ScrollController(this.menu.scrollModel())
                .setTheme(this.uiTheme())
                .setOnChanged(rows -> this.sendScrollRowsToServer());
        this.layoutScrollTrack();
    }

    private void layoutScrollTrack() {
        if (this.scrollController == null) {
            return;
        }
        this.scrollController.setTrackBounds(
                this.scrollbarTrackLeft(),
                this.scrollbarTrackTop(),
                AbstractChestMenu.SCROLLBAR_TRACK_WIDTH,
                this.scrollbarTrackHeightPx());
    }

    @Override
    protected void containerTick() {
        this.tickScrollFromSyncedBlockEntity();
        this.layoutScrollTrack();
        super.containerTick();
    }

    /**
     * Snap immediately so future windowed-slot writes match the logical slice the server broadcasts.
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

    private int scrollbarTrackHeightPx() {
        return this.chestRowsVisible() * this.menu.chestSlotStep();
    }

    private int scrollbarTrackLeft() {
        int step = this.menu.chestSlotStep();
        return this.leftPos
                + this.menu.chestSlotGridLeft()
                + this.slotsPerRow() * step
                + AbstractChestMenu.SCROLLBAR_GAP_AFTER_GRID;
    }

    private int scrollbarTrackTop() {
        // Slot chrome sits at itemY-1; keep the track flush with that top edge.
        return this.topPos + this.menu.chestSlotGridTop() - 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean inBounds) {
        this.layoutScrollTrack();
        if (this.scrollController != null && this.scrollController.mouseClicked(event)) {
            return true;
        }
        return super.mouseClicked(event, inBounds);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrollController != null && this.scrollController.mouseDragged(event)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.scrollController != null) {
            this.scrollController.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.layoutScrollTrack();
        if (this.scrollController != null
                && this.scrollController.mouseScrolled(
                        mouseX, mouseY, scrollY, this.isMouseOverChestGrid(mouseX, mouseY))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.drawThemedChestBackground(graphics, mouseX, mouseY, partialTick);
        this.layoutScrollTrack();
        if (this.scrollController != null) {
            this.scrollController.draw(graphics, mouseX, mouseY);
        }
    }
}
