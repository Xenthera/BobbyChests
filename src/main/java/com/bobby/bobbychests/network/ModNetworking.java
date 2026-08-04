package com.bobby.bobbychests.network;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.blockentity.TieredGlobalChest;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ModNetworking {
    private ModNetworking() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("bobbychests")
                .playToServer(SetGlobalStorageIdPayload.TYPE, SetGlobalStorageIdPayload.STREAM_CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (!(ctx.player().level() instanceof ServerLevel level)) {
                            return;
                        }
                        BlockEntity be = level.getBlockEntity(payload.pos());
                        if (be instanceof TieredGlobalChest chest) {
                            chest.setGlobalStorageId(payload.id());
                        }
                    });
                })
                .playToServer(SetLockedPayload.TYPE, SetLockedPayload.STREAM_CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (!(ctx.player().level() instanceof ServerLevel level)) {
                            return;
                        }
                        BlockEntity be = level.getBlockEntity(payload.pos());
                        if (!(be instanceof TieredGlobalChest chest)) {
                            return;
                        }

                        // Only the owner can toggle while the chest is locked.
                        if (chest.isLocked() && chest.getOwnerUuid() != null && !chest.getOwnerUuid().equals(ctx.player().getUUID())) {
                            return;
                        }

                        chest.setLocked(payload.locked(), ctx.player());
                    });
                })
                .playToServer(SortChestPayload.TYPE, SortChestPayload.STREAM_CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (!(ctx.player().level() instanceof ServerLevel level)) {
                            return;
                        }
                        BlockEntity be = level.getBlockEntity(payload.pos());
                        if (!(be instanceof AbstractTieredChestBlockEntity chest)) {
                            return;
                        }
                        if (!chest.canPlayerOpen(ctx.player())) {
                            return;
                        }

                        chest.sortActiveContents();
                    });
                })
                .playToServer(SetUpgradeTabOpenPayload.TYPE, SetUpgradeTabOpenPayload.STREAM_CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (!(ctx.player().containerMenu instanceof com.bobby.bobbychests.chest.menu.AbstractChestMenu menu)) {
                            return;
                        }
                        if (!menu.getChestPos().equals(payload.pos())) {
                            return;
                        }
                        menu.setUpgradeSlotsActive(payload.open());
                    });
                })
                .playBidirectional(
                        SetScrollableChestScrollPayload.TYPE,
                        SetScrollableChestScrollPayload.STREAM_CODEC,
                        ModNetworking::handleScrollableChestScrollFromClient,
                        ModNetworking::handleScrollableChestScrollFromServer);
    }

    private static void handleScrollableChestScrollFromClient(SetScrollableChestScrollPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player().containerMenu instanceof AbstractScrollableChestMenu menu)) {
                return;
            }
            if (!menu.getChestPos().equals(payload.pos())) {
                return;
            }
            menu.setScrollRows(payload.scrollRows());
        });
    }

    private static void handleScrollableChestScrollFromServer(SetScrollableChestScrollPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player().containerMenu instanceof AbstractScrollableChestMenu menu)) {
                return;
            }
            if (!menu.getChestPos().equals(payload.pos())) {
                return;
            }
            // The following full-state broadcast only repaints visible rows. Clear the whole client mirror first so
            // off-screen rows from the previous backing store cannot flash when the player scrolls before fresh data.
            menu.clearChestStorageMirrorBeforeResync();
            menu.setScrollRows(payload.scrollRows());
        });
    }

}

