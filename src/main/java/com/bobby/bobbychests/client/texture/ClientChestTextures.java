package com.bobby.bobbychests.client.texture;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.ChestSpriteNames;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpriteSourcesEvent;

@EventBusSubscriber(modid = BobbyChests.MODID, value = Dist.CLIENT)
public final class ClientChestTextures {

    private ClientChestTextures() {
    }

    @SubscribeEvent
    public static void registerSpriteSources(RegisterSpriteSourcesEvent event) {
        event.register(ChestSpriteNames.CHEST_COMPOSITE_SOURCE, ChestCompositeSource.MAP_CODEC);
    }
}
