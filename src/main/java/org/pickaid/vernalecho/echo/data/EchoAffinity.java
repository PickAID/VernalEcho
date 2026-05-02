package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record EchoAffinity(double playerEcho, double wildEcho, double randomEcho) {
    public static final EchoAffinity PLAYER_WEIGHTED = new EchoAffinity(0.7D, 0.2D, 0.1D);
    public static final EchoAffinity WILD_WEIGHTED = new EchoAffinity(0.05D, 0.85D, 0.1D);

    public static final Codec<EchoAffinity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("playerEcho", PLAYER_WEIGHTED.playerEcho).forGetter(EchoAffinity::playerEcho),
        Codec.DOUBLE.optionalFieldOf("wildEcho", PLAYER_WEIGHTED.wildEcho).forGetter(EchoAffinity::wildEcho),
        Codec.DOUBLE.optionalFieldOf("randomEcho", PLAYER_WEIGHTED.randomEcho).forGetter(EchoAffinity::randomEcho)
    ).apply(instance, EchoAffinity::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoAffinity> STREAM_CODEC = StreamCodec.of(
        (buf, affinity) -> {
            buf.writeDouble(affinity.playerEcho);
            buf.writeDouble(affinity.wildEcho);
            buf.writeDouble(affinity.randomEcho);
        },
        buf -> new EchoAffinity(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );
}
