package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record EchoChunkData(double accumulatedWeight, long lastTriggerGameTime, List<EchoRecord> records) {
    private static final double THRESHOLD = 10.0D;
    private static final double BASE_CHANCE = 0.15D;
    private static final int MAX_RECORDS_PER_CHUNK = 24;

    public static final MapCodec<EchoChunkData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.DOUBLE.optionalFieldOf("accumulatedWeight", 0.0D).forGetter(EchoChunkData::accumulatedWeight),
        Codec.LONG.optionalFieldOf("lastTriggerGameTime", 0L).forGetter(EchoChunkData::lastTriggerGameTime),
        EchoRecord.CODEC.listOf().optionalFieldOf("records", List.of()).forGetter(EchoChunkData::records)
    ).apply(instance, EchoChunkData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoChunkData> STREAM_CODEC = StreamCodec.of(
        EchoChunkData::write,
        EchoChunkData::read
    );

    public static EchoChunkData empty() {
        return new EchoChunkData(0.0D, 0L, List.of());
    }

    public boolean isEmpty() {
        return this.accumulatedWeight <= 0.0D && this.records.isEmpty();
    }

    public boolean hasWildEcho() {
        return this.records.stream().anyMatch(record -> record.origin() == EchoOrigin.WILD);
    }

    public EchoChunkData addResidue(
        UUID playerUUID,
        Vec3 pos,
        float yaw,
        long gameTime,
        double amount,
        GearSnapshot gearSnapshot,
        EchoAffinity affinity,
        boolean boosted,
        EchoPose pose,
        RandomSource random
    ) {
        double newWeight = Math.min(64.0D, this.accumulatedWeight + amount);
        if (newWeight < THRESHOLD) {
            return new EchoChunkData(newWeight, this.lastTriggerGameTime, this.records);
        }

        double densityFactor = 1.0D / (1.0D + this.records.size());
        double cooldownFactor = clamp((gameTime - this.lastTriggerGameTime) / 1200.0D, 0.5D, 1.5D);
        double chance = BASE_CHANCE * densityFactor * cooldownFactor;
        if (random.nextDouble() >= chance) {
            return new EchoChunkData(Math.min(newWeight, THRESHOLD * 1.8D), this.lastTriggerGameTime, this.records);
        }

        EchoRecord record = new EchoRecord(
            UUID.randomUUID(),
            playerUUID,
            pos,
            gameTime,
            newWeight,
            gearSnapshot,
            affinity,
            boosted,
            EchoOrigin.PLAYER_ACTIVITY,
            pose,
            yaw
        );
        return this.addRecord(record, newWeight * 0.3D, gameTime);
    }

    public EchoChunkData addWildEcho(EchoRecord record) {
        if (this.hasWildEcho()) {
            return this;
        }
        return this.addRecord(record, this.accumulatedWeight, this.lastTriggerGameTime);
    }

    public EchoChunkData placeCapturedEcho(EchoRecord record, long gameTime) {
        return this.addRecord(record, this.accumulatedWeight, gameTime);
    }

    public EchoChunkData removeRecord(UUID recordId) {
        int index = -1;
        for (int i = 0; i < this.records.size(); i++) {
            if (this.records.get(i).id().equals(recordId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return this;
        }
        List<EchoRecord> newRecords = new ArrayList<>(this.records);
        newRecords.remove(index);
        return new EchoChunkData(this.accumulatedWeight, this.lastTriggerGameTime, List.copyOf(newRecords));
    }

    private EchoChunkData addRecord(EchoRecord record, double newWeight, long newLastTriggerGameTime) {
        List<EchoRecord> newRecords = new ArrayList<>(this.records);
        newRecords.add(record);
        while (newRecords.size() > MAX_RECORDS_PER_CHUNK) {
            newRecords.removeFirst();
        }
        return new EchoChunkData(newWeight, newLastTriggerGameTime, List.copyOf(newRecords));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void write(RegistryFriendlyByteBuf buf, EchoChunkData data) {
        buf.writeDouble(data.accumulatedWeight);
        buf.writeLong(data.lastTriggerGameTime);
        buf.writeVarInt(data.records.size());
        for (EchoRecord record : data.records) {
            EchoRecord.STREAM_CODEC.encode(buf, record);
        }
    }

    private static EchoChunkData read(RegistryFriendlyByteBuf buf) {
        double accumulatedWeight = buf.readDouble();
        long lastTriggerGameTime = buf.readLong();
        int recordCount = buf.readVarInt();
        List<EchoRecord> records = new ArrayList<>(Math.min(recordCount, MAX_RECORDS_PER_CHUNK));
        for (int i = 0; i < recordCount; i++) {
            records.add(EchoRecord.STREAM_CODEC.decode(buf));
        }
        return new EchoChunkData(accumulatedWeight, lastTriggerGameTime, List.copyOf(records));
    }
}
