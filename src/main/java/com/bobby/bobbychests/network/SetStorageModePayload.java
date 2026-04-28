package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetStorageModePayload(BlockPos pos, boolean usingGlobalStorage) implements CustomPacketPayload {
    public static final Type<SetStorageModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_storage_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStorageModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetStorageModePayload::pos,
                    ByteBufCodecs.BOOL.cast(),
                    SetStorageModePayload::usingGlobalStorage,
                    SetStorageModePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
