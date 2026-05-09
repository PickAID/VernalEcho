package org.pickaid.vernalecho.echo.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.client.sound.EchoBellSoundManager;
import org.pickaid.vernalecho.echo.item.BellTarget;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;

@EventBusSubscriber(modid = VernalEcho.MOD_ID, value = Dist.CLIENT)
public final class EchoCaptureBeamRenderer {
    private static final int LENGTH_SEGMENTS = 56;
    private static final int RING_VERTICES = 10;
    private static final float TUBE_RADIUS_BASE = 0.10F;
    private static final float PULSE_AMP = 0.25F;
    private static final float PULSE_FREQ = 1.6F;
    private static final float PULSE_FLOW_SPEED = 0.95F;
    private static final float WAVE_AMP = 0.16F;
    private static final float WAVE_FREQ = 1.4F;
    private static final float WAVE_TIME_SPEED = 0.6F;
    private static final float COLOR_R = 0.36F;
    private static final float COLOR_G = 0.78F;
    private static final float COLOR_B = 1.00F;
    private static final float ALPHA_PEAK = 1.00F;

    private static final float WAVE_SECONDARY_FREQ_RATIO = 1.13F;
    private static final float WAVE_SECONDARY_PHASE_OFFSET = 0.4F;
    private static final float WAVE_SECONDARY_AMP_RATIO = 0.5F;
    private static final float LENGTH_SEGMENTS_INV = 1.0F / LENGTH_SEGMENTS;
    private static final float RING_TWO_PI_OVER_N = (float) (Math.PI * 2.0) / RING_VERTICES;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float[] CENTERS = new float[(LENGTH_SEGMENTS + 1) * 3];
    private static final float[] RADII = new float[LENGTH_SEGMENTS + 1];
    private static final float[] RING_COS = new float[RING_VERTICES];
    private static final float[] RING_SIN = new float[RING_VERTICES];

    static {
        for (int k = 0; k < RING_VERTICES; k++) {
            float ang = k * RING_TWO_PI_OVER_N;
            RING_COS[k] = (float) Math.cos(ang);
            RING_SIN[k] = (float) Math.sin(ang);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        EchoBellSoundManager.update(mc);

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(EchoBeamPipeline.RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        float time = (Util.getMillis() % 600000L) / 1000.0F;
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        for (Player renderedPlayer : mc.level.players()) {
            renderPlayerBeams(
                consumer,
                matrix,
                renderedPlayer,
                partialTick,
                renderedPlayer == player && firstPerson,
                time
            );
        }

        poseStack.popPose();
        bufferSource.endBatch(EchoBeamPipeline.RENDER_TYPE);
    }

    private static BellTarget readTarget(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(EchoDataComponents.BELL_TARGET);
    }

    private static void renderPlayerBeams(
        VertexConsumer consumer,
        Matrix4f matrix,
        Player player,
        float partialTick,
        boolean firstPerson,
        float time
    ) {
        BellTarget mainTarget = readTarget(player.getItemInHand(InteractionHand.MAIN_HAND));
        if (mainTarget != null) {
            Vec3 anchor = bellAnchor(player, InteractionHand.MAIN_HAND, partialTick, firstPerson);
            renderBeam(consumer, matrix, mainTarget.pos(), anchor, time);
        }
        BellTarget offTarget = readTarget(player.getItemInHand(InteractionHand.OFF_HAND));
        if (offTarget != null) {
            Vec3 anchor = bellAnchor(player, InteractionHand.OFF_HAND, partialTick, firstPerson);
            renderBeam(consumer, matrix, offTarget.pos(), anchor, time);
        }
    }

    public static Vec3 bellAnchor(Player player, InteractionHand hand, float partialTick, boolean firstPerson) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick).normalize();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            double yaw = Math.toRadians(player.getYRot());
            right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(look).normalize();
        boolean leftHanded = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.LEFT);
        if (firstPerson) {
            double sideBias = leftHanded ? -0.42D : 0.42D;
            return eye.add(look.scale(0.58D))
                .add(right.scale(sideBias))
                .add(up.scale(-0.30D));
        }
        double sideBias = leftHanded ? -0.08D : 0.08D;
        return eye.add(look.scale(0.66D))
            .add(right.scale(sideBias))
            .add(up.scale(0.22D));
    }

    public static Vec3 bellAnchorAtRest(Player player, InteractionHand hand, float partialTick) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float sideSign = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        double localSide = sideSign * 0.375D;
        double localForward = 0.10D;
        float bodyYawDeg = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float bodyYawRad = bodyYawDeg * Mth.DEG_TO_RAD;
        double sinY = Mth.sin(bodyYawRad);
        double cosY = Mth.cos(bodyYawRad);
        double worldDx = localSide * cosY - localForward * sinY;
        double worldDz = localSide * sinY + localForward * cosY;
        double handHeight = player.isCrouching() ? 0.55D : 0.80D;
        return player.getPosition(partialTick).add(worldDx, handHeight, worldDz);
    }

    private static void renderBeam(VertexConsumer consumer, Matrix4f matrix, Vec3 src, Vec3 dst, float time) {
        float srcX = (float) src.x;
        float srcY = (float) src.y + 1.0F;
        float srcZ = (float) src.z;
        float axisX = (float) dst.x - srcX;
        float axisY = (float) dst.y - srcY;
        float axisZ = (float) dst.z - srcZ;
        float length = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (length < 0.1F) {
            return;
        }
        float invLen = 1.0F / length;
        float ux = axisX * invLen, uy = axisY * invLen, uz = axisZ * invLen;
        float helperX, helperY, helperZ;
        if (Math.abs(uy) > 0.95F) {
            helperX = 1.0F; helperY = 0.0F;
        } else {
            helperX = 0.0F; helperY = 1.0F;
        }
        helperZ = 0.0F;
        float perpAx = uy * helperZ - uz * helperY;
        float perpAy = uz * helperX - ux * helperZ;
        float perpAz = ux * helperY - uy * helperX;
        float perpANorm = (float) (1.0 / Math.sqrt(perpAx * perpAx + perpAy * perpAy + perpAz * perpAz));
        perpAx *= perpANorm; perpAy *= perpANorm; perpAz *= perpANorm;
        float perpBx = perpAy * uz - perpAz * uy;
        float perpBy = perpAz * ux - perpAx * uz;
        float perpBz = perpAx * uy - perpAy * ux;
        float waveTime = time * WAVE_TIME_SPEED;
        float pulseTime = time * PULSE_FLOW_SPEED;
        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i * LENGTH_SEGMENTS_INV;
            float taper = (float) Math.sin(Math.PI * t);
            float wavePhase = t * WAVE_FREQ + waveTime;
            float lateral = (float) Math.sin(wavePhase) * WAVE_AMP * taper;
            float perp = (float) Math.cos(wavePhase * WAVE_SECONDARY_FREQ_RATIO + WAVE_SECONDARY_PHASE_OFFSET)
                * WAVE_AMP * WAVE_SECONDARY_AMP_RATIO * taper;
            float baseX = srcX + ux * length * t;
            float baseY = srcY + uy * length * t;
            float baseZ = srcZ + uz * length * t;
            int idx = i * 3;
            CENTERS[idx]     = baseX + perpAx * lateral + perpBx * perp;
            CENTERS[idx + 1] = baseY + perpAy * lateral + perpBy * perp;
            CENTERS[idx + 2] = baseZ + perpAz * lateral + perpBz * perp;
            float pulsePhase = t * PULSE_FREQ * TWO_PI - pulseTime * TWO_PI;
            float radiusBase = TUBE_RADIUS_BASE * (0.5F + 0.5F * taper);
            float pulse = 1.0F + PULSE_AMP * (float) Math.sin(pulsePhase);
            RADII[i] = radiusBase * pulse;
        }
        for (int i = 0; i < LENGTH_SEGMENTS; i++) {
            int idx0 = i * 3;
            int idx1 = idx0 + 3;
            float c0x = CENTERS[idx0],     c0y = CENTERS[idx0 + 1], c0z = CENTERS[idx0 + 2];
            float c1x = CENTERS[idx1],     c1y = CENTERS[idx1 + 1], c1z = CENTERS[idx1 + 2];
            float r0 = RADII[i];
            float r1 = RADII[i + 1];
            float t0 = i * LENGTH_SEGMENTS_INV;
            float t1 = (i + 1) * LENGTH_SEGMENTS_INV;
            float a0 = (float) Math.sin(Math.PI * t0) * ALPHA_PEAK;
            float a1 = (float) Math.sin(Math.PI * t1) * ALPHA_PEAK;
            int color0 = ARGB.colorFromFloat(a0, COLOR_R, COLOR_G, COLOR_B);
            int color1 = ARGB.colorFromFloat(a1, COLOR_R, COLOR_G, COLOR_B);
            for (int k = 0; k < RING_VERTICES; k++) {
                int kNext = (k + 1) % RING_VERTICES;
                float cosK = RING_COS[k],     sinK = RING_SIN[k];
                float cosN = RING_COS[kNext], sinN = RING_SIN[kNext];
                float dir0x = perpAx * cosK + perpBx * sinK;
                float dir0y = perpAy * cosK + perpBy * sinK;
                float dir0z = perpAz * cosK + perpBz * sinK;
                float dir1x = perpAx * cosN + perpBx * sinN;
                float dir1y = perpAy * cosN + perpBy * sinN;
                float dir1z = perpAz * cosN + perpBz * sinN;
                float u0 = k * (1.0F / RING_VERTICES);
                float u1 = (k + 1) * (1.0F / RING_VERTICES);
                putVertex(consumer, matrix, c0x + dir0x * r0, c0y + dir0y * r0, c0z + dir0z * r0, color0, u0, t0);
                putVertex(consumer, matrix, c0x + dir1x * r0, c0y + dir1y * r0, c0z + dir1z * r0, color0, u1, t0);
                putVertex(consumer, matrix, c1x + dir1x * r1, c1y + dir1y * r1, c1z + dir1z * r1, color1, u1, t1);
                putVertex(consumer, matrix, c1x + dir0x * r1, c1y + dir0y * r1, c1z + dir0z * r1, color1, u0, t1);
            }
        }
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int color, float u, float v) {
        consumer.addVertex(matrix, x, y, z)
            .setUv(u, v)
            .setColor(color);
    }
}
