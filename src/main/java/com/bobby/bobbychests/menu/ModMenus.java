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
    public static final Supplier<MenuType<FirstChestMenu>> FIRST_CHEST_MENU =
            MENUS.register("first_chest_menu", () -> IMenuTypeExtension.create(FirstChestMenu::clientConstructor));
    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}