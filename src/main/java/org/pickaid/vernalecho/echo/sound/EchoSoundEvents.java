package org.pickaid.vernalecho.echo.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.pickaid.vernalecho.VernalEcho;

public final class EchoSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, VernalEcho.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_BELL_CAPTURE_WHISPER =
        SOUND_EVENTS.register(
            "item.echo_bell.capture_whisper",
            () -> SoundEvent.createVariableRangeEvent(VernalEcho.id("item.echo_bell.capture_whisper"))
        );

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
