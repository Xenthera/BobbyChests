package com.bobby.bobbychests.menu;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Ensures our container close logic runs on disconnect.
 */
public final class TieredChestLogoutHandler {
    private TieredChestLogoutHandler() {}

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.containerMenu instanceof AbstractChestMenu) {
            player.closeContainer();
        }
    }
}

