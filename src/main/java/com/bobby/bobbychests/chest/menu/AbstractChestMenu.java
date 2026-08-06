package com.bobby.bobbychests.chest.menu;

import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.server.level.ServerPlayer;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.ChestModeContainer;
import com.bobby.bobbychests.chest.upgrade.ChestModeCards;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeContainer;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;
import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
import com.bobby.bobbychests.registry.ModItems;

import com.bobby.bobbycore.client.gui.layout.GuiLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared plumbing for global chest menus (lock/id metadata + quick-move).
 * Concrete tiers control slot layout and GUI dimensions.
 */
public abstract class AbstractChestMenu extends AbstractContainerMenu {
    /** @deprecated Use {@link ChestTier#MAX_UPGRADE_SLOTS} / {@link #getUpgradeSlotCount()}. */
    @Deprecated
    public static final int UPGRADE_SLOT_COUNT = ChestTier.MAX_UPGRADE_SLOTS;
    /** @deprecated Unused; strip width is no longer part of imageWidth. */
    @Deprecated
    public static final int UPGRADE_STRIP_GAP_PX = 10;
    @Deprecated
    public static final int UPGRADE_STRIP_PADDING_PX = 4;
    @Deprecated
    public static final int UPGRADE_STRIP_WIDTH_PX = UPGRADE_STRIP_GAP_PX + (UPGRADE_STRIP_PADDING_PX * 2) + 18;

    public static final int DISABLED_SLOT_POS = -9999;

    /** Keep in sync with {@link com.bobby.bobbychests.client.chest.screen.tab.UpgradeSlotsTab}. */
    public static final int UPGRADE_TAB_PAD = 4;
    public static final int UPGRADE_TAB_CONTENT_TOP = 28;
    /** Nudges the whole tab stack up a little; the strip sat lower than it needed to. */
    public static final int TAB_STRIP_Y_OFFSET = -8;
    /** Top of the tab strip. The screen reads this too, so the slots and the panels cannot drift. */
    public static final int UPGRADE_TAB_ORIGIN_Y = GuiLayout.tabStripOriginY() + TAB_STRIP_Y_OFFSET;
    public static final int UPGRADE_SLOT_SIZE = 18;

    /**
     * Closed tab height and the gap between tabs.
     *
     * <p>Duplicated from {@code ExpandableTab.DEFAULT_CLOSED_HEIGHT} and {@code TabStrip.VERTICAL_GAP}
     * rather than referenced, because those live in BobbyCore's client package and this class also
     * runs on a dedicated server.
     */
    private static final int TAB_CLOSED_HEIGHT = 24;
    private static final int TAB_VERTICAL_GAP = 1;

    /** Tab strip order. Mode sits above upgrades. */
    private static final int MODE_TAB_INDEX = 0;
    private static final int UPGRADES_TAB_INDEX = 1;

    /**
     * Panel-relative Y of the content area of the tab at {@code tabIndex}.
     *
     * <p>Slot positions are fixed at construction — {@link Slot#x} and {@link Slot#y} are final — so
     * they cannot follow a tab that moves. They do not need to: the strip only ever has one tab
     * open, so every tab above the open one is at its closed height and each tab's open position is
     * known in advance. Change the tab order and these indices have to change with it.
     */
    private static int tabContentTop(int tabIndex) {
        return UPGRADE_TAB_ORIGIN_Y
                + tabIndex * (TAB_CLOSED_HEIGHT + TAB_VERTICAL_GAP)
                + UPGRADE_TAB_CONTENT_TOP;
    }

    private static final int CHEST_SLOT_ORIGIN_X = GuiLayout.contentSlotOriginX();
    private static final int CHEST_SLOT_ORIGIN_Y = GuiLayout.slottedContentTop();
    private static final int CHEST_SLOT_STEP = 18;
    /** Air between storage grid and player inventory (label sits in this band). */
    private static final int PLAYER_INV_GAP_BELOW_GRID = 22 + GuiLayout.CONTENT_BOTTOM_PAD;
    private static final int CHEST_PANEL_SIDE_PAD_PX = GuiLayout.panelSidePad();
    /** Player inv block: 3 rows + hotbar gap + hotbar. */
    private static final int PLAYER_INV_BLOCK_H = 3 * 18 + 4 + 18;

    /**
     * Extra panel width reserved for a vertical scrollbar to the right of the slot grid
     * (gap + track + right edge pad). Keeps the track inside {@link #getImageWidthPx()}.
     */
    public static final int SCROLLBAR_GAP_AFTER_GRID = 2;
    public static final int SCROLLBAR_TRACK_WIDTH = 10;
    public static final int SCROLLBAR_RIGHT_EDGE_PAD =
            GuiLayout.CONTENT_WELL_EDGE + GuiLayout.CONTENT_INNER_PAD;
    public static final int SCROLLBAR_GUTTER_PX =
            SCROLLBAR_GAP_AFTER_GRID + SCROLLBAR_TRACK_WIDTH + SCROLLBAR_RIGHT_EDGE_PAD;

    public record TieredChestClientPayload(
            BlockPos chestPos,
            int maxChannelId,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            ChestTier tier) {

        public static TieredChestClientPayload read(RegistryFriendlyByteBuf buf) {
            BlockPos chestPos = buf.readBlockPos();
            int maxChannelId = buf.readVarInt();
            int initialChestId = buf.readVarInt();
            boolean initialLocked = buf.readBoolean();
            String owner = buf.readUtf();
            UUID initialOwnerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
            boolean initialUsingGlobalStorage = buf.readBoolean();
            // The fluid and energy menus are shared across tiers, so unlike the per-tier item menus
            // they cannot infer capacity or theme from their own class and need it sent.
            ChestTier tier = ChestTier.fromIdOrDefault(buf.readUtf(), ChestTier.WOOD);
            return new TieredChestClientPayload(chestPos, maxChannelId, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage, tier);
        }
    }

    public static int chestPanelWidthPx(int chestSlotColumns) {
        // Symmetric side pads so the 18px chrome grid is centered.
        return CHEST_PANEL_SIDE_PAD_PX + chestSlotColumns * CHEST_SLOT_STEP + CHEST_PANEL_SIDE_PAD_PX;
    }

    /** Panel width that fits the slot grid plus an in-panel scrollbar gutter. */
    public static int scrollableChestPanelWidthPx(int chestSlotColumns) {
        return CHEST_PANEL_SIDE_PAD_PX
                + chestSlotColumns * CHEST_SLOT_STEP
                + SCROLLBAR_GUTTER_PX;
    }

    public static int imageWidthChestGridPlusUpgradeStrip(int chestSlotColumns) {
        // Name kept for callers; width is the chest panel only (tabs draw outside).
        return chestPanelWidthPx(chestSlotColumns);
    }

    public static int imageHeightForChestRows(int chestRows) {
        return playerInventoryTopYBelowGrid(chestRows)
                + PLAYER_INV_BLOCK_H
                + GuiLayout.CONTENT_BOTTOM_PAD
                + 2;
    }

    public static int playerInventoryLeftXCenteredUnderGrid(int chestSlotsPerRow) {
        return CHEST_SLOT_ORIGIN_X + ((chestSlotsPerRow - 9) * CHEST_SLOT_STEP) / 2;
    }

    public static int playerInventoryTopYBelowGrid(int chestRows) {
        return CHEST_SLOT_ORIGIN_Y + chestRows * CHEST_SLOT_STEP + PLAYER_INV_GAP_BELOW_GRID;
    }

    /** Panel-relative left of the storage grid. */
    public int chestSlotGridLeft() {
        return CHEST_SLOT_ORIGIN_X;
    }

    /** Panel-relative top of the storage grid. */
    public int chestSlotGridTop() {
        return CHEST_SLOT_ORIGIN_Y;
    }

    public int chestSlotStep() {
        return CHEST_SLOT_STEP;
    }

    /** Columns spanned by the open chest grid. */
    public int getChestGridColumns() {
        return this.chestGridColumns;
    }

    /** Visible storage rows in the open chest grid. */
    public int getChestGridRows() {
        return this.chestGridRows;
    }

    protected final void setChestGridSize(int columns, int rows) {
        this.chestGridColumns = Math.max(1, columns);
        this.chestGridRows = Math.max(1, rows);
    }

    protected final Container container;
    protected final Container upgradeContainer;
    /** Single mode-card slot backing; see {@link ChestModeContainer}. */
    private final Container modeContainer;
    protected final Level level;
    /** The player this menu was opened for; needed to swap it when an upgrade changes the mode. */
    private final Player viewingPlayer;
    protected final BlockPos chestPos;
    protected final int initialChestId;
    protected final boolean initialLocked;
    protected final UUID initialOwnerUuid;
    protected final boolean initialUsingGlobalStorage;
    protected final int maxChannelId;

    protected int chestSlotCount;
    /** Visible storage grid columns (panel layout). */
    private int chestGridColumns = 9;
    /** Visible storage grid rows (panel layout). */
    private int chestGridRows = 1;
    /** Panel-relative Y of the player inventory band (set by {@link #addPlayerInventorySlots}). */
    private int playerInventoryTopY = -1;

    protected AbstractChestMenu(
            net.minecraft.world.inventory.MenuType<?> type,
            int syncID,
            Inventory playerInventory,
            Container container,
            Container upgradeContainer,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId) {
        super(type, syncID);
        this.container = Objects.requireNonNull(container);
        this.upgradeContainer = Objects.requireNonNull(upgradeContainer);
        // Derived from the upgrade container rather than passed in, so no tier menu needs a new
        // constructor argument. Server-side that container knows its block entity; client-side the
        // menu works off a mirror, exactly as the upgrade slots do.
        this.modeContainer = upgradeContainer instanceof ChestUpgradeContainer serverUpgrades
                ? new ChestModeContainer(serverUpgrades.getManager().getChest())
                : new SimpleContainer(ChestModeContainer.SIZE);
        this.level = playerInventory.player.level();
        this.viewingPlayer = playerInventory.player;
        this.chestPos = chestPos;
        this.initialChestId = initialChestId;
        this.initialLocked = initialLocked;
        this.initialOwnerUuid = initialOwnerUuid;
        this.initialUsingGlobalStorage = initialUsingGlobalStorage;
        this.maxChannelId = maxChannelId;

        this.container.startOpen(playerInventory.player);
        this.upgradeContainer.startOpen(playerInventory.player);
    }

    public abstract int getImageWidthPx();

    public abstract int getImageHeightPx();

    public int getChestPanelWidthPx() {
        return this.getImageWidthPx();
    }

    public final Container getContainer() {
        return this.container;
    }

    public final Container getModeContainer() {
        return this.modeContainer;
    }

    public final Container getUpgradeContainer() {
        return this.upgradeContainer;
    }

    public final BlockPos getChestPos() {
        return this.chestPos;
    }

    public final int getInitialChestId() {
        return this.initialChestId;
    }

    public final int getMaxChannelId() {
        return this.maxChannelId;
    }

    public final boolean getInitialLocked() {
        return this.initialLocked;
    }

    public final UUID getInitialOwnerUuid() {
        return this.initialOwnerUuid;
    }

    public final boolean getInitialUsingGlobalStorage() {
        return this.initialUsingGlobalStorage;
    }

    /**
     * Client-only wipe of {@link #container}'s synced mirror when the server's backing chest storage is about to
     * diverge from what the client remembers (GLOBAL pool vs LOCAL, channel full resync, etc.).
     * <p>Clears stale items from the previous backing until slot packets repaint the mirror.
     * Does not touch {@link #upgradeContainer}.</p>
     */
    public final void clearChestStorageMirrorBeforeResync() {
        Level lvl = this.level;
        if (lvl == null || !lvl.isClientSide()) {
            return;
        }
        if (!(this.container instanceof SimpleContainer mirror)) {
            return;
        }
        for (int i = 0; i < mirror.getContainerSize(); i++) {
            mirror.setItem(i, ItemStack.EMPTY);
        }
    }

    public final int getChestSlotCount() {
        return this.chestSlotCount;
    }

    public final int getUpgradeSlotCount() {
        return this.upgradeContainer.getContainerSize();
    }

    public final int getFirstUpgradeSlotIndex() {
        return this.chestSlotCount;
    }

    public final int getFirstPlayerSlotIndex() {
        return this.chestSlotCount + this.getUpgradeSlotCount() + ChestModeContainer.SIZE;
    }

    /** Upgrade slot X relative to the GUI left (panel width + tab attachment + tab padding). */
    public final int getUpgradeSlotBaseX() {
        return this.getChestPanelWidthPx() + this.getUpgradeTabAttachmentOffset() + UPGRADE_TAB_PAD;
    }

    /**
     * Extra left pixels folded into the upgrade tab's 9-slice (keeps flush, widens on X).
     * Scrollable chests override this so the tab covers the scrollbar overhang.
     */
    public int getUpgradeTabAttachmentOffset() {
        return 0;
    }

    public final int getUpgradeSlotY(int index) {
        return tabContentTop(UPGRADES_TAB_INDEX) + index * UPGRADE_SLOT_SIZE;
    }

    public final Slot getUpgradeSlot(int index) {
        return this.slots.get(this.getFirstUpgradeSlotIndex() + index);
    }

    public final boolean areUpgradeSlotsActive() {
        for (int i = 0; i < this.getUpgradeSlotCount(); i++) {
            Slot slot = this.getUpgradeSlot(i);
            if (slot instanceof UpgradeSlot upgradeSlot && upgradeSlot.isActive()) {
                return true;
            }
        }
        return false;
    }

    public final void setUpgradeSlotsActive(boolean active) {
        for (int i = 0; i < this.getUpgradeSlotCount(); i++) {
            Slot slot = this.getUpgradeSlot(i);
            if (slot instanceof UpgradeSlot upgradeSlot) {
                upgradeSlot.setActive(active);
            }
        }
    }

    public final void moveUpgradeSlotsOffScreen() {
        setUpgradeSlotsActive(false);
    }

    public final void placeUpgradeSlots(int localX, int firstLocalY, int stepY) {
        setUpgradeSlotsActive(true);
    }

    /**
     * @return empty if {@code stack} may be installed into {@code excludeSlot}; otherwise a deny reason for tooltips.
     */
    public final Optional<Component> getUpgradeInstallDenyReason(ItemStack stack, int excludeSlot) {
        if (stack.isEmpty() || !ChestUpgradeManager.isUpgradeCard(stack)) {
            return Optional.empty();
        }

        // Mode cards belong in the mode tab. Say so rather than silently refusing.
        if (ChestModeCards.isModeCard(stack)) {
            return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.mode_not_upgrade"));
        }

        Item item = stack.getItem();
        if (item == ModItems.DEEP_STORAGE_UPGRADE_CARD.get() && !this.allowDeepStorageUpgradeCard()) {
            return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.deep_tier_not_allowed"));
        }

        Item deep = ModItems.DEEP_STORAGE_UPGRADE_CARD.get();
        Item networking = ModItems.NETWORKING_UPGRADE_CARD.get();

        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            if (i == excludeSlot) {
                continue;
            }
            ItemStack existing = this.upgradeContainer.getItem(i);
            if (existing.isEmpty()) {
                continue;
            }
            Item other = existing.getItem();
            if (other == item) {
                return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.duplicate"));
            }
            if (item == deep && other == networking) {
                return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.deep_vs_network"));
            }
            if (item == networking && other == deep) {
                return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.network_vs_deep"));
            }
        }

        // Deep storage counts stacks and leave-last protects the final item; neither means anything
        // to a tank or an FE buffer, so they are refused while a mode card is in.
        if (item == deep && this.storesNonItems()) {
            return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.deep_vs_resource"));
        }
        if (item == ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get() && this.storesNonItems()) {
            return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.leave_last_vs_resource"));
        }
        return Optional.empty();
    }

    /** True when a mode card is installed, i.e. the chest is a tank or an FE buffer. */
    private boolean storesNonItems() {
        return ChestModeCards.isModeCard(this.getModeContainer().getItem(0));
    }

    /**
     * Whether the storage grid holds anything.
     *
     * <p>Reads the menu's own container so it works on the client, where the deny tooltip is drawn,
     * as well as on the server. Transfer slots in a fluid or energy menu are not storage, and those
     * menus report empty here so the card can always be pulled back out.
     */
    protected boolean hasStoredItems() {
        for (int i = 0; i < this.chestSlotCount; i++) {
            if (!this.container.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public final boolean canInstallUpgradeCard(ItemStack stack, int excludeSlot) {
        if (!ChestUpgradeManager.isUpgradeCard(stack)) {
            return false;
        }
        return this.getUpgradeInstallDenyReason(stack, excludeSlot).isEmpty();
    }

    /**
     * @return empty if {@code stack} may go in the mode slot; otherwise a reason for the tooltip
     */
    public final Optional<Component> getModeInstallDenyReason(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (!ChestModeCards.isModeCard(stack)) {
            // Covers upgrade cards and anything else that is simply not a mode card.
            return Optional.of(Component.translatable(
                    ChestUpgradeManager.isUpgradeCard(stack)
                            ? "gui.bobbychests.mode.deny.upgrade_not_mode"
                            : "gui.bobbychests.mode.deny.not_a_mode_card"));
        }
        // Same reasoning as before the mode slot existed: changing what a chest stores while it is
        // holding items would have to do something with those items.
        if (this.hasStoredItems()) {
            return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.not_empty"));
        }
        // Deep storage counts stacks and leave-last protects the final item; neither means anything
        // to a tank or a buffer, so they block the swap rather than being quietly ignored.
        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            Item installed = this.upgradeContainer.getItem(i).getItem();
            if (installed == ModItems.DEEP_STORAGE_UPGRADE_CARD.get()) {
                return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.resource_vs_deep"));
            }
            if (installed == ModItems.LEAVE_LAST_ITEM_UPGRADE_CARD.get()) {
                return Optional.of(Component.translatable("gui.bobbychests.upgrade.deny.resource_vs_leave_last"));
            }
        }
        return Optional.empty();
    }

    public final boolean canInstallModeCard(ItemStack stack) {
        return ChestModeCards.isModeCard(stack) && this.getModeInstallDenyReason(stack).isEmpty();
    }

    /** Plain grid at the standard chest-origin coordinates; sets {@link #chestSlotCount}. */
    protected final void addStandardChestGridSlots(int slotsPerRow, int rows) {
        this.setChestGridSize(slotsPerRow, rows);
        this.chestSlotCount = slotsPerRow * rows;
        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < slotsPerRow; col++) {
                this.addSlot(
                        new Slot(
                                this.container,
                                index++,
                                CHEST_SLOT_ORIGIN_X + col * CHEST_SLOT_STEP,
                                CHEST_SLOT_ORIGIN_Y + row * CHEST_SLOT_STEP));
            }
        }
    }

    /**
     * The single mode-card slot, added right after the upgrade slots.
     *
     * <p>Placed inside {@link #addUpgradeSlots()} rather than exposed separately so every tier menu
     * picks it up without a constructor change — they all call that method at the same point, just
     * before the player inventory, which is exactly where this slot belongs in the index order.
     */
    private void addModeSlot() {
        this.addSlot(new ModeSlot(this.modeContainer, 0, this.getModeSlotX(), this.getModeSlotY()) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AbstractChestMenu.this.canInstallModeCard(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return 1;
            }
        });
    }

    /** Mode slot X, in its own tab below the upgrades tab. */
    public final int getModeSlotX() {
        return this.getChestPanelWidthPx() + this.getUpgradeTabAttachmentOffset() + UPGRADE_TAB_PAD;
    }

    public final int getModeSlotY() {
        return tabContentTop(MODE_TAB_INDEX);
    }

    /** Menu index of the mode slot. It sits immediately after the upgrade slots. */
    public final int getModeSlotIndex() {
        return this.getFirstUpgradeSlotIndex() + this.getUpgradeSlotCount();
    }

    public final Slot getModeSlot() {
        return this.slots.get(this.getModeSlotIndex());
    }

    public final boolean isModeSlotActive() {
        return this.getModeSlot() instanceof ModeSlot modeSlot && modeSlot.isActive();
    }

    public final void setModeSlotActive(boolean active) {
        if (this.getModeSlot() instanceof ModeSlot modeSlot) {
            modeSlot.setActive(active);
        }
    }

    /** Mode-card slot that hides while its tab is closed, exactly as {@link UpgradeSlot} does. */
    public static class ModeSlot extends Slot {
        private boolean active;

        public ModeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }

    protected final void addUpgradeSlots() {
        for (int i = 0; i < this.getUpgradeSlotCount(); i++) {
            int idx = i;
            this.addSlot(new UpgradeSlot(
                    this.upgradeContainer,
                    idx,
                    this.getUpgradeSlotBaseX(),
                    this.getUpgradeSlotY(idx)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return AbstractChestMenu.this.canInstallUpgradeCard(stack, idx);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
        }
        this.addModeSlot();
    }

    /**
     * Upgrade slot that can be deactivated while its tab is closed or animating.
     */
    public static class UpgradeSlot extends Slot {
        private boolean active;

        public UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }
    }

    /**
     * Deep storage is intended for dirt chests only right now, but the rest of the implementation can be tier-agnostic.
     */
    protected boolean allowDeepStorageUpgradeCard() {
        return true;
    }

    protected final void addPlayerInventorySlots(Inventory playerInventory, int leftX, int topY) {
        this.playerInventoryTopY = topY;
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, leftX + col * 18, topY + row * 18));
            }
        }
        // Hotbar
        int hotbarY = topY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, leftX + col * 18, hotbarY));
        }
    }

    /** Panel-relative top of the player-inventory tint band; {@code -1} if unset. */
    public final int getPlayerInventoryTopY() {
        return this.playerInventoryTopY;
    }

    /**
     * Chest slots must be added before this is called, and {@link #chestSlotCount} must be set.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack previous = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            previous = stack.copy();
            if (index < this.chestSlotCount) {
                long deep = DeepStorageStacks.getDeepCount(stack);
                if (deep > 0L) {
                    // Deep markers stay in the source slot after one stack is extracted, which vanilla quick-move
                    // can interpret as "keep transferring". The actual transfer is handled in clicked(QUICK_MOVE).
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < this.getFirstPlayerSlotIndex()) {
                if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (ChestModeCards.isModeCard(stack)) {
                    // Its own branch because a mode card is deliberately not an upgrade card, so it
                    // would otherwise fall through to the storage grid even with its tab wide open.
                    boolean movedToMode = this.isModeSlotActive()
                            && this.moveItemStackTo(stack, this.getModeSlotIndex(), this.getModeSlotIndex() + 1, false);
                    if (!movedToMode && !this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ChestUpgradeManager.isUpgradeCard(stack)) {
                    // Stops before the mode slot: an upgrade card has no business there, and relying
                    // on that slot's mayPlace to say so leaves the intent implicit.
                    boolean movedToUpgrade = this.areUpgradeSlotsActive()
                            && this.moveItemStackTo(stack, this.getFirstUpgradeSlotIndex(), this.getModeSlotIndex(), false);
                    if (!movedToUpgrade && !this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (DeepStorageMenuActions.tryMoveIntoDeepStorage(this, stack)) {
                        // handled
                    } else if (!this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
                        if (this.hasVoidUpgradeInstalled()) {
                            stack.setCount(0);
                        } else {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return previous;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        if (slotId >= 0 && slotId < this.chestSlotCount && this.canUseDeepStorageInMenu()) {
            if (DeepStorageMenuActions.handleDeepStorageClick(this, slotId, button, containerInput, player)) {
                return;
            }
        }
        super.clicked(slotId, button, containerInput, player);
    }

    final boolean canUseDeepStorageInMenu() {
        return this.allowDeepStorageUpgradeCard()
                && this.hasDeepStorageUpgradeInstalled()
                && !this.hasNetworkingUpgradeInstalled();
    }

    final boolean moveItemStackToRange(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return this.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    protected final boolean hasVoidUpgradeInstalled() {
        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            if (this.upgradeContainer.getItem(i).getItem() == ModItems.VOID_UPGRADE_CARD.get()) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasLockUpgradeInstalled() {
        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            if (this.upgradeContainer.getItem(i).getItem() == ModItems.LOCK_UPGRADE_CARD.get()) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasDeepStorageUpgradeInstalled() {
        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            if (this.upgradeContainer.getItem(i).getItem() == ModItems.DEEP_STORAGE_UPGRADE_CARD.get()) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasNetworkingUpgradeInstalled() {
        for (int i = 0; i < this.upgradeContainer.getContainerSize(); i++) {
            if (this.upgradeContainer.getItem(i).getItem() == ModItems.NETWORKING_UPGRADE_CARD.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The resource mode this menu is built for. Item menus show a slot grid; the fluid and energy
     * menus override this and show a gauge instead.
     */
    public ChestResourceMode expectedResourceMode() {
        return ChestResourceMode.ITEM;
    }

    /**
     * Ticks the menu, and swaps it out when an upgrade card has changed what this chest stores.
     *
     * <p>{@code broadcastChanges} runs once per tick for the viewing player, which makes it the
     * natural place for this: outside any click being processed, and somewhere the cursor can be
     * checked. Both matter — see
     * {@link AbstractTieredChestBlockEntity#swapMenuForResourceMode(ServerPlayer)}.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        this.swapMenuIfResourceModeChanged();
    }

    private void swapMenuIfResourceModeChanged() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        // Never mid-drag: whatever is on the cursor would belong to a menu that is about to stop
        // existing.
        if (!this.getCarried().isEmpty()) {
            return;
        }
        if (!(this.level.getBlockEntity(this.chestPos) instanceof AbstractTieredChestBlockEntity chest)) {
            return;
        }
        if (chest.getResourceMode() == this.expectedResourceMode() || chest.isSwappingMenu()) {
            return;
        }
        if (this.viewingPlayer instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu == this) {
            chest.swapMenuForResourceMode(serverPlayer);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
        this.upgradeContainer.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player) && this.upgradeContainer.stillValid(player);
    }

    public Level getLevel() {
        return this.level;
    }
}
