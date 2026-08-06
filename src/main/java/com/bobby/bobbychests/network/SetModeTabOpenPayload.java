package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Syncs whether the mode tab is fully open so shift-click matches client/server. */
public record SetModeTabOpenPayload(BlockPos pos, boolean open) implements CustomPacketPayload {
    public static final Type<SetModeTabOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_mode_tab_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetModeTabOpenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetModeTabOpenPayload::pos,
                    ByteBufCodecs.BOOL.cast(),
                    SetModeTabOpenPayload::open,
                    SetModeTabOpenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
