package org.pickaid.vernalecho.echo.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.pickaid.vernalecho.VernalEcho;
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

    private EchoCaptureBeamRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        BellTarget mainTarget = readTarget(player.getItemInHand(InteractionHand.MAIN_HAND));
        BellTarget offTarget = readTarget(player.getItemInHand(InteractionHand.OFF_HAND));
        if (mainTarget == null && offTarget == null) {
            return;
        }

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(EchoBeamPipeline.RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = poseStack.last().pose();

        float time = (Util.getMillis() % 600000L) / 1000.0F;

        if (mainTarget != null) {
            renderBeam(consumer, matrix, mainTarget.pos(), handAnchor(player, InteractionHand.MAIN_HAND, partialTick), time);
        }
        if (offTarget != null) {
            renderBeam(consumer, matrix, offTarget.pos(), handAnchor(player, InteractionHand.OFF_HAND, partialTick), time);
        }

        poseStack.popPose();
        bufferSource.endBatch(EchoBeamPipeline.RENDER_TYPE);
    }

    private static BellTarget readTarget(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(EchoDataComponents.BELL_TARGET);
    }

    private static Vec3 handAnchor(LocalPlayer player, InteractionHand hand, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        boolean leftHanded = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.LEFT);
        double sideBias = leftHanded ? -0.22D : 0.22D;
        return eye.add(look.scale(0.55D)).add(right.scale(sideBias)).add(0.0D, -0.18D, 0.0D);
    }

    private static void renderBeam(VertexConsumer consumer, Matrix4f matrix, Vec3 src, Vec3 dst, float time) {
        Vec3 srcAdj = src.add(0.0D, 1.0D, 0.0D);
        Vec3 axisVec = dst.subtract(srcAdj);
        double length = axisVec.length();
        if (length < 0.1D) {
            return;
        }
        Vec3 unitAxis = axisVec.scale(1.0D / length);
        Vec3 helper = Math.abs(unitAxis.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 perpA = unitAxis.cross(helper).normalize();
        Vec3 perpB = perpA.cross(unitAxis).normalize();

        Vec3[] centers = new Vec3[LENGTH_SEGMENTS + 1];
        float[] radii = new float[LENGTH_SEGMENTS + 1];

        float waveTime = time * WAVE_TIME_SPEED;
        float pulseTime = time * PULSE_FLOW_SPEED;
        float twoPi = (float) (Math.PI * 2.0);

        for (int i = 0; i <= LENGTH_SEGMENTS; i++) {
            float t = i / (float) LENGTH_SEGMENTS;
            float taper = (float) Math.sin(Math.PI * t);

            // 中线
            Vec3 base = srcAdj.add(unitAxis.scale(length * t));
            float wavePhase = t * WAVE_FREQ + waveTime;
            float lateral = (float) Math.sin(wavePhase) * WAVE_AMP * taper;
            float perp = (float) Math.cos(wavePhase * 1.13F + 0.4F) * WAVE_AMP * 0.5F * taper;
            centers[i] = base.add(perpA.scale(lateral)).add(perpB.scale(perp));

            // 涌动效果
            float pulsePhase = t * PULSE_FREQ * twoPi - pulseTime * twoPi;
            float radiusBase = TUBE_RADIUS_BASE * (0.5F + 0.5F * taper);
            float pulse = 1.0F + PULSE_AMP * (float) Math.sin(pulsePhase);
            radii[i] = radiusBase * pulse;
        }

        Vec3[] ringDirs = new Vec3[RING_VERTICES];
        for (int k = 0; k < RING_VERTICES; k++) {
            float ang = k * twoPi / RING_VERTICES;
            ringDirs[k] = perpA.scale(Math.cos(ang)).add(perpB.scale(Math.sin(ang)));
        }

        for (int i = 0; i < LENGTH_SEGMENTS; i++) {
            Vec3 c0 = centers[i];
            Vec3 c1 = centers[i + 1];
            float r0 = radii[i];
            float r1 = radii[i + 1];
            float t0 = i / (float) LENGTH_SEGMENTS;
            float t1 = (i + 1) / (float) LENGTH_SEGMENTS;
            float a0 = (float) Math.sin(Math.PI * t0) * ALPHA_PEAK;
            float a1 = (float) Math.sin(Math.PI * t1) * ALPHA_PEAK;
            int color0 = ARGB.colorFromFloat(a0, COLOR_R, COLOR_G, COLOR_B);
            int color1 = ARGB.colorFromFloat(a1, COLOR_R, COLOR_G, COLOR_B);

            for (int k = 0; k < RING_VERTICES; k++) {
                int kNext = (k + 1) % RING_VERTICES;
                Vec3 dir0 = ringDirs[k];
                Vec3 dir1 = ringDirs[kNext];

                Vec3 p00 = c0.add(dir0.scale(r0));
                Vec3 p01 = c0.add(dir1.scale(r0));
                Vec3 p11 = c1.add(dir1.scale(r1));
                Vec3 p10 = c1.add(dir0.scale(r1));

                float u0 = k / (float) RING_VERTICES;
                float u1 = (k + 1) / (float) RING_VERTICES;

                putVertex(consumer, matrix, p00, color0, u0, t0);
                putVertex(consumer, matrix, p01, color0, u1, t0);
                putVertex(consumer, matrix, p11, color1, u1, t1);
                putVertex(consumer, matrix, p10, color1, u0, t1);
            }
        }
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f matrix, Vec3 p, int color, float u, float v) {
        consumer.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z)
            .setUv(u, v)
            .setColor(color);
    }
}
