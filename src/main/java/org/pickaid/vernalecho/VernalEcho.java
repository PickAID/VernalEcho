package org.pickaid.vernalecho;

import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.item.tabs.EchoCreativeTabs;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;
import org.pickaid.vernalecho.echo.item.EchoItems;
import org.pickaid.vernalecho.echo.server.EchoActivityHandler;
import org.pickaid.vernalecho.echo.sound.EchoSoundEvents;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.pickaid.vernalecho.data.VernalEchoDataGenerators;
import org.pickaid.vernalecho.echo.worldgen.EchoFeatures;

@Mod(VernalEcho.MOD_ID)
public final class VernalEcho {
    public static final String MOD_ID = "vernalecho";

    public VernalEcho(IEventBus modBus) {
        EchoAttachments.register(modBus);
        EchoDataComponents.register(modBus);
        EchoSoundEvents.register(modBus);
        EchoItems.register(modBus);
        EchoCreativeTabs.register(modBus);
        EchoFeatures.register(modBus);
        modBus.addListener(VernalEchoDataGenerators::gatherServerData);
        EchoActivityHandler.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
