package org.pickaid.vernalecho.echo.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.pickaid.vernalecho.VernalEcho;

public final class EchoItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VernalEcho.MOD_ID);

    public static final DeferredItem<EchoBellItem> ECHO_BELL =
        ITEMS.registerItem("echo_bell", properties -> new EchoBellItem(properties
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)));

    private EchoItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
