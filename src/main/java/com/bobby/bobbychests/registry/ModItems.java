package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static DeferredRegister.Items ITEMS = DeferredRegister.createItems(BobbyChests.MODID);



    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
