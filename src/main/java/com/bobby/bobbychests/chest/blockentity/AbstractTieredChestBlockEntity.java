package com.bobby.bobbychests.chest.blockentity;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.storage.ChestContentSorter;
import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.chest.storage.ChestResourceTransfer;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.chest.storage.ChestModeContainer;
import com.bobby.bobbychests.chest.storage.ChestTransferContainer;
import com.bobby.bobbychests.chest.storage.DeepStorageStacks;
import com.bobby.bobbychests.chest.storage.GlobalTieredChestData;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.chest.upgrade.ChestUpgradeManager;
import com.bobby.bobbychests.chest.ChestTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractTieredChestBlockEntity extends ChestBlockEntity implements TieredGlobalChest {

    private static final String TAG_GLOBAL_STORAGE_ID = "bobbychests:global_storage_id";
    private static final String TAG_STORAGE_MODE = "bobbychests:storage_mode";
    private static final String TAG_LOCKED = "bobbychests:locked";
    private static final String TAG_OWNER_UUID = "bobbychests:owner_uuid";
    private static final String TAG_LOCAL_ITEMS = "bobbychests:local_items";
    private static final String TAG_RESOURCE_MODE = "bobbychests:resource_mode";
    private static final String TAG_LOCAL_RESOURCES = "bobbychests:local_resources";
    /** Level shown to clients; in global mode this is a copy of the pool, not this block's own state. */
    private static final String TAG_DISPLAY_RESOURCES = "bobbychests:display_resources";
    private static final String TAG_TRANSFER_ITEMS = "bobbychests:transfer_items";
    private static final String TAG_MODE_CARD = "bobbychests:mode_card";

    /**
     * Minimum ticks between level broadcasts.
     *
     * <p>A pipe filling a tank changes the amount many times per second, and each broadcast goes to
     * every player tracking the chunk, so they are worth spacing out — but only in time. Gating on
     * the size of the change as well meant small ones were never sent at all.
     *
     * <p>Two ticks gives ten updates a second, which reads as live, and {@link #serverTick()}
     * guarantees anything held back still lands.
     */
    private static final int LEVEL_BROADCAST_MIN_TICKS = 2;

    /**
     * {@link #lastLevelBroadcastTick} before anything has been sent, and after a change that must go
     * out on the next opportunity regardless of the rate limit.
     *
     * <p>Checked by identity rather than subtracted from. {@code gameTime - Long.MIN_VALUE} overflows
     * to a large negative number, so an elapsed-time test against it reads as "no time has passed"
     * for every game time there is — which is what silently switched the whole broadcast off.
     */
    private static final long NEVER_BROADCAST = Long.MIN_VALUE;

    private record StoredSlot(int slot, ItemStack stack) {
        private static final Codec<StoredSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("Slot").forGetter(StoredSlot::slot),
                ItemStack.OPTIONAL_CODEC.fieldOf("Stack").forGetter(StoredSlot::stack)
        ).apply(instance, StoredSlot::new));
    }

    private final ChestTier tier;
    private int globalStorageId;
    private ChestStorageMode storageMode = ChestStorageMode.LOCAL;
    private ChestResourceMode resourceMode = ChestResourceMode.ITEM;
    private boolean locked;
    private UUID ownerUuid;
    private final ResourceHandler<ItemResource> itemResourceHandler = new RoutedChestItemResourceHandler(this);
    private final ResourceHandler<ItemResource> voidingItemResourceHandler = new VoidingItemResourceHandler(this, this.itemResourceHandler);
    private final ResourceHandler<FluidResource> fluidResourceHandler = new RoutedChestFluidResourceHandler(this);
    private final EnergyHandler energyHandler = new RoutedChestEnergyHandler(this);
    private final ChestUpgradeManager upgradeManager;
    private boolean savingLocalInventory;

    /** Fluid/energy contents when this chest is LOCAL. In GLOBAL mode the pool is used instead. */
    private final ChestResourceContents localResources = new ChestResourceContents();
    /** Client-side mirror of whatever storage is active, populated from the update tag. */
    private final ChestResourceContents displayResources = new ChestResourceContents();
    /** Bucket/battery in and out slots for a fluid or energy chest. Not storage; see ChestTransferContainer. */
    private final NonNullList<ItemStack> transferItems = NonNullList.withSize(ChestTransferContainer.SIZE, ItemStack.EMPTY);
    /** The one mode card, held apart from the upgrade slots so it costs none of them. */
    private final NonNullList<ItemStack> modeItems = NonNullList.withSize(ChestModeContainer.SIZE, ItemStack.EMPTY);
    /** Guards the transfer-on-change / change-on-transfer cycle from recursing. */
    private boolean runningTransfer;
    /** Non-zero while {@link #swapMenu} is exchanging menus; see {@link #isSwappingMenu()}. */
    private int menuSwapDepth;
    private long lastLevelBroadcastTick = NEVER_BROADCAST;
    private int lastBroadcastFluidAmount = -1;
    private int lastBroadcastEnergy = -1;
    private FluidResource lastBroadcastFluid = FluidResource.EMPTY;

    // Loading can happen before the block entity is attached to a level. In that case,
    // wait until setLevel before forcing storage mode to match the installed upgrades.
    private boolean reconcileStorageModeWhenLevelAvailable;

    protected AbstractTieredChestBlockEntity(BlockEntityType<? extends AbstractTieredChestBlockEntity> type, BlockPos worldPosition, BlockState blockState, ChestTier tier) {
        super(type, worldPosition, blockState);
        this.tier = tier;
        this.upgradeManager = new ChestUpgradeManager(this);
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
        if (this.getResourceMode() != ChestResourceMode.ITEM) {
            return false;
        }
        return this.upgradeManager.capabilities().canUseDeepStorage();
    }

    /** Whether this chest stores items, fluid, or FE. Derived from the installed upgrade cards. */
    public ChestResourceMode getResourceMode() {
        return this.resourceMode;
    }

    public int getFluidCapacityMb() {
        return this.tier.fluidCapacityMb();
    }

    public int getEnergyCapacityFe() {
        return this.tier.energyCapacityFe();
    }

    /**
     * The live fluid/energy contents this chest reads and writes.
     *
     * <p>Server-side this is the local holder or the shared pool depending on storage mode, so
     * callers never branch on it. Client-side it is the mirror filled from the update tag, which
     * is what the screen gauge and the block renderer read.
     */
    public ChestResourceContents getActiveResources() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return this.displayResources;
        }
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && level instanceof ServerLevel serverLevel) {
            return GlobalTieredChestData.get(serverLevel).getResourcesForChest(this);
        }
        return this.localResources;
    }

    /** Call after mutating {@link #getActiveResources()} so the pool persists and clients catch up. */
    public void onResourcesChanged() {
        this.setChanged();
        this.maybeBroadcastResourceLevel(false);
        // In a shared pool the other chests on this channel just changed too, without anything
        // happening to them that they could notice. They have to be told.
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            data.notifyPooledLevelChanged(serverLevel, data.keyForChest(this));
        }
        // A tank that just gained fluid may now be able to fill a bucket waiting in the input slot.
        this.runTransferSlots();
    }

    /** Sends this chest's level to clients if it differs from what they were last told. */
    public void sendResourceLevelIfChanged() {
        this.maybeBroadcastResourceLevel(false);
    }

    /**
     * Same as {@link #sendResourceLevelIfChanged()} but may bypass the rate limit.
     *
     * <p>Used by {@link GlobalTieredChestData#notifyPooledLevelChanged} so a forced pool notify does
     * not recurse back into another pool fan-out.
     */
    public void maybeBroadcastResourceLevelFromPool(boolean force) {
        this.maybeBroadcastResourceLevel(force);
    }

    /**
     * Pushes the current level to tracking clients immediately, ignoring the rate limit.
     *
     * <p>For discrete player actions (sneak-bucket, sneak-battery) where waiting a tick or two for
     * {@link #serverTick()} would leave the block looking empty after the item already changed.
     */
    public void forceBroadcastResourceLevel() {
        this.maybeBroadcastResourceLevel(true);
        if (this.getStorageMode() == ChestStorageMode.GLOBAL && this.getLevel() instanceof ServerLevel serverLevel) {
            GlobalTieredChestData data = GlobalTieredChestData.get(serverLevel);
            data.notifyPooledLevelChanged(serverLevel, data.keyForChest(this), true);
        }
    }

    public NonNullList<ItemStack> getTransferItems() {
        return this.transferItems;
    }

    public NonNullList<ItemStack> getModeItems() {
        return this.modeItems;
    }

    /** The installed mode card, or empty. Drives {@link #getResourceMode()}. */
    public ItemStack getModeCard() {
        return this.modeItems.get(0);
    }

    /** Called by {@link ChestModeContainer} when the card is put in or taken out. */
    public void onModeCardChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.reconcileResourceModeWithUpgrades();
        }
    }

    /** Called by {@link ChestTransferContainer} whenever a player moves something in or out. */
    public void onTransferSlotsChanged() {
        this.setChanged();
        this.runTransferSlots();
    }

    private void runTransferSlots() {
        if (this.runningTransfer) {
            return;
        }
        if (!(this.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (this.resourceMode == ChestResourceMode.ITEM) {
            return;
        }
        this.runningTransfer = true;
        try {
            if (ChestResourceTransfer.run(this, new ChestTransferContainer(this))) {
                this.setChanged();
                this.maybeBroadcastResourceLevel(true);
            }
        } finally {
            this.runningTransfer = false;
        }
    }

    private void dropModeCard(Level level, BlockPos pos) {
        ItemStack card = this.modeItems.get(0);
        if (card.isEmpty()) {
            return;
        }
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), card.copy());
        this.modeItems.set(0, ItemStack.EMPTY);
    }

    private void dropTransferItems(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.transferItems.size(); slot++) {
            ItemStack stack = this.transferItems.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            this.transferItems.set(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Pushes the current level to clients, subject to {@link #LEVEL_BROADCAST_MIN_TICKS} unless
     * {@code force} is set.
     */
    private void maybeBroadcastResourceLevel(boolean force) {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.resourceMode == ChestResourceMode.ITEM) {
            return;
        }

        // Compare against what the client was last told, rather than tracking a dirty flag. The
        // comparison cannot get out of step with reality, so a change can never be forgotten.
        ChestResourceContents active = this.getActiveResources();
        boolean changed = active.fluidAmount() != this.lastBroadcastFluidAmount
                || active.energy() != this.lastBroadcastEnergy
                || !active.fluid().equals(this.lastBroadcastFluid);
        if (!changed) {
            return;
        }

        // Rate limit only. Anything held back here stays different from the last broadcast, so
        // serverTick picks it up within a tick or two.
        //
        // The previous version also required the level to move by 1% of capacity, and silently
        // dropped anything smaller. On a netherite chest that was 324,000 FE before the gauge would
        // so much as twitch, and — worse — the final insert before a pipe went idle was usually
        // under the threshold, so it was discarded and the client stayed stale until the chunk
        // reloaded. That is the "never updates until I reopen the world" bug.
        //
        // NEVER_BROADCAST is compared, not subtracted: see the constant.
        if (!force
                && this.lastLevelBroadcastTick != NEVER_BROADCAST
                && serverLevel.getGameTime() - this.lastLevelBroadcastTick < LEVEL_BROADCAST_MIN_TICKS) {
            return;
        }

        this.lastLevelBroadcastTick = serverLevel.getGameTime();
        this.lastBroadcastFluid = active.fluid();
        this.lastBroadcastFluidAmount = active.fluidAmount();
        this.lastBroadcastEnergy = active.energy();
        this.sendResourceLevelToTrackers();
    }

    /**
     * Server tick for fluid and energy chests.
     *
     * <p>Exists so a level change always reaches the client eventually. Broadcasting only from the
     * point of change cannot do that on its own: whatever the rate limit holds back needs something
     * to come along afterwards and send it, and a chest that fills from a pipe and then goes quiet
     * has no further change to ride on.
     *
     * <p>Also how pooled chests keep in step. Every chest on a shared channel compares the pool
     * against what it last sent, so they all converge without anyone having to fan updates out.
     */
    public void serverTick() {
        if (this.resourceMode == ChestResourceMode.ITEM) {
            return;
        }
        this.maybeBroadcastResourceLevel(false);
    }

    /** Item slots this chest stores. Tier-driven so it cannot drift from the tier's tank capacity. */
    public int getSlotCount() {
        return this.tier.storageSlots();
    }

    public void onUpgradeInventoryChanged() {
        if (this.level != null && !this.level.isClientSide()) {
            this.reconcileResourceModeWithUpgrades();
            this.discardDeepStorageContentsIfUpgradeMissing();
            this.reconcileStorageModeWithUpgrades();
            this.reconcileLockedStateWithUpgrades();
        }
    }

    /**
     * Follows the fluid/energy cards into the matching resource mode.
     *
     * <p>Leaving a fluid or energy mode discards this chest's local contents. The menu refuses to
     * install a fluid or energy card while the chest holds items, and refuses to swap between
     * fluid and energy at all, so the only way to reach a discard here is pulling the card back
     * out — at which point the tank is on screen and the player can see what they are dumping.
     * Global pools are left alone: they belong to the channel, not to this block.
     */
    private void reconcileResourceModeWithUpgrades() {
        ChestResourceMode target = this.upgradeManager.capabilities().resourceMode();
        if (target == this.resourceMode) {
            return;
        }
        this.resourceMode = target;
        if (this.getStorageMode() == ChestStorageMode.LOCAL) {
            this.localResources.clear();
        }
        this.setChanged();
        // A mode change repaints both the GUI and the block, so it always goes out immediately.
        this.lastLevelBroadcastTick = NEVER_BROADCAST;
        this.requestClientUpdate();
        this.invalidateCapabilitiesForModeChange();
        // Open menus are not swapped here. See AbstractChestMenu#broadcastChanges: doing it from
        // inside the click that moved the card is what kept destroying the card.
    }

    /**
     * Tells the capability system this block now offers a different capability.
     *
     * <p>Adjacent pipes cache their neighbours' capabilities and only re-resolve when the level
     * says something changed. Without this, a chest that just became a tank keeps looking like an
     * item chest to everything touching it until the pipe or the chest is broken and replaced.
     */
    private void invalidateCapabilitiesForModeChange() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        level.invalidateCapabilities(pos);
        // Neighbours cache in both directions, so nudge them into re-resolving as well.
        for (Direction direction : Direction.values()) {
            level.invalidateCapabilities(pos.relative(direction));
        }
        level.updateNeighborsAt(pos, this.getBlockState().getBlock());
    }

    /**
     * Swaps the open screen for anyone looking at this chest.
     *
     * <p>The item grid and the tank gauge are different menus, so a card that changes the mode has
     * to re-open rather than repaint. Deferred to the end of the tick because this runs from inside
     * the upgrade slot's own click handling, and swapping a player's container mid-click leaves the
     * click operating on a menu that is no longer theirs.
     */
    /**
     * Swaps {@code player}'s open menu for the one this chest's current mode calls for.
     *
     * <p>Called from {@link AbstractChestMenu#broadcastChanges()} once per tick, and only while the
     * player's cursor is empty. Both conditions matter. Doing it from inside the click that moved
     * the card meant swapping the container out from under a half-finished operation, and the card
     * on the cursor belonged to a menu that no longer existed by the time the click wrote it back —
     * so it vanished. Waiting for an empty cursor means there is simply nothing in flight to lose.
     *
     * <p>{@link #isSwappingMenu()} keeps the openers counter quiet through the exchange, so the lid
     * does not slam and the chest sound does not replay for something the player experiences as the
     * same chest staying open.
     */
    public void swapMenuForResourceMode(ServerPlayer player) {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = serverLevel.getBlockState(this.getBlockPos());
        MenuProvider provider = state.getMenuProvider(serverLevel, this.getBlockPos());
        if (provider == null) {
            player.closeContainer();
            return;
        }

        this.menuSwapDepth++;
        try {
            player.openMenu(provider);
        } finally {
            this.menuSwapDepth--;
        }
    }

    /** True while a mode change is exchanging one menu for another on the same chest. */
    public boolean isSwappingMenu() {
        return this.menuSwapDepth > 0;
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
        // Belt and braces: sendBlockUpdated is about a block whose state has not changed, and does
        // not reliably carry the block entity with it. Mode and channel changes repaint the block,
        // so they cannot afford to be dropped either.
        this.sendResourceLevelToTrackers();
    }

    /**
     * Pushes this block entity's data to every player tracking its chunk, directly.
     *
     * <p>{@link #requestClientUpdate()} asks the chunk system to notice a block change and resend
     * the block entity along with it, which is the usual route but is bookkeeping about a block
     * whose state has not actually changed — the level moving is invisible to it. In practice a
     * tank's level would sit stale on the client until something forced a genuine block update, and
     * for a networked chest the only thing that did was opening it, which toggles the hidden
     * observer state. Hence sending the packet ourselves: there is nothing to infer.
     */
    private void sendResourceLevelToTrackers() {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        Packet<ClientGamePacketListener> packet = this.getUpdatePacket();
        if (packet == null) {
            return;
        }
        ChunkPos chunkPos = ChunkPos.containing(this.getBlockPos());
        for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
            player.connection.send(packet);
        }
    }

    /**
     * Applies only the fields {@link #getUpdateTag} actually sends.
     *
     * <p>The default NeoForge path runs the full {@link #loadAdditional} against that partial tag,
     * which clears mode cards, upgrades, and transfer slots on the client every time the tank level
     * moves. Those belong on disk and in the open menu, not in the level broadcast.
     */
    private void applyClientUpdateTag(ValueInput input) {
        this.globalStorageId = input.getIntOr(TAG_GLOBAL_STORAGE_ID, this.globalStorageId);
        this.storageMode = input.getBooleanOr(TAG_STORAGE_MODE, this.storageMode == ChestStorageMode.GLOBAL)
                ? ChestStorageMode.GLOBAL
                : ChestStorageMode.LOCAL;
        this.locked = input.getBooleanOr(TAG_LOCKED, this.locked);
        String uuidStr = input.getStringOr(TAG_OWNER_UUID, this.ownerUuid == null ? "" : this.ownerUuid.toString());
        this.ownerUuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
        this.resourceMode = ChestResourceMode.fromIdOrDefault(
                input.getStringOr(TAG_RESOURCE_MODE, this.resourceMode.id()), this.resourceMode);
        input.read(TAG_DISPLAY_RESOURCES, ChestResourceContents.CODEC)
                .ifPresent(this.displayResources::copyFrom);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        this.applyClientUpdateTag(valueInput);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        this.applyClientUpdateTag(input);
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

    /**
     * Item capability, or null when this chest is a tank or an energy cell.
     *
     * <p>Returning null rather than an empty handler is what makes a fluid chest invisible to item
     * pipes: {@code level.getCapability(Capabilities.Item.BLOCK, ...)} simply finds nothing there.
     */
    public @Nullable ResourceHandler<ItemResource> getItemResourceHandler(@Nullable Direction side) {
        if (this.getResourceMode() != ChestResourceMode.ITEM) {
            return null;
        }
        if (this.getUpgradeManager().capabilities().canVoidWhenFull()) {
            return this.voidingItemResourceHandler;
        }
        return this.itemResourceHandler;
    }

    /** Fluid capability, or null unless a fluid upgrade card is installed. */
    public @Nullable ResourceHandler<FluidResource> getFluidResourceHandler(@Nullable Direction side) {
        return this.getResourceMode() == ChestResourceMode.FLUID ? this.fluidResourceHandler : null;
    }

    /** Energy capability, or null unless an energy upgrade card is installed. */
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        return this.getResourceMode() == ChestResourceMode.ENERGY ? this.energyHandler : null;
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
        this.loadLocalItems(input);
        this.upgradeManager.load(input);
        // Client update packets can contain only part of the saved data.
        this.globalStorageId = input.getIntOr(TAG_GLOBAL_STORAGE_ID, 0);
        this.storageMode = input.getBooleanOr(TAG_STORAGE_MODE, false) ? ChestStorageMode.GLOBAL : ChestStorageMode.LOCAL;
        this.locked = input.getBooleanOr(TAG_LOCKED, false);
        String uuidStr = input.getStringOr(TAG_OWNER_UUID, "");
        this.ownerUuid = uuidStr.isEmpty() ? null : UUID.fromString(uuidStr);
        this.resourceMode = ChestResourceMode.fromIdOrDefault(
                input.getStringOr(TAG_RESOURCE_MODE, ""), ChestResourceMode.ITEM);
        for (int i = 0; i < this.transferItems.size(); i++) {
            this.transferItems.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_TRANSFER_ITEMS), this.transferItems);
        this.modeItems.set(0, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty(TAG_MODE_CARD), this.modeItems);
        input.read(TAG_LOCAL_RESOURCES, ChestResourceContents.CODEC)
                .ifPresentOrElse(this.localResources::copyFrom, this.localResources::clear);
        // Only present on client update packets; harmless to read on the server, where it is unused.
        input.read(TAG_DISPLAY_RESOURCES, ChestResourceContents.CODEC)
                .ifPresentOrElse(this.displayResources::copyFrom, this.displayResources::clear);
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
        output.putString(TAG_RESOURCE_MODE, this.resourceMode.id());
        ContainerHelper.saveAllItems(output.child(TAG_TRANSFER_ITEMS), this.transferItems);
        ContainerHelper.saveAllItems(output.child(TAG_MODE_CARD), this.modeItems);
        // Only this block's own tank, never the shared pool: that belongs to GlobalTieredChestData.
        output.store(TAG_LOCAL_RESOURCES, ChestResourceContents.CODEC, this.localResources);
        this.saveLocalItems(output);
        this.upgradeManager.save(output);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(TAG_GLOBAL_STORAGE_ID, this.globalStorageId);
        tag.putBoolean(TAG_STORAGE_MODE, this.getStorageMode() == ChestStorageMode.GLOBAL);
        tag.putBoolean(TAG_LOCKED, this.locked);
        tag.putString(TAG_OWNER_UUID, this.ownerUuid == null ? "" : this.ownerUuid.toString());
        tag.putString(TAG_RESOURCE_MODE, this.resourceMode.id());
        // Whatever storage is actually live, so the client mirror is right in both LOCAL and GLOBAL.
        ChestResourceContents active = this.getActiveResources();
        ChestResourceContents.CODEC
                .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), active)
                .resultOrPartial(BobbyChests.LOGGER::error)
                .ifPresent(encoded -> tag.put(TAG_DISPLAY_RESOURCES, encoded));
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

    private void loadLocalItems(ValueInput input) {
        var storedSlots = input.read(TAG_LOCAL_ITEMS, StoredSlot.CODEC.listOf());
        if (storedSlots.isEmpty()) {
            return;
        }

        NonNullList<ItemStack> local = this.localItems();
        for (int slot = 0; slot < local.size(); slot++) {
            local.set(slot, ItemStack.EMPTY);
        }
        for (StoredSlot stored : storedSlots.get()) {
            if (stored.slot() < 0 || stored.slot() >= local.size()) {
                continue;
            }
            local.set(stored.slot(), stored.stack().copy());
        }
    }

    private void saveLocalItems(ValueOutput output) {
        NonNullList<ItemStack> local = this.localItems();
        ArrayList<StoredSlot> storedSlots = new ArrayList<>();
        for (int slot = 0; slot < local.size(); slot++) {
            ItemStack stack = local.get(slot);
            if (!stack.isEmpty()) {
                storedSlots.add(new StoredSlot(slot, stack.copy()));
            }
        }
        output.store(TAG_LOCAL_ITEMS, StoredSlot.CODEC.listOf(), storedSlots);
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
            // Only reached without a retain card; with one, the early return above leaves these to
            // travel on the dropped chest item alongside everything else it saved.
            this.dropTransferItems(level, pos);
            this.dropModeCard(level, pos);
        }
        // Fluid and FE cannot be spilled as entities, so without a retain card they are simply gone.
        // The retain path above returns before this, having copied them onto the dropped chest item.
        if (level != null && !level.isClientSide() && this.getStorageMode() == ChestStorageMode.LOCAL) {
            this.localResources.clear();
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

