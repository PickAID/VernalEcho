package org.pickaid.vernalecho.echo.client.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;
import org.pickaid.vernalecho.echo.data.EchoRecord;

public record EchoTooltipRenderState(
    EchoRecord echo,
    long gameTime,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public EchoTooltipRenderState(
        EchoRecord echo,
        long gameTime,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(echo, gameTime, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
