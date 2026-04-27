package com.bobby.bobbychests.blockentity;

import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestContainer;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.bobby.bobbychests.menu.AbstractChestMenu;
import com.bobby.bobbychests.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.tier.ChestTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractTieredChestBlockEntity extends ChestBlockEntity implements TieredGlobalChest {

    private static final String TAG_GLOBAL_STORAGE_ID = "bobbychests:global_storage_id";
    private static final String TAG_LOCKED = "bobbychests:locked";
    private static final String TAG_OWNER_UUID = "bobbychests:owner_uuid";

    private final ChestTier tier;
    private int globalStorageId;
    private boolean locked;
    private UUID ownerUuid;
    private final ResourceHandler<ItemResource> itemResourceHandler = new GlobalChestItemResourceHandler(this);

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
                return c instanceof GlobalTieredChestContainer global && global.getChest() == AbstractTieredChestBlockEntity.this;
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
                if (level instanceof ServerLevel serverLevel) {
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
                if (level instanceof ServerLevel serverLevel) {
                    GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
                    GlobalTieredChestData.StorageKey key = data.keyForChest(AbstractTieredChestBlockEntity.this);
                    int publicCount = data.getPublicOpenViewerCount(key);
                    if (publicCount > 0) {
                        AbstractTieredChestBlockEntity.this.signalOpenCount(level, pos, state, oldCount, publicCount);
                        return;
                    }
                }
                AbstractTieredChestBlockEntity.this.signalOpenCount(level, pos, state, oldCount, newCount);
                if (level instanceof ServerLevel serverLevel && oldCount <= 0 && newCount > 0) {
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

    public abstract int getSlotCount();

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

    @Override
    public void setGlobalStorageId(int globalStorageId) {
        int clamped = Math.max(0, Math.min(globalStorageId, this.tier.maxChannelId()));
        if (clamped == this.globalStorageId) {
            return;
        }
        if (this.getLevel() instanceof ServerLevel serverLevel) {
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
            GlobalTieredChestData.StorageKey oldKey = data.keyForChest(this);
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
            data.registerOrUpdateChest(this);

            GlobalTieredChestData.StorageKey newKey = data.keyForChest(this);
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
        if (level instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).registerOrUpdateChest(this);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).registerOrUpdateChest(this);
        }
    }

    @Override
    public void setRemoved() {
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).unregisterChest(this);
        }
        super.setRemoved();
    }

    /**
     * When a chest block item is placed, vanilla applies DataComponents.CONTAINER from the item onto the BE
     * via ItemContainerContents.copyInto(getItems()). Our server .getItems() is the shared global list,
     * so that would overwrite every chest's storage with the item's (usually empty) container.
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
        if (level instanceof ServerLevel serverLevel) {
            return GlobalTieredChestData.get(serverLevel).getItemsForChest(this);
        }
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        Level level = this.getLevel();
        if (level instanceof ServerLevel) {
            this.ensureLocalItemsSize(stacks.size());
            this.clearLocalItems();
            return;
        }
        super.setItems(stacks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (this.getLevel() instanceof ServerLevel) {
            this.clearLocalItems();
        }
        this.globalStorageId = input.getIntOr(TAG_GLOBAL_STORAGE_ID, 0);
        this.locked = input.getBooleanOr(TAG_LOCKED, false);
        String uuidStr = input.getStringOr(TAG_OWNER_UUID, "");
        this.ownerUuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        this.clearLocalItems();
        super.saveAdditional(output);

        output.putInt(TAG_GLOBAL_STORAGE_ID, this.globalStorageId);
        output.putBoolean(TAG_LOCKED, this.locked);
        output.putString(TAG_OWNER_UUID, this.ownerUuid == null ? "" : this.ownerUuid.toString());
    }

    private void ensureLocalItemsSize(int size) {
        if (this.items == null || this.items.size() != size) {
            this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        }
    }

    private void clearLocalItems() {
        if (this.items == null) {
            return;
        }
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
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
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).markChangedAndNotify(this);
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = super.removeItemNoUpdate(slot);
        if (!stack.isEmpty() && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData.get(serverLevel).markChangedAndNotify(this);
        }
        return stack;
    }

    /**
     * Vanilla BaseContainerBlockEntity#clearContent does getItems().clear(). Our .getItems()
     * returns the shared global list on the server, so clearing would wipe every chest's storage.
     */
    @Override
    public void clearContent() {
        if (this.getLevel() instanceof ServerLevel) {
            this.clearLocalItems();
            return;
        }
        super.clearContent();
    }

    /**
     * Before the block entity is removed, vanilla calls Containers.dropContents on any Container BE,
     * which pulls every stack out for item entities. That would empty the shared global inventory when one chest breaks.
     * Contents stay in GlobalBobbyBaseChestData; block drops still come from the block loot table.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.getLevel() instanceof ServerLevel) {
            return;
        }
        super.preRemoveSideEffects(pos, state);
    }

}

