package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.chest.storage.ChestContentSorter;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestData;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.LockCode;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.storage.TagValueOutput;
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
    private final ResourceHandler<ItemResource> voidingItemResourceHandler = new VoidingItemResourceHandler(this, this.itemResourceHandler);
    private final ChestUpgradeManager upgradeManager = new ChestUpgradeManager(this);
    private boolean savingLocalInventory;

    // Loading can happen before the block entity is attached to a level. In that case,
    // wait until setLevel before forcing storage mode to match the installed upgrades.
    private boolean reconcileStorageModeWhenLevelAvailable;

    protected AbstractTieredChestBlockEntity(BlockEntityType<? extends AbstractTieredChestBlockEntity> type, BlockPos worldPosition, BlockState blockState, ChestTier tier) {
        super(type, worldPosition, blockState);
        this.tier = tier;
        this.setItems(NonNullList.withSize(27 * 2, ItemStack.EMPTY));

        // Our menus wrap the chest in RoutedChestContainer, so vanilla's counter would not see them.
        this.openersCounter = new TieredChestOpenersCounter(this);
    }

    @Override
    public ChestTier getTier() {
        return this.tier;
    }

    public ChestUpgradeManager getUpgradeManager() {
        return this.upgradeManager;
    }

    protected boolean supportsDeepStorage() {
        return false;
    }

    public final boolean canUseDeepStorage() {
        if (!this.supportsDeepStorage()) {
            return false;
        }
        if (this.getStorageMode() != ChestStorageMode.LOCAL) {
            return false;
        }
        return this.upgradeManager.capabilities().canUseDeepStorage();
    }

    public abstract int getSlotCount();

    public void onUpgradeInventoryChanged() {
        if (this.level != null && !this.level.isClientSide()) {
            this.discardDeepStorageContentsIfUpgradeMissing();
            this.reconcileStorageModeWithUpgrades();
            this.reconcileLockedStateWithUpgrades();
        }
    }

    private void discardDeepStorageContentsIfUpgradeMissing() {
        if (this.upgradeManager.capabilities().canUseDeepStorage()) {
            return;
        }
        NonNullList<ItemStack> active = this.getActiveItems();
        int size = Math.min(this.getSlotCount(), active.size());
        boolean changed = false;
        for (int i = 0; i < size; i++) {
            if (DeepStorageStacks.getDeepCount(active.get(i)) <= 0L) {
                continue;
            }
            active.set(i, ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            this.setChanged();
            this.requestClientUpdate();
        }
    }

    private void reconcileLockedStateWithUpgrades() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        if (this.locked && !this.upgradeManager.capabilities().canUseLocking()) {
            this.setLocked(false, null);
        }
    }

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

    private void reconcileStorageModeAfterLoad() {
        Level lvl = this.getLevel();
        if (lvl == null) {
            this.reconcileStorageModeWhenLevelAvailable = true;
            return;
        }
        if (lvl instanceof ServerLevel && !lvl.isClientSide()) {
            this.reconcileStorageModeWithUpgrades();
        }
    }

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
            this.afterStorageIdentityChanged(serverLevel, data, oldKey, openers, true, false);
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

    void signalOpenCountFromCounter(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        this.signalOpenCount(level, pos, state, oldCount, newCount);
    }

    private @Nullable GlobalTieredChestData.StorageKey currentGlobalStorageKey(GlobalTieredChestData data) {
        return this.getStorageMode() == ChestStorageMode.GLOBAL ? data.keyForChest(this) : null;
    }

    private void updateGlobalRegistration(GlobalTieredChestData data) {
        if (this.getStorageMode() == ChestStorageMode.GLOBAL) {
            data.registerOrUpdateChest(this);
        } else {
            data.unregisterChest(this);
        }
    }

    private void afterStorageIdentityChanged(
            ServerLevel serverLevel,
            GlobalTieredChestData data,
            @Nullable GlobalTieredChestData.StorageKey oldKey,
            int openers,
            boolean updateRegistration,
            boolean updateRegistrationBeforeOpeners) {
        this.setChanged();
        this.requestClientUpdate();

        if (updateRegistration && updateRegistrationBeforeOpeners) {
            this.updateGlobalRegistration(data);
        }

        GlobalTieredChestData.StorageKey newKey = this.currentGlobalStorageKey(data);
        data.onChestKeyChangedWhileOpen(serverLevel, oldKey, newKey, openers);

        if (updateRegistration && !updateRegistrationBeforeOpeners) {
            this.updateGlobalRegistration(data);
        }

        if (this.shouldResetScrollableMenuOnStorageKeyChange()) {
            AbstractScrollableChestMenu.resetScrollForEveryoneUsingChest(serverLevel, this.getBlockPos());
        }
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
                this.requestClientUpdate();
                return;
            }
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            GlobalTieredChestData.StorageKey oldKey = data.keyForChest(this);
            int openers = this.openersCounter.getOpenerCount();

            this.globalStorageId = clamped;
            this.afterStorageIdentityChanged(serverLevel, data, oldKey, openers, true, true);
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

    public @Nullable ResourceHandler<ItemResource> getItemResourceHandler(@Nullable Direction side) {
        if (this.getUpgradeManager().capabilities().canVoidWhenFull()) {
            return this.voidingItemResourceHandler;
        }
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
                if (actor != null) {
                    this.ownerUuid = actor.getUUID();
                }
            } else {
                this.ownerUuid = null;
            }
            this.afterStorageIdentityChanged(serverLevel, data, oldKey, openers, this.getStorageMode() == ChestStorageMode.GLOBAL, true);
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
        if (!level.isClientSide() && this.reconcileStorageModeWhenLevelAvailable && level instanceof ServerLevel) {
            this.reconcileStorageModeWhenLevelAvailable = false;
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
     * Skip the vanilla container component on the server. A global chest item should not replace the shared
     * inventory used by every chest on the same storage key.
     *
     * Name, vanilla lock, and loot table data are still safe to apply here.
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
        // Vanilla inventory code calls this method directly. In global mode, server-side inventory changes
        // must go to the shared storage list, except while saving this block entity's own local items.
        if (!this.savingLocalInventory && this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            return GlobalTieredChestData.get(serverLevel).getItemsForChest(this);
        }
        return this.localItems();
    }

    public NonNullList<ItemStack> getActiveItems() {
        return this.getItems();
    }

    public void sortActiveContents() {
        NonNullList<ItemStack> activeItems = this.getActiveItems();
        int size = Math.min(this.getSlotCount(), activeItems.size());
        ChestContentSorter.sort(activeItems, size, this.canUseDeepStorage());
        this.setChanged();
        if (this.getLevel() instanceof ServerLevel serverLevel && this.shouldResetScrollableMenuOnStorageKeyChange()) {
            AbstractScrollableChestMenu.resetScrollForEveryoneUsingChest(serverLevel, this.getBlockPos());
        }
    }

    /**
     * Creates the block-entity data for a retained chest drop.
     *
     * The item carries upgrades and settings, but its inventory is always this chest's local inventory.
     * Shared global contents stay in GlobalTieredChestData.
     */
    public final TagValueOutput createRetainedDropTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithFullMetadata(registries);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.store(tag);

        // Do not copy shared global storage into an item stack. Placing that item later would duplicate it
        // into this chest's local inventory.
        NonNullList<ItemStack> local = this.localItems();
        int size = Math.min(this.getSlotCount(), local.size());
        NonNullList<ItemStack> snapshot = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) {
            ItemStack stack = local.get(i);
            snapshot.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        output.discard("Items");
        ContainerHelper.saveAllItems(output, snapshot);
        return output;
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
        // Client update packets can contain only part of the saved data.
        this.globalStorageId = input.getIntOr(TAG_GLOBAL_STORAGE_ID, 0);
        this.storageMode = input.getBooleanOr(TAG_STORAGE_MODE, false) ? ChestStorageMode.GLOBAL : ChestStorageMode.LOCAL;
        this.locked = input.getBooleanOr(TAG_LOCKED, false);
        String uuidStr = input.getStringOr(TAG_OWNER_UUID, "");
        this.ownerUuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
        this.reconcileStorageModeAfterLoad();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        this.savingLocalInventory = true;
        try {
            super.saveAdditional(output);
        } finally {
            this.savingLocalInventory = false;
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
        tag.putInt(TAG_GLOBAL_STORAGE_ID, this.globalStorageId);
        tag.putBoolean(TAG_STORAGE_MODE, this.getStorageMode() == ChestStorageMode.GLOBAL);
        tag.putBoolean(TAG_LOCKED, this.locked);
        tag.putString(TAG_OWNER_UUID, this.ownerUuid == null ? "" : this.ownerUuid.toString());
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
     * In global mode, item changes must also dirty GlobalTieredChestData. Otherwise the shared inventory
     * can change in memory and never be written to disk.
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
     * Vanilla clearContent calls getItems().clear(). In global mode that would clear the shared inventory.
     */
    @Override
    public void clearContent() {
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel) {
            return;
        }
        super.clearContent();
    }

    /**
     * Breaking a global chest leaves the shared inventory alone. Only this block's local items are dropped.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        Level level = this.getLevel();
        if (level != null && !level.isClientSide() && this.upgradeManager.capabilities().canRetainItemsOnBreak()) {
            // The dropped chest item already carries the retained data.
            return;
        }
        if (level != null && !level.isClientSide()) {
            this.upgradeManager.dropContents(level, pos);
        }
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel) {
            this.dropLocalItems(this.getLevel(), pos);
            return;
        }
        if (level != null && !level.isClientSide() && this.canUseDeepStorage()) {
            // Do not try to spill deep-storage counts into the world.
            NonNullList<ItemStack> active = this.getActiveItems();
            int size = Math.min(this.getSlotCount(), active.size());
            for (int i = 0; i < size; i++) {
                active.set(i, ItemStack.EMPTY);
            }
            this.setChanged();
        }
        super.preRemoveSideEffects(pos, state);
    }

}

