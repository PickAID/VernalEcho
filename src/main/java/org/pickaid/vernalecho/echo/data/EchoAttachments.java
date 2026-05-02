package org.pickaid.vernalecho.echo.data;

import org.pickaid.vernalecho.VernalEcho;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EchoAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, VernalEcho.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EchoChunkData>> ECHO_CHUNK =
        ATTACHMENTS.register("echo_chunk", () -> AttachmentType
            .builder(EchoChunkData::empty)
            .serialize(EchoChunkData.CODEC, data -> !data.isEmpty())
            .sync(EchoChunkData.STREAM_CODEC)
            .build());

    private EchoAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
