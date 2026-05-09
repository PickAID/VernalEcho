package org.pickaid.vernalecho.echo.client.sound;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.pickaid.vernalecho.echo.client.fx.EchoCaptureBeamRenderer;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;
import org.pickaid.vernalecho.echo.sound.EchoSoundEvents;

public final class SoundInstance extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 10;
    private static final int FADE_OUT_TICKS = 18;
    private static final float TARGET_VOLUME = 0.42F;

    private final UUID playerId;
    private final InteractionHand hand;
    private int activeTicks;
    private int fadeOutTicks;
    private float fadeStartVolume;
    private boolean fadingOut;

    SoundInstance(UUID playerId, InteractionHand hand, Player player) {
        super(
            EchoSoundEvents.ECHO_BELL_CAPTURE_WHISPER.get(),
            SoundSource.PLAYERS,
            net.minecraft.client.resources.sounds.SoundInstance.createUnseededRandom()
        );
        this.playerId = playerId;
        this.hand = hand;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 0.96F;
        this.updatePosition(player);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Player player = findPlayer();
        if (player == null || !hasCaptureTarget(player)) {
            beginFadeOut();
        } else {
            updatePosition(player);
        }

        if (this.fadingOut) {
            this.fadeOutTicks++;
            float fade = 1.0F - Mth.clamp(this.fadeOutTicks / (float) FADE_OUT_TICKS, 0.0F, 1.0F);
            this.volume = this.fadeStartVolume * fade;
            if (this.fadeOutTicks >= FADE_OUT_TICKS) {
                this.stop();
            }
            return;
        }

        this.activeTicks++;
        float fadeIn = Mth.clamp(this.activeTicks / (float) FADE_IN_TICKS, 0.0F, 1.0F);
        this.volume = TARGET_VOLUME * fadeIn;
        this.pitch = 0.95F + 0.03F * Mth.sin(this.activeTicks * 0.08F);
    }

    void keepActive() {
        if (this.fadingOut) {
            this.fadingOut = false;
            this.fadeOutTicks = 0;
        }
    }

    void beginFadeOut() {
        if (!this.fadingOut && !this.isStopped()) {
            this.fadingOut = true;
            this.fadeOutTicks = 0;
            this.fadeStartVolume = this.volume;
        }
    }

    private Player findPlayer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? null : mc.level.getPlayerByUUID(this.playerId);
    }

    private boolean hasCaptureTarget(Player player) {
        ItemStack stack = player.getItemInHand(this.hand);
        return !stack.isEmpty() && stack.has(EchoDataComponents.BELL_TARGET);
    }

    private void updatePosition(Player player) {
        Vec3 anchor = EchoCaptureBeamRenderer.bellAnchor(player, this.hand, 1.0F, false);
        this.x = anchor.x;
        this.y = anchor.y;
        this.z = anchor.z;
    }
}
