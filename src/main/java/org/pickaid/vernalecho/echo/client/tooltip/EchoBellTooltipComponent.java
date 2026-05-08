package org.pickaid.vernalecho.echo.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.jspecify.annotations.NonNull;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.item.EchoTooltipImage;

public class EchoBellTooltipComponent implements ClientTooltipComponent {
    private static final int WIDTH_STAND = 52;
    private static final int HEIGHT_STAND = 72;
    private static final float SCALE_STAND = 30.0F;
    private static final int WIDTH_FALLEN = 80;
    private static final int HEIGHT_FALLEN = 56;
    private static final float SCALE_FALLEN = 24.0F;

    private final EchoRecord echo;
    private final boolean fallen;

    public EchoBellTooltipComponent(EchoTooltipImage image) {
        this.echo = image.echo();
        this.fallen = this.echo.pose() == EchoPose.FALLEN;
    }

    @Override
    public int getHeight(@NonNull Font font) {
        return this.fallen ? HEIGHT_FALLEN : HEIGHT_STAND;
    }

    @Override
    public int getWidth(@NonNull Font font) {
        return this.fallen ? WIDTH_FALLEN : WIDTH_STAND;
    }

    @Override
    public void extractImage(@NonNull Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        long gameTime = Minecraft.getInstance().level != null
            ? Minecraft.getInstance().level.getGameTime()
            : 0L;
        int width = this.fallen ? WIDTH_FALLEN : WIDTH_STAND;
        int height = this.fallen ? HEIGHT_FALLEN : HEIGHT_STAND;
        float scale = this.fallen ? SCALE_FALLEN : SCALE_STAND;
        graphics.submitPictureInPictureRenderState(new EchoTooltipRenderState(
            this.echo,
            gameTime,
            x,
            y,
            x + width,
            y + height,
            scale,
            graphics.peekScissorStack()
        ));
    }
}
