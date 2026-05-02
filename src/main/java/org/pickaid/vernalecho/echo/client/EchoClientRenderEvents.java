package org.pickaid.vernalecho.echo.client;

import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.data.EchoChunkData;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderContext;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderers;
import org.pickaid.vernalecho.echo.client.render.EchoPropSilhouetteRenderer;
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
        int rendered = 0;

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
                        renderEcho(record, poseStack, bufferSource, cameraPos, level.getGameTime());
                        rendered++;
                    }
                    if (rendered >= 32) {
                        break;
                    }
                }
            }
        }

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
        float progress = record.formationProgress(gameTime);
        if (progress <= 0.02F) {
            return;
        }

        float pulse = 0.82F + 0.18F * Mth.sin((gameTime + record.id().getLeastSignificantBits() % 97L) * 0.08F);
        float alpha = (0.16F + 0.22F * progress) * pulse;
        int color = ARGB.colorFromFloat(alpha, 0.24F, 0.78F, 1.0F);
        Vec3 pos = record.pos();

        poseStack.pushPose();
        poseStack.translate(pos.x() - cameraPos.x(), pos.y() - cameraPos.y(), pos.z() - cameraPos.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - record.yaw()));
        if (record.pose() == EchoPose.FALLEN) {
            poseStack.translate(0.0F, 0.18F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(82.0F));
        }
        float scale = 0.94F + 0.025F * Mth.sin((gameTime + record.id().getMostSignificantBits() % 131L) * 0.06F);
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        resetModel();
        EchoPoseRenderContext context = EchoPoseRenderers.apply(record.pose(), new EchoPoseRenderContext(record, MODEL, gameTime));

        VertexConsumer consumer = bufferSource.getBuffer(ECHO_RENDER_TYPE);
        MODEL.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        EchoPropSilhouetteRenderer.render(context.props(), MODEL, poseStack, consumer, color, FULL_BRIGHT);
        poseStack.popPose();
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
}
