package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum EchoOrigin implements StringRepresentable {
    PLAYER_ACTIVITY("player_activity"),
    WILD("wild");

    public static final Codec<EchoOrigin> CODEC = StringRepresentable.fromEnum(EchoOrigin::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoOrigin> STREAM_CODEC = StreamCodec.of(
        (buf, origin) -> buf.writeEnum(origin),
        buf -> buf.readEnum(EchoOrigin.class)
    );

    private final String serializedName;

    EchoOrigin(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
