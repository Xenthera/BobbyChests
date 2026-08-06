package com.bobby.bobbychests.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bakes the fluid and energy variants of each tier chest at atlas-stitch time, by alpha-blending a
 * shared window overlay onto the tier's existing chest texture.
 *
 * <p>The alternative was checking in 32 more PNGs — eight tiers times fluid and energy times the
 * with-channel and without-channel pair. Doing it here means the source of truth stays the 16
 * chest textures that were already in the repo: redraw a tier's art and its fluid and energy
 * variants follow, with no code change and no regenerated files to forget.
 *
 * <p>Same technique as BobbyPipes' {@code PipeCompositeSource}, minus the tint step, which pipes
 * need to recolour a shared white base and chests do not: each chest here has its own artwork
 * already. Every output is a single flattened image referenced exactly like any static texture.
 */
public record ChestCompositeSource(List<Entry> entries) implements SpriteSource {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MapCodec<ChestCompositeSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Entry.CODEC.listOf().fieldOf("entries").forGetter(ChestCompositeSource::entries))
                    .apply(i, ChestCompositeSource::new));

    /**
     * One baked output.
     *
     * @param output   the sprite id the block entity renderer asks for
     * @param base     the tier's existing chest texture
     * @param overlays alpha-blended on top of the base, in order
     * @param cutouts  erase masks applied last: wherever a mask is opaque, the output becomes
     *                 fully transparent
     */
    public record Entry(Identifier output, Identifier base, List<Identifier> overlays, List<Identifier> cutouts) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                i -> i.group(
                                Identifier.CODEC.fieldOf("output").forGetter(Entry::output),
                                Identifier.CODEC.fieldOf("base").forGetter(Entry::base),
                                Identifier.CODEC.listOf().optionalFieldOf("overlays", List.of())
                                        .forGetter(Entry::overlays),
                                Identifier.CODEC.listOf().optionalFieldOf("cutouts", List.of())
                                        .forGetter(Entry::cutouts))
                        .apply(i, Entry::new));
    }

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        // Cached because every entry for a given tier shares a base, and every entry of a given
        // kind shares an overlay, so without this each of the two images is read sixteen times.
        Map<Identifier, NativeImage> cache = new HashMap<>();
        for (Entry entry : this.entries) {
            output.add(entry.output(), loader -> bake(resourceManager, entry, cache));
        }
    }

    private static @Nullable SpriteContents bake(
            ResourceManager resourceManager, Entry entry, Map<Identifier, NativeImage> cache) {
        NativeImage base;
        try {
            base = load(resourceManager, entry.base(), cache);
        } catch (IOException e) {
            LOGGER.error("Unable to load chest composite base {} for {}", entry.base(), entry.output(), e);
            return null;
        }

        NativeImage composed = new NativeImage(base.getWidth(), base.getHeight(), false);
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                composed.setPixel(x, y, base.getPixel(x, y));
            }
        }

        for (Identifier overlayId : entry.overlays()) {
            NativeImage overlay;
            try {
                overlay = load(resourceManager, overlayId, cache);
            } catch (IOException e) {
                LOGGER.error("Unable to load chest composite overlay {} for {}", overlayId, entry.output(), e);
                composed.close();
                return null;
            }
            int w = Math.min(composed.getWidth(), overlay.getWidth());
            int h = Math.min(composed.getHeight(), overlay.getHeight());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    composed.setPixel(x, y, ARGB.alphaBlend(composed.getPixel(x, y), overlay.getPixel(x, y)));
                }
            }
        }

        // Erasing is a separate pass rather than an overlay because alpha-blending a transparent
        // pixel is a no-op: it cannot open a hole in the wall underneath, only leave it alone.
        for (Identifier cutoutId : entry.cutouts()) {
            NativeImage cutout;
            try {
                cutout = load(resourceManager, cutoutId, cache);
            } catch (IOException e) {
                LOGGER.error("Unable to load chest composite cutout {} for {}", cutoutId, entry.output(), e);
                composed.close();
                return null;
            }
            int w = Math.min(composed.getWidth(), cutout.getWidth());
            int h = Math.min(composed.getHeight(), cutout.getHeight());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (ARGB.alpha(cutout.getPixel(x, y)) > 0) {
                        composed.setPixel(x, y, 0);
                    }
                }
            }
        }

        return new SpriteContents(entry.output(), new FrameSize(composed.getWidth(), composed.getHeight()), composed);
    }

    private static NativeImage load(
            ResourceManager resourceManager, Identifier id, Map<Identifier, NativeImage> cache) throws IOException {
        NativeImage cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        Identifier textureId = TEXTURE_ID_CONVERTER.idToFile(id);
        Optional<Resource> resource = resourceManager.getResource(textureId);
        if (resource.isEmpty()) {
            throw new IOException("No such texture: " + textureId);
        }
        NativeImage image;
        try (InputStream stream = resource.get().open()) {
            image = NativeImage.read(stream);
        }
        cache.put(id, image);
        return image;
    }

    @Override
    public MapCodec<ChestCompositeSource> codec() {
        return MAP_CODEC;
    }
}
