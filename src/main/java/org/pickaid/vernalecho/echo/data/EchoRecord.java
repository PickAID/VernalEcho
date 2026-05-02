package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record EchoRecord(
    UUID id,
    UUID playerUUID,
    Vec3 pos,
    long spawnTime,
    double weight,
    GearSnapshot gearSnapshot,
    EchoAffinity affinity,
    boolean boosted,
    EchoOrigin origin,
    EchoPose pose,
    float yaw
) {
    public static final Codec<EchoRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(EchoRecord::id),
        UUIDUtil.STRING_CODEC.fieldOf("playerUUID").forGetter(EchoRecord::playerUUID),
        Vec3.CODEC.fieldOf("pos").forGetter(EchoRecord::pos),
        Codec.LONG.fieldOf("spawnTime").forGetter(EchoRecord::spawnTime),
        Codec.DOUBLE.optionalFieldOf("weight", 0.0D).forGetter(EchoRecord::weight),
        GearSnapshot.CODEC.optionalFieldOf("gearSnapshot", GearSnapshot.EMPTY).forGetter(EchoRecord::gearSnapshot),
        EchoAffinity.CODEC.optionalFieldOf("affinity", EchoAffinity.PLAYER_WEIGHTED).forGetter(EchoRecord::affinity),
        Codec.BOOL.optionalFieldOf("boosted", false).forGetter(EchoRecord::boosted),
        EchoOrigin.CODEC.optionalFieldOf("origin", EchoOrigin.PLAYER_ACTIVITY).forGetter(EchoRecord::origin),
        EchoPose.CODEC.optionalFieldOf("pose", EchoPose.IDLE).forGetter(EchoRecord::pose),
        Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(EchoRecord::yaw)
    ).apply(instance, EchoRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoRecord> STREAM_CODEC = StreamCodec.of(
        EchoRecord::write,
        EchoRecord::read
    );

    public float formationProgress(long gameTime) {
        if (this.origin == EchoOrigin.WILD) {
            return 1.0F;
        }
        long age = Math.max(0L, gameTime - this.spawnTime);
        return (float)Math.min(1.0D, age / 1200.0D);
    }

    private static void write(RegistryFriendlyByteBuf buf, EchoRecord record) {
        buf.writeUUID(record.id);
        buf.writeUUID(record.playerUUID);
        Vec3.STREAM_CODEC.encode(buf, record.pos);
        buf.writeLong(record.spawnTime);
        buf.writeDouble(record.weight);
        GearSnapshot.STREAM_CODEC.encode(buf, record.gearSnapshot);
        EchoAffinity.STREAM_CODEC.encode(buf, record.affinity);
        buf.writeBoolean(record.boosted);
        EchoOrigin.STREAM_CODEC.encode(buf, record.origin);
        EchoPose.STREAM_CODEC.encode(buf, record.pose);
        buf.writeFloat(record.yaw);
    }

    private static EchoRecord read(RegistryFriendlyByteBuf buf) {
        return new EchoRecord(
            buf.readUUID(),
            buf.readUUID(),
            Vec3.STREAM_CODEC.decode(buf),
            buf.readLong(),
            buf.readDouble(),
            GearSnapshot.STREAM_CODEC.decode(buf),
            EchoAffinity.STREAM_CODEC.decode(buf),
            buf.readBoolean(),
            EchoOrigin.STREAM_CODEC.decode(buf),
            EchoPose.STREAM_CODEC.decode(buf),
            buf.readFloat()
        );
    }
}
