package com.bobby.bobbychests.chest.menu.resource;

import com.bobby.bobbychests.chest.ChestTier;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.storage.ChestTransferContainer;
import com.bobby.bobbycore.client.gui.layout.GuiLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Shared layout for the tank and energy-cell screens.
 *
 * <p>One menu class per resource kind rather than per tier, because unlike the item chests the
 * layout does not change with tier — only the capacity number does, and that arrives as synced
 * data. The two transfer slots occupy the storage-slot range, so all of {@link AbstractChestMenu}'s
 * quick-move and upgrade-slot index arithmetic keeps working without an override.
 */
public abstract class AbstractResourceChestMenu extends AbstractChestMenu {

    /** Standard nine-wide chest panel. */
    private static final int PANEL_COLUMNS = 9;

    /**
     * Every fluid and energy chest uses this panel height regardless of tier — the three-row shape
     * of an iron chest.
     *
     * <p>Sizing it per tier instead was tried, so that installing a card never resized the window,
     * but a one-row dirt or wooden panel left the gauge too small to read. A uniform panel is worth
     * the resize: the tier only changes the capacity number, not the amount of UI there is to show.
     * The item chests keep their own per-tier sizes; this is not shared with them.
     */
    private static final int PANEL_ROWS = 3;

    /** Slot pitch, the unit the item grid and therefore the panel height is measured in. */
    private static final int SLOT = 18;

    public static final int GAUGE_WIDTH = 20;

    private final ChestTier tier;

    protected AbstractResourceChestMenu(
            MenuType<?> type,
            int syncID,
            Inventory playerInventory,
            Container container,
            Container upgradeContainer,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId,
            ChestTier tier) {
        super(type, syncID, playerInventory, container, upgradeContainer, chestPos, initialChestId,
                initialLocked, initialOwnerUuid, initialUsingGlobalStorage, maxChannelId);
        this.tier = tier;

        this.addTransferSlots();
        this.addUpgradeSlots();
        this.addPlayerInventorySlots(
                playerInventory,
                AbstractChestMenu.playerInventoryLeftXCenteredUnderGrid(PANEL_COLUMNS),
                AbstractChestMenu.playerInventoryTopYBelowGrid(PANEL_ROWS));
    }

    public final ChestTier getTier() {
        return this.tier;
    }

    public static int gaugeX() {
        return GuiLayout.contentSlotOriginX();
    }

    public static int gaugeY() {
        return GuiLayout.slottedContentTop();
    }

    /** Full height of the content band, so the gauge reads as the centrepiece it is. */
    public static int gaugeHeight() {
        return PANEL_ROWS * SLOT;
    }

    /** Transfer slots stack at the right end of the content band, input above output. */
    public static int transferSlotX() {
        return GuiLayout.contentSlotOriginX() + (PANEL_COLUMNS - 1) * SLOT;
    }

    public static int transferSlotY(int index) {
        // Centred as a pair against the three-row band.
        return GuiLayout.slottedContentTop() + (gaugeHeight() - 2 * SLOT) / 2 + index * SLOT;
    }

    /** Readout sits between the gauge and the slots, on the horizontal room that frees up. */
    public static int readoutX() {
        return gaugeX() + GAUGE_WIDTH + 8;
    }

    /** Right edge the readout is kept clear of, so it never runs under the transfer slots. */
    public static int readoutMaxX() {
        return transferSlotX() - 4;
    }

    /** Vertically centred on the content band. */
    public static int readoutY() {
        return gaugeY() + (gaugeHeight() - 8) / 2;
    }

    private void addTransferSlots() {
        this.setChestGridSize(PANEL_COLUMNS, PANEL_ROWS);
        this.chestSlotCount = ChestTransferContainer.SIZE;

        this.addSlot(new Slot(this.container, ChestTransferContainer.INPUT_SLOT,
                transferSlotX(), transferSlotY(0)));
        // Output is result-only: the transfer pushes into it, players may only take from it.
        this.addSlot(new Slot(this.container, ChestTransferContainer.OUTPUT_SLOT,
                transferSlotX(), transferSlotY(1)) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /**
     * Transfer slots are a work surface, not contents, so a fluid or energy chest always counts as
     * empty for the purpose of swapping upgrade cards back out.
     */
    @Override
    protected boolean hasStoredItems() {
        return false;
    }

    @Override
    protected boolean allowDeepStorageUpgradeCard() {
        return false;
    }

    /** Plain nine-wide panel: no scrollbar gutter, because there is no grid to scroll. */
    @Override
    public int getImageWidthPx() {
        return AbstractChestMenu.chestPanelWidthPx(PANEL_COLUMNS);
    }

    @Override
    public int getImageHeightPx() {
        return AbstractChestMenu.imageHeightForChestRows(PANEL_ROWS);
    }
}
