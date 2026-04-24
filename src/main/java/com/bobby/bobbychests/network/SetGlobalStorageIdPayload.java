package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetGlobalStorageIdPayload(BlockPos pos, int id) implements CustomPacketPayload {
    public static final Type<SetGlobalStorageIdPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_global_storage_id"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetGlobalStorageIdPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetGlobalStorageIdPayload::pos,
                    ByteBufCodecs.VAR_INT.cast(),
                    SetGlobalStorageIdPayload::id,
                    SetGlobalStorageIdPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

