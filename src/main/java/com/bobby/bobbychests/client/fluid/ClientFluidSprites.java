package com.bobby.bobbychests.client.fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

/**
 * Looks up the still texture and tint colour a fluid renders with.
 *
 * <p>Both the tank gauge in the GUI and the window on the block need the genuine fluid texture
 * rather than a stand-in, and both need it from ordinary client code rather than from inside the
 * chunk renderer. {@code ModelManager#getFluidStateModelSet} exposes exactly that: the same baked
 * {@link FluidModel} the world uses, carrying the still sprite on the blocks atlas and the tint
 * source that gives water its biome colour.
 *
 * <p>Note for anyone porting the same trick into BobbyPipes: its {@code FluidGrid} falls back to
 * bucket icons on the belief that fluid rendering cannot be called into ad hoc. That was true of an
 * earlier pipeline but is not true here.
 */
public final class ClientFluidSprites {

    private ClientFluidSprites() {
    }

    private static @Nullable FluidModel modelFor(FluidResource fluid) {
        if (fluid.isEmpty()) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getModelManager() == null) {
            return null;
        }
        FluidState state = fluid.getFluid().defaultFluidState();
        return minecraft.getModelManager().getFluidStateModelSet().get(state);
    }

    /** The fluid's still sprite, or null if it has no model (which should not happen in practice). */
    public static @Nullable TextureAtlasSprite stillSprite(FluidResource fluid) {
        FluidModel model = modelFor(fluid);
        return model == null ? null : model.stillMaterial().sprite();
    }

    /**
     * The multiply-tint for the fluid's sprite, opaque white when it does not define one.
     *
     * <p>Water's texture is greyscale and only becomes blue through this; lava's is already coloured
     * and has no tint source at all.
     */
    public static int tint(FluidResource fluid) {
        FluidModel model = modelFor(fluid);
        if (model == null || model.fluidTintSource() == null) {
            return 0xFFFFFFFF;
        }
        int color = model.fluidTintSource().color(fluid.getFluid().defaultFluidState());
        // Tint sources may leave the alpha byte at zero; the gauge must not vanish because of it.
        return (color & 0xFF000000) == 0 ? (color | 0xFF000000) : color;
    }
}
