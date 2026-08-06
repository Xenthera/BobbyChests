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
        this.add("item.bobbychests.upgrade_alloy_blend", "Upgrade Alloy Blend");
        this.add("item.bobbychests.upgrade_alloy", "Upgrade Alloy");
        this.add("item.bobbychests.blank_upgrade_card", "Blank Upgrade Card");
        this.add("item.bobbychests.networking_upgrade_card", "Networking Upgrade");
        this.add("item.bobbychests.infinite_upgrade_card", "Creative Infinite Upgrade");
        this.add("item.bobbychests.void_upgrade_card", "Void Upgrade");
        this.add("item.bobbychests.leave_last_item_upgrade_card", "Leave-Last Upgrade");
        this.add("item.bobbychests.retain_items_upgrade_card", "Retain Items Upgrade");
        this.add("item.bobbychests.lock_upgrade_card", "Lock Upgrade");
        this.add("item.bobbychests.deep_storage_upgrade_card", "Deep Storage Upgrade");
        this.add("item.bobbychests.fluid_upgrade_card", "Fluid Upgrade");
        this.add("item.bobbychests.energy_upgrade_card", "Energy Upgrade");

        this.add("gui.bobbychests.tab.upgrades", "Upgrades");
        this.add("gui.bobbychests.tab.lock", "Lock");
        this.add("gui.bobbychests.tab.network", "Network");
        this.add("gui.bobbychests.upgrade.deny.duplicate", "This upgrade is already installed");
        this.add("gui.bobbychests.upgrade.deny.deep_vs_network", "Cannot use Deep Storage with Networking");
        this.add("gui.bobbychests.upgrade.deny.network_vs_deep", "Cannot use Networking with Deep Storage");
        this.add("gui.bobbychests.upgrade.deny.deep_tier_not_allowed", "Deep Storage is only for Dirt and Wooden chests");
        this.add("gui.bobbychests.upgrade.deny.fluid_vs_energy", "Cannot use Fluid with Energy");
        this.add("gui.bobbychests.upgrade.deny.energy_vs_fluid", "Cannot use Energy with Fluid");
        this.add("gui.bobbychests.upgrade.deny.not_empty", "Empty the chest first");
        this.add("gui.bobbychests.upgrade.deny.deep_vs_resource", "Deep Storage only works with items");
        this.add("gui.bobbychests.upgrade.deny.leave_last_vs_resource", "Leave-Last only works with items");
        this.add("gui.bobbychests.upgrade.deny.resource_vs_deep", "Cannot use this with Deep Storage");
        this.add("gui.bobbychests.upgrade.deny.resource_vs_leave_last", "Cannot use this with Leave-Last");

        this.add("gui.bobbychests.tank.empty", "Empty");
        this.add("gui.bobbychests.tank.amount", "%s / %s mB");
        this.add("gui.bobbychests.energy.amount", "%s / %s FE");
        this.add("gui.bobbychests.slot.fill", "Fill from item");
        this.add("gui.bobbychests.slot.drain", "Emptied items");
        // Tier adjectives, so a fluid or energy chest reads "Wooden Fluid Chest".
        this.add("tier.bobbychests.dirt", "Dirt");
        this.add("tier.bobbychests.wood", "Wooden");
        this.add("tier.bobbychests.copper", "Copper");
        this.add("tier.bobbychests.iron", "Iron");
        this.add("tier.bobbychests.gold", "Gold");
        this.add("tier.bobbychests.diamond", "Diamond");
        this.add("tier.bobbychests.emerald", "Emerald");
        this.add("tier.bobbychests.netherite", "Netherite");
        this.add("container.bobbychests.fluid_chest", "%s Fluid Chest");
        this.add("container.bobbychests.energy_chest", "%s Energy Chest");

        for (BobbyChestData.ChestDefinition chest : BobbyChestData.CHESTS) {
            this.add(chest.block().get(), chest.displayName());
            this.add("container.bobbychests." + chest.id(), chest.displayName());
        }

        this.add("bobbychests.configuration.title", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml", "Bobby Chests Config");
        this.add("bobbychests.configuration.section.bobbychests.common.toml.title", "Bobby Chests Config");
    }
}
