package org.pickaid.vernalecho.echo.client.sound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;

public final class SoundManager {
    private static final Map<CaptureSoundKey, SoundInstance> ACTIVE_SOUNDS = new HashMap<>();
    private static final Set<CaptureSoundKey> CURRENT_TARGETS = new HashSet<>();
    private static long lastUpdateGameTime = Long.MIN_VALUE;

    public static void update(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            stopAll();
            lastUpdateGameTime = Long.MIN_VALUE;
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (lastUpdateGameTime == gameTime) {
            return;
        }
        lastUpdateGameTime = gameTime;
        CURRENT_TARGETS.clear();

        for (Player player : mc.level.players()) {
            ensureSound(mc, player, InteractionHand.MAIN_HAND);
            ensureSound(mc, player, InteractionHand.OFF_HAND);
        }

        Iterator<Map.Entry<CaptureSoundKey, SoundInstance>> iterator = ACTIVE_SOUNDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CaptureSoundKey, SoundInstance> entry = iterator.next();
            SoundInstance sound = entry.getValue();
            if (!CURRENT_TARGETS.contains(entry.getKey())) {
                sound.beginFadeOut();
            }
            if (sound.isStopped()) {
                iterator.remove();
            }
        }
    }

    private static void ensureSound(Minecraft mc, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.has(EchoDataComponents.BELL_TARGET)) {
            return;
        }

        CaptureSoundKey key = new CaptureSoundKey(player.getUUID(), hand);
        CURRENT_TARGETS.add(key);
        SoundInstance sound = ACTIVE_SOUNDS.get(key);
        if (sound == null || sound.isStopped()) {
            sound = new SoundInstance(key.playerId(), key.hand(), player);
            ACTIVE_SOUNDS.put(key, sound);
            mc.getSoundManager().play(sound);
        } else {
            sound.keepActive();
        }
    }

    private static void stopAll() {
        for (SoundInstance sound : ACTIVE_SOUNDS.values()) {
            sound.beginFadeOut();
        }
        ACTIVE_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
        CURRENT_TARGETS.clear();
    }

    private record CaptureSoundKey(UUID playerId, InteractionHand hand) {
    }
}
