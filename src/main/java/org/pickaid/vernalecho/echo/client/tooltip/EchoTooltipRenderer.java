package org.pickaid.vernalecho.echo.client.tooltip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderContext;
import org.pickaid.vernalecho.echo.client.render.EchoPoseRenderers;
import org.pickaid.vernalecho.echo.client.render.EchoPropSilhouetteRenderer;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.EchoRecord;

public class EchoTooltipRenderer extends PictureInPictureRenderer<EchoTooltipRenderState> {
    private static final Identifier SKINLESS_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final RenderType ECHO_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(SKINLESS_TEXTURE, false);
    private static final int FULL_BRIGHT = 15728880;
    private final PlayerModel model;

    public EchoTooltipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
        this.model = new PlayerModel(
            LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(),
            false
        );
    }

    @Override
    public @NonNull Class<EchoTooltipRenderState> getRenderStateClass() {
        return EchoTooltipRenderState.class;
    }

    @Override
    protected @NonNull String getTextureLabel() {
        return "im a tooltipppppp";
    }

    @Override
    protected void renderToTexture(EchoTooltipRenderState state, PoseStack poseStack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.PLAYER_SKIN);

        EchoRecord record = state.echo();
        long gameTime = state.gameTime();
        boolean fallen = record.pose() == EchoPose.FALLEN;
        long offset = Math.floorMod(record.id().getLeastSignificantBits(), 12000L);
        float rotationY = fallen
            ? 210.0F
            : ((Util.getMillis() + offset) % 12000L) * (360.0F / 12000.0F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(12.0F));
        poseStack.translate(0.0F, -0.75F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        if (fallen) {
            poseStack.translate(0.0F, 0.18F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(82.0F));
        }
        poseStack.translate(0.0F, -0.751F, 0.0F);

        resetModel();
        EchoPoseRenderContext context = EchoPoseRenderers.apply(
            record.pose(),
            new EchoPoseRenderContext(record, this.model, gameTime)
        );

        float pulse = 0.84F + 0.16F * Mth.sin((gameTime + record.id().getLeastSignificantBits() % 97L) * 0.08F);
        float alpha = 0.78F * pulse;
        int color = ARGB.colorFromFloat(alpha, 0.28F, 0.78F, 1.0F);

        VertexConsumer consumer = this.bufferSource.getBuffer(ECHO_RENDER_TYPE);
        this.model.renderToBuffer(poseStack, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        EchoPropSilhouetteRenderer.render(context.props(), this.model, poseStack, consumer, color, FULL_BRIGHT);
        this.bufferSource.endBatch();
        poseStack.popPose();
    }

    private void resetModel() {
        this.model.resetPose();
        this.model.hat.visible = false;
        this.model.jacket.visible = false;
        this.model.leftSleeve.visible = false;
        this.model.rightSleeve.visible = false;
        this.model.leftPants.visible = false;
        this.model.rightPants.visible = false;
    }
}
