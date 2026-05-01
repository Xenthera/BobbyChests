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
        this.add("item.bobbychests.infinite_upgrade_card", "Infinite Upgrade Card");
        this.add("item.bobbychests.void_upgrade_card", "Void Upgrade Card");
        this.add("item.bobbychests.leave_last_item_upgrade_card", "Leave-Last Upgrade Card");
        this.add("item.bobbychests.retain_items_upgrade_card", "Retain Items Upgrade Card");
        this.add("item.bobbychests.lock_upgrade_card", "Lock Upgrade Card");
        this.add("item.bobbychests.deep_storage_upgrade_card", "Deep Storage Upgrade Card");

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            this.add(chest.block().get(), chest.displayName());
            this.add("container.bobbychests." + chest.id(), chest.displayName());
        }

        this.add("bobbychests.configuration.title", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml.title", "Bobby Chests Config");
    }
}
