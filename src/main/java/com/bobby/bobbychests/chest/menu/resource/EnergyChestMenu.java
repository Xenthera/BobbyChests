package com.bobby.bobbychests.chest.menu.resource;

import com.bobby.bobbychests.chest.ChestTier;
import com.bobby.bobbychests.chest.menu.AbstractChestMenu;
import com.bobby.bobbychests.chest.storage.ChestTransferContainer;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

import java.util.UUID;

public final class EnergyChestMenu extends AbstractResourceChestMenu {

    public static EnergyChestMenu clientConstructor(int syncId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        var p = AbstractChestMenu.TieredChestClientPayload.read(buf);
        return new EnergyChestMenu(
                syncId,
                playerInventory,
                new SimpleContainer(ChestTransferContainer.SIZE),
                new SimpleContainer(p.tier().upgradeSlotCount()),
                p.chestPos(),
                p.initialChestId(),
                p.initialLocked(),
                p.initialOwnerUuid(),
                p.initialUsingGlobalStorage(),
                p.maxChannelId(),
                p.tier());
    }

    public EnergyChestMenu(
            int syncID,
            Inventory playerInventory,
            Container container,
            Container upgradeContainer,
            BlockPos chestPos,
            int initialChestId,
            boolean initialLocked,
            UUID initialOwnerUuid,
            boolean initialUsingGlobalStorage,
            int maxChannelId,
            ChestTier tier) {
        super(ModMenus.ENERGY_CHEST_MENU.get(), syncID, playerInventory, container, upgradeContainer,
                chestPos, initialChestId, initialLocked, initialOwnerUuid, initialUsingGlobalStorage,
                maxChannelId, tier);
    }

    @Override
    public ChestResourceMode expectedResourceMode() {
        return ChestResourceMode.ENERGY;
    }

    public int getCapacityFe() {
        return this.getTier().energyCapacityFe();
    }
}
