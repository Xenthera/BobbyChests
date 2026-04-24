package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetLockedPayload(BlockPos pos, boolean locked) implements CustomPacketPayload {
    public static final Type<SetLockedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_locked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetLockedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetLockedPayload::pos,
                    ByteBufCodecs.BOOL.cast(),
                    SetLockedPayload::locked,
                    SetLockedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

