package com.bobby.bobbychests.globalcheststorage;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.block.BobbyBaseChestBlock;
import com.bobby.bobbychests.blockentity.BobbyBaseChestBlockEntity;
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

public class GlobalBobbyBaseChestData extends SavedData {

    public static final int SLOT_COUNT = 54; // 9 x 6

    /**
     * Runtime-only key used to track which chests reference which storage.
     * Not persisted; it is rebuilt as chunks load.
     */
    public record StorageKey(UUID owner, int id) {}
    public record DimPos(ResourceKey<Level> dimension, BlockPos pos) {}

    private record StorageEntry(String owner, int id, List<ItemStack> items) {
        static final Codec<StorageEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("Owner", "").forGetter(StorageEntry::owner),
                Codec.INT.fieldOf("Id").forGetter(StorageEntry::id),
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("Items").forGetter(StorageEntry::items)
        ).apply(instance, StorageEntry::new));
    }

    // How Minecraft serializes this SavedData instance to disk (and back).
    public static final Codec<GlobalBobbyBaseChestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StorageEntry.CODEC.listOf().optionalFieldOf("Storages", List.of()).forGetter(GlobalBobbyBaseChestData::toEntryList)
    ).apply(instance, GlobalBobbyBaseChestData::fromEntries));
    public static final SavedDataType<GlobalBobbyBaseChestData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "global_bobby_base_chest"),
            GlobalBobbyBaseChestData::new,   // brand-new world / missing file
            CODEC
    );

    private final Map<Integer, NonNullList<ItemStack>> publicStorages;
    private final Map<UUID, Map<Integer, NonNullList<ItemStack>>> privateStorages;

    // Runtime-only index so we can notify comparators for all chests on the same key.
    private final Map<StorageKey, Set<DimPos>> attachedChests = new HashMap<>();
    private final Map<DimPos, StorageKey> chestIndex = new HashMap<>();
    private final Map<StorageKey, Integer> openViewers = new HashMap<>();

    /** Used when the file doesn't exist yet. */
    public GlobalBobbyBaseChestData() {
        this.publicStorages = new HashMap<>();
        this.privateStorages = new HashMap<>();
    }
    /** Used when loading from disk (Codec path). */
    private GlobalBobbyBaseChestData(Map<Integer, NonNullList<ItemStack>> publicStorages,
                                     Map<UUID, Map<Integer, NonNullList<ItemStack>>> privateStorages) {
        this.publicStorages = publicStorages;
        this.privateStorages = privateStorages;
    }

    private static NonNullList<ItemStack> toFixedSizeStorage(List<ItemStack> list) {
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT && i < list.size(); i++) {
            items.set(i, list.get(i).copy());
        }
        return items;
    }

    private static GlobalBobbyBaseChestData fromEntries(List<StorageEntry> entries) {
        Map<Integer, NonNullList<ItemStack>> publicStorages = new HashMap<>();
        Map<UUID, Map<Integer, NonNullList<ItemStack>>> privateStorages = new HashMap<>();
        for (StorageEntry entry : entries) {
            if (entry.owner() == null || entry.owner().isEmpty()) {
                publicStorages.put(entry.id(), toFixedSizeStorage(entry.items()));
                continue;
            }
            UUID ownerUuid = UUID.fromString(entry.owner());
            Map<Integer, NonNullList<ItemStack>> byId = privateStorages.computeIfAbsent(ownerUuid, ignored -> new HashMap<>());
            byId.put(entry.id(), toFixedSizeStorage(entry.items()));
        }
        return new GlobalBobbyBaseChestData(publicStorages, privateStorages);
    }

    private List<StorageEntry> toEntryList() {
        if (this.publicStorages.isEmpty() && this.privateStorages.isEmpty()) {
            return List.of();
        }
        int sizeHint = this.publicStorages.size();
        for (var e : this.privateStorages.entrySet()) {
            sizeHint += e.getValue().size();
        }
        List<StorageEntry> entries = new ArrayList<>(sizeHint);
        for (var e : this.publicStorages.entrySet()) {
            entries.add(new StorageEntry("", e.getKey(), List.copyOf(e.getValue())));
        }
        for (var ownerEntry : this.privateStorages.entrySet()) {
            String owner = ownerEntry.getKey().toString();
            for (var e : ownerEntry.getValue().entrySet()) {
                entries.add(new StorageEntry(owner, e.getKey(), List.copyOf(e.getValue())));
            }
        }
        return entries;
    }

    public static GlobalBobbyBaseChestData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public NonNullList<ItemStack> getItemsPublic(int id) {
        return this.publicStorages.computeIfAbsent(id, ignored -> NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY));
    }

    public NonNullList<ItemStack> getItemsPrivate(UUID owner, int id) {
        Map<Integer, NonNullList<ItemStack>> byId = this.privateStorages.computeIfAbsent(owner, ignored -> new HashMap<>());
        return byId.computeIfAbsent(id, ignored -> NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY));
    }

    public NonNullList<ItemStack> getItemsForChest(BobbyBaseChestBlockEntity chest) {
        int id = chest.getGlobalStorageId();
        if (chest.isLocked() && chest.getOwnerUuid() != null) {
            return getItemsPrivate(chest.getOwnerUuid(), id);
        }
        return getItemsPublic(id);
    }

    public StorageKey keyForChest(BobbyBaseChestBlockEntity chest) {
        UUID owner = (chest.isLocked() ? chest.getOwnerUuid() : null);
        return new StorageKey(owner, chest.getGlobalStorageId());
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
    public void registerOrUpdateChest(BobbyBaseChestBlockEntity chest) {
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

    public void unregisterChest(BobbyBaseChestBlockEntity chest) {
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
            if (!(state.getBlock() instanceof BobbyBaseChestBlock)) {
                return;
            }
            if (!state.hasProperty(BobbyBaseChestBlock.OBSERVER_OPEN)) {
                return;
            }
            boolean next = !state.getValue(BobbyBaseChestBlock.OBSERVER_OPEN);
            // Use flags that notify neighbors; the state change is what observers actually detect.
            targetLevel.setBlock(pos, state.setValue(BobbyBaseChestBlock.OBSERVER_OPEN, next), 3);
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
        if (!(be instanceof BobbyBaseChestBlockEntity chest)) {
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
        // If it becomes blocked while globally open, force-close just this chest.
        // If it becomes unblocked while globally open, attempt to open it.
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

    public void onChestOpen(BobbyBaseChestBlockEntity chest) {
        if (!(chest.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        StorageKey key = keyForChest(chest);
        if (!isPublicKey(key)) {
            return;
        }
        adjustPublicOpenViewers(serverLevel, key, 1);
    }

    public void onChestClose(BobbyBaseChestBlockEntity chest) {
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
            // Only pulse observers on the closed -> open edge.
            toggleObserverSignal(level, key);
        }
    }

    public void markChangedAndNotify(BobbyBaseChestBlockEntity chest) {
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


}
