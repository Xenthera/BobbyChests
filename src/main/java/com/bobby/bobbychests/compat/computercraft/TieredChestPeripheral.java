package com.bobby.bobbychests.compat.computercraft;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TieredChestPeripheral implements IPeripheral {
    private final AbstractTieredChestBlockEntity chest;
    private final @Nullable Direction side;

    public TieredChestPeripheral(AbstractTieredChestBlockEntity chest, @Nullable Direction side) {
        this.chest = chest;
        this.side = side;
    }

    @Override
    public String getType() {
        return BobbyChests.MODID + ":tiered_chest";
    }

    @Override
    public Set<String> getAdditionalTypes() {
        return this.isPrivateChest() ? Set.of() : Set.of("inventory");
    }

    @Override
    public @Nullable Object getTarget() {
        return this.isPrivateChest() ? null : this.chest;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof TieredChestPeripheral peripheral && peripheral.chest == this.chest;
    }

    @LuaFunction(mainThread = true)
    public final boolean isPrivate() {
        return this.isPrivateChest();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer getChestId() {
        if (this.isPrivateChest()) {
            return null;
        }
        return this.chest.getGlobalStorageId();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer setChestId(int id) {
        if (this.isPrivateChest()) {
            return null;
        }
        this.chest.setGlobalStorageId(id);
        return this.chest.getGlobalStorageId();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer getMaxChestId() {
        if (this.isPrivateChest()) {
            return null;
        }
        return this.chest.getTier().maxChannelId();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable String getTier() {
        if (this.isPrivateChest()) {
            return null;
        }
        return this.chest.getTier().id();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable String getOwner() {
        return this.chest.getOwnerUuid() == null ? null : this.chest.getOwnerUuid().toString();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer size() {
        ResourceHandler<ItemResource> handler = this.handler();
        return handler == null ? null : handler.size();
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer getItemCount(Optional<Integer> id) {
        if (this.isPrivateChest() || !(this.chest.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        int channelId = id.orElse(this.chest.getGlobalStorageId());
        if (channelId < 0 || channelId > this.chest.getTier().maxChannelId()) {
            return null;
        }
        return GlobalTieredChestData.get(serverLevel).getItemCountForChestId(this.chest, channelId);
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Map<Integer, Map<String, ?>> list() {
        ResourceHandler<ItemResource> handler = this.handler();
        if (handler == null || !(this.chest.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Map<Integer, Map<String, ?>> result = new HashMap<>();
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            int amount = handler.getAmountAsInt(slot);
            if (!resource.isEmpty() && amount > 0) {
                result.put(slot + 1, VanillaDetailRegistries.ITEM_STACK.getBasicDetails(serverLevel.registryAccess(), resource.toStack(amount)));
            }
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Map<String, ?> getItemDetail(int slot) throws LuaException {
        ResourceHandler<ItemResource> handler = this.handler();
        if (handler == null || !(this.chest.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        checkSlot(slot, handler.size());
        ItemResource resource = handler.getResource(slot - 1);
        int amount = handler.getAmountAsInt(slot - 1);
        return resource.isEmpty() || amount <= 0
                ? null
                : VanillaDetailRegistries.ITEM_STACK.getDetails(serverLevel.registryAccess(), resource.toStack(amount));
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Long getItemLimit(int slot) throws LuaException {
        ResourceHandler<ItemResource> handler = this.handler();
        if (handler == null) {
            return null;
        }

        checkSlot(slot, handler.size());
        ItemResource resource = handler.getResource(slot - 1);
        return handler.getCapacityAsLong(slot - 1, resource.isEmpty() ? ItemResource.of(Items.DIRT) : resource);
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer pushItems(
            IComputerAccess computer,
            String toName,
            int fromSlot,
            Optional<Integer> limit,
            Optional<Integer> toSlot
    ) throws LuaException {
        ResourceHandler<ItemResource> from = this.handler();
        if (from == null) {
            return null;
        }

        ResourceHandler<ItemResource> to = extractHandler(computer.getAvailablePeripheral(toName));
        if (to == null) {
            throw new LuaException("Target '" + toName + "' is not an inventory");
        }

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        checkSlot(fromSlot, from.size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) {
            checkSlot(toSlot.get(), to.size(), "To slot out of range (%s)");
        }
        return actualLimit <= 0 ? 0 : moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    @LuaFunction(mainThread = true)
    public final @Nullable Integer pullItems(
            IComputerAccess computer,
            String fromName,
            int fromSlot,
            Optional<Integer> limit,
            Optional<Integer> toSlot
    ) throws LuaException {
        ResourceHandler<ItemResource> to = this.handler();
        if (to == null) {
            return null;
        }

        ResourceHandler<ItemResource> from = extractHandler(computer.getAvailablePeripheral(fromName));
        if (from == null) {
            throw new LuaException("Source '" + fromName + "' is not an inventory");
        }

        int actualLimit = limit.orElse(Integer.MAX_VALUE);
        checkSlot(fromSlot, from.size(), "From slot out of range (%s)");
        if (toSlot.isPresent()) {
            checkSlot(toSlot.get(), to.size(), "To slot out of range (%s)");
        }
        return actualLimit <= 0 ? 0 : moveItem(from, fromSlot - 1, to, toSlot.orElse(0) - 1, actualLimit);
    }

    private boolean isPrivateChest() {
        return this.chest.isLocked();
    }

    private @Nullable ResourceHandler<ItemResource> handler() {
        if (this.isPrivateChest()) {
            return null;
        }
        return this.chest.getItemResourceHandler(this.side);
    }

    private static void checkSlot(int slot, int size) throws LuaException {
        checkSlot(slot, size, "Slot out of range (%s)");
    }

    private static void checkSlot(int slot, int size, String message) throws LuaException {
        if (slot < 1 || slot > size) {
            throw new LuaException(message.formatted(slot));
        }
    }

    private static @Nullable ResourceHandler<ItemResource> extractHandler(@Nullable IPeripheral peripheral) {
        if (peripheral == null) {
            return null;
        }
        if (peripheral instanceof TieredChestPeripheral tieredChestPeripheral) {
            return tieredChestPeripheral.handler();
        }

        Object target = peripheral.getTarget();
        if (target instanceof BlockEntity blockEntity) {
            if (blockEntity.isRemoved() || !(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
                return null;
            }
            ResourceHandler<ItemResource> handler = serverLevel.getCapability(
                    Capabilities.Item.BLOCK,
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    blockEntity,
                    peripheral instanceof TieredChestPeripheral tieredChestPeripheral ? tieredChestPeripheral.side : null
            );
            if (handler != null) {
                return handler;
            }
        }

        return target instanceof Container container ? VanillaContainerWrapper.of(container) : null;
    }

    private static int moveItem(ResourceHandler<ItemResource> from, int fromSlot, ResourceHandler<ItemResource> to, int toSlot, int limit) {
        ItemResource resource = from.getResource(fromSlot);
        if (resource.isEmpty()) {
            return 0;
        }

        int maxExtracted;
        try (Transaction transaction = Transaction.openRoot()) {
            maxExtracted = from.extract(fromSlot, resource, limit, transaction);
        }
        if (maxExtracted <= 0) {
            return 0;
        }

        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = insert(to, toSlot, resource, maxExtracted, transaction);
            if (accepted <= 0) {
                return 0;
            }
            if (from.extract(fromSlot, resource, accepted, transaction) != accepted) {
                return 0;
            }
            transaction.commit();
            return accepted;
        }
    }

    private static int insert(ResourceHandler<ItemResource> to, int toSlot, ItemResource resource, int amount, Transaction transaction) {
        if (toSlot >= 0) {
            return to.insert(toSlot, resource, amount, transaction);
        }

        int inserted = 0;
        for (int slot = 0; slot < to.size() && inserted < amount; slot++) {
            inserted += to.insert(slot, resource, amount - inserted, transaction);
        }
        return inserted;
    }
}
