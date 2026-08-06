package com.bobby.bobbychests.chest;

import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.chest.storage.ChestResourceTransfer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/**
 * Sneak-using a fluid or energy chest with a bucket or battery fills or empties it in hand.
 *
 * <p>Handled as an interaction event rather than in the block's {@code useItemOn}, because that
 * method is never reached in this case: {@code ServerPlayerGameMode} computes
 * {@code suppressUsingBlock = isSecondaryUseActive() && holdingSomething} and skips the block
 * entirely, going straight to using the item. With a bucket in hand that means placing the fluid in
 * the world — the opposite of what was wanted. Intercepting the interaction is the only way to get
 * in front of that.
 *
 * <p>Runs on both sides. The transfer itself is server-only, but the client has to cancel too or it
 * predicts the bucket being used and drops a fluid block next to the chest, which then lingers
 * because nothing on the server ever contradicted it.
 *
 * <p>One interaction moves one bucket, however many are in the stack — see
 * {@link ChestResourceTransfer#runAgainst}.
 */
public final class ChestHandTransferHandler {

    private ChestHandTransferHandler() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive() || event.getItemStack().isEmpty()) {
            return;
        }

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof AbstractTieredChestBlockEntity chest)) {
            return;
        }
        ChestResourceMode mode = chest.getResourceMode();
        if (mode == ChestResourceMode.ITEM || !chest.canPlayerOpen(player)) {
            return;
        }

        ItemAccess access = ItemAccess.forPlayerInteraction(player, event.getHand()).oneByOne();
        if (access.getResource().isEmpty() || !canExchange(access, mode)) {
            // Nothing the chest could exchange with. Left alone so sneak-placing a block against a
            // tank still works normally.
            return;
        }

        if (!event.getLevel().isClientSide()) {
            if (ChestResourceTransfer.runAgainst(chest, access)) {
                // Discrete click: do not wait on the pipe rate-limit / serverTick flush.
                chest.forceBroadcastResourceLevel();
            }
        }

        // Cancelled on both sides, and regardless of whether anything actually moved: a full tank
        // should refuse the bucket, not fall through to spilling it on the floor.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Whether the held item is the kind of thing this chest could trade with at all. */
    private static boolean canExchange(ItemAccess access, ChestResourceMode mode) {
        return switch (mode) {
            case FLUID -> access.getCapability(Capabilities.Fluid.ITEM) != null;
            case ENERGY -> access.getCapability(Capabilities.Energy.ITEM) != null;
            case ITEM -> false;
        };
    }
}
