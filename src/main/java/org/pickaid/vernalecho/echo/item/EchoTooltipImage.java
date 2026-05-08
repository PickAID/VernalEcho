package org.pickaid.vernalecho.echo.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.pickaid.vernalecho.echo.data.EchoRecord;

public record EchoTooltipImage(EchoRecord echo) implements TooltipComponent {
}
