package com.bobby.bobbychests.client.chest.render;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.chest.blockentity.AbstractTieredChestBlockEntity;
import com.bobby.bobbychests.chest.storage.ChestStorageMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public abstract class AbstractChestRenderer<T extends AbstractTieredChestBlockEntity> extends ChestRenderer<T> {
    private final MultiblockChestResources<SpriteId> spritesGlobal;
    private final MultiblockChestResources<SpriteId> spritesLocal;

    protected AbstractChestRenderer(BlockEntityRendererProvider.Context context, String baseTextureName) {
        super(context);
        SpriteId globalSingle = new SpriteId(
                Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(BobbyChests.MODID, "entity/chest/" + baseTextureName));
        SpriteId localSingle = new SpriteId(
                Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(BobbyChests.MODID, "entity/chest/" + baseTextureName + "_no_id"));
        // Our chests never form doubles, but ChestRenderState still has a type. Use the same sprite for all.
        this.spritesGlobal = new MultiblockChestResources<>(globalSingle, globalSingle, globalSingle);
        this.spritesLocal = new MultiblockChestResources<>(localSingle, localSingle, localSingle);
    }

    @Override
    protected @Nullable SpriteId getCustomSprite(T blockEntity, ChestRenderState renderState) {
        MultiblockChestResources<SpriteId> sprites =
                blockEntity.getStorageMode() == ChestStorageMode.GLOBAL ? this.spritesGlobal : this.spritesLocal;
        return sprites.select(renderState.type);
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
        SpriteId sprite = state.customSprite != null ? state.customSprite : this.spritesGlobal.select(state.type);
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
    }
}

