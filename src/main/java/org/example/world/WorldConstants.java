package org.example.world;

public final class WorldConstants {

    private WorldConstants() {}

    // Horizontal footprint of a chunk; the vertical extent is the full world height.
    // A chunk is therefore a column CHUNK_SIZE_XZ × WORLD_HEIGHT × CHUNK_SIZE_XZ.
    public static final int CHUNK_SIZE_XZ = 32;
    public static final int WORLD_HEIGHT  = 256;

    public static final int CHUNK_LOAD_RADIUS          = 12;
    public static final int CHUNK_UNLOAD_RADIUS        = 15;
    public static final int MAX_CHUNK_UPLOADS_PER_FRAME = 4;

    public static final byte BLOCK_AIR    = 0;
    public static final byte BLOCK_STONE  = 1;
    public static final byte BLOCK_DIRT   = 2;
    public static final byte BLOCK_GRASS  = 3;
    public static final byte BLOCK_WOOD   = 4;
    public static final byte BLOCK_LEAVES = 5;
    public static final byte BLOCK_WATER   = 6;
    public static final byte BLOCK_IRON    = 7;
    public static final byte BLOCK_DIAMOND = 8;
    public static final byte BLOCK_TORCH   = 9;
    public static final byte BLOCK_SAND    = 10;

    // Fluid simulation (STEP-32).
    // Level encoding: BLOCK_WATER (6) acts as source water (level FLUID_MAX_LEVEL = 8, infinite).
    // Flowing water levels 7 down to 1 use dedicated block ids 11..17 (BLOCK_WATER_FLOW_BASE + level).
    // Lava source uses BLOCK_LAVA (18); flowing lava levels 6 down to 1 use ids 19..24
    // (BLOCK_LAVA_FLOW_BASE + level). BLOCK_OBSIDIAN (25) is produced when water touches a lava source.
    // This single-byte-per-cell encoding keeps VoxelChunkData data-only with no parallel array.
    public static final byte BLOCK_WATER_FLOW_BASE = 10;  // ids 11..17 = flowing water level 7..1
    public static final byte BLOCK_LAVA            = 18;
    public static final byte BLOCK_LAVA_FLOW_BASE  = 18;  // ids 19..24 = flowing lava level 6..1
    public static final byte BLOCK_OBSIDIAN         = 25;
    // Crafting table (id 26): placed by the player; opens the 3×3 crafting grid on right-click.
    public static final byte BLOCK_CRAFTING_TABLE   = 26;

    // Fluid levels: source water is FLUID_SOURCE_LEVEL; flowing water and lava decrement from there.
    // Level 0 is ephemeral and dries up immediately (converted to air at the start of evaluation).
    public static final int FLUID_SOURCE_LEVEL      = 8;
    public static final int FLUID_MAX_FLOW_LEVEL    = 7;   // highest flowing (non-source) level
    public static final int FLUID_MIN_LEVEL         = 1;   // lowest level before drying to air

    // Water ticks every WATER_TICK_INTERVAL simulation ticks; lava ticks less often (heavier fluid).
    public static final int WATER_TICK_INTERVAL     = 2;
    public static final int LAVA_TICK_INTERVAL      = 8;

    // Hard cap on fluid cell evaluations per tick to bound worst-case CPU cost.
    public static final int MAX_FLUID_UPDATES_PER_TICK = 512;

    // Lighting (STEP-21). Skylight and blocklight are 4-bit levels packed in one byte per cell; the
    // brightest reachable level is MAX_LIGHT_LEVEL and each block of travel drops the level by one.
    // A torch is the only emitter so far; TORCH_EMISSION is its source level (one below full so its
    // own cell is bright but a lit room still reads darker than open sky).
    public static final int  MAX_LIGHT_LEVEL = 15;
    public static final int  TORCH_EMISSION  = 14;

    // Ore scatter into underground stone (see OreStage). One eligible stone block in RARITY
    // (statistically) becomes the ore. Diamond is rarer and confined to the deepest layers.
    public static final int IRON_RARITY        = 36;
    public static final int DIAMOND_RARITY     = 360;
    public static final int DIAMOND_MAX_LEVEL  = 16;
    public static final int ORE_MIN_LEVEL      = 1;

    // Debug-only flat world: a flat plain at this altitude, used by GenerationPipeline.flat() which
    // is off the default path (the live world uses overworld()). Stone fills the column below; the
    // surface is randomly capped (see FlatTerrainStage). Also caps OreStage's underground scan.
    public static final int   FLAT_SURFACE_LEVEL  = 60;

    public static final long  WORLD_SEED          = 42L;
    // Base sits well above WATER_LEVEL so dry land dominates and water only pools in real
    // depressions (lakes) and river channels, instead of flooding every low column.
    public static final int   TERRAIN_BASE_HEIGHT = 46;
    public static final int   TERRAIN_AMPLITUDE   = 10;
    public static final int   MOUNTAIN_AMPLITUDE  = 140;

    // Lakes: with plains kept flat (low TERRAIN_AMPLITUDE) the gentle hills no longer dip
    // below sea level, so a separate low-frequency noise sinks broad shallow basins into the
    // lowlands where water pools. Low frequency keeps plains locally flat between basins.
    public static final double BASIN_SCALE     = 0.010;
    public static final double BASIN_THRESHOLD = 0.05;
    public static final int    BASIN_OCTAVES   = 2;
    public static final int    BASIN_DEPTH     = 12;

    // Only mountain noise above this threshold lifts terrain, so plains and rolling hills
    // dominate and dramatic ranges stay rare. Range of the mask is roughly [-1, 1].
    public static final double MOUNTAIN_THRESHOLD = 0.20;

    // Valleys below this fill with water; surfaces at/above ROCK_LEVEL expose bare stone.
    public static final int   WATER_LEVEL = 40;
    public static final int   ROCK_LEVEL  = 96;

    // Water rendering (STEP-20). The top face of a water cell sits this fraction of a block below
    // the cell ceiling, so the surface reads as a slightly sunken sheet (beta look) and never
    // z-fights the solid block beside it. The other water faces stay full-height.
    public static final float WATER_SURFACE_DROP = 0.1f;
    // Translucent water blend: alpha lets the sea floor show through; the tint cools the texel
    // toward blue. Multiplied against the sampled water atlas tile in the water fragment shader.
    public static final float WATER_ALPHA  = 0.6f;
    public static final float WATER_TINT_R = 0.30f;
    public static final float WATER_TINT_G = 0.55f;
    public static final float WATER_TINT_B = 0.95f;

    // Rivers: a ridge of low-frequency noise (|noise| near zero) carves a winding channel
    // down to RIVER_BED_DEPTH below sea level. Only terrain up to RIVER_MAX_ELEVATION is
    // eligible, so rivers stay in the lowlands and never gouge canyons through mountains.
    public static final double RIVER_SCALE          = 0.004;
    public static final double RIVER_HALF_WIDTH     = 0.04;
    public static final int    RIVER_OCTAVES        = 2;
    public static final int    RIVER_BED_DEPTH      = 4;
    // Shared lowland ceiling: water features (rivers, lake basins) only apply at or below this
    // elevation, so they stay in the lowlands and never gouge channels or dents into mountains.
    public static final int    RIVER_MAX_ELEVATION  = WATER_LEVEL + 10;

    // One column in TREE_RARITY (statistically) sprouts a tree.
    public static final int TREE_RARITY            = 160;
    public static final int TREE_TRUNK_MIN_HEIGHT  = 4;
    public static final int TREE_TRUNK_MAX_HEIGHT  = 6;
    public static final int TREE_CANOPY_RADIUS     = 2;

    public static final float GRAVITY           = 20.0f;
    public static final float TERMINAL_VELOCITY = -50.0f;
    public static final float JUMP_IMPULSE      = 8.0f;
    public static final float PLAYER_EYE_HEIGHT = 1.6f;
    // Vertical margin (in blocks) above the surface where the player spawns, so they drop onto solid
    // ground instead of clipping into it or spawning inside terrain.
    public static final float PLAYER_SPAWN_CLEARANCE = 200.0f;
    // How far (in blocks) the player can reach to break a block, measured from the eye.
    public static final float PLAYER_REACH      = 5.0f;
    // Damage dealt per hit with bare hands. Tools will later raise this (or the per-block rate).
    public static final int   BARE_HAND_DAMAGE  = 1;

    public static final float FLY_VERTICAL_SPEED        = 20.0f;
    public static final float DOUBLE_TAP_WINDOW_SECONDS = 0.3f;

    // Inventory: a fixed grid of HOTBAR_SLOTS quick-access slots followed by BACKPACK_SLOTS storage.
    // The hotbar occupies the first HOTBAR_SLOTS entries of the slot array. A single stack holds at
    // most MAX_STACK items of one kind.
    public static final int MAX_STACK      = 64;
    public static final int HOTBAR_SLOTS   = 9;
    public static final int BACKPACK_SLOTS = 27;
    public static final int INVENTORY_SLOTS = HOTBAR_SLOTS + BACKPACK_SLOTS;

    // Sentinel for PlayerInput.hotbarSelect when no number key (1..9) was pressed this tick.
    public static final int NO_HOTBAR_SELECT = -1;

    // Dropped items (STEP-18). A broken block spawns a small physics entity at the block centre that
    // the player walks into to collect. The collider is a small cube; the spawn impulse gives it a
    // little hop and scatter so multiple drops do not stack on the exact same spot.
    public static final float ITEM_COLLIDER_SIZE   = 0.25f;
    public static final float ITEM_SPAWN_POP_SPEED  = 4.0f;
    public static final float ITEM_SPAWN_SCATTER    = 1.5f;
    // How close (centre to centre) the player must come before a drop is collected.
    public static final float ITEM_PICKUP_RADIUS    = 1.5f;
    // Grace period before a fresh drop becomes collectable, so it is not grabbed the instant it spawns.
    public static final float ITEM_PICKUP_DELAY      = 0.5f;
    // Visual edge length of the rendered drop cube (the test cube mesh is one unit wide).
    public static final float ITEM_RENDER_SCALE      = 0.3f;
    // Fraction of horizontal speed shed each tick once a drop rests on the ground (0 = frictionless,
    // 1 = stops instantly), so the spawn scatter dies out instead of sliding forever.
    public static final float ITEM_GROUND_FRICTION   = 0.3f;

    // Day/night cycle (STEP-23). dayFraction in [0,1): 0=dawn, 0.25=noon, 0.5=dusk, 0.75=midnight.
    // A full real-time day lasts DAY_LENGTH_SECONDS. Sun height drives a global skylight factor that
    // never falls below NIGHT_LIGHT, so torches and interiors stay readable in the dark.
    public static final float DAY_LENGTH_SECONDS = 600.0f;
    public static final float NIGHT_LIGHT        = 0.18f;
    // Fraction of the day spent in dawn=0 and noon=0.25 etc. is fixed by the sine of dayFraction;
    // these thresholds shape the global-light ramp between night and full day.
    public static final float DAY_LIGHT_HORIZON_MARGIN = 0.05f;

    // Player health (STEP-24). Health is whole "hearts"; MAX_HEALTH is also the respawn amount.
    public static final int   MAX_HEALTH = 20;

    // Fall damage. Landing at or below SAFE_FALL_SPEED (positive m/s) is harmless; above it, damage
    // grows linearly with the excess at FALL_DAMAGE_PER_SPEED points per m/s (see FallDamage). The
    // threshold sits above a normal JUMP_IMPULSE landing so ordinary hops never hurt.
    public static final float SAFE_FALL_SPEED       = 14.0f;
    public static final float FALL_DAMAGE_PER_SPEED = 0.5f;

    // Drowning. Submerged time builds up; once a full lungful (BREATH_SECONDS) is spent the player
    // loses DROWN_DAMAGE every DROWN_INTERVAL seconds. Breath refills (faster than it drains) in air.
    public static final float BREATH_SECONDS   = 10.0f;
    public static final float DROWN_INTERVAL   = 1.0f;
    public static final int   DROWN_DAMAGE     = 1;
    public static final float BREATH_REFILL_RATE = 4.0f;

    // I-frames granted by any damage event, so a single hit cannot drain health on back-to-back ticks.
    public static final float DAMAGE_IMMUNITY_SECONDS = 0.5f;

    // Slow passive regen (STEP-25 couples it to hunger): after this many seconds without taking
    // damage, heal REGEN_AMOUNT every REGEN_INTERVAL seconds up to MAX_HEALTH — but ONLY while the
    // player is well fed (food >= REGEN_FOOD_THRESHOLD). HungerSystem owns this heal so it can both
    // gate it on food and pay for it by spending hunger; HealthSystem no longer regenerates.
    public static final float REGEN_DELAY_SECONDS = 5.0f;
    public static final float REGEN_INTERVAL      = 2.0f;
    public static final int   REGEN_AMOUNT        = 1;

    // Hunger (STEP-25). Food is whole "drumsticks", MAX_FOOD the full bar (also the respawn amount).
    // Saturation is a hidden reservoir capped at the current food: activity drains it before food.
    public static final int   MAX_FOOD = 20;

    // Activity builds "exhaustion"; each EXHAUSTION_THRESHOLD reached spends one saturation point, or
    // one food point once saturation is empty. The per-activity costs are tuned so ordinary play
    // drains the bar slowly (sprinting/jumping faster than standing still). Taking damage also tires.
    public static final float EXHAUSTION_THRESHOLD       = 4.0f;
    public static final float EXHAUSTION_PER_BLOCK_MOVED = 0.01f;
    public static final float EXHAUSTION_PER_JUMP        = 0.2f;
    public static final float EXHAUSTION_PER_DAMAGE      = 0.1f;

    // Health regen is gated on a near-full bar; each heal spends one food/saturation point so a player
    // cannot heal indefinitely without eating.
    public static final int   REGEN_FOOD_THRESHOLD = 18;
    public static final float REGEN_HUNGER_COST     = 1.0f;

    // Starvation: at food == 0 the player loses STARVE_DAMAGE every STARVE_INTERVAL seconds. For this
    // project starvation may reach 0 health (no min-health floor like vanilla's easy mode).
    public static final float STARVE_INTERVAL = 4.0f;
    public static final int   STARVE_DAMAGE   = 1;

    // Consumable item ids live OUTSIDE the block id range (blocks are 0..9) so a food id is never
    // mistaken for a placeable block. See world.Foods for their restore values.
    public static final int ITEM_APPLE = 100;
    public static final int ITEM_BREAD = 101;

    // Intermediate crafting items (non-block, non-tool, non-food).
    // Sticks are crafted from planks and used as tool handles.
    public static final int ITEM_STICK = 102;

    // Crafting grid sizes. The player inventory always carries a 2×2 personal grid; a crafting table
    // block expands it to the standard 3×3 grid.
    public static final int CRAFTING_GRID_SMALL = 2;
    public static final int CRAFTING_GRID_LARGE  = 3;

    // Block gravity (STEP-33). Gravel is the second gravity-affected block after sand; id 27 sits
    // just above the crafting table (26).
    public static final byte BLOCK_GRAVEL = 27;

    // Highest valid block id; an item id above this is a non-block (e.g. food, tool) and must not be placed.
    public static final int MAX_BLOCK_ID = BLOCK_GRAVEL;

    // Random tick (STEP-33). Number of random cells sampled per chunk per tick.
    // Low enough to bound CPU cost; high enough that grass spreads visibly over a few seconds.
    public static final int  RANDOM_TICKS_PER_CHUNK      = 3;
    // Minimum skylight level required for grass to grow on dirt (same scale as LightEngine 0..15).
    public static final int  GRASS_GROWTH_LIGHT_THRESHOLD = 9;
    // Gravity tick interval: move one gravity block per this many simulation ticks (rate-limits chute CPU).
    public static final int  GRAVITY_TICK_INTERVAL        = 2;

    // Biome noise (STEP-34). Two independent low-frequency Perlin maps drive biome selection:
    // temperature (T) and humidity (H). Low scales keep biome regions wide and transitions smooth.
    public static final double BIOME_TEMPERATURE_SCALE = 0.0012;
    public static final double BIOME_HUMIDITY_SCALE    = 0.0014;
    public static final int    BIOME_NOISE_OCTAVES     = 2;

    // Biome T/H classification thresholds (values in [-1, 1] range of fractal noise output).
    // Ocean: low temperature (cold) regardless of humidity.
    // Desert: high temperature, low humidity.
    // Forest: moderate temperature, high humidity.
    // Mountains: high temperature, moderate-high humidity (cold peaks handled by ROCK_LEVEL).
    // Plains: everything else.
    public static final double BIOME_OCEAN_TEMP_THRESHOLD    = -0.25;
    public static final double BIOME_DESERT_TEMP_THRESHOLD   =  0.30;
    public static final double BIOME_DESERT_HUMID_THRESHOLD  =  0.10;
    public static final double BIOME_FOREST_HUMID_THRESHOLD  =  0.25;
    public static final double BIOME_MOUNTAIN_TEMP_THRESHOLD =  0.05;

    // Per-biome terrain amplitude modifiers applied on top of base terrain.
    // Negative = depression (oceans), large positive = tall mountains.
    public static final double BIOME_OCEAN_BASE_OFFSET     = -20.0;
    public static final double BIOME_OCEAN_AMPLITUDE_SCALE =  0.3;
    public static final double BIOME_PLAINS_AMPLITUDE_SCALE = 0.5;
    public static final double BIOME_FOREST_AMPLITUDE_SCALE = 0.7;
    public static final double BIOME_DESERT_BASE_OFFSET    =  4.0;
    public static final double BIOME_DESERT_AMPLITUDE_SCALE = 0.5;
    public static final double BIOME_MOUNTAIN_BASE_OFFSET  =  10.0;
    public static final double BIOME_MOUNTAIN_AMPLITUDE_SCALE = 1.5;

    // Blend radius for biome interpolation: how many noise units around a query point contribute
    // to the weighted average. Larger value = smoother transitions, more computation.
    public static final double BIOME_BLEND_RADIUS = 0.04;

    // Per-biome tree rarity (1-in-N columns sprouts a tree; higher = sparser).
    // Desert and ocean have no trees (set to a sentinel too large to match any hash).
    public static final int TREE_RARITY_FOREST  = 40;
    public static final int TREE_RARITY_PLAINS  = 200;
    public static final int TREE_RARITY_NONE    = Integer.MAX_VALUE;

    // Desert sand depth: number of sub-surface sand layers before switching to stone.
    // Kept at 3 so the block 4 layers below surface is always stone, consistent with
    // TerrainStageTest.stoneAppearsDeepBelowSurface which checks surface-4.
    public static final int DESERT_SAND_DEPTH = 3;

    // Tool item ids (see world.ItemRegistry for the full table and per-material constants).
    // These mirror ItemRegistry.*  so call-sites in WorldConstants-land don't have to import ItemRegistry.
    public static final int TOOL_ID_FIRST = 200;
    public static final int TOOL_ID_LAST  = 239;

    // Lava contact damage: applied by HealthSystem when the player stands in a lava block.
    // Short i-frames prevent rapid repeated damage; value intentionally higher than drowning.
    public static final int   LAVA_DAMAGE          = 4;
    public static final float LAVA_DAMAGE_INTERVAL = 0.5f;

    // Mob drop item ids (non-block, non-tool; sit in the 100s alongside food and sticks).
    // 100=APPLE 101=BREAD 102=STICK, so mob drops start at 103.
    public static final int ITEM_LEATHER = 103;
    public static final int ITEM_BEEF    = 104;
    public static final int ITEM_PORK    = 105;
    public static final int ITEM_WOOL    = 106;
    public static final int ITEM_FEATHER = 107;

    // Hostile mob combat and AI (STEP-31).
    // Detection range (blocks) within which a hostile mob spots the player.
    public static final float MOB_DETECTION_RANGE    = 16.0f;
    // Attack range (blocks): mob must be this close to land a hit.
    public static final float MOB_ATTACK_RANGE       = 1.5f;
    // Base damage dealt by a mob per attack.
    public static final int   MOB_ATTACK_DAMAGE      = 2;
    // Cooldown (seconds) between mob attacks (i-frames on the player gate this further).
    public static final float MOB_ATTACK_COOLDOWN    = 1.0f;
    // How often (seconds) CHASE state recomputes the A* path.
    public static final float MOB_PATH_RECOMPUTE_INTERVAL = 0.8f;
    // Maximum node budget for a single A* search (bounds worst-case CPU per mob per tick).
    public static final int   PATHFINDER_NODE_BUDGET  = 256;
    // Maximum A* path length in blocks; longer paths are rejected to avoid huge memory use.
    public static final int   PATHFINDER_MAX_DISTANCE = 32;
    // Skylight level at or above which an unshielded mob is considered in "high daylight".
    public static final int   HOSTILE_SPAWN_MAX_LIGHT = 7;
    // Skylight level at which undead mobs (zombie/skeleton) start burning in sunlight.
    public static final int   UNDEAD_BURN_SKYLIGHT    = 12;
    // Damage dealt per fire tick to undead in full sunlight.
    public static final int   UNDEAD_BURN_DAMAGE      = 1;
    // How often (seconds) undead burn damage is applied.
    public static final float UNDEAD_BURN_INTERVAL    = 1.0f;
    // Horizontal knockback speed (blocks/s) applied to a mob that is hit by the player.
    public static final float MOB_KNOCKBACK_SPEED     = 6.0f;
    // Vertical knockback impulse (blocks/s) on mob hit.
    public static final float MOB_KNOCKBACK_VERTICAL  = 3.0f;
    // Bare-handed melee damage when the player is not holding a sword.
    public static final int   PLAYER_FIST_DAMAGE      = 1;
    // Melee weapon damage for each sword tier (plain int lookup by ToolMaterial ordinal).
    public static final int   SWORD_DAMAGE_WOOD       = 4;
    public static final int   SWORD_DAMAGE_STONE      = 5;
    public static final int   SWORD_DAMAGE_IRON       = 6;
    public static final int   SWORD_DAMAGE_GOLD       = 4;
    public static final int   SWORD_DAMAGE_DIAMOND    = 7;
    // Attack range for the player (distance from eye to eligible mob).
    public static final float PLAYER_ATTACK_RANGE     = 3.5f;
    // Minimum cosine between the look direction and the direction to a mob for it to be hit
    // (~60° cone), so the player strikes what they face rather than mobs behind them.
    public static final float PLAYER_ATTACK_AIM_DOT   = 0.5f;
    // Approximate height of a mob's torso above its feet; used as the melee aim point.
    public static final float MOB_TORSO_HEIGHT        = 0.9f;
    // Population cap for hostile mobs within the area radius.
    public static final int   MAX_HOSTILE_PER_AREA    = 20;
    // Spawn attempt interval for the hostile spawn system (seconds).
    public static final float HOSTILE_SPAWN_INTERVAL  = 4.0f;
    // Day fraction boundaries: dayFraction in [0, 0.5) is day; hostile mobs can spawn
    // any time but require low light; undead burn during full day (frac < 0.45).
    public static final float UNDEAD_BURN_DAY_MAX     = 0.45f;
    // Creeper explosion damage (area, future: block destruction is a TODO).
    public static final int   CREEPER_EXPLOSION_DAMAGE = 49;
    // Creeper fuse time (seconds) before detonation once within attack range.
    public static final float CREEPER_FUSE_SECONDS    = 1.5f;
    // Creeper explosion radius (blocks).
    public static final float CREEPER_EXPLOSION_RADIUS = 4.0f;

    // Passive mob AI (STEP-30).
    // How long (in seconds) a mob stays IDLE before choosing a wander target.
    public static final float MOB_IDLE_MIN_SECONDS   = 2.0f;
    public static final float MOB_IDLE_MAX_SECONDS   = 6.0f;
    // How long (in seconds) a mob wanders before returning to IDLE.
    public static final float MOB_WANDER_MAX_SECONDS = 5.0f;
    // How long (in seconds) a mob flees after being hit.
    public static final float MOB_FLEE_SECONDS       = 3.0f;
    // Horizontal speed while wandering and fleeing.
    public static final float MOB_WANDER_SPEED       = 1.8f;
    public static final float MOB_FLEE_SPEED         = 4.5f;
    // Horizontal speed of a hostile mob chasing the player (faster than wander, near flee speed).
    public static final float MOB_CHASE_SPEED        = 3.6f;
    // Radius within which a random wander target is chosen (in blocks, XZ plane).
    public static final float MOB_WANDER_RADIUS      = 8.0f;
    // A mob attempts a 1-block jump when it hits a wall while wandering.
    public static final float MOB_JUMP_IMPULSE        = 6.0f;
    // Passive mob spawn rules.
    // Minimum distance from the player before a spawn attempt is made.
    public static final float MOB_SPAWN_MIN_RADIUS    = 12.0f;
    // Maximum distance from the player for a spawn attempt (must be within loaded chunks).
    public static final float MOB_SPAWN_MAX_RADIUS    = 48.0f;
    // Cap on passive mobs that can co-exist within one chunk area.
    public static final int   MAX_PASSIVE_PER_AREA    = 10;
    // Number of world chunks (radius) that define the "area" used for the population cap.
    public static final int   MOB_AREA_CHUNK_RADIUS   = 4;
    // How often (in seconds) the spawn system attempts a spawn pass.
    public static final float MOB_SPAWN_INTERVAL      = 5.0f;
    // How often (in seconds) the spawn system checks for mobs to despawn.
    public static final float MOB_DESPAWN_INTERVAL    = 10.0f;
    // A mob beyond this distance from the player is eligible for despawn.
    public static final float MOB_DESPAWN_RADIUS      = 80.0f;
    // Pack size range for a single spawn attempt.
    public static final int   MOB_PACK_MIN            = 1;
    public static final int   MOB_PACK_MAX            = 4;
    // Day/night threshold: dayFraction < NIGHT_START or > NIGHT_END is daytime.
    // 0=dawn, 0.25=noon, 0.5=dusk, 0.75=midnight. Daytime = [0, 0.5).
    public static final float MOB_SPAWN_DAY_FRACTION_MAX = 0.5f;
    // A mob is considered grounded when blocked, used in AI jump logic.
    public static final float MOB_OBSTACLE_CHECK_DIST = 0.6f;
}
