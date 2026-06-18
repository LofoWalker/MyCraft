package org.example.systems;

import org.example.components.DamageTimers;
import org.example.components.Grounded;
import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Hunger;
import org.example.components.HungerTimers;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.components.PlayerInput;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;

import static org.example.systems.GameModeQuery.isCreative;
import org.example.world.Foods;
import org.example.world.HungerMath;
import org.example.world.Inventories;
import org.example.world.WorldConstants;

/**
 * Owns the hunger bar and ALL hunger-driven health changes. Each tick it accumulates exhaustion from
 * activity (horizontal movement, jumping), drains saturation then food, processes a single eat action,
 * and then couples to {@link Health}: a near-full bar slowly regenerates health (paying with hunger),
 * while an empty bar deals periodic starvation damage.
 *
 * <p>Regen ownership: {@link HealthSystem} no longer regenerates — it only applies damage events and
 * respawns. HungerSystem is the sole healer so the two never double-heal. It reads
 * {@link DamageTimers#sinceDamage()} (advanced by HealthSystem) only to honour the post-damage regen
 * delay, but never writes it. Scheduled AFTER HealthSystem so damage and i-frames for this tick are
 * already settled before regen/starvation is decided.
 */
public final class HungerSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(Hunger.class, Health.class, Velocity.class)) {
            Entity entity = new Entity(eid);
            // Creative mode: hunger does not drain and cannot cause starvation.
            if (isCreative(world, entity)) continue;
            accumulateActivity(world, entity, dt);
            drainExhaustion(world, entity);
            applyEating(world, entity);
            coupleHealth(world, entity, dt);
        }
    }

    // --- Activity → exhaustion ---

    private static void accumulateActivity(World world, Entity entity, float dt) {
        Velocity velocity = world.get(entity, Velocity.class).orElseThrow();
        float distance = horizontalSpeed(velocity) * dt;
        float added = HungerMath.exhaustionFromMovement(distance, 0f) + jumpCost(world, entity);
        HungerTimers timers = timers(world, entity);
        world.add(entity, withExhaustion(timers, HungerMath.addExhaustion(timers.exhaustion(), added)));
    }

    private static float horizontalSpeed(Velocity velocity) {
        return (float) Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z());
    }

    // A jump is an upward velocity while still flagged Grounded (the impulse tick), so it is counted
    // once per leap rather than every airborne tick.
    private static float jumpCost(World world, Entity entity) {
        boolean wantsJump = world.get(entity, PlayerInput.class).map(PlayerInput::jump).orElse(false);
        boolean grounded  = world.has(entity, Grounded.class);
        return wantsJump && grounded ? WorldConstants.EXHAUSTION_PER_JUMP : 0f;
    }

    private static void drainExhaustion(World world, Entity entity) {
        HungerTimers timers = timers(world, entity);
        HungerMath.DrainResult result = HungerMath.drain(hunger(world, entity), timers.exhaustion());
        world.add(entity, result.hunger());
        world.add(entity, withExhaustion(timers, result.exhaustion()));
    }

    // --- Eating ---

    private static void applyEating(World world, Entity entity) {
        boolean eat = world.get(entity, PlayerInput.class).map(PlayerInput::eat).orElse(false);
        if (!eat) return;

        Inventory inventory = world.get(entity, Inventory.class).orElse(null);
        if (inventory == null) return;
        int slot = world.get(entity, Hotbar.class).map(Hotbar::selectedSlot).orElse(0);
        ItemStack held = Inventories.get(inventory, slot);
        if (held.isEmpty() || !Foods.isFood(held.itemId())) return;

        Foods.Food food = Foods.byId(held.itemId());
        world.add(entity, HungerMath.eat(hunger(world, entity), food.foodRestore(), food.saturationRestore()));
        world.add(entity, Inventories.removeOne(inventory, slot));
    }

    // --- Health coupling (sole owner of hunger-funded regen + starvation) ---

    private static void coupleHealth(World world, Entity entity, float dt) {
        Health health = world.get(entity, Health.class).orElseThrow();
        HungerMath.TickResult tick = HungerMath.tickHealth(hunger(world, entity), timers(world, entity),
                dt, regenAllowed(world, entity), atFullHealth(health));

        world.add(entity, tick.hunger());
        world.add(entity, tick.timers());
        if (tick.healthDelta() != 0) applyHealthDelta(world, entity, health, tick.healthDelta());
    }

    private static void applyHealthDelta(World world, Entity entity, Health health, int delta) {
        int next = Math.min(health.current() + delta, health.max());
        world.add(entity, new Health(next, health.max()));
    }

    private static boolean regenAllowed(World world, Entity entity) {
        float since = world.get(entity, DamageTimers.class).map(DamageTimers::sinceDamage).orElse(0f);
        return since >= WorldConstants.REGEN_DELAY_SECONDS;
    }

    private static boolean atFullHealth(Health health) {
        return health.current() >= health.max();
    }

    // --- Component accessors with sane defaults ---

    private static Hunger hunger(World world, Entity entity) {
        return world.get(entity, Hunger.class)
                .orElse(new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD));
    }

    private static HungerTimers timers(World world, Entity entity) {
        return world.get(entity, HungerTimers.class).orElse(new HungerTimers(0f, 0f, 0f));
    }

    private static HungerTimers withExhaustion(HungerTimers timers, float exhaustion) {
        return new HungerTimers(exhaustion, timers.regenAccum(), timers.starveAccum());
    }
}
