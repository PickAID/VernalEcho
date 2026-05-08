package org.pickaid.vernalecho.echo.item.datacomponents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.item.BellTarget;

public final class EchoDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VernalEcho.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EchoRecord>> CAPTURED_ECHO =
        COMPONENTS.registerComponentType("captured_echo", builder -> builder
            .persistent(EchoRecord.CODEC)
            .networkSynchronized(EchoRecord.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BellTarget>> BELL_TARGET =
        COMPONENTS.registerComponentType("bell_target", builder -> builder
            .persistent(BellTarget.CODEC)
            .networkSynchronized(BellTarget.STREAM_CODEC));

    private EchoDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
