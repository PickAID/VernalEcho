package org.pickaid.vernalecho.echo.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.worldgen.config.WildEchoFeatureConfiguration;

public final class EchoFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, VernalEcho.MOD_ID);

    public static final ResourceKey<ConfiguredFeature<?, ?>> WILD_ECHO_CONFIGURED =
        ResourceKey.create(Registries.CONFIGURED_FEATURE, VernalEcho.id("wild_echo"));
    public static final ResourceKey<PlacedFeature> WILD_ECHO_PLACED =
        ResourceKey.create(Registries.PLACED_FEATURE, VernalEcho.id("wild_echo"));

    public static final DeferredHolder<Feature<?>, WildEchoFeature> WILD_ECHO =
        FEATURES.register("wild_echo", () -> new WildEchoFeature(WildEchoFeatureConfiguration.CODEC));

    private EchoFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
