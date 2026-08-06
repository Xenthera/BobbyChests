package com.bobby.bobbychests.chest.storage;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.block.AbstractTieredChestBlock;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.ChestTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class GlobalTieredChestData extends SavedData {

    /**
     * Runtime-only key used to track which chests reference which storage.
     * Not persisted; it is rebuilt as chunks load.
     */
    public record StorageKey(ChestTier tier, UUID owner, int id) {}
    public record DimPos(ResourceKey<Level> dimension, BlockPos pos) {}

    private record StorageEntry(String tier, String owner, int id, List<ItemStack> items) {
        static final Codec<StorageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("Tier", "").forGetter(StorageEntry::tier),
                Codec.STRING.optionalFieldOf("Owner", "").forGetter(StorageEntry::owner),
                Codec.INT.fieldOf("Id").forGetter(StorageEntry::id),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("Items").forGetter(StorageEntry::items)
        ).apply(instance, StorageEntry::new));
    }

    /** Fluid and energy counterpart to {@link StorageEntry}, keyed the same (tier, owner, id) way. */
    private record ResourceEntry(String tier, String owner, int id, ChestResourceContents contents) {
        static final Codec<ResourceEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("Tier", "").forGetter(ResourceEntry::tier),
                Codec.STRING.optionalFieldOf("Owner", "").forGetter(ResourceEntry::owner),
                Codec.INT.fieldOf("Id").forGetter(ResourceEntry::id),
                ChestResourceContents.CODEC.fieldOf("Contents").forGetter(ResourceEntry::contents)
        ).apply(instance, ResourceEntry::new));
    }

    // How Minecraft serializes this SavedData instance to disk (and back).
    // "Resources" is optional and defaults to empty so worlds saved before fluid/energy chests existed
    // keep loading untouched.
    public static final Codec<GlobalTieredChestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StorageEntry.CODEC.listOf().optionalFieldOf("Storages", List.of()).forGetter(GlobalTieredChestData::toEntryList),
            ResourceEntry.CODEC.listOf().optionalFieldOf("Resources", List.of()).forGetter(GlobalTieredChestData::toResourceEntryList)
    ).apply(instance, GlobalTieredChestData::fromEntries));

    /**
     * IMPORTANT: keep the same SavedData id so existing worlds keep loading.
     */
    public static final SavedDataType<GlobalTieredChestData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "global_bobby_base_chest"),
            GlobalTieredChestData::new,
            CODEC
    );

    private final Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> publicStorages;
    private final Map<UUID, Map<ChestTier, Map<Integer, NonNullList<ItemStack>>>> privateStorages;

    // Fluid/energy pools, kept wholly separate from the item pools above: a fluid chest on channel 5
    // and an item chest on channel 5 are different storages that happen to share a channel number.
    private final Map<ChestTier, Map<Integer, ChestResourceContents>> publicResources;
    private final Map<UUID, Map<ChestTier, Map<Integer, ChestResourceContents>>> privateResources;

    // Runtime-only index so we can notify comparators for all chests on the same key.
    private final Map<StorageKey, Set<DimPos>> attachedChests = new HashMap<>();
    private final Map<DimPos, StorageKey> chestIndex = new HashMap<>();
    private final Map<StorageKey, Integer> openViewers = new HashMap<>();

    /** Used when the file doesn't exist yet. */
    public GlobalTieredChestData() {
        this.publicStorages = new HashMap<>();
        this.privateStorages = new HashMap<>();
        this.publicResources = new HashMap<>();
        this.privateResources = new HashMap<>();
    }

    /** Used when loading from disk (Codec path). */
    private GlobalTieredChestData(Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> publicStorages,
                                  Map<UUID, Map<ChestTier, Map<Integer, NonNullList<ItemStack>>>> privateStorages,
                                  Map<ChestTier, Map<Integer, ChestResourceContents>> publicResources,
                                  Map<UUID, Map<ChestTier, Map<Integer, ChestResourceContents>>> privateResources) {
        this.publicStorages = publicStorages;
        this.privateStorages = privateStorages;
        this.publicResources = publicResources;
        this.privateResources = privateResources;
    }

    private static NonNullList<ItemStack> resizedCopy(List<ItemStack> list, int slotCount) {
        NonNullList<ItemStack> items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < slotCount && i < list.size(); i++) {
            items.set(i, list.get(i).copy());
        }
        return items;
    }

    private static GlobalTieredChestData fromEntries(List<StorageEntry> entries, List<ResourceEntry> resourceEntries) {
        Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> publicStorages = new HashMap<>();
        Map<UUID, Map<ChestTier, Map<Integer, NonNullList<ItemStack>>>> privateStorages = new HashMap<>();
        Map<ChestTier, Map<Integer, ChestResourceContents>> publicResources = new HashMap<>();
        Map<UUID, Map<ChestTier, Map<Integer, ChestResourceContents>>> privateResources = new HashMap<>();
        for (ResourceEntry entry : resourceEntries) {
            ChestTier tier = ChestTier.fromIdOrDefault(entry.tier(), ChestTier.WOOD);
            ChestResourceContents contents = entry.contents().copy();
            if (entry.owner() == null || entry.owner().isEmpty()) {
                publicResources.computeIfAbsent(tier, ignored -> new HashMap<>()).put(entry.id(), contents);
                continue;
            }
            privateResources
                    .computeIfAbsent(UUID.fromString(entry.owner()), ignored -> new HashMap<>())
                    .computeIfAbsent(tier, ignored -> new HashMap<>())
                    .put(entry.id(), contents);
        }
        for (StorageEntry entry : entries) {
            // If the tier is missing (older saves), treat it as WOOD. It's the only tier currently in the mod.
            ChestTier tier = ChestTier.fromIdOrDefault(entry.tier(), ChestTier.WOOD);

            // Must be a mutable NonNullList: NonNullList.copyOf wraps codec lists that are often immutable
            // (List.of / unmodifiable), and routed container mutations would throw after load.
            List<ItemStack> rawItems = entry.items();
            NonNullList<ItemStack> loaded = resizedCopy(rawItems, rawItems.size());

            if (entry.owner() == null || entry.owner().isEmpty()) {
                Map<Integer, NonNullList<ItemStack>> byId = publicStorages.computeIfAbsent(tier, ignored -> new HashMap<>());
                byId.put(entry.id(), loaded);
                continue;
            }
            UUID ownerUuid = UUID.fromString(entry.owner());
            Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> byTier = privateStorages.computeIfAbsent(ownerUuid, ignored -> new HashMap<>());
            Map<Integer, NonNullList<ItemStack>> byId = byTier.computeIfAbsent(tier, ignored -> new HashMap<>());
            byId.put(entry.id(), loaded);
        }
        return new GlobalTieredChestData(publicStorages, privateStorages, publicResources, privateResources);
    }

    private List<ResourceEntry> toResourceEntryList() {
        if (this.publicResources.isEmpty() && this.privateResources.isEmpty()) {
            return List.of();
        }
        List<ResourceEntry> entries = new ArrayList<>();
        for (var tierEntry : this.publicResources.entrySet()) {
            String tier = tierEntry.getKey().id();
            for (var e : tierEntry.getValue().entrySet()) {
                // Skip drained storages so an empty channel someone once opened does not live on disk forever.
                if (e.getValue().isEmpty()) {
                    continue;
                }
                entries.add(new ResourceEntry(tier, "", e.getKey(), e.getValue().copy()));
            }
        }
        for (var ownerEntry : this.privateResources.entrySet()) {
            String owner = ownerEntry.getKey().toString();
            for (var tierEntry : ownerEntry.getValue().entrySet()) {
                String tier = tierEntry.getKey().id();
                for (var e : tierEntry.getValue().entrySet()) {
                    if (e.getValue().isEmpty()) {
                        continue;
                    }
                    entries.add(new ResourceEntry(tier, owner, e.getKey(), e.getValue().copy()));
                }
            }
        }
        return entries;
    }

    private List<StorageEntry> toEntryList() {
        if (this.publicStorages.isEmpty() && this.privateStorages.isEmpty()) {
            return List.of();
        }
        int sizeHint = 0;
        for (var e : this.publicStorages.entrySet()) {
            sizeHint += e.getValue().size();
        }
        for (var ownerEntry : this.privateStorages.entrySet()) {
            for (var tierEntry : ownerEntry.getValue().entrySet()) {
                sizeHint += tierEntry.getValue().size();
            }
        }
        List<StorageEntry> entries = new ArrayList<>(sizeHint);
        for (var tierEntry : this.publicStorages.entrySet()) {
            String tier = tierEntry.getKey().id();
            for (var e : tierEntry.getValue().entrySet()) {
                entries.add(new StorageEntry(tier, "", e.getKey(), List.copyOf(e.getValue())));
            }
        }
        for (var ownerEntry : this.privateStorages.entrySet()) {
            String owner = ownerEntry.getKey().toString();
            for (var tierEntry : ownerEntry.getValue().entrySet()) {
                String tier = tierEntry.getKey().id();
                for (var e : tierEntry.getValue().entrySet()) {
                    entries.add(new StorageEntry(tier, owner, e.getKey(), List.copyOf(e.getValue())));
                }
            }
        }
        return entries;
    }

    public static GlobalTieredChestData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    private NonNullList<ItemStack> getItemsPublic(ChestTier tier, int id, int slotCount) {
        Map<Integer, NonNullList<ItemStack>> byId = this.publicStorages.computeIfAbsent(tier, ignored -> new HashMap<>());
        NonNullList<ItemStack> items = byId.computeIfAbsent(id, ignored -> NonNullList.withSize(slotCount, ItemStack.EMPTY));
        if (items.size() != slotCount) {
            items = resizedCopy(items, slotCount);
            byId.put(id, items);
        }
        return items;
    }

    private NonNullList<ItemStack> getItemsPrivate(ChestTier tier, UUID owner, int id, int slotCount) {
        Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> byTier = this.privateStorages.computeIfAbsent(owner, ignored -> new HashMap<>());
        Map<Integer, NonNullList<ItemStack>> byId = byTier.computeIfAbsent(tier, ignored -> new HashMap<>());
        NonNullList<ItemStack> items = byId.computeIfAbsent(id, ignored -> NonNullList.withSize(slotCount, ItemStack.EMPTY));
        if (items.size() != slotCount) {
            items = resizedCopy(items, slotCount);
            byId.put(id, items);
        }
        return items;
    }

    public NonNullList<ItemStack> getItemsForChest(AbstractTieredChestBlockEntity chest) {
        int id = chest.getGlobalStorageId();
        int slotCount = chest.getSlotCount();
        if (chest.isLocked() && chest.getOwnerUuid() != null) {
            return getItemsPrivate(chest.getTier(), chest.getOwnerUuid(), id, slotCount);
        }
        return getItemsPublic(chest.getTier(), id, slotCount);
    }

    /**
     * The shared fluid/energy contents for {@code chest}'s current storage key.
     *
     * <p>Mirrors {@link #getItemsForChest} exactly: locked chests read the owner's private pool,
     * everything else reads the public one. The returned holder is live, so callers mutate it in
     * place and then {@link #markChangedAndNotify} to persist and notify.
     */
    public ChestResourceContents getResourcesForChest(AbstractTieredChestBlockEntity chest) {
        int id = chest.getGlobalStorageId();
        if (chest.isLocked() && chest.getOwnerUuid() != null) {
            return this.privateResources
                    .computeIfAbsent(chest.getOwnerUuid(), ignored -> new HashMap<>())
                    .computeIfAbsent(chest.getTier(), ignored -> new HashMap<>())
                    .computeIfAbsent(id, ignored -> new ChestResourceContents());
        }
        return this.publicResources
                .computeIfAbsent(chest.getTier(), ignored -> new HashMap<>())
                .computeIfAbsent(id, ignored -> new ChestResourceContents());
    }

    /**
     * Count items for a channel without creating a new empty storage entry if that channel has never existed.
     */
    public int getItemCountForChestId(AbstractTieredChestBlockEntity chest, int id) {
        NonNullList<ItemStack> items;
        if (chest.isLocked() && chest.getOwnerUuid() != null) {
            Map<ChestTier, Map<Integer, NonNullList<ItemStack>>> byTier = this.privateStorages.get(chest.getOwnerUuid());
            if (byTier == null) {
                return 0;
            }
            Map<Integer, NonNullList<ItemStack>> byId = byTier.get(chest.getTier());
            if (byId == null) {
                return 0;
            }
            items = byId.get(id);
        } else {
            Map<Integer, NonNullList<ItemStack>> byId = this.publicStorages.get(chest.getTier());
            if (byId == null) {
                return 0;
            }
            items = byId.get(id);
        }
        if (items == null) {
            return 0;
        }

        int total = 0;
        for (ItemStack item : items) {
            total += item.getCount();
        }
        return total;
    }

    public StorageKey keyForChest(AbstractTieredChestBlockEntity chest) {
        UUID owner = (chest.isLocked() ? chest.getOwnerUuid() : null);
        return new StorageKey(chest.getTier(), owner, chest.getGlobalStorageId());
    }

    private boolean isPublicKey(StorageKey key) {
        return key.owner() == null;
    }

    public int getPublicOpenViewerCount(StorageKey key) {
        if (!isPublicKey(key)) {
            return 0;
        }
        return this.openViewers.getOrDefault(key, 0);
    }

    /**
     * Register or rebind a chest position to the current storage key.
     * Call on chunk load and whenever the chest's id/lock changes.
     */
    public void registerOrUpdateChest(AbstractTieredChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        DimPos dimPos = new DimPos(serverLevel.dimension(), chest.getBlockPos());
        StorageKey newKey = keyForChest(chest);
        StorageKey oldKey = this.chestIndex.put(dimPos, newKey);
        if (oldKey != null && oldKey.equals(newKey)) {
            return;
        }
        if (oldKey != null) {
            Set<DimPos> oldSet = this.attachedChests.get(oldKey);
            if (oldSet != null) {
                oldSet.remove(dimPos);
                if (oldSet.isEmpty()) {
                    this.attachedChests.remove(oldKey);
                }
            }
        }
        this.attachedChests.computeIfAbsent(newKey, ignored -> new HashSet<>()).add(dimPos);

        // If this public storage is currently "open somewhere", newly loaded chests should appear open too.
        if (isPublicKey(newKey) && this.openViewers.getOrDefault(newKey, 0) > 0) {
            setLidOpen(serverLevel, dimPos, true);
        }
    }

    public void unregisterChest(AbstractTieredChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        DimPos dimPos = new DimPos(serverLevel.dimension(), chest.getBlockPos());
        StorageKey oldKey = this.chestIndex.remove(dimPos);
        if (oldKey == null) {
            return;
        }
        Set<DimPos> set = this.attachedChests.get(oldKey);
        if (set != null) {
            set.remove(dimPos);
            if (set.isEmpty()) {
                this.attachedChests.remove(oldKey);
            }
        }
    }

    /**
     * Tells every chest sharing {@code key} that the pooled level moved.
     *
     * <p>Each one re-checks against its own last-sent state and sends if it differs, so there is no
     * double send and each keeps its own rate limit.
     *
     * <p>This has to be pushed rather than left for each chest to notice on its own tick. The chest
     * that was actually interacted with learns about the change directly, but the others have no
     * event to hang it on — their contents changed without anything happening to them.
     */
    public void notifyPooledLevelChanged(ServerLevel anyLevel, StorageKey key) {
        this.notifyPooledLevelChanged(anyLevel, key, false);
    }

    /**
     * @param force when true, bypass each chest's level-broadcast rate limit (hand transfers).
     */
    public void notifyPooledLevelChanged(ServerLevel anyLevel, StorageKey key, boolean force) {
        forEachAttached(anyLevel, key, (targetLevel, pos) -> {
            if (targetLevel.getBlockEntity(pos) instanceof AbstractTieredChestBlockEntity chest) {
                if (force) {
                    chest.maybeBroadcastResourceLevelFromPool(true);
                } else {
                    chest.sendResourceLevelIfChanged();
                }
            }
        });
    }

    public void notifyStorageChanged(ServerLevel level, StorageKey key) {
        forEachAttached(level, key, (targetLevel, pos) -> {
            Block block = targetLevel.getBlockState(pos).getBlock();
            targetLevel.updateNeighbourForOutputSignal(pos, block);
            targetLevel.updateNeighborsAt(pos, block);
        });
    }

    /**
     * Flip a hidden chest blockstate so observers can detect an "open" event.
     * Both state variants should point at the same model in the blockstate json.
     */
    public void toggleObserverSignal(ServerLevel level, StorageKey key) {
        forEachAttached(level, key, (targetLevel, pos) -> {
            var state = targetLevel.getBlockState(pos);
            if (!state.hasProperty(AbstractTieredChestBlock.OBSERVER_OPEN)) {
                return;
            }
            boolean next = !state.getValue(AbstractTieredChestBlock.OBSERVER_OPEN);
            // Use flags that notify neighbors; the state change is what observers actually detect.
            targetLevel.setBlock(pos, state.setValue(AbstractTieredChestBlock.OBSERVER_OPEN, next), 3);
        });
    }

    private static void setLidOpen(ServerLevel level, DimPos dimPos, boolean open) {
        ServerLevel target = level.getServer().getLevel(dimPos.dimension());
        if (target == null) {
            return;
        }
        var state = target.getBlockState(dimPos.pos());
        Block block = state.getBlock();
        if (open && ChestBlock.isChestBlockedAt(target, dimPos.pos())) {
            // If a chest is blocked from above, vanilla won't open it. Keep it visually closed until unblocked.
            return;
        }
        // ChestBlockEntity listens for blockEvent id=1 to drive ChestLidController.shouldBeOpen(count>0).
        target.blockEvent(dimPos.pos(), block, 1, open ? 1 : 0);
    }

    /**
     * Called when the block above a chest changes. If the chest is on a public key with active viewers,
     * open/close this specific chest based on whether it's blocked from above.
     */
    public void onChestAboveChanged(ServerLevel level, BlockPos chestPos) {
        var be = level.getBlockEntity(chestPos);
        if (!(be instanceof AbstractTieredChestBlockEntity chest)) {
            return;
        }
        StorageKey key = keyForChest(chest);
        if (!isPublicKey(key)) {
            return;
        }
        if (this.openViewers.getOrDefault(key, 0) <= 0) {
            return;
        }
        boolean blocked = ChestBlock.isChestBlockedAt(level, chestPos);
        setLidOpen(level, new DimPos(level.dimension(), chestPos), !blocked);
    }

    private void setAllLidsOpen(ServerLevel level, StorageKey key, boolean open) {
        Set<DimPos> positions = this.attachedChests.get(key);
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (DimPos dimPos : List.copyOf(positions)) {
            setLidOpen(level, dimPos, open);
        }
    }

    public void onChestOpen(AbstractTieredChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        StorageKey key = keyForChest(chest);
        if (!isPublicKey(key)) {
            return;
        }
        adjustPublicOpenViewers(serverLevel, key, 1);
    }

    public void onChestClose(AbstractTieredChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        StorageKey key = keyForChest(chest);
        if (!isPublicKey(key)) {
            return;
        }
        adjustPublicOpenViewers(serverLevel, key, -1);
    }

    public void onChestKeyChangedWhileOpen(ServerLevel level, StorageKey oldKey, StorageKey newKey, int openerCount) {
        if (openerCount <= 0) {
            return;
        }
        if (oldKey != null && isPublicKey(oldKey)) {
            adjustPublicOpenViewers(level, oldKey, -openerCount);
        }
        if (newKey != null && isPublicKey(newKey)) {
            adjustPublicOpenViewers(level, newKey, openerCount);
        }
    }

    private void adjustPublicOpenViewers(ServerLevel level, StorageKey key, int delta) {
        if (delta == 0) {
            return;
        }
        int previous = this.openViewers.getOrDefault(key, 0);
        int next = previous + delta;
        if (next <= 0) {
            if (previous > 0) {
                this.openViewers.remove(key);
                setAllLidsOpen(level, key, false);
            }
            return;
        }
        this.openViewers.put(key, next);
        if (previous <= 0) {
            setAllLidsOpen(level, key, true);
            toggleObserverSignal(level, key);
        }
    }

    public void markChangedAndNotify(AbstractTieredChestBlockEntity chest) {
        this.setDirty();
        if (chest.getLevel() instanceof ServerLevel serverLevel) {
            StorageKey key = keyForChest(chest);
            notifyStorageChanged(serverLevel, key);
        }
    }

    @FunctionalInterface
    private interface LevelPosConsumer {
        void accept(ServerLevel level, BlockPos pos);
    }

    private void forEachAttached(ServerLevel anyLevel, StorageKey key, LevelPosConsumer consumer) {
        Set<DimPos> positions = this.attachedChests.get(key);
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (DimPos dimPos : List.copyOf(positions)) {
            ServerLevel target = anyLevel.getServer().getLevel(dimPos.dimension());
            if (target == null) {
                continue;
            }
            consumer.accept(target, dimPos.pos());
        }
    }

    public void markChanged() {
        this.setDirty();
    }

    /**
     * DEV/DEBUG helper: wipe all stored inventories.
     * This is irreversible for the current world save.
     */
    public void clearAllItems() {
        this.publicStorages.clear();
        this.privateStorages.clear();
        this.publicResources.clear();
        this.privateResources.clear();
        this.setDirty();
    }
}

