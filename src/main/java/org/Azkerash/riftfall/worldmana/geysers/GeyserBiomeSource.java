package org.Azkerash.riftfall.worldmana.geysers;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Set;
import java.util.stream.Stream;

/** The generator's actual biome source: saved palettes and uncached queries share this resolver. */
@EventBusSubscriber(modid="riftfall")
public final class GeyserBiomeSource extends BiomeSource {
    public static final MapCodec<GeyserBiomeSource> CODEC=BiomeSource.CODEC.fieldOf("delegate")
        .xmap(GeyserBiomeSource::new,source->source.delegate);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> TYPES=DeferredRegister.create(Registries.BIOME_SOURCE,"riftfall");
    static {TYPES.register("mana_geysers",()->CODEC);}
    private final BiomeSource delegate;
    private volatile ServerLevel level;
    private volatile Set<Holder<Biome>> biomes;
    public GeyserBiomeSource(BiomeSource delegate){this.delegate=delegate;}
    public static void register(IEventBus bus){TYPES.register(bus);}
    @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return possibleBiomes().stream();}
    @Override public Set<Holder<Biome>> possibleBiomes(){return biomes==null?delegate.possibleBiomes():biomes;}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        var world=level;
        return world==null?delegate.getNoiseBiome(x,y,z,sampler):GeyserBiomes.resolve(world,delegate,x,y,z,sampler);
    }
    public static BiomeSource original(BiomeSource source){
        while(source instanceof GeyserBiomeSource geysers)source=geysers.delegate;
        return source;
    }
    @SubscribeEvent public static void loaded(LevelEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel world)||!world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
        var generator=world.getChunkSource().getGenerator();
        var source=generator.getBiomeSource() instanceof GeyserBiomeSource existing?existing:new GeyserBiomeSource(generator.getBiomeSource());
        source.biomes=GeyserBiomes.possible(world,source.delegate.possibleBiomes());
        source.level=world;
        ((org.Azkerash.riftfall.mixin.GeyserChunkGeneratorAccess)generator).riftfall$biomeSource(source);
        // Vanilla's feature sorter captures the constructor's original source. Rebuild its lazy
        // supplier as well, so custom-biome features are included on both new and reopened saves.
        ((org.Azkerash.riftfall.mixin.GeyserChunkGeneratorAccess)generator).riftfall$featuresPerStep(
            net.neoforged.neoforge.common.util.Lazy.of(()->FeatureSorter.buildFeaturesPerStep(
                java.util.List.copyOf(source.possibleBiomes()),biome->generator.getBiomeGenerationSettings(biome).features(),true)));
        ((org.Azkerash.riftfall.mixin.GeyserStructureStateAccess)world.getChunkSource().getGeneratorState()).riftfall$biomeSource(source);
    }
}
