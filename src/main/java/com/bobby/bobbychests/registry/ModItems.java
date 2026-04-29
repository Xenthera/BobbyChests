package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.item.ChestUpgradeCardItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {

    public static DeferredRegister.Items ITEMS = DeferredRegister.createItems(BobbyChests.MODID);

    public static final DeferredItem<ChestUpgradeCardItem> NETWORKING_UPGRADE_CARD =
            ITEMS.registerItem("networking_upgrade_card", ChestUpgradeCardItem::new);


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
