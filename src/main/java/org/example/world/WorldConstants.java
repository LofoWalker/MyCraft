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

    // Highest valid block id; an item id above this is a non-block (e.g. food) and must not be placed.
    public static final int MAX_BLOCK_ID = BLOCK_TORCH;

    // TODO(STEP-24): lava contact damage + short i-frames once BLOCK_LAVA and its atlas tile exist.
    // Out of scope here: no lava block/tile is added, so the HealthSystem only handles fall + drowning.
}
