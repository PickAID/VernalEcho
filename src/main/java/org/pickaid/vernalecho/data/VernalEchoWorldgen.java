package org.pickaid.vernalecho.data;

import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.worldgen.EchoFeatures;
import org.pickaid.vernalecho.echo.worldgen.config.WildEchoFeatureConfiguration;

public final class VernalEchoWorldgen {
    public static final int DEFAULT_WILD_ECHO_RARITY = 128;

    private static final ResourceKey<BiomeModifier> ADD_WILD_ECHOES =
        ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, VernalEcho.id("add_wild_echoes"));

    private VernalEchoWorldgen() {
    }

    public static RegistrySetBuilder registrySetBuilder() {
        return new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, VernalEchoWorldgen::bootstrapConfiguredFeatures)
            .add(Registries.PLACED_FEATURE, VernalEchoWorldgen::bootstrapPlacedFeatures)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, VernalEchoWorldgen::bootstrapBiomeModifiers);
    }

    private static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(
            EchoFeatures.WILD_ECHO_CONFIGURED,
            new ConfiguredFeature<>(EchoFeatures.WILD_ECHO.get(), WildEchoFeatureConfiguration.DEFAULT)
        );
    }

    private static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(
            EchoFeatures.WILD_ECHO_PLACED,
            new PlacedFeature(
                configuredFeatures.getOrThrow(EchoFeatures.WILD_ECHO_CONFIGURED),
                List.of(
                    RarityFilter.onAverageOnceEvery(DEFAULT_WILD_ECHO_RARITY),
                    InSquarePlacement.spread(),
                    HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG),
                    BiomeFilter.biome()
                )
            )
        );
    }

    private static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        context.register(
            ADD_WILD_ECHOES,
            new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placedFeatures.getOrThrow(EchoFeatures.WILD_ECHO_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION
            )
        );
    }
}
