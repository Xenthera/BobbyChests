package com.bobby.bobbychests;

import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.client.chest.render.copper.CopperChestRenderer;
import com.bobby.bobbychests.client.chest.render.diamond.DiamondChestRenderer;
import com.bobby.bobbychests.client.chest.render.dirt.DirtChestRenderer;
import com.bobby.bobbychests.client.chest.render.emerald.EmeraldChestRenderer;
import com.bobby.bobbychests.client.chest.render.gold.GoldChestRenderer;
import com.bobby.bobbychests.client.chest.render.iron.IronChestRenderer;
import com.bobby.bobbychests.client.chest.render.netherite.NetheriteChestRenderer;
import com.bobby.bobbychests.client.chest.render.wooden.WoodenChestRenderer;
import com.bobby.bobbychests.client.chest.screen.copper.CopperChestScreen;
import com.bobby.bobbychests.client.chest.screen.diamond.DiamondChestScreen;
import com.bobby.bobbychests.client.chest.screen.dirt.DirtChestScreen;
import com.bobby.bobbychests.client.chest.screen.emerald.EmeraldChestScreen;
import com.bobby.bobbychests.client.chest.screen.gold.GoldChestScreen;
import com.bobby.bobbychests.client.chest.screen.iron.IronChestScreen;
import com.bobby.bobbychests.client.chest.screen.netherite.NetheriteChestScreen;
import com.bobby.bobbychests.client.chest.screen.resource.EnergyChestScreen;
import com.bobby.bobbychests.client.chest.screen.resource.FluidChestScreen;
import com.bobby.bobbychests.client.chest.screen.wooden.WoodenChestScreen;
import com.bobby.bobbychests.registry.ModMenus;
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

        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlockEntities.WOODEN_CHEST.get(), WoodenChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.COPPER_CHEST.get(), CopperChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.IRON_CHEST.get(), IronChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.GOLD_CHEST.get(), GoldChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DIAMOND_CHEST.get(), DiamondChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.EMERALD_CHEST.get(), EmeraldChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.NETHERITE_CHEST.get(), NetheriteChestRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.DIRT_CHEST.get(), DirtChestRenderer::new);
        });
    }
    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event){
        event.register(ModMenus.WOODEN_CHEST_MENU.get(), WoodenChestScreen::new);
        event.register(ModMenus.COPPER_CHEST_MENU.get(), CopperChestScreen::new);
        event.register(ModMenus.IRON_CHEST_MENU.get(), IronChestScreen::new);
        event.register(ModMenus.GOLD_CHEST_MENU.get(), GoldChestScreen::new);
        event.register(ModMenus.DIAMOND_CHEST_MENU.get(), DiamondChestScreen::new);
        event.register(ModMenus.EMERALD_CHEST_MENU.get(), EmeraldChestScreen::new);
        event.register(ModMenus.NETHERITE_CHEST_MENU.get(), NetheriteChestScreen::new);
        event.register(ModMenus.DIRT_CHEST_MENU.get(), DirtChestScreen::new);
        event.register(ModMenus.FLUID_CHEST_MENU.get(), FluidChestScreen::new);
        event.register(ModMenus.ENERGY_CHEST_MENU.get(), EnergyChestScreen::new);
    }
}
