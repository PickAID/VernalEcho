package org.pickaid.vernalecho.echo.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.pickaid.vernalecho.echo.worldgen.config.WildEchoFeatureConfiguration;

public final class WildEchoFeature extends Feature<WildEchoFeatureConfiguration> {
    public WildEchoFeature(Codec<WildEchoFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<WildEchoFeatureConfiguration> context) {
        return EchoNaturalSpawner.tryPlaceFromFeature(context.level(), context.origin(), context.random(), context.config());
    }
}
