package com.bobby.bobbychests.command;

import com.bobby.bobbychests.globalcheststorage.GlobalTieredChestData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {
    private ModCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("bobbychests")

                        .then(Commands.literal("clear_global")
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> {
                                            CommandSourceStack src = ctx.getSource();
                                            if (!(src.getLevel() instanceof ServerLevel level)) {
                                                return 0;
                                            }
                                            GlobalTieredChestData.get(level).clearAllItems();
                                            src.sendSuccess(() -> Component.literal("Cleared BobbyChests global ITEM storage for this world."), true);
                                            return 1;
                                        })
                                )
                                .executes(ctx -> {
                                    ctx.getSource().sendFailure(Component.literal("This will delete all BobbyChests global ITEM inventories for this world. Run /bobbychests clear_global confirm"));
                                    return 0;
                                })
                        )
        );
    }
}

