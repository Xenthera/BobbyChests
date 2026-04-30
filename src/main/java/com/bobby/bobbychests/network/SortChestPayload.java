package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SortChestPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<SortChestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "sort_chest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortChestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SortChestPayload::pos,
                    SortChestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

