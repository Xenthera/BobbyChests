package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Synchronizes the first visible row between a scrollable chest menu's client and server copies. */
public record SetScrollableChestScrollPayload(BlockPos pos, int scrollRows) implements CustomPacketPayload {
    public static final Type<SetScrollableChestScrollPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_scrollable_chest_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetScrollableChestScrollPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetScrollableChestScrollPayload::pos,
                    ByteBufCodecs.VAR_INT.cast(),
                    SetScrollableChestScrollPayload::scrollRows,
                    SetScrollableChestScrollPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
