package org.pickaid.vernalecho.echo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.math.Axis;

public final class EchoPropSilhouetteRenderer {
    private EchoPropSilhouetteRenderer() {
    }

    public static void render(
        List<EchoPropRenderPlan> plans,
        PlayerModel model,
        PoseStack poseStack,
        VertexConsumer consumer,
        int color,
        int lightCoords
    ) {
        for (EchoPropRenderPlan plan : plans) {
            poseStack.pushPose();
            applyAnchor(plan.anchor(), model, poseStack);
            renderStyle(plan.style(), poseStack, consumer, color, lightCoords);
            poseStack.popPose();
        }
    }

    private static void applyAnchor(EchoPropAnchor anchor, PlayerModel model, PoseStack poseStack) {
        switch (anchor) {
            case RIGHT_HAND -> {
                model.translateToHand(null, HumanoidArm.RIGHT, poseStack);
                poseStack.translate(0.0F, 0.62F, -0.02F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F));
            }
            case LEFT_HAND -> {
                model.translateToHand(null, HumanoidArm.LEFT, poseStack);
                poseStack.translate(0.0F, 0.62F, -0.02F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-8.0F));
            }
            case GROUND_RIGHT -> {
                poseStack.translate(0.36F, 1.42F, -0.18F);
                poseStack.mulPose(Axis.XP.rotationDegrees(82.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(18.0F));
            }
        }
    }

    private static void renderStyle(EchoPropStyle style, PoseStack poseStack, VertexConsumer consumer, int color, int lightCoords) {
        switch (style) {
            case SHIELD -> {
                poseStack.translate(0.02F, 0.26F, -0.05F);
                poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
                drawBox(poseStack, consumer, -0.19F, -0.05F, -0.035F, 0.19F, 0.42F, 0.035F, color, lightCoords);
            }
            case BOOK -> {
                poseStack.translate(0.0F, 0.25F, -0.05F);
                poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
                drawBox(poseStack, consumer, -0.22F, 0.0F, -0.025F, -0.015F, 0.27F, 0.025F, color, lightCoords);
                drawBox(poseStack, consumer, 0.015F, 0.0F, -0.025F, 0.22F, 0.27F, 0.025F, color, lightCoords);
            }
            case ITEM -> {
                poseStack.translate(0.0F, 0.24F, -0.04F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(26.0F));
                drawBox(poseStack, consumer, -0.055F, -0.02F, -0.03F, 0.055F, 0.32F, 0.03F, color, lightCoords);
            }
        }
    }

    private static void drawBox(
        PoseStack poseStack,
        VertexConsumer consumer,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        int color,
        int lightCoords
    ) {
        PoseStack.Pose pose = poseStack.last();
        quad(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, color, lightCoords);
        quad(pose, consumer, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, color, lightCoords);
        quad(pose, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, color, lightCoords);
        quad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, color, lightCoords);
        quad(pose, consumer, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, color, lightCoords);
        quad(pose, consumer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, color, lightCoords);
    }

    private static void quad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float normalX,
        float normalY,
        float normalZ,
        int color,
        int lightCoords
    ) {
        vertex(pose, consumer, x1, y1, z1, 0.0F, 0.0F, normalX, normalY, normalZ, color, lightCoords);
        vertex(pose, consumer, x2, y2, z2, 1.0F, 0.0F, normalX, normalY, normalZ, color, lightCoords);
        vertex(pose, consumer, x3, y3, z3, 1.0F, 1.0F, normalX, normalY, normalZ, color, lightCoords);
        vertex(pose, consumer, x4, y4, z4, 0.0F, 1.0F, normalX, normalY, normalZ, color, lightCoords);
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float x,
        float y,
        float z,
        float u,
        float v,
        float normalX,
        float normalY,
        float normalZ,
        int color,
        int lightCoords
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(lightCoords)
            .setNormal(pose, normalX, normalY, normalZ);
    }
}
