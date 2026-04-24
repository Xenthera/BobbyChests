package com.bobby.bobbychests.client.render;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.blockentity.AbstractTieredChestBlockEntity;
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

public abstract class AbstractChestRenderer<T extends AbstractTieredChestBlockEntity> extends ChestRenderer<T> {
    private final MultiblockChestResources<SpriteId> spritesForTier;

    protected AbstractChestRenderer(BlockEntityRendererProvider.Context context, String baseTextureName) {
        super(context);
        SpriteId single = new SpriteId(
                Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(BobbyChests.MODID, "entity/chest/" + baseTextureName));
        SpriteId left = new SpriteId(
                Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(BobbyChests.MODID, "entity/chest/" + baseTextureName + "_left"));
        SpriteId right = new SpriteId(
                Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath(BobbyChests.MODID, "entity/chest/" + baseTextureName + "_right"));
        this.spritesForTier = new MultiblockChestResources<>(single, left, right);
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        // Copy of vanilla ChestRenderer#submit, but choosing our own sprite IDs.
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.facing));

        float open = state.open;
        open = 1.0F - open;
        open = 1.0F - open * open * open;

        SpriteId sprite = this.spritesForTier.select(state.type);
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

