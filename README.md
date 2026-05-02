# Vernal Echo（春日影）

> A system of lingering player echoes, formed from actions and memory, waiting to be awakened.

Vernal Echo is a NeoForge `26.1.2` mod project built from the PickAID template line. The current repository contains the buildable project shell plus the first implementation design for the Spring Remnant System.

## Current Build Target

- Minecraft / NeoForge line: `26.1.2`
- Resolved NeoForge artifact: `net.neoforged:neoforge:26.1.2.30-beta`
- Loader: NeoForge
- Java toolchain: JDK `25`
- Verified Java bytecode: class major version `69`
- Primary config: `project.toml`
- Mod id: `vernalecho`
- Package root: `cn.mihono.vernalecho`

The Gradle daemon may still print a Java 17 runtime if Gradle itself was launched with Java 17. This project compiles through the configured Java 25 toolchain from `gradle/template-defaults.toml`.

## Source Basis

This design is based on the sources Gradle resolves for this repository, not on older MCP or 1.21.1 assumptions.

Checked local artifacts:

- `build/moddev/artifacts/minecraft-patched-26.1.2.30-beta-sources.jar`
- `neoforge-26.1.2.30-beta-sources.jar`

Important `26.1.2` API facts found in those sources:

- Minecraft resource ids use `net.minecraft.resources.Identifier`.
- `SavedData` no longer exposes the older NBT `save(CompoundTag, HolderLookup.Provider)` override.
- Persistent `SavedData` is loaded through `SavedDataType<T>` and a `Codec<T>`.
- `SavedDataStorage#computeIfAbsent` takes a `SavedDataType<T>`.
- Chunk-local custom data should use NeoForge data attachments.
- `ChunkDataEvent` uses `SerializableChunkData`.
- `ChunkDataEvent.Load` is fired on the main server thread, and it is safe to interact with attachments on the provided chunk.
- `ChunkDataEvent.Save` is also on the main server thread, but runs after chunk serialization; attachment changes made there are not included in that save.
- `AttachmentType<T>` must be registered to `NeoForgeRegistries.Keys#ATTACHMENT_TYPES`.
- After mutating an attachment on `ChunkAccess`, call `ChunkAccess#markUnsaved`.
- `IAttachmentSerializer<T>` uses `ValueInput` and `ValueOutput`; do not write new code against raw NBT serializer signatures.

## Vision

Vernal Echo makes the world remember player activity without filling it with always-ticking entities. Movement, combat, deaths, interactions, and multiplayer presence leave low-cost records in nearby chunks. Those records slowly mature into echoes that can be collected or awakened later.

The core loop is:

```text
Player Activity -> Weight Accumulation -> Echo Generation -> Formation -> Collection -> Summoning
```

## Design Status

This README is an implementation design draft, not a claim that the gameplay systems already exist. The repository currently starts with a buildable NeoForge template, an entry class, metadata templates, and a placeholder mixin. The next work should replace the placeholder with the data, event, render, and summon systems described below.

## System Architecture

### 1. Echo Data Layer

Echoes begin as data, not entities.

Recommended storage split for NeoForge `26.1.2`:

- Chunk attachment: primary storage for dormant/forming echo records in that chunk.
- `EchoWorldSavedData`: optional per-dimension indexes, cooldown summaries, and coarse density statistics.
- `EchoRecord`: one latent echo payload.
- `GearSnapshot`: minimal copied equipment state used later when a player echo is awakened.
- `EchoAffinity`: player/wild/random weighting used to decide the summoned echo type.

Chunk attachments should be the default for records because the data is chunk-local. `SavedData` should not become a giant global map of every echo unless a global index is genuinely needed.

Expected dormant record shape:

```json
{
    "id": "uuid",
    "playerUUID": "uuid",
    "pos": [0.0, 64.0, 0.0],
    "spawnTime": 0,
    "weight": 0.0,
    "gearSnapshot": {},
    "affinity": {
        "playerEcho": 0.7,
        "wildEcho": 0.2,
        "randomEcho": 0.1
    },
    "boosted": false
}
```

Attachment registration should follow this shape:

```java
private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
    DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, VernalEcho.MOD_ID);

public static final DeferredHolder<AttachmentType<?>, AttachmentType<EchoChunkData>> ECHO_CHUNK =
    ATTACHMENTS.register("echo_chunk", () -> AttachmentType
        .builder(EchoChunkData::empty)
        .serialize(EchoChunkData.CODEC)
        .build());
```

Chunk mutation should set the new attachment value and mark the chunk unsaved:

```java
EchoChunkData current = chunk.getData(EchoAttachments.ECHO_CHUNK);
chunk.setData(EchoAttachments.ECHO_CHUNK, current.addWeight(amount));
chunk.markUnsaved();
```

If a global index is added, use `SavedDataType` and a codec:

```java
public static final SavedDataType<EchoWorldData> TYPE = new SavedDataType<>(
    VernalEcho.id("echo_world"),
    EchoWorldData::new,
    EchoWorldData.CODEC
);

EchoWorldData data = level.getDataStorage().computeIfAbsent(EchoWorldData.TYPE);
data.setDirty();
```

### 2. Activity Capture

Activity should be event-driven. Avoid a global player tick accumulator.

Initial hook points verified in the `26.1.2.30-beta` sources:

- `PlayerInteractEvent.RightClickBlock`: low-weight residue around block interaction.
- `PlayerInteractEvent.EntityInteract`: low or medium residue around entity interaction.
- `LivingDeathEvent`: high-weight residue near player death or combat death.
- `ChunkDataEvent.Load`: safe place to inspect already-loaded chunk attachments if needed.
- `ChunkDataEvent.Save`: observation only; do not mutate data expecting it to persist in that same save.

All of these game events are fired on `NeoForge.EVENT_BUS`. Registration of attachment types belongs on the mod event bus.

### 3. Weight Accumulation

Each activity writes weighted residue into the current chunk and, when useful, a small bounded neighborhood.

Suggested first weights:

| Activity                    |                Weight |
| --------------------------- | --------------------: |
| Long movement through chunk |                 `0.2` |
| Block interaction           |                 `0.5` |
| Entity interaction          |                 `0.8` |
| Combat damage nearby        |                 `1.0` |
| Player death                |                 `4.0` |
| Multiplayer overlap         | `+25%` local modifier |

Accumulation is spatially local and should not scan the whole world. Use the current chunk, optional adjacent chunks, and bounded density checks.

### 4. Threshold And Dice Roll

When chunk-local residue reaches the threshold, it does not spawn immediately. It rolls.

```text
threshold = 10
chance = baseChance * densityFactor * cooldownFactor
baseChance = 0.15
densityFactor = 1 / (1 + nearbyEchoCount)
cooldownFactor = clamp(timeSinceLastTrigger / 60s, 0.5, 1.5)
```

On success:

- Create a dormant `EchoRecord` in the chunk attachment.
- Play subtle local audio feedback within 8 blocks.
- Reduce residue instead of clearing all of it:

```text
accumulatedWeight *= 0.3
```

### 5. Formation

Formation should use lazy evaluation.

Do not tick every dormant echo. Compute progress only when a player enters range, opens an echo UI, collects an echo, or the chunk data is otherwise queried.

```text
progress = clamp((currentGameTime - spawnTime) * baseSpeed * environmentFactor, 0, 1)
```

Formation phases:

- `Dormant`: only data exists.
- `Forming`: client-visible effect, no AI, no collision, no entity tick.
- `Collectable`: interaction target or server-side commandable record.
- `Awakened`: temporary combat/support entity.

### 6. Rendering

The forming phase should not be a permanent entity.

First implementation target:

- client-only particle and translucent silhouette effect near the record position
- render only when player is close enough
- no pathfinding, collision, or server tick

If a later version needs a visible interaction target, add a short-lived marker only while players are nearby.

### 7. Collection

Collection turns a mature record into an inventory/resource state instead of instantly summoning it.

Collection should:

- validate progress server-side
- remove or mark the world record in the chunk attachment
- call `markUnsaved()` after attachment mutation
- grant an echo token, capability-like attachment, or item component to the player
- preserve affinity and gear snapshot data

### 8. Summoning

Summoning chooses echo type from affinity.

```text
if playerEcho > 0.60:
    summon Player Echo
elif wildEcho > 0.60:
    summon Wild Echo
else:
    summon Hybrid Echo
```

Player Echo:

- uses stored gear snapshot
- selects the strongest usable weapon
- follows the summoner

Wild Echo:

- rolls equipment from biome or local context
- may be neutral or hostile

Hybrid Echo:

- mixes stored and random traits
- can become unstable in later versions

Summoned echoes should live for `30-60` seconds and never drop copied equipment.

### 9. Performance Rules

- No global ticking echo manager.
- No permanent hidden entities for dormant records.
- Chunk-local data lives on chunk attachments.
- All scans are chunk-local or small-radius.
- Formation progress is calculated lazily.
- Density suppression prevents echo spam.
- `SavedData` is reserved for coarse per-level coordination, not every chunk payload.

## First Implementation Milestones

1. Add an attachment registration class for `EchoChunkData`.
2. Add `EchoRecord`, `EchoAffinity`, `GearSnapshot`, and codec-backed chunk data model types.
3. Register attachment types on the mod event bus.
4. Register game event handlers on `NeoForge.EVENT_BUS`.
5. Capture player interactions and deaths into chunk-local weight.
6. Add threshold roll and dormant record generation.
7. Add optional `EchoWorldSavedData` with `SavedDataType` and `Codec` for cooldown/density summaries.
8. Add debug command or log output for nearby records.
9. Add client forming particles.
10. Add collection and summon prototypes.

## Development

Build:

```bash
./gradlew build
```

Generate IDEA run configs:

```bash
./gradlew genIntellijRuns
```

Run client:

```bash
./gradlew runClient
```

Local-only settings go in `project.local.toml`; do not commit tokens or machine-specific run settings.
