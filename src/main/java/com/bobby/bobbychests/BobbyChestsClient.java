package com.bobby.bobbychests;

import com.bobby.bobbychests.blockentity.ModBlockEntities;
import com.bobby.bobbychests.client.render.FirstChestRenderer;
import com.bobby.bobbychests.client.screen.FirstChestScreen;
import com.bobby.bobbychests.menu.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = BobbyChests.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = BobbyChests.MODID, value = Dist.CLIENT)
public class BobbyChestsClient {
    public BobbyChestsClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        BobbyChests.LOGGER.info("HELLO FROM CLIENT SETUP");
        BobbyChests.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        event.enqueueWork(() -> BlockEntityRenderers.register(ModBlockEntities.FIRST_CHEST.get(), FirstChestRenderer::new));
    }
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event){
        event.register(ModMenus.FIRST_CHEST_MENU.get(), FirstChestScreen::new);
    }
}
