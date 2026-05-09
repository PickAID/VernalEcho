package org.pickaid.vernalecho.echo.network;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.data.EchoRecord;

public record EchoCaptureFinishedPayload(
    UUID playerId,
    InteractionHand hand,
    Vec3 sourcePos,
    EchoRecord record
) implements CustomPacketPayload {
    public static final Type<EchoCaptureFinishedPayload> TYPE =
        new Type<>(VernalEcho.id("capture_finished"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoCaptureFinishedPayload> STREAM_CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, EchoCaptureFinishedPayload::playerId,
            InteractionHand.STREAM_CODEC, EchoCaptureFinishedPayload::hand,
            Vec3.STREAM_CODEC, EchoCaptureFinishedPayload::sourcePos,
            EchoRecord.STREAM_CODEC, EchoCaptureFinishedPayload::record,
            EchoCaptureFinishedPayload::new
        );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
