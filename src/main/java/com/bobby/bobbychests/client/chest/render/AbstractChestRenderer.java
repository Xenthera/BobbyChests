package com.bobby.bobbychests.client.chest.render;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.ChestSpriteNames;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.ChestResourceContents;
import com.bobby.bobbychests.chest.storage.ChestResourceMode;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.bobby.bobbychests.client.chest.screen.widget.EnergyGauge;
import com.bobby.bobbychests.client.fluid.ClientFluidSprites;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public abstract class AbstractChestRenderer<T extends AbstractTieredChestBlockEntity> extends ChestRenderer<T> {

    /** Sprite for each (resource mode, global) pair. Item-mode entries are the original textures. */
    private final Map<ChestResourceMode, SpriteId> globalSprites = new EnumMap<>(ChestResourceMode.class);
    private final Map<ChestResourceMode, SpriteId> localSprites = new EnumMap<>(ChestResourceMode.class);

    /** Plain white cell texture, tinted per charge level to draw the energy meter. */
    private static final SpriteId ENERGY_CELL_SPRITE = new SpriteId(
            TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(BobbyChests.MODID, "block/energy_cell"));

    protected AbstractChestRenderer(BlockEntityRendererProvider.Context context, String baseTextureName) {
        super(context);
        for (ChestResourceMode mode : ChestResourceMode.values()) {
            this.globalSprites.put(mode, sprite(baseTextureName, mode, true));
            this.localSprites.put(mode, sprite(baseTextureName, mode, false));
        }
    }

    /**
     * Item-mode sprites are the authored PNGs; fluid and energy are composited onto them at
     * atlas-stitch time by {@link com.bobby.bobbychests.client.texture.ChestCompositeSource}. The
     * naming here has to agree with the datagen that declares those entries — see
     * {@link com.bobby.bobbychests.chest.ChestSpriteNames}, which builds the same strings.
     */
    private static SpriteId sprite(String baseTextureName, ChestResourceMode mode, boolean global) {
        return new SpriteId(Sheets.CHEST_SHEET, ChestSpriteNames.spriteId(baseTextureName, mode, global));
    }

    /**
     * Covariant override so the extra window data can ride along.
     *
     * <p>{@link ChestRenderer} fixes its state type rather than taking it as a type parameter, so
     * this is the only seam available. It works because the renderer stores whatever comes back here
     * and hands the same object back to extract and submit.
     */
    @Override
    public TieredChestRenderState createRenderState() {
        return new TieredChestRenderState();
    }

    @Override
    protected @Nullable SpriteId getCustomSprite(T blockEntity, ChestRenderState renderState) {
        Map<ChestResourceMode, SpriteId> sprites =
                blockEntity.getStorageMode() == ChestStorageMode.GLOBAL ? this.globalSprites : this.localSprites;
        return sprites.get(blockEntity.getResourceMode());
    }

    @Override
    public void extractRenderState(
            T blockEntity,
            ChestRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        if (!(state instanceof TieredChestRenderState tiered)) {
            return;
        }

        ChestResourceMode mode = blockEntity.getResourceMode();
        tiered.resourceMode = mode;
        tiered.fluidSprite = null;
        tiered.fill = 0.0F;
        tiered.tint = 0xFFFFFFFF;
        if (mode == ChestResourceMode.ITEM) {
            return;
        }

        // Client-side this reads the mirror the block entity fills from its update tag, which the
        // server throttles rather than resending on every millibucket.
        ChestResourceContents contents = blockEntity.getActiveResources();
        if (mode == ChestResourceMode.FLUID) {
            int capacity = blockEntity.getFluidCapacityMb();
            if (contents.isFluidEmpty() || capacity <= 0) {
                return;
            }
            tiered.fill = Math.min(1.0F, (float) contents.fluidAmount() / capacity);
            tiered.fluidSprite = ClientFluidSprites.stillSprite(contents.fluid());
            tiered.tint = ClientFluidSprites.tint(contents.fluid());
            return;
        }

        int capacity = blockEntity.getEnergyCapacityFe();
        if (contents.energy() <= 0 || capacity <= 0) {
            return;
        }
        tiered.fill = Math.min(1.0F, (float) contents.energy() / capacity);
        // Same red-to-green ramp the GUI gauge uses, so a glance at the block and a glance at the
        // screen agree about how charged it is.
        tiered.tint = EnergyGauge.colorFor(tiered.fill);
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // Copy of vanilla ChestRenderer#submit, but choosing our own sprite IDs.
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.facing));

        float open = state.open;
        open = 1.0F - open;
        open = 1.0F - open * open * open;

        // Vanilla submit uses state.customSprite if present; we set that in getCustomSprite().
        SpriteId sprite = state.customSprite != null
                ? state.customSprite
                : this.globalSprites.get(ChestResourceMode.ITEM);
        ChestModel model = this.models.select(state.type);
        collector.submitModel(
                model,
                Float.valueOf(open),
                poseStack,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                sprite,
                this.sprites,
                0,
                state.breakProgress);

        poseStack.popPose();

        // Drawn outside the facing rotation on purpose: the four window ports are symmetric about
        // the block, so turning the chest maps the set onto itself.
        //
        if (!(state instanceof TieredChestRenderState tiered)) {
            return;
        }
        if (tiered.resourceMode == ChestResourceMode.FLUID) {
            // Passes the chest's own sprite so the ports look into the chest's inner walls rather
            // than through the block. Runs even when empty, for that reason.
            ChestWindowRenderer.submitFluid(
                    tiered,
                    poseStack,
                    collector,
                    tiered.fluidSprite,
                    state.customSprite == null ? null : this.sprites.get(state.customSprite));
        } else if (tiered.resourceMode == ChestResourceMode.ENERGY) {
            ChestWindowRenderer.submitEnergy(
                    tiered, poseStack, collector, this.sprites.get(ENERGY_CELL_SPRITE));
        }
    }
}
