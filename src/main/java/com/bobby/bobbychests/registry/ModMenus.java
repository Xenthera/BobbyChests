package com.bobby.bobbychests.registry;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.menu.copper.CopperChestMenu;
import com.bobby.bobbychests.chest.menu.diamond.DiamondChestMenu;
import com.bobby.bobbychests.chest.menu.dirt.DirtChestMenu;
import com.bobby.bobbychests.chest.menu.emerald.EmeraldChestMenu;
import com.bobby.bobbychests.chest.menu.gold.GoldChestMenu;
import com.bobby.bobbychests.chest.menu.iron.IronChestMenu;
import com.bobby.bobbychests.chest.menu.netherite.NetheriteChestMenu;
import com.bobby.bobbychests.chest.menu.resource.EnergyChestMenu;
import com.bobby.bobbychests.chest.menu.resource.FluidChestMenu;
import com.bobby.bobbychests.chest.menu.wooden.WoodenChestMenu;
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
    public static final Supplier<MenuType<NetheriteChestMenu>> NETHERITE_CHEST_MENU =
            MENUS.register("netherite_chest_menu", () -> IMenuTypeExtension.create(NetheriteChestMenu::clientConstructor));
    public static final Supplier<MenuType<DirtChestMenu>> DIRT_CHEST_MENU =
            MENUS.register("dirt_chest_menu", () -> IMenuTypeExtension.create(DirtChestMenu::clientConstructor));

    // One menu each for fluid and energy, shared by every tier: the layout does not change with
    // tier, only the capacity, and that arrives in the client payload.
    public static final Supplier<MenuType<FluidChestMenu>> FLUID_CHEST_MENU =
            MENUS.register("fluid_chest_menu", () -> IMenuTypeExtension.create(FluidChestMenu::clientConstructor));
    public static final Supplier<MenuType<EnergyChestMenu>> ENERGY_CHEST_MENU =
            MENUS.register("energy_chest_menu", () -> IMenuTypeExtension.create(EnergyChestMenu::clientConstructor));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}