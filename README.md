# Vernal Echo（春日影）

> A system of lingering player echoes, formed from actions and memory, waiting to be awakened.

Vernal Echo is a NeoForge `26.1.2` mod project built from the PickAID template line. The current repository contains the buildable project shell plus the first implementation design for the Spring Remnant System.

## Vision

Vernal Echo makes the world remember player activity without filling it with always-ticking entities. Movement, combat, deaths, interactions, and multiplayer presence leave low-cost records in nearby chunks. Those records slowly mature into echoes that can be collected or awakened later.

The core loop is:

```text
Player Activity -> Weight Accumulation -> Echo Generation -> Formation -> Collection -> Summoning
```

## Current Build Target

- Minecraft / NeoForge template line: `26.1.2`
- Loader: NeoForge
- Java toolchain: JDK `25`
- Primary config: `project.toml`
- Mod id: `vernalecho`
- Package root: `cn.mihono.vernalecho`

## Design Status

This README is an implementation design draft, not a claim that the gameplay systems already exist. The repository currently starts with a buildable NeoForge template, an entry class, metadata templates, and a placeholder mixin. The next work should replace the placeholder with the data, event, render, and summon systems described below.

## System Architecture

### 1. Echo Data Layer

Echoes begin as data, not entities.

Recommended storage split:

- `EchoWorldSavedData`: global per-dimension index, cooldowns, and coarse density statistics.
- Chunk-local echo records: dormant/forming records grouped by `ChunkPos`.
- `EchoRecord`: immutable-ish record payload for one latent echo.

MCP-backed note: NeoForge `SavedData` is intended for level data and must call `setDirty()` after mutation so it is saved. NeoForge docs also recommend data attachments when the data is specific to chunks, block entities, or entities. For this system, use `SavedData` for the global index and keep chunk-specific records isolated behind a chunk key so they can later move to chunk attachments if 26.1.2 APIs make that cleaner.

Example dormant record shape:

```json
{
    "id": "uuid",
    "playerUUID": "uuid",
    "chunk": [0, 0],
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

### 2. Activity Capture

Activity should be event-driven. Avoid a global player tick accumulator.

Initial hook points:

- Player interaction: collect low-weight activity around block/item/entity use.
- Living death: add high-weight residue near player deaths or combat.
- Chunk load/save: load and flush chunk-scoped echo records.
- Player login/logout: optional multiplayer presence weighting.

MCP-backed note: `PlayerInteractEvent` subevents and `LivingDeathEvent` are fired on the NeoForge event bus in the indexed `1.21.1/neoforge` sources. `ChunkDataEvent.Load` is documented as async during chunk loading, so write the design so chunk data load only deserializes data and does not touch live world state.

### 3. Weight Accumulation

Each activity writes weighted residue into the current chunk and nearby chunks.

Suggested first weights:

| Activity                    |                Weight |
| --------------------------- | --------------------: |
| Long movement through chunk |                 `0.2` |
| Block interaction           |                 `0.5` |
| Entity interaction          |                 `0.8` |
| Combat damage nearby        |                 `1.0` |
| Player death                |                 `4.0` |
| Multiplayer overlap         | `+25%` local modifier |

Accumulation is spatially local and should not scan the whole world. Use chunk keys and bounded neighborhood queries.

### 4. Threshold And Dice Roll

When a chunk record reaches threshold, it does not spawn immediately. It rolls.

```text
threshold = 10
chance = baseChance * densityFactor * cooldownFactor
baseChance = 0.15
densityFactor = 1 / (1 + nearbyEchoCount)
cooldownFactor = clamp(timeSinceLastTrigger / 60s, 0.5, 1.5)
```

On success:

- Create a dormant `EchoRecord`.
- Play subtle local audio feedback within 8 blocks.
- Reduce residue instead of clearing all of it:

```text
accumulatedWeight *= 0.3
```

### 5. Formation

Formation should use lazy evaluation.

Do not tick every dormant echo. Compute progress only when a player enters range, opens an echo UI, or the chunk is otherwise queried.

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

If a later version needs a visible interaction target, add a very short-lived marker entity only while players are nearby.

### 7. Collection

Collection turns a mature record into an inventory/resource state instead of instantly summoning it.

Collection should:

- validate progress server-side
- remove or mark the world record
- grant an echo token / capability / attachment to the player
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

- rolls equipment from biome / local context
- may be neutral or hostile

Hybrid Echo:

- mixes stored and random traits
- can become unstable in later versions

Summoned echoes should live for `30-60` seconds and never drop copied equipment.

### 9. Performance Rules

- No global ticking echo manager.
- No permanent hidden entities for dormant records.
- All scans are chunk-local or small-radius.
- Chunk load only deserializes data.
- Formation progress is calculated lazily.
- Density suppression prevents echo spam.

## First Implementation Milestones

1. Replace placeholder mixin with a clean event registration class.
2. Add `EchoRecord`, `EchoAffinity`, and `GearSnapshot` model types.
3. Add `EchoWorldSavedData` with NBT save/load and `setDirty()` on mutation.
4. Capture player interactions and deaths into chunk-local weight.
5. Add threshold roll and dormant record generation.
6. Add debug command or log output for nearby records.
7. Add client forming particles.
8. Add collection and summon prototypes.

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
