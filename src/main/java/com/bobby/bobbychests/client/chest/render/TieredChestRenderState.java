package com.bobby.bobbychests.client.chest.render;

import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;

/**
 * Chest render state plus what the window needs to draw.
 *
 * <p>{@link net.minecraft.client.renderer.blockentity.ChestRenderer} binds its state type to
 * {@link ChestRenderState} rather than taking it as a type parameter, so this cannot be plugged in
 * through generics. It works anyway because {@code createRenderState()} may be overridden
 * covariantly to return a subclass; the renderer hands back whatever it was given, and
 * {@link AbstractChestRenderer} casts on the way in and out.
 */
public class TieredChestRenderState extends ChestRenderState {

    public ChestResourceMode resourceMode = ChestResourceMode.ITEM;
    /** Filled fraction of the tank or buffer, 0 to 1. */
    public float fill;
    /** Still sprite of the stored fluid; null in item and energy mode, or when the tank is empty. */
    public @Nullable TextureAtlasSprite fluidSprite;
    /** Multiply-tint for {@link #fluidSprite}, or the bar colour in energy mode. */
    public int tint = 0xFFFFFFFF;
}
