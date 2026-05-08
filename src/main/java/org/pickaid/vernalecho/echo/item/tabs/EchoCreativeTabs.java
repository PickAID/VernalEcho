package org.pickaid.vernalecho.echo.item.tabs;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.pickaid.vernalecho.echo.item.EchoItems;

public final class EchoCreativeTabs {
    public static void register(IEventBus modBus) {
        modBus.addListener(EchoCreativeTabs::onBuildContents);
    }

    private static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(EchoItems.ECHO_BELL.get());
        }
    }
}
