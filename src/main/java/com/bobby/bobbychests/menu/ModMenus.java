package com.bobby.bobbychests.menu;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BobbyChests.MODID);
    public static final Supplier<MenuType<WoodenChestMenu>> WOODEN_CHEST_MENU =
            MENUS.register("wooden_chest_menu", () -> IMenuTypeExtension.create(WoodenChestMenu::clientConstructor));
    public static final Supplier<MenuType<CopperChestMenu>> COPPER_CHEST_MENU =
            MENUS.register("copper_chest_menu", () -> IMenuTypeExtension.create(CopperChestMenu::clientConstructor));
    public static final Supplier<MenuType<IronChestMenu>> IRON_CHEST_MENU =
            MENUS.register("iron_chest_menu", () -> IMenuTypeExtension.create(IronChestMenu::clientConstructor));
    public static final Supplier<MenuType<GoldChestMenu>> GOLD_CHEST_MENU =
            MENUS.register("gold_chest_menu", () -> IMenuTypeExtension.create(GoldChestMenu::clientConstructor));
    public static final Supplier<MenuType<DiamondChestMenu>> DIAMOND_CHEST_MENU =
            MENUS.register("diamond_chest_menu", () -> IMenuTypeExtension.create(DiamondChestMenu::clientConstructor));
    public static final Supplier<MenuType<EmeraldChestMenu>> EMERALD_CHEST_MENU =
            MENUS.register("emerald_chest_menu", () -> IMenuTypeExtension.create(EmeraldChestMenu::clientConstructor));
    public static final Supplier<MenuType<DirtChestMenu>> DIRT_CHEST_MENU =
            MENUS.register("dirt_chest_menu", () -> IMenuTypeExtension.create(DirtChestMenu::clientConstructor));
    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}