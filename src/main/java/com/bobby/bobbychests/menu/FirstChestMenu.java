package com.bobby.bobbychests.menu;

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

public class FirstChestMenu extends AbstractContainerMenu {

    public static final int CHEST_SLOTS = 9 * 6;

    private final Container container;
    private final Level level;
    private final BlockPos chestPos;
    private final int initialChestId;
    private final boolean initialLocked;
    private final UUID initialOwnerUuid;

    public static FirstChestMenu clientConstructor(int syncId, Inventory playerInventory) {
        return new FirstChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), BlockPos.ZERO, 0, false, null);
    }
    
    public static FirstChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int id = buf.readVarInt();
        boolean locked = buf.readBoolean();
        String owner = buf.readUtf();
        UUID ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);
        return new FirstChestMenu(syncId, playerInventory, new SimpleContainer(CHEST_SLOTS), pos, id, locked, ownerUuid);
    }

    public FirstChestMenu(int syncID, Inventory playerInventory, Container container, BlockPos chestPos, int initialChestId, boolean initialLocked, UUID initialOwnerUuid) {
        super(ModMenus.FIRST_CHEST_MENU.get(), syncID);

        this.container = Objects.requireNonNull(container);
        this.level = playerInventory.player.level();
        this.chestPos = chestPos;
        this.initialChestId = initialChestId;
        this.initialLocked = initialLocked;
        this.initialOwnerUuid = initialOwnerUuid;

        this.container.startOpen(playerInventory.player);

        // Container slots (9x6)
        int slotIndex = 0;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(this.container, slotIndex++, 8 + col * 18, 18 + row * 18));
            }
        }
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    public Container getContainer() {
        return this.container;
    }

    public BlockPos getChestPos() {
        return this.chestPos;
    }

    public int getInitialChestId() {
        return this.initialChestId;
    }

    public boolean getInitialLocked() {
        return this.initialLocked;
    }

    public UUID getInitialOwnerUuid() {
        return this.initialOwnerUuid;
    }
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack previous = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            previous = stack.copy();
            if (index < CHEST_SLOTS) {
                if (!this.moveItemStackTo(stack, CHEST_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, CHEST_SLOTS, false)) {
                return ItemStack.EMPTY;
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
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }
    public Level getLevel() {
        return this.level;
    }
}
