package org.pickaid.vernalecho.echo.client;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.client.tooltip.EchoBellTooltipComponent;
import org.pickaid.vernalecho.echo.client.tooltip.EchoTooltipRenderState;
import org.pickaid.vernalecho.echo.client.tooltip.EchoTooltipRenderer;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.item.EchoBellItem;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;
import org.pickaid.vernalecho.echo.item.EchoTooltipImage;

@EventBusSubscriber(modid = VernalEcho.MOD_ID, value = Dist.CLIENT)
public final class EchoClientHooks {
    private EchoClientHooks() {
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(EchoTooltipImage.class, EchoBellTooltipComponent::new);
    }

    @SubscribeEvent
    public static void onRegisterPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(EchoTooltipRenderState.class, EchoTooltipRenderer::new);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EchoBellItem)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        EchoRecord captured = stack.get(EchoDataComponents.CAPTURED_ECHO);
        if (captured == null) {
            tooltip.add(Component.translatable("item.vernalecho.echo_bell.tooltip").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.vernalecho.echo_bell.sealed").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
            "item.vernalecho.echo_bell.origin." + captured.origin().getSerializedName()
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
            "item.vernalecho.echo_bell.pose." + captured.pose().getSerializedName()
        ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
