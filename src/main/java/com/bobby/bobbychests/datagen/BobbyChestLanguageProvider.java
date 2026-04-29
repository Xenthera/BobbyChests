package com.bobby.bobbychests.datagen;

import com.bobby.bobbychests.BobbyChests;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class BobbyChestLanguageProvider extends LanguageProvider {
    public BobbyChestLanguageProvider(PackOutput output, String locale) {
        super(output, BobbyChests.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        this.add("itemGroup.bobbychests", "Bobby Chests");
        this.add("item.bobbychests.networking_upgrade_card", "Networking Upgrade Card");

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            this.add(chest.block().get(), chest.displayName());
            this.add("container.bobbychests." + chest.id(), chest.displayName());
        }

        this.add("bobbychests.configuration.title", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml.title", "Bobby Chests Config");
    }
}
