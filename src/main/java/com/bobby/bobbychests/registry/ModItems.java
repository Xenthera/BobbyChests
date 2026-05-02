package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.item.ChestUpgradeCardItem;
import com.bobby.bobbychests.item.CreativeInfiniteUpgradeCardItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {

    public static DeferredRegister.Items ITEMS = DeferredRegister.createItems(BobbyChests.MODID);

    public static final DeferredItem<ChestUpgradeCardItem> NETWORKING_UPGRADE_CARD =
            ITEMS.registerItem("networking_upgrade_card", ChestUpgradeCardItem::new);

    public static final DeferredItem<CreativeInfiniteUpgradeCardItem> INFINITE_UPGRADE_CARD =
            ITEMS.registerItem("infinite_upgrade_card", CreativeInfiniteUpgradeCardItem::new);

    public static final DeferredItem<ChestUpgradeCardItem> VOID_UPGRADE_CARD =
            ITEMS.registerItem("void_upgrade_card", ChestUpgradeCardItem::new);

    public static final DeferredItem<ChestUpgradeCardItem> LEAVE_LAST_ITEM_UPGRADE_CARD =
            ITEMS.registerItem("leave_last_item_upgrade_card", ChestUpgradeCardItem::new);

    public static final DeferredItem<ChestUpgradeCardItem> RETAIN_ITEMS_UPGRADE_CARD =
            ITEMS.registerItem("retain_items_upgrade_card", ChestUpgradeCardItem::new);

    public static final DeferredItem<ChestUpgradeCardItem> LOCK_UPGRADE_CARD =
            ITEMS.registerItem("lock_upgrade_card", ChestUpgradeCardItem::new);

    public static final DeferredItem<ChestUpgradeCardItem> DEEP_STORAGE_UPGRADE_CARD =
            ITEMS.registerItem("deep_storage_upgrade_card", ChestUpgradeCardItem::new);


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
