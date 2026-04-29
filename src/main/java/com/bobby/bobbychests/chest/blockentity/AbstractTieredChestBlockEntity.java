package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestData;
import com.bobby.bobbychests.chest.storage.RoutedChestContainer;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;
import com.bobby.bobbychests.chest.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractTieredChestBlockEntity extends ChestBlockEntity implements TieredGlobalChest {

    private static final String TAG_GLOBAL_STORAGE_ID = "bobbychests:global_storage_id";
    private static final String TAG_STORAGE_MODE = "bobbychests:storage_mode";
    private static final String TAG_LOCKED = "bobbychests:locked";
    private static final String TAG_OWNER_UUID = "bobbychests:owner_uuid";

    private final ChestTier tier;
    private int globalStorageId;
    private ChestStorageMode storageMode = ChestStorageMode.LOCAL;
    private boolean locked;
    private UUID ownerUuid;
    private final ResourceHandler<ItemResource> itemResourceHandler = new RoutedChestItemResourceHandler(this);
    private final ChestUpgradeManager upgradeManager = new ChestUpgradeManager(this);
    private boolean savingLocalItems;

    /** Set when upgrades were hydrated from disk while this BE had no level yet; drained in {@link #setLevel}. */
    private boolean reconcileStorageModeAfterLevel;
    protected AbstractTieredChestBlockEntity(BlockEntityType<? extends AbstractTieredChestBlockEntity> type, BlockPos worldPosition, BlockState blockState, ChestTier tier) {
        super(type, worldPosition, blockState);
        this.tier = tier;
        this.setItems(NonNullList.withSize(27 * 2, ItemStack.EMPTY));

        // ChestBlockEntity's default openersCounter only recognizes vanilla ChestMenu.
        // Replace it so our custom menu keeps the lid open.
        this.openersCounter = new ContainerOpenersCounter() {
            @Override
            public boolean isOwnContainer(Player player) {
                if (!(player.containerMenu instanceof AbstractChestMenu menu)) {
                    return false;
                }
                Container c = menu.getContainer();
                if (c == AbstractTieredChestBlockEntity.this) {
                    return true;
                }
                return c instanceof RoutedChestContainer routed && routed.getChest() == AbstractTieredChestBlockEntity.this;
            }

            @Override
            protected void onOpen(Level level, BlockPos pos, BlockState state) {
                if (state.getBlock() instanceof ChestBlock chestBlock) {
                    float pitch = level.getRandom().nextFloat() * 0.1F + 0.9F;
                    level.playSound(
                            null,
                            pos,
                            chestBlock.getOpenChestSound(),
                            SoundSource.BLOCKS,
                            0.5F,
                            pitch);
                }
            }

            @Override
            protected void onClose(Level level, BlockPos pos, BlockState state) {
                if (AbstractTieredChestBlockEntity.this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
                    GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
                    GlobalTieredChestData.StorageKey key = data.keyForChest(AbstractTieredChestBlockEntity.this);
                    if (data.getPublicOpenViewerCount(key) > 0) {
                        return;
                    }
                }
                if (state.getBlock() instanceof ChestBlock chestBlock) {
                    float pitch = level.getRandom().nextFloat() * 0.1F + 0.9F;
                    level.playSound(
                            null,
                            pos,
                            chestBlock.getCloseChestSound(),
                            SoundSource.BLOCKS,
                            0.5F,
                            pitch);
                }
            }

            @Override
            protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
                if (AbstractTieredChestBlockEntity.this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
                    GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
                    GlobalTieredChestData.StorageKey key = data.keyForChest(AbstractTieredChestBlockEntity.this);
                    int publicCount = data.getPublicOpenViewerCount(key);
                    if (publicCount > 0) {
                        AbstractTieredChestBlockEntity.this.signalOpenCount(level, pos, state, oldCount, publicCount);
                        return;
                    }
                }
                AbstractTieredChestBlockEntity.this.signalOpenCount(level, pos, state, oldCount, newCount);
                if (AbstractTieredChestBlockEntity.this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel && oldCount <= 0 && newCount > 0) {
                    GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
                    GlobalTieredChestData.StorageKey key = data.keyForChest(AbstractTieredChestBlockEntity.this);
                    // Private (non-public) keys don't use the shared viewer-count mechanism.
                    if (data.getPublicOpenViewerCount(key) <= 0) {
                        data.toggleObserverSignal(serverLevel, key);
                    }
                }
            }
        };
    }

    @Override
    public ChestTier getTier() {
        return this.tier;
    }

    public ChestUpgradeManager getUpgradeManager() {
        return this.upgradeManager;
    }

    public abstract int getSlotCount();

    public void onUpgradeInventoryChanged() {
        if (this.level != null && !this.level.isClientSide()) {
            this.reconcileStorageModeWithUpgrades();
        }
    }

    /**
     * GLOBAL vs LOCAL tracks pooled storage eligibility: it follows the networking upgrade card if present,
     * and falls back to LOCAL when no such card occupies any upgrade slot.
     */
    private void reconcileStorageModeWithUpgrades() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        ChestStorageMode target = this.upgradeManager.capabilities().canUseGlobalPooledStorage()
                ? ChestStorageMode.GLOBAL
                : ChestStorageMode.LOCAL;
        if (this.storageMode != target) {
            this.setStorageMode(target);
        }
    }

    private void finishUpgradeDrivenStorageReconcileAfterLoad() {
        Level lvl = this.getLevel();
        if (lvl == null) {
            this.reconcileStorageModeAfterLevel = true;
            return;
        }
        if (lvl instanceof ServerLevel && !lvl.isClientSide()) {
            this.reconcileStorageModeWithUpgrades();
        }
    }

    /**
     * When channel / lock / owner changes, whether open {@link AbstractScrollableChestMenu}s for this chest should
     * reset row scroll (tiers with a scrollable menu override to {@code true}).
     */
    protected boolean shouldResetScrollableMenuOnStorageKeyChange() {
        return false;
    }

    @Override
    public int getGlobalStorageId() {
        return this.globalStorageId;
    }

    public ChestStorageMode getStorageMode() {
        return this.storageMode;
    }

    public void setStorageMode(ChestStorageMode nextMode) {
        if (nextMode == this.storageMode) {
            return;
        }
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey oldKey = this.getStorageMode() == ChestStorageMode.GLOBAL ? data.keyForChest(this) : null;
            int openers = this.openersCounter.getOpenerCount();

            this.storageMode = nextMode;
            this.setChanged();
            this.requestClientUpdate();

            GlobalTieredChestData.StorageKey newKey = this.getStorageMode() == ChestStorageMode.GLOBAL ? data.keyForChest(this) : null;
            data.onChestKeyChangedWhileOpen(serverLevel, oldKey, newKey, openers);
            if (this.getStorageMode() == ChestStorageMode.GLOBAL) {
                data.registerOrUpdateChest(this);
            } else {
                data.unregisterChest(this);
            }
            if (this.shouldResetScrollableMenuOnStorageKeyChange()) {
                AbstractScrollableChestMenu.resetScrollForEveryoneUsingChest(serverLevel, this.getBlockPos());
            }
            return;
        }

        this.storageMode = nextMode;
        this.setChanged();
    }

    private void requestClientUpdate() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = this.getBlockState();
        level.sendBlockUpdated(this.getBlockPos(), state, state, 3);
    }

    @Override
    public void setGlobalStorageId(int globalStorageId) {
        int clamped = Math.max(0, Math.min(globalStorageId, this.tier.maxChannelId()));
        if (clamped == this.globalStorageId) {
            return;
        }
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            if (this.getStorageMode() != ChestStorageMode.GLOBAL) {
                this.globalStorageId = clamped;
                this.setChanged();
                return;
            }
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey oldKey = data.keyForChest(this);
            int openers = this.openersCounter.getOpenerCount();

            this.globalStorageId = clamped;
            this.setChanged();
            data.registerOrUpdateChest(this);

            GlobalTieredChestData.StorageKey newKey = data.keyForChest(this);
            data.onChestKeyChangedWhileOpen(serverLevel, oldKey, newKey, openers);
            if (this.shouldResetScrollableMenuOnStorageKeyChange()) {
                AbstractScrollableChestMenu.resetScrollForEveryoneUsingChest(serverLevel, this.getBlockPos());
            }
            return;
        }

        this.globalStorageId = clamped;
        this.setChanged();
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public boolean canPlayerOpen(Player player) {
        if (!this.locked) {
            return true;
        }
        if (this.ownerUuid == null) {
            return false;
        }
        return this.ownerUuid.equals(player.getUUID());
    }

    /**
     * NeoForge "transfer" capability exposure for pipes/automation.
     */
    public @Nullable ResourceHandler<ItemResource> getItemResourceHandler(@Nullable Direction side) {
        return this.itemResourceHandler;
    }

    @Override
    public void setLocked(boolean locked, Player actor) {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey oldKey = this.getStorageMode() == ChestStorageMode.GLOBAL ? data.keyForChest(this) : null;
            int openers = this.openersCounter.getOpenerCount();

            this.locked = locked;
            if (locked) {
                // When taking the lock from an unlocked state, the locker becomes the owner.
                if (actor != null) {
                    this.ownerUuid = actor.getUUID();
                }
            } else {
                // When unlocked, clear ownership so anyone can claim it again later.
                this.ownerUuid = null;
            }
            this.setChanged();
            if (this.getStorageMode() == ChestStorageMode.GLOBAL) {
                data.registerOrUpdateChest(this);
            }

            GlobalTieredChestData.StorageKey newKey = this.getStorageMode() == ChestStorageMode.GLOBAL ? data.keyForChest(this) : null;
            data.onChestKeyChangedWhileOpen(serverLevel, oldKey, newKey, openers);
            if (this.shouldResetScrollableMenuOnStorageKeyChange()) {
                AbstractScrollableChestMenu.resetScrollForEveryoneUsingChest(serverLevel, this.getBlockPos());
            }
            return;
        }

        this.locked = locked;
        if (locked) {
            if (actor != null) {
                this.ownerUuid = actor.getUUID();
            }
        } else {
            this.ownerUuid = null;
        }
        this.setChanged();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && this.reconcileStorageModeAfterLevel && level instanceof ServerLevel) {
            this.reconcileStorageModeAfterLevel = false;
            this.reconcileStorageModeWithUpgrades();
        }
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).registerOrUpdateChest(this);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).registerOrUpdateChest(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).unregisterChest(this);
        }
        super.setRemoved();
    }

    /**
     * When a chest block item is placed, vanilla applies DataComponents.CONTAINER from the item onto the BE
     * via ItemContainerContents.copyInto(getItems()). If the active route is global, that would overwrite
     * every chest's storage with the item's (usually empty) container.
     *
     * We still apply custom name, lock, and seeded loot-table components so block-item metadata behaves normally.
     */
    @Override
    protected void applyImplicitComponents(DataComponentGetter getter) {
        if (this.getLevel() instanceof ServerLevel) {
            this.name = getter.get(DataComponents.CUSTOM_NAME);
            this.lockKey = getter.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
            var loot = getter.get(DataComponents.CONTAINER_LOOT);
            if (loot != null) {
                this.setLootTable(loot.lootTable());
                this.setLootTableSeed(loot.seed());
            }
            return;
        }
        super.applyImplicitComponents(getter);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        Level level = this.getLevel();
        if (!this.savingLocalItems && this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            return GlobalTieredChestData.get(serverLevel).getItemsForChest(this);
        }
        return this.localItems();
    }

    public NonNullList<ItemStack> getActiveItems() {
        return this.getItems();
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.items = this.resizedLocalCopy(stacks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ensureLocalItemsSize(this.getSlotCount());
        this.upgradeManager.load(input);
        this.globalStorageId = input.getIntOr(TAG_GLOBAL_STORAGE_ID, 0);
        this.storageMode = input.getBooleanOr(TAG_STORAGE_MODE, false) ? ChestStorageMode.GLOBAL : ChestStorageMode.LOCAL;
        this.locked = input.getBooleanOr(TAG_LOCKED, false);
        String uuidStr = input.getStringOr(TAG_OWNER_UUID, "");
        this.ownerUuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
        this.finishUpgradeDrivenStorageReconcileAfterLoad();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        this.savingLocalItems = true;
        try {
            super.saveAdditional(output);
        } finally {
            this.savingLocalItems = false;
        }

        output.putInt(TAG_GLOBAL_STORAGE_ID, this.globalStorageId);
        output.putBoolean(TAG_STORAGE_MODE, this.getStorageMode() == ChestStorageMode.GLOBAL);
        output.putBoolean(TAG_LOCKED, this.locked);
        output.putString(TAG_OWNER_UUID, this.ownerUuid == null ? "" : this.ownerUuid.toString());
        this.upgradeManager.save(output);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean(TAG_STORAGE_MODE, this.getStorageMode() == ChestStorageMode.GLOBAL);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private NonNullList<ItemStack> localItems() {
        this.ensureLocalItemsSize(this.getSlotCount());
        return this.items;
    }

    private NonNullList<ItemStack> resizedLocalCopy(NonNullList<ItemStack> stacks) {
        int size = this.getSlotCount();
        NonNullList<ItemStack> copy = NonNullList.withSize(size, ItemStack.EMPTY);
        int copySize = Math.min(size, stacks.size());
        for (int i = 0; i < copySize; i++) {
            copy.set(i, stacks.get(i));
        }
        return copy;
    }

    private void ensureLocalItemsSize(int size) {
        if (this.items == null) {
            this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        } else if (this.items.size() != size) {
            this.items = this.resizedLocalCopy(this.items);
        }
    }

    private void dropLocalItems(Level level, BlockPos pos) {
        NonNullList<ItemStack> localItems = this.localItems();
        for (int slot = 0; slot < localItems.size(); slot++) {
            ItemStack stack = localItems.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            localItems.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int getContainerSize() {
        return this.getSlotCount();
    }

    /**
     * Hoppers and other automation call setItem/removeItem on this block entity, which only invokes
     * .setChanged() here — not GlobalBobbyBaseChestData.markChanged(). Without that, the shared
     * net.minecraft.world.level.saveddata.SavedData never becomes dirty and is not written on save, so a restart looks like a full reset.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).markChangedAndNotify(this);
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = super.removeItemNoUpdate(slot);
        if (!stack.isEmpty() && this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).markChangedAndNotify(this);
        }
        return stack;
    }

    /**
     * Vanilla BaseContainerBlockEntity#clearContent does getItems().clear(). In global mode, that would wipe
     * every chest on the same storage key.
     */
    @Override
    public void clearContent() {
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel) {
            return;
        }
        super.clearContent();
    }

    /**
     * In global mode, active contents stay in GlobalTieredChestData, but inactive local contents still belong
     * to this specific block entity and must be dropped before the BE disappears.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        Level level = this.getLevel();
        if (level != null && !level.isClientSide()) {
            this.upgradeManager.dropContents(level, pos);
        }
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel) {
            this.dropLocalItems(this.getLevel(), pos);
            return;
        }
        super.preRemoveSideEffects(pos, state);
    }

}

