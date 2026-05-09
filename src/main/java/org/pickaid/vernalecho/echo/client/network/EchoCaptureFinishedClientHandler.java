package org.pickaid.vernalecho.echo.client.network;

import org.pickaid.vernalecho.echo.client.EchoClientRenderEvents;
import org.pickaid.vernalecho.echo.network.s2c.EchoCaptureFinishedPayload;

public final class EchoCaptureFinishedClientHandler {

    public static void handle(EchoCaptureFinishedPayload payload) {
        EchoClientRenderEvents.onCaptureFinished(payload);
    }
}
