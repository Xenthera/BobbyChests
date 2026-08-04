package com.bobby.bobbychests.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Syncs whether the upgrades tab is fully open so shift-click matches client/server. */
public record SetUpgradeTabOpenPayload(BlockPos pos, boolean open) implements CustomPacketPayload {
    public static final Type<SetUpgradeTabOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("bobbychests", "set_upgrade_tab_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetUpgradeTabOpenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.cast(),
                    SetUpgradeTabOpenPayload::pos,
                    ByteBufCodecs.BOOL.cast(),
                    SetUpgradeTabOpenPayload::open,
                    SetUpgradeTabOpenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
