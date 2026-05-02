package org.pickaid.vernalecho.echo.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.pickaid.vernalecho.echo.data.EchoAffinity;
import org.pickaid.vernalecho.echo.data.EchoAttachments;
import org.pickaid.vernalecho.echo.data.EchoChunkData;
import org.pickaid.vernalecho.echo.data.EchoOrigin;
import org.pickaid.vernalecho.echo.data.EchoPose;
import org.pickaid.vernalecho.echo.data.EchoRecord;
import org.pickaid.vernalecho.echo.data.GearSnapshot;
import org.pickaid.vernalecho.echo.worldgen.config.WildEchoFeatureConfiguration;

public final class EchoNaturalSpawner {
    private static final UUID WILD_PLAYER_UUID = new UUID(0L, 0L);
    private static final int ENTER_CHUNK_SCAN_RADIUS = 2;
    private static final int BACKFILL_CHANCE = 192;
    private static final WildEchoFeatureConfiguration BACKFILL_CONFIG = new WildEchoFeatureConfiguration(3, 5, 0.4D);
    private static final Item[] WILD_HAND_ITEMS = new Item[]{
        Items.AMETHYST_SHARD,
        Items.BONE,
        Items.CLOCK,
        Items.COMPASS,
        Items.EMERALD,
        Items.FLOWER_POT,
        Items.NAME_TAG,
        Items.PAPER,
        Items.STICK
    };

    private EchoNaturalSpawner() {
    }

    public static void handleEnteringSection(EntityEvent.EnteringSection event) {
        if (!event.didChunkChange() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int centerChunkX = SectionPos.x(event.getPackedNewPos());
        int centerChunkZ = SectionPos.z(event.getPackedNewPos());
        for (int radius = 0; radius <= ENTER_CHUNK_SCAN_RADIUS; radius++) {
            scanRing(level, player, centerChunkX, centerChunkZ, radius);
        }
    }

    public static boolean tryPlaceFromFeature(WorldGenLevel level, BlockPos origin, RandomSource random, WildEchoFeatureConfiguration config) {
        WildPlacement placement = pickFeaturePlacement(level, origin, random, config);
        if (placement == null) {
            return false;
        }
        ChunkAccess chunk = level.getChunk(
            SectionPos.blockToSectionCoord(placement.pos().getX()),
            SectionPos.blockToSectionCoord(placement.pos().getZ()),
            ChunkStatus.EMPTY,
            false
        );
        if (chunk == null) {
            return false;
        }
        return tryAddWildEcho(level.getLevel(), chunk, placement, random, true);
    }

    private static void scanRing(ServerLevel level, Player player, int centerChunkX, int centerChunkZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                    continue;
                }

                int chunkX = centerChunkX + dx;
                int chunkZ = centerChunkZ + dz;
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk levelChunk)) {
                    continue;
                }

                RandomSource random = RandomSource.create(seedForChunk(level, chunkX, chunkZ));
                if (random.nextInt(BACKFILL_CHANCE) != 0) {
                    continue;
                }

                WildPlacement placement = pickBackfillPlacement(level, levelChunk, random);
                BlockPos pos = placement == null ? null : placement.pos();
                if (pos != null && player.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) <= 80.0D * 80.0D) {
                    tryAddWildEcho(level, levelChunk, placement, random, false);
                }
            }
        }
    }

    private static WildPlacement pickFeaturePlacement(
        WorldGenLevel level,
        BlockPos origin,
        RandomSource random,
        WildEchoFeatureConfiguration config
    ) {
        if (config.chestSearchRadius() > 0 && random.nextDouble() < config.chestLinkedChance()) {
            WildPlacement chestPlacement = pickChestLinkedPlacement(level, origin, config.chestSearchRadius(), random);
            if (chestPlacement != null) {
                return chestPlacement;
            }
        }

        for (int attempt = 0; attempt < config.surfaceSearchAttempts(); attempt++) {
            int x = attempt == 0 ? origin.getX() : origin.getX() + random.nextInt(9) - 4;
            int z = attempt == 0 ? origin.getZ() : origin.getZ() + random.nextInt(9) - 4;
            int y = attempt == 0 ? origin.getY() : level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (canPlaceAt(level, pos)) {
                return randomSurfacePlacement(pos, random);
            }
        }
        return null;
    }

    private static WildPlacement pickBackfillPlacement(ServerLevel level, ChunkAccess chunk, RandomSource random) {
        BlockPos origin = chunk.getPos().getMiddleBlockPosition(level.getSeaLevel());
        if (BACKFILL_CONFIG.chestSearchRadius() > 0 && random.nextDouble() < BACKFILL_CONFIG.chestLinkedChance()) {
            WildPlacement chestPlacement = pickChestLinkedPlacement(level, origin, BACKFILL_CONFIG.chestSearchRadius(), random);
            if (chestPlacement != null) {
                return chestPlacement;
            }
        }

        int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
        int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
        BlockPos pos = new BlockPos(x, y, z);
        return canPlaceAt(level, pos) ? randomSurfacePlacement(pos, random) : null;
    }

    private static boolean tryAddWildEcho(ServerLevel level, ChunkAccess chunk, WildPlacement placement, RandomSource random, boolean fromFeature) {
        EchoChunkData current = chunk.getData(EchoAttachments.ECHO_CHUNK);
        if (current.hasWildEcho()) {
            return false;
        }
        BlockPos pos = placement.pos();

        EchoRecord record = new EchoRecord(
            wildId(level, chunk, pos),
            WILD_PLAYER_UUID,
            new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D),
            level.getGameTime(),
            fromFeature ? 10.0D : 6.0D,
            placement.gearSnapshot(),
            EchoAffinity.WILD_WEIGHTED,
            false,
            EchoOrigin.WILD,
            placement.pose(),
            placement.yaw()
        );
        EchoChunkData updated = current.addWildEcho(record);
        if (updated == current) {
            return false;
        }

        chunk.setData(EchoAttachments.ECHO_CHUNK, updated);
        return true;
    }

    private static WildPlacement pickChestLinkedPlacement(WorldGenLevel level, BlockPos origin, int radius, RandomSource random) {
        WildPlacement selected = null;
        int seen = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int y = origin.getY() - 4; y <= origin.getY() + 3; y++) {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!isTreasureContainer(state)) {
                        continue;
                    }

                    WildPlacement candidate = placementInFrontOfContainer(level, cursor.immutable(), state, origin, random);
                    if (candidate != null && random.nextInt(++seen) == 0) {
                        selected = candidate;
                    }
                }
            }
        }
        return selected;
    }

    private static WildPlacement placementInFrontOfContainer(
        WorldGenLevel level,
        BlockPos containerPos,
        BlockState state,
        BlockPos origin,
        RandomSource random
    ) {
        Direction primary = state.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(directionFrom(containerPos, origin));
        if (primary == Direction.UP || primary == Direction.DOWN) {
            primary = directionFrom(containerPos, origin);
        }

        Direction[] candidates = new Direction[]{
            primary,
            primary.getClockWise(),
            primary.getCounterClockWise(),
            primary.getOpposite()
        };
        for (Direction direction : candidates) {
            BlockPos pos = containerPos.relative(direction);
            if (canPlaceAt(level, pos)) {
                return new WildPlacement(
                    pos,
                    EchoPose.CROUCH_REACHING,
                    Direction.getYRot(direction.getOpposite()),
                    gear(randomTreasureItem(random), ItemStack.EMPTY)
                );
            }
        }
        return null;
    }

    private static Direction directionFrom(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dz != 0) {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return Direction.NORTH;
    }

    private static WildPlacement randomSurfacePlacement(BlockPos pos, RandomSource random) {
        EchoPose pose = randomWildPose(random);
        return new WildPlacement(pos, pose, random.nextFloat() * 360.0F, randomWildGear(pose, random));
    }

    private static boolean canPlaceAt(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
            && level.getFluidState(pos).isEmpty()
            && !level.getBlockState(pos.below()).isAir()
            && level.getFluidState(pos.below()).isEmpty();
    }

    private static boolean isTreasureContainer(BlockState state) {
        return state.is(Tags.Blocks.CHESTS) || state.is(Tags.Blocks.BARRELS);
    }

    private static long seedForChunk(ServerLevel level, int chunkX, int chunkZ) {
        long seed = level.getSeed();
        seed ^= (long)chunkX * 341873128712L;
        seed ^= (long)chunkZ * 132897987541L;
        seed ^= level.dimension().identifier().hashCode() * 31L;
        seed ^= 0x51EED5EEDL;
        return seed;
    }

    private static UUID wildId(ServerLevel level, ChunkAccess chunk, BlockPos pos) {
        String raw = level.dimension().identifier() + ":" + level.getSeed() + ":" + chunk.getPos() + ":" + pos.asLong();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static EchoPose randomWildPose(RandomSource random) {
        return switch (random.nextInt(13)) {
            case 0, 1, 2 -> EchoPose.IDLE;
            case 3, 4 -> EchoPose.WALKING;
            case 5, 6 -> EchoPose.CROUCHING;
            case 7, 8 -> EchoPose.REACHING;
            case 9 -> EchoPose.GUARDING;
            case 10 -> EchoPose.CROUCH_REACHING;
            case 11 -> EchoPose.READING;
            default -> EchoPose.FALLEN;
        };
    }

    private static GearSnapshot randomWildGear(EchoPose pose, RandomSource random) {
        return switch (pose) {
            case GUARDING -> gear(ItemStack.EMPTY, new ItemStack(Items.SHIELD));
            case READING -> gear(new ItemStack(Items.BOOK), ItemStack.EMPTY);
            case REACHING, CROUCH_REACHING -> gear(randomTreasureItem(random), ItemStack.EMPTY);
            case IDLE, WALKING, CROUCHING -> random.nextInt(3) == 0 ? gear(randomTreasureItem(random), ItemStack.EMPTY) : GearSnapshot.EMPTY;
            case FALLEN -> random.nextInt(4) == 0 ? gear(randomTreasureItem(random), ItemStack.EMPTY) : GearSnapshot.EMPTY;
        };
    }

    private static ItemStack randomTreasureItem(RandomSource random) {
        return new ItemStack(WILD_HAND_ITEMS[random.nextInt(WILD_HAND_ITEMS.length)]);
    }

    private static GearSnapshot gear(ItemStack mainHand, ItemStack offHand) {
        return new GearSnapshot(
            mainHand,
            offHand,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY,
            ItemStack.EMPTY
        );
    }

    private record WildPlacement(BlockPos pos, EchoPose pose, float yaw, GearSnapshot gearSnapshot) {
    }
}
