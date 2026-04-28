package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BobbyChestAssetProvider implements DataProvider {
    private final PackOutput.PathProvider blockstatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;
    private final PackOutput.PathProvider itemDefinitionPathProvider;

    public BobbyChestAssetProvider(PackOutput output) {
        this.blockstatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.itemDefinitionPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            Identifier id = Identifier.fromNamespaceAndPath(BobbyChests.MODID, chest.id());
            futures.add(DataProvider.saveStable(output, blockstate(chest), this.blockstatePathProvider.json(id)));
            futures.add(DataProvider.saveStable(output, blockModel(chest), this.modelPathProvider.json(Identifier.fromNamespaceAndPath(BobbyChests.MODID, "block/" + chest.id()))));
            futures.add(DataProvider.saveStable(output, itemModel(), this.modelPathProvider.json(Identifier.fromNamespaceAndPath(BobbyChests.MODID, "item/" + chest.id()))));
            futures.add(DataProvider.saveStable(output, itemDefinition(chest), this.itemDefinitionPathProvider.json(id)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "BobbyChests Chest Assets";
    }

    private static JsonObject blockstate(BobbyChestData.ChestDefinition chest) {
        JsonObject root = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject closed = new JsonObject();
        JsonObject open = new JsonObject();

        closed.addProperty("model", BobbyChests.MODID + ":block/" + chest.id());
        open.addProperty("model", BobbyChests.MODID + ":block/" + chest.id());
        variants.add("observer_open=false", closed);
        variants.add("observer_open=true", open);
        root.add("variants", variants);
        return root;
    }

    private static JsonObject blockModel(BobbyChestData.ChestDefinition chest) {
        JsonObject root = new JsonObject();
        JsonObject textures = new JsonObject();

        root.addProperty("parent", "minecraft:block/chest");
        textures.addProperty("particle", chest.particleTexture());
        root.add("textures", textures);
        return root;
    }

    private static JsonObject itemModel() {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:item/chest");
        return root;
    }

    private static JsonObject itemDefinition(BobbyChestData.ChestDefinition chest) {
        JsonObject root = new JsonObject();
        JsonObject model = new JsonObject();
        JsonObject specialModel = new JsonObject();

        root.add("model", model);
        model.addProperty("type", "minecraft:special");
        model.addProperty("base", "minecraft:item/chest");
        model.add("model", specialModel);
        specialModel.addProperty("type", "minecraft:chest");
        specialModel.addProperty("texture", BobbyChests.MODID + ":" + chest.id());
        return root;
    }
}
