package com.bobby.bobbychests.menu;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Ensures our container close logic runs on disconnect.
 *
 * <p>If a player disconnects while viewing a {@link BobbyBaseChestMenu}, we explicitly close the container so
 * {@link BobbyBaseChestMenu#removed(net.minecraft.world.entity.player.Player)} fires and decrements global viewer counts.</p>
 */
public final class BobbyBaseChestLogoutHandler {

    private BobbyBaseChestLogoutHandler() {}

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.containerMenu instanceof BobbyBaseChestMenu) {
            player.closeContainer();
        }
    }
}

