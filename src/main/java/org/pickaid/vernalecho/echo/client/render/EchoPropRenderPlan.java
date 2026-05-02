package org.pickaid.vernalecho.echo.client.render;

import net.minecraft.world.item.ItemStack;

public record EchoPropRenderPlan(ItemStack stack, EchoPropStyle style, EchoPropAnchor anchor) {
}
