package com.bobby.bobbychests;

import com.bobby.bobbychests.chest.menu.TieredChestLogoutHandler;
import com.bobby.bobbychests.command.ModCommands;
import com.bobby.bobbychests.datagen.DataGenerators;
import com.bobby.bobbychests.network.ModNetworking;
import com.bobby.bobbychests.registry.ModBlockEntities;
import com.bobby.bobbychests.registry.ModBlocks;
import com.bobby.bobbychests.registry.ModCapabilities;
import com.bobby.bobbychests.registry.ModItems;
import com.bobby.bobbychests.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;

@Mod(BobbyChests.MODID)
public class BobbyChests {
    public static final String MODID = "bobbychests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOBBY_CHESTS_TAB = CREATIVE_MODE_TABS.register("bobby_chests", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bobbychests"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> ModBlocks.WOODEN_CHEST.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.DIRT_CHEST.get());
                output.accept(ModBlocks.WOODEN_CHEST.get());
                output.accept(ModBlocks.COPPER_CHEST.get());
                output.accept(ModBlocks.IRON_CHEST.get());
                output.accept(ModBlocks.GOLD_CHEST.get());
                output.accept(ModBlocks.DIAMOND_CHEST.get());
                output.accept(ModBlocks.EMERALD_CHEST.get());
                output.accept(ModBlocks.NETHERITE_CHEST.get());
                output.accept(ModItems.NETWORKING_UPGRADE_CARD.get());
            }).build());

    public BobbyChests(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(DataGenerators::gatherData);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);

        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(TieredChestLogoutHandler::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ModCommands::register);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }
}
