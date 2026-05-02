package org.pickaid.vernalecho.echo.server;

import org.pickaid.vernalecho.echo.data.EchoAffinity;
import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.data.EchoChunkData;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.GearSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.pickaid.vernalecho.echo.worldgen.EchoNaturalSpawner;

public final class EchoActivityHandler {
    private EchoActivityHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EchoActivityHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(EchoActivityHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(EchoActivityHandler::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(EchoActivityHandler::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EchoActivityHandler::onEnteringSection);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level() instanceof ServerLevel level) {
            EchoPose pose = player.isCrouching() ? EchoPose.CROUCHING : EchoPose.REACHING;
            addResidue(level, player, event.getHitVec().getLocation(), 0.5D, pose, false);
        }
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level() instanceof ServerLevel level) {
            addResidue(level, player, event.getTarget().position(), 0.8D, EchoPose.REACHING, false);
        }
    }

    private static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level() instanceof ServerLevel level) {
            addResidue(level, player, event.getTarget().position(), 1.0D, EchoPose.GUARDING, false);
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            addResidue(level, player, player.position(), 4.0D, EchoPose.FALLEN, true);
        }

        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player && source != event.getEntity()) {
            addResidue(level, player, event.getEntity().position(), 1.0D, EchoPose.GUARDING, false);
        }
    }

    private static void onEnteringSection(EntityEvent.EnteringSection event) {
        EchoNaturalSpawner.handleEnteringSection(event);
    }

    private static void addResidue(ServerLevel level, Player player, Vec3 pos, double amount, EchoPose pose, boolean boosted) {
        LevelChunk chunk = level.getChunkAt(BlockPos.containing(pos));
        EchoChunkData current = chunk.getData(EchoAttachments.ECHO_CHUNK);
        EchoChunkData updated = current.addResidue(
            player.getUUID(),
            pos,
            player.getYRot(),
            level.getGameTime(),
            amount,
            GearSnapshot.capture(player),
            EchoAffinity.PLAYER_WEIGHTED,
            boosted,
            pose,
            level.getRandom()
        );

        if (updated != current) {
            chunk.setData(EchoAttachments.ECHO_CHUNK, updated);
            if (updated.records().size() > current.records().size()) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x(), pos.y() + 0.4D, pos.z(), 8, 0.35D, 0.45D, 0.35D, 0.01D);
            }
        }
    }
}
