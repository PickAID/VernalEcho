package org.pickaid.vernalecho.echo.client.fx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.pickaid.vernalecho.VernalEcho;

@EventBusSubscriber(modid = VernalEcho.MOD_ID, value = Dist.CLIENT)
public final class EchoBeamPipeline {
    public static final Identifier PIPELINE_ID = VernalEcho.id("pipeline/echo_beam");
    public static final Identifier SHADER_ID = VernalEcho.id("core/echo_beam");

    public static final RenderPipeline PIPELINE = RenderPipeline.builder(
            RenderPipelines.MATRICES_FOG_SNIPPET,
            RenderPipelines.GLOBALS_SNIPPET
        )
        .withLocation(PIPELINE_ID)
        .withVertexShader(SHADER_ID)
        .withFragmentShader(SHADER_ID)
        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withCull(false)
        .build();

    public static final RenderType RENDER_TYPE = RenderType.create(
        PIPELINE_ID.toString(),
        RenderSetup.builder(PIPELINE).createRenderSetup()
    );

    private EchoBeamPipeline() {
    }

    @SubscribeEvent
    public static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }
}
