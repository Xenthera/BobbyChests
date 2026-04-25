package com.bobby.bobbychests.block;

import com.bobby.bobbychests.BobbyChests;
import com.bobby.bobbychests.block.copper.CopperChestBlock;
import com.bobby.bobbychests.block.diamond.DiamondChestBlock;
import com.bobby.bobbychests.block.dirt.DirtChestBlock;
import com.bobby.bobbychests.block.emerald.EmeraldChestBlock;
import com.bobby.bobbychests.block.gold.GoldChestBlock;
import com.bobby.bobbychests.block.iron.IronChestBlock;
import com.bobby.bobbychests.block.netherite.NetheriteChestBlock;
import com.bobby.bobbychests.block.wooden.WoodenChestBlock;
import com.bobby.bobbychests.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;


public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BobbyChests.MODID);

    public static final DeferredBlock<Block> WOODEN_CHEST = registerBlock("wooden_chest",
            props -> new WoodenChestBlock(props
                    .strength(1)
                    .sound(SoundType.WOOD))
    );

    public static final DeferredBlock<Block> COPPER_CHEST = registerBlock("copper_chest",
            props -> new CopperChestBlock(props
                    .strength(1.5f)
                    .sound(SoundType.COPPER))
    );

    public static final DeferredBlock<Block> IRON_CHEST = registerBlock("iron_chest",
            props -> new IronChestBlock(props
                    .strength(2)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> GOLD_CHEST = registerBlock("gold_chest",
            props -> new GoldChestBlock(props
                    .strength(2)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> DIAMOND_CHEST = registerBlock("diamond_chest",
            props -> new DiamondChestBlock(props
                    .strength(3f)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> EMERALD_CHEST = registerBlock("emerald_chest",
            props -> new EmeraldChestBlock(props
                    .strength(3.5f)
                    .sound(SoundType.METAL))
    );

    public static final DeferredBlock<Block> NETHERITE_CHEST = registerBlock("netherite_chest", NetheriteChestBlock::new);

    public static final DeferredBlock<Block> DIRT_CHEST = registerBlock("dirt_chest",
            props -> new DirtChestBlock(props
                    .strength(0.5f)
                    .sound(SoundType.GRAVEL))
    );

    // Helper to register block and a block item from it
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.registerItem(name, (properties) -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
