package org.pickaid.vernalecho.echo.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.pickaid.vernalecho.VernalEcho;
import org.pickaid.vernalecho.echo.client.network.EchoCaptureFinishedClientHandler;

@EventBusSubscriber(modid = VernalEcho.MOD_ID)
public final class EchoNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            EchoCaptureFinishedPayload.TYPE,
            EchoCaptureFinishedPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> EchoCaptureFinishedClientHandler.handle(payload)
            )
        );
    }
}
