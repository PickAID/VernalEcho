package org.pickaid.vernalecho.echo.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record BellTarget(UUID recordId, Vec3 pos) {
    public static final Codec<BellTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.STRING_CODEC.fieldOf("recordId").forGetter(BellTarget::recordId),
        Vec3.CODEC.fieldOf("pos").forGetter(BellTarget::pos)
    ).apply(instance, BellTarget::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BellTarget> STREAM_CODEC = StreamCodec.of(
        (buf, target) -> {
            buf.writeUUID(target.recordId);
            Vec3.STREAM_CODEC.encode(buf, target.pos);
        },
        buf -> new BellTarget(buf.readUUID(), Vec3.STREAM_CODEC.decode(buf))
    );
}
