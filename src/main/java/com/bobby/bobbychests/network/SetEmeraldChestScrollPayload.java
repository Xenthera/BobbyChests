package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetEmeraldChestScrollPayload(BlockPos pos, int scrollRows) implements CustomPacketPayload {
    public static final Type<SetEmeraldChestScrollPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_emerald_chest_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetEmeraldChestScrollPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetEmeraldChestScrollPayload::pos,
                    ByteBufCodecs.VAR_INT.cast(),
                    SetEmeraldChestScrollPayload::scrollRows,
                    SetEmeraldChestScrollPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
