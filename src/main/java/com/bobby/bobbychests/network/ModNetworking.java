package com.bobby.bobbychests.network;

import com.bobby.bobbychests.blockentity.BobbyBaseChestBlockEntity;
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
                        if (be instanceof BobbyBaseChestBlockEntity chest) {
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
                        if (!(be instanceof BobbyBaseChestBlockEntity chest)) {
                            return;
                        }

                        // Only the owner can toggle while the chest is locked.
                        if (chest.isLocked() && chest.getOwnerUuid() != null && !chest.getOwnerUuid().equals(ctx.player().getUUID())) {
                            return;
                        }

                        chest.setLocked(payload.locked(), ctx.player());
                    });
                });
    }
}

