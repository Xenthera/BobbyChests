package com.bobby.bobbychests.network;

import com.bobby.bobbychests.chest.blockentity.TieredGlobalChest;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.menu.AbstractScrollableChestMenu;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

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
                .playToServer(SetStorageModePayload.TYPE, SetStorageModePayload.STREAM_CODEC, (payload, ctx) -> {
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

                        chest.setStorageMode(payload.usingGlobalStorage() ? ChestStorageMode.GLOBAL : ChestStorageMode.LOCAL);
                    });
                })
                .playToServer(SetScrollableChestScrollPayload.TYPE, SetScrollableChestScrollPayload.STREAM_CODEC, (payload, ctx) -> {
                    ctx.enqueueWork(() -> {
                        if (!(ctx.player().containerMenu instanceof AbstractScrollableChestMenu menu)) {
                            return;
                        }
                        if (!menu.getChestPos().equals(payload.pos())) {
                            return;
                        }
                        menu.setScrollRows(payload.scrollRows());
                    });
                });
    }
}

