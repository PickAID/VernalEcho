package org.pickaid.vernalecho.echo.worldgen.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record WildEchoFeatureConfiguration(
    int surfaceSearchAttempts,
    int chestSearchRadius,
    double chestLinkedChance
) implements FeatureConfiguration {
    public static final WildEchoFeatureConfiguration DEFAULT = new WildEchoFeatureConfiguration(4, 5, 0.35D);

    public static final Codec<WildEchoFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, 16).fieldOf("surfaceSearchAttempts").forGetter(WildEchoFeatureConfiguration::surfaceSearchAttempts),
        Codec.intRange(0, 12).fieldOf("chestSearchRadius").forGetter(WildEchoFeatureConfiguration::chestSearchRadius),
        Codec.doubleRange(0.0D, 1.0D).fieldOf("chestLinkedChance").forGetter(WildEchoFeatureConfiguration::chestLinkedChance)
    ).apply(instance, WildEchoFeatureConfiguration::new));
}
