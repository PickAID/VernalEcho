package org.pickaid.vernalecho.echo.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.data.EchoChunkData;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.client.fx.EchoCaptureBeamRenderer;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderContext;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderers;
import org.pickaid.vernalecho.echo.client.render.EchoPropSilhouetteRenderer;
import org.pickaid.vernalecho.echo.item.BellTarget;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = VernalEcho.MOD_ID, value = Dist.CLIENT)
public final class EchoClientRenderEvents {
    private static final Identifier SKINLESS_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final RenderType ECHO_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(SKINLESS_TEXTURE, false);
    private static final PlayerModel MODEL = new PlayerModel(
        LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(),
        false
    );
    private static final int FULL_BRIGHT = 15728880;
    private static final double RENDER_DISTANCE_SQR = 48.0D * 48.0D;
    private static final int CAPTURE_FINISH_ANIMATION_TICKS = 18;
    private static final Map<CaptureHandKey, ActiveCaptureState> ACTIVE_CAPTURE_TARGETS = new HashMap<>();
    private static final Map<CaptureHandKey, ActiveCaptureState> CURRENT_CAPTURE_TARGETS = new HashMap<>();
    private static final Map<UUID, EchoRecord> VISIBLE_ECHOES = new HashMap<>();
    private static final Map<UUID, CaptureFinishAnimation> FINISH_ANIMATIONS = new HashMap<>();

    private EchoClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderEchoes(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        int centerChunkX = SectionPos.blockToSectionCoord(player.blockPosition().getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(player.blockPosition().getZ());
        long gameTime = level.getGameTime();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        int rendered = 0;

        VISIBLE_ECHOES.clear();
        for (int chunkX = centerChunkX - 3; chunkX <= centerChunkX + 3 && rendered < 32; chunkX++) {
            for (int chunkZ = centerChunkZ - 3; chunkZ <= centerChunkZ + 3 && rendered < 32; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                EchoChunkData data = chunk.getExistingDataOrNull(EchoAttachments.ECHO_CHUNK);
                if (data == null) {
                    continue;
                }

                for (EchoRecord record : data.records()) {
                    if (record.pos().distanceToSqr(player.position()) <= RENDER_DISTANCE_SQR) {
                        VISIBLE_ECHOES.put(record.id(), record);
                        renderEcho(record, poseStack, bufferSource, cameraPos, gameTime);
                        rendered++;
                    }
                    if (rendered >= 32) {
                        break;
                    }
                }
            }
        }

        double renderTime = gameTime + partialTick;
        updateCaptureFinishAnimations(level, player, partialTick, firstPerson, renderTime);
        rendered += renderCaptureFinishAnimations(poseStack, bufferSource, cameraPos, gameTime, renderTime);

        if (rendered > 0) {
            bufferSource.endBatch(ECHO_RENDER_TYPE);
        }
    }

    private static void renderEcho(
        EchoRecord record,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Vec3 cameraPos,
        long gameTime
    ) {
        renderEcho(record, poseStack, bufferSource, cameraPos, gameTime, record.pos(), 1.0F, 1.0F);
    }

    private static void renderEcho(
        EchoRecord record,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Vec3 cameraPos,
        long gameTime,
        Vec3 renderPos,
        float animationScale,
        float animationAlpha
    ) {
        float progress = record.formationProgress(gameTime);
        if (progress <= 0.02F) {
            return;
        }

        float pulse = 0.82F + 0.18F * Mth.sin((gameTime + record.id().getLeastSignificantBits() % 97L) * 0.08F);
        float alpha = (0.16F + 0.22F * progress) * pulse * animationAlpha;
        int color = ARGB.colorFromFloat(alpha, 0.24F, 0.78F, 1.0F);

        poseStack.pushPose();
        poseStack.translate(renderPos.x() - cameraPos.x(), renderPos.y() - cameraPos.y(), renderPos.z() - cameraPos.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - record.yaw()));
        if (record.pose() == EchoPose.FALLEN) {
            poseStack.translate(0.0F, 0.18F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(82.0F));
        }
        float scale = 0.94F + 0.025F * Mth.sin((gameTime + record.id().getMostSignificantBits() % 131L) * 0.06F);
        scale *= animationScale;
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        resetModel();
        EchoPoseRenderContext context = EchoPoseRenderers.apply(record.pose(), new EchoPoseRenderContext(record, MODEL, gameTime));

        VertexConsumer consumer = bufferSource.getBuffer(ECHO_RENDER_TYPE);
        MODEL.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        EchoPropSilhouetteRenderer.render(context.props(), MODEL, poseStack, consumer, color, FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void updateCaptureFinishAnimations(
        ClientLevel level,
        LocalPlayer localPlayer,
        float partialTick,
        boolean firstPerson,
        double renderTime
    ) {
        CURRENT_CAPTURE_TARGETS.clear();
        for (Player player : level.players()) {
            updateCaptureHand(player, InteractionHand.MAIN_HAND, localPlayer, partialTick, firstPerson, renderTime);
            updateCaptureHand(player, InteractionHand.OFF_HAND, localPlayer, partialTick, firstPerson, renderTime);
        }
        ACTIVE_CAPTURE_TARGETS.clear();
        ACTIVE_CAPTURE_TARGETS.putAll(CURRENT_CAPTURE_TARGETS);
    }

    private static void updateCaptureHand(
        Player player,
        InteractionHand hand,
        LocalPlayer localPlayer,
        float partialTick,
        boolean firstPerson,
        double renderTime
    ) {
        ItemStack stack = player.getItemInHand(hand);
        BellTarget currentTarget = stack.get(EchoDataComponents.BELL_TARGET);
        CaptureHandKey key = new CaptureHandKey(player.getUUID(), hand);
        boolean useFirstPersonAnchor = player == localPlayer && firstPerson;
        if (currentTarget != null) {
            Vec3 anchor = useFirstPersonAnchor
                ? EchoCaptureBeamRenderer.bellAnchor(player, hand, partialTick, true)
                : restingBellAnchor(player, hand, partialTick);
            CURRENT_CAPTURE_TARGETS.put(key, new ActiveCaptureState(currentTarget, anchor));
            return;
        }

        ActiveCaptureState previousState = ACTIVE_CAPTURE_TARGETS.get(key);
        if (previousState == null) {
            return;
        }
        EchoRecord captured = stack.get(EchoDataComponents.CAPTURED_ECHO);
        if (captured == null || !captured.id().equals(previousState.target().recordId()) || FINISH_ANIMATIONS.containsKey(captured.id())) {
            return;
        }

        EchoRecord record = VISIBLE_ECHOES.getOrDefault(captured.id(), captured);
        FINISH_ANIMATIONS.put(record.id(), new CaptureFinishAnimation(record, record.pos(), previousState.anchor(), renderTime));
    }

    private static Vec3 restingBellAnchor(Player player, InteractionHand hand, float partialTick) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -0.30D : 0.30D;
        float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * ((float)Math.PI / 180.0F);
        double sin = Mth.sin(bodyRot);
        double cos = Mth.cos(bodyRot);
        double yOff = player.getBoundingBox().getYsize() - 1.22D + (player.isCrouching() ? -0.12D : 0.0D);
        return player.getPosition(partialTick).add(
            side * cos - 0.10D * sin,
            yOff,
            side * sin + 0.10D * cos
        );
    }

    private static int renderCaptureFinishAnimations(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Vec3 cameraPos,
        long gameTime,
        double renderTime
    ) {
        int rendered = 0;
        Iterator<CaptureFinishAnimation> iterator = FINISH_ANIMATIONS.values().iterator();
        while (iterator.hasNext()) {
            CaptureFinishAnimation animation = iterator.next();
            float age = (float) ((renderTime - animation.startTime()) / CAPTURE_FINISH_ANIMATION_TICKS);
            if (age >= 1.0F) {
                iterator.remove();
                continue;
            }

            float shrinkProgress = Mth.clamp(age / 0.45F, 0.0F, 1.0F);
            float flyProgress = Mth.clamp((age - 0.28F) / 0.72F, 0.0F, 1.0F);
            float easedFly = flyProgress * flyProgress * (3.0F - 2.0F * flyProgress);
            float scale = Mth.lerp(shrinkProgress, 1.0F, 0.34F) * Mth.lerp(flyProgress, 1.0F, 0.58F);
            float alpha = 1.0F - 0.90F * flyProgress;
            Vec3 pos = animation.source().lerp(animation.destination(), easedFly);
            if (flyProgress > 0.0F) {
                double arc = Math.sin(Math.PI * flyProgress) * 0.22D;
                pos = pos.add(0.0D, arc, 0.0D);
            }
            renderEcho(animation.record(), poseStack, bufferSource, cameraPos, gameTime, pos, scale, alpha);
            rendered++;
        }
        return rendered;
    }

    private static void resetModel() {
        MODEL.resetPose();
        MODEL.hat.visible = false;
        MODEL.jacket.visible = false;
        MODEL.leftSleeve.visible = false;
        MODEL.rightSleeve.visible = false;
        MODEL.leftPants.visible = false;
        MODEL.rightPants.visible = false;
    }

    private record CaptureHandKey(UUID playerId, InteractionHand hand) {
    }

    private record ActiveCaptureState(BellTarget target, Vec3 anchor) {
    }

    private record CaptureFinishAnimation(
        EchoRecord record,
        Vec3 source,
        Vec3 destination,
        double startTime
    ) {
    }
}
