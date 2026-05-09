package org.pickaid.vernalecho.echo.client.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.pickaid.vernalecho.echo.client.EchoClientRenderEvents;
import org.pickaid.vernalecho.echo.network.EchoCaptureFinishedPayload;

public final class EchoCaptureFinishedClientHandler {

    public static void handle(EchoCaptureFinishedPayload payload) {
        EchoClientRenderEvents.onCaptureFinished(payload);
    }
}
