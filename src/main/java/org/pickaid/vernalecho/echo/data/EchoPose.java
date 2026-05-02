package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum EchoPose implements StringRepresentable {
    IDLE("idle"),
    WALKING("walking"),
    CROUCHING("crouching"),
    GUARDING("guarding"),
    REACHING("reaching"),
    CROUCH_REACHING("crouch_reaching"),
    READING("reading"),
    FALLEN("fallen");

    public static final Codec<EchoPose> CODEC = StringRepresentable.fromEnum(EchoPose::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoPose> STREAM_CODEC = StreamCodec.of(
        (buf, pose) -> buf.writeEnum(pose),
        buf -> buf.readEnum(EchoPose.class)
    );

    private final String serializedName;

    EchoPose(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
