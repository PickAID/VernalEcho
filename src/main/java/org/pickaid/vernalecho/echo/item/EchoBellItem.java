package org.pickaid.vernalecho.echo.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.item.datacomponents.EchoDataComponents;
import org.pickaid.vernalecho.echo.server.EchoCaptureService;

public class EchoBellItem extends Item {
    public static final int USE_DURATION_TICKS = 8 * 20;
    public static final double SCAN_DISTANCE = 8.0D;
    public static final int RELEASE_COOLDOWN_TICKS = 3 * 20;

    public EchoBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public int getUseDuration(@NonNull ItemStack stack, @NonNull LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public @NonNull InteractionResult use(@NonNull Level level, Player player, @NonNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.has(EchoDataComponents.CAPTURED_ECHO)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            EchoRecord target = EchoCaptureService.findInSight(serverPlayer, SCAN_DISTANCE);
            if (target == null) {
                return InteractionResult.PASS;
            }
            stack.set(EchoDataComponents.BELL_TARGET, new BellTarget(target.id(), target.pos()));
            serverPlayer.level().playSound(
                null,
                serverPlayer.blockPosition(),
                SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS,
                0.4F,
                1.2F
            );
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public boolean releaseUsing(@NonNull ItemStack stack, Level level, @NonNull LivingEntity entity, int remainingTime) {
        if (!level.isClientSide()) {
            stack.remove(EchoDataComponents.BELL_TARGET);
        }
        return true;
    }

    @Override
    public @NonNull ItemStack finishUsingItem(@NonNull ItemStack stack, @NonNull Level level, @NonNull LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return stack;
        }
        BellTarget target = stack.get(EchoDataComponents.BELL_TARGET);
        stack.remove(EchoDataComponents.BELL_TARGET);
        if (target == null) {
            return stack;
        }
        EchoRecord captured = EchoCaptureService.consumeRecord(serverLevel, target.pos(), target.recordId());
        if (captured == null) {
            return stack;
        }
        stack.set(EchoDataComponents.CAPTURED_ECHO, captured);
        serverLevel.sendParticles(
            ParticleTypes.SOUL,
            target.pos().x, target.pos().y + 0.5D, target.pos().z,
            16, 0.3D, 0.3D, 0.3D, 0.05D
        );
        if (entity instanceof Player player) {
            serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.7F,
                1.4F
            );
            player.getCooldowns().addCooldown(stack, RELEASE_COOLDOWN_TICKS);
        }
        return stack;
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player contextPlayer = context.getPlayer();
        if (contextPlayer != null && contextPlayer.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        EchoRecord captured = stack.get(EchoDataComponents.CAPTURED_ECHO);
        if (captured == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos placePos = context.getClickedPos().above();
        Vec3 newPos = Vec3.atBottomCenterOf(placePos);
        float newYaw = context.getRotation();
        EchoRecord released = new EchoRecord(
            captured.id(),
            captured.playerUUID(),
            newPos,
            serverLevel.getGameTime(),
            captured.weight(),
            captured.gearSnapshot(),
            captured.affinity(),
            captured.boosted(),
            captured.origin(),
            captured.pose(),
            newYaw
        );
        EchoCaptureService.releaseRecord(serverLevel, released);
        stack.remove(EchoDataComponents.CAPTURED_ECHO);
        if (contextPlayer != null) {
            contextPlayer.getCooldowns().addCooldown(stack, RELEASE_COOLDOWN_TICKS);
        }
        serverLevel.sendParticles(
            ParticleTypes.SOUL,
            newPos.x, newPos.y + 0.5D, newPos.z,
            16, 0.3D, 0.3D, 0.3D, 0.05D
        );
        serverLevel.playSound(
            null,
            placePos,
            SoundEvents.AMETHYST_BLOCK_CHIME,
            SoundSource.PLAYERS,
            0.7F,
            0.8F
        );
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NonNull Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        EchoRecord captured = stack.get(EchoDataComponents.CAPTURED_ECHO);
        if (captured == null) {
            return Optional.empty();
        }
        return Optional.of(new EchoTooltipImage(captured));
    }
}
