package com.bobby.bobbychests.chest.menu;

import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;
import com.bobby.bobbychests.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

/**
 * Shared plumbing for global chest menus (lock/id metadata + quick-move).
 * Concrete tiers control slot layout and GUI dimensions.
 */
public abstract class AbstractChestMenu extends AbstractContainerMenu {
    public static final int UPGRADE_SLOT_COUNT = 3;
    public static final int UPGRADE_STRIP_GAP_PX = 10;
    public static final int UPGRADE_STRIP_PADDING_PX = 4;
    public static final int UPGRADE_STRIP_WIDTH_PX = UPGRADE_STRIP_GAP_PX + (UPGRADE_STRIP_PADDING_PX * 2) + 18;

    private static final int CHEST_PANEL_EDGE_PAD_PX = 14;
    private static final int CHEST_SLOT_ORIGIN_X = 8;
    private static final int CHEST_SLOT_ORIGIN_Y = 18;
    private static final int CHEST_SLOT_STEP = 18;
    private static final int PLAYER_INV_GAP_BELOW_GRID = 14;

    public record TieredChestClientPayload(
            BlockPos chestPos,
            int maxChannelId,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage) {

        public static TieredChestClientPayload read(RegistryFriendlyByteBuf buf) {
            BlockPos chestPos = buf.readBlockPos();
            int maxChannelId = buf.readVarInt();
            int initialChestId = buf.readVarInt();
            boolean initialLocked = buf.readBoolean();
            String owner = buf.readUtf();
            UUID initialOwnerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
            boolean initialUsingGlobalStorage = buf.readBoolean();
            return new TieredChestClientPayload(chestPos, maxChannelId, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage);
        }
    }

    public static int chestPanelWidthPx(int chestSlotColumns) {
        return CHEST_PANEL_EDGE_PAD_PX + chestSlotColumns * CHEST_SLOT_STEP;
    }

    public static int imageWidthChestGridPlusUpgradeStrip(int chestSlotColumns) {
        return chestPanelWidthPx(chestSlotColumns) + UPGRADE_STRIP_WIDTH_PX;
    }

    public static int imageHeightForChestRows(int chestRows) {
        return 114 + chestRows * CHEST_SLOT_STEP;
    }

    public static int playerInventoryLeftXCenteredUnderGrid(int chestSlotsPerRow) {
        return CHEST_SLOT_ORIGIN_X + ((chestSlotsPerRow - 9) * CHEST_SLOT_STEP) / 2;
    }

    public static int playerInventoryTopYBelowGrid(int chestRows) {
        return CHEST_SLOT_ORIGIN_Y + chestRows * CHEST_SLOT_STEP + PLAYER_INV_GAP_BELOW_GRID;
    }

    protected final Container container;
    protected final Container upgradeContainer;
    protected final Level level;
    protected final BlockPos chestPos;
    protected final int initialChestId;
    protected final boolean initialLocked;
    protected final UUID initialOwnerUuid;
    protected final boolean initialUsingGlobalStorage;
    protected final int maxChannelId;

    protected int chestSlotCount;

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
        this.level = playerInventory.player.level();
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
        return this.getImageWidthPx() - UPGRADE_STRIP_WIDTH_PX;
    }

    public final Container getContainer() {
        return this.container;
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
     * <p>Prevents stale items from the previous backing until full slot packets repaint the mirror — without touching
     * {@link #upgradeContainer}.</p>
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
        return UPGRADE_SLOT_COUNT;
    }

    public final int getFirstUpgradeSlotIndex() {
        return this.chestSlotCount;
    }

    public final int getFirstPlayerSlotIndex() {
        return this.chestSlotCount + this.getUpgradeSlotCount();
    }

    /** Left edge X for every upgrade slot (strip is a vertical column). */
    public final int getUpgradeSlotBaseX() {
        return this.getChestPanelWidthPx() + UPGRADE_STRIP_GAP_PX + UPGRADE_STRIP_PADDING_PX;
    }

    public final int getUpgradeSlotY(int index) {
        return 18 + index * 18;
    }

    /** Plain grid at the standard chest-origin coordinates; sets {@link #chestSlotCount}. */
    protected final void addStandardChestGridSlots(int slotsPerRow, int rows) {
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

    protected final void addUpgradeSlots() {
        for (int i = 0; i < this.getUpgradeSlotCount(); i++) {
            this.addSlot(new UpgradeSlot(this.upgradeContainer, i, this.getUpgradeSlotBaseX(), this.getUpgradeSlotY(i)));
        }
    }

    protected final void addPlayerInventorySlots(Inventory playerInventory, int leftX, int topY) {
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
                if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < this.getFirstPlayerSlotIndex()) {
                if (!this.moveItemStackTo(stack, this.getFirstPlayerSlotIndex(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (ChestUpgradeManager.isUpgradeCard(stack)) {
                    if (!this.moveItemStackTo(stack, this.getFirstUpgradeSlotIndex(), this.getFirstPlayerSlotIndex(), false)) {
                        if (!this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                } else {
                    if (!this.moveItemStackTo(stack, 0, this.chestSlotCount, false)) {
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

