package org.pickaid.vernalecho.echo.server;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.data.EchoChunkData;
import org.pickaid.vernalecho.echo.data.EchoRecord;

public final class EchoCaptureService {
    private static final double LOCK_RADIUS = 1.4D;

    public static @Nullable EchoRecord findInSight(ServerPlayer player, double maxDistance) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        ChunkPos centerCp = player.chunkPosition();
        int chunkRange = (int) Math.ceil(maxDistance / 16.0D) + 1;

        EchoRecord best = null;
        double bestPerp = Double.MAX_VALUE;
        double bestForward = Double.MAX_VALUE;

        for (int dx = -chunkRange; dx <= chunkRange; dx++) {
            for (int dz = -chunkRange; dz <= chunkRange; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centerCp.x() + dx, centerCp.z() + dz, false);
                if (chunk == null) {
                    continue;
                }
                EchoChunkData data = chunk.getData(EchoAttachments.ECHO_CHUNK);
                for (EchoRecord record : data.records()) {
                    Vec3 p = record.pos();
                    Vec3 toRecord = p.subtract(eye);
                    double forward = toRecord.dot(look);
                    if (forward <= 0.0D || forward > maxDistance) {
                        continue;
                    }
                    Vec3 nearest = eye.add(look.scale(forward));
                    double perp = p.distanceTo(nearest);
                    if (perp > LOCK_RADIUS) {
                        continue;
                    }
                    if (perp < bestPerp || (perp == bestPerp && forward < bestForward)) {
                        best = record;
                        bestPerp = perp;
                        bestForward = forward;
                    }
                }
            }
        }
        return best;
    }

    public static @Nullable EchoRecord consumeRecord(ServerLevel level, Vec3 recordPos, UUID recordId) {
        LevelChunk chunk = level.getChunkAt(BlockPos.containing(recordPos));
        EchoChunkData data = chunk.getData(EchoAttachments.ECHO_CHUNK);
        EchoRecord found = null;
        for (EchoRecord record : data.records()) {
            if (record.id().equals(recordId)) {
                found = record;
                break;
            }
        }
        if (found == null) {
            return null;
        }
        EchoChunkData updated = data.removeRecord(recordId);
        chunk.setData(EchoAttachments.ECHO_CHUNK, updated);
        chunk.markUnsaved();
        return found;
    }

    public static void releaseRecord(ServerLevel level, EchoRecord record) {
        LevelChunk chunk = level.getChunkAt(BlockPos.containing(record.pos()));
        EchoChunkData data = chunk.getData(EchoAttachments.ECHO_CHUNK);
        EchoChunkData updated = data.placeCapturedEcho(record, level.getGameTime());
        chunk.setData(EchoAttachments.ECHO_CHUNK, updated);
        chunk.markUnsaved();
    }
}
