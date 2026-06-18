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
import org.example.ecs.World;
import org.example.world.Foods;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HungerSystemTest {

    private static final float DT = 1f / 60f;

    private World        world;
    private Entity       player;
    private HungerSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        system = new HungerSystem();
        player = world.create();
        world.add(player, new Health(WorldConstants.MAX_HEALTH, WorldConstants.MAX_HEALTH));
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, WorldConstants.MAX_FOOD));
        world.add(player, new HungerTimers(0f, 0f, 0f));
        world.add(player, new Velocity(0f, 0f, 0f));
        world.add(player, idle());
        // Past the post-damage delay so regen is allowed by default.
        world.add(player, new DamageTimers(WorldConstants.REGEN_DELAY_SECONDS, 0f, 0f));
    }

    private static PlayerInput idle() {
        return new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                false, 0, WorldConstants.NO_HOTBAR_SELECT, false);
    }

    private static PlayerInput eating() {
        return new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                true, 0, WorldConstants.NO_HOTBAR_SELECT, false);
    }

    private Hunger hunger() {
        return world.get(player, Hunger.class).orElseThrow();
    }

    private int health() {
        return world.get(player, Health.class).orElseThrow().current();
    }

    private void advance(float seconds) {
        int steps = Math.round(seconds / DT);
        for (int i = 0; i < steps; i++) system.update(world, DT);
    }

    // --- activity drains hunger ---

    @Test
    void movingDrainsSaturationThenFood() {
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, 2f));
        world.add(player, new Velocity(6f, 0f, 0f)); // sustained horizontal speed
        // Exhaustion accrues at EXHAUSTION_PER_BLOCK_MOVED (~0.01/block) and one point drains every
        // EXHAUSTION_THRESHOLD (4.0), i.e. ~400 blocks. Move long enough to cross several thresholds.
        advance(200f);

        Hunger h = hunger();
        assertTrue(h.saturation() < 2f || h.food() < WorldConstants.MAX_FOOD,
                "activity must drain saturation then food");
    }

    @Test
    void jumpingAddsExhaustion() {
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, 0f));
        world.add(player, new Grounded());
        world.add(player, eatingFreeJump());

        int before = hunger().food();
        for (int i = 0; i < WorldConstants.MAX_FOOD; i++) system.update(world, DT);
        assertTrue(hunger().food() < before, "repeated jumps must drain food");
    }

    private static PlayerInput eatingFreeJump() {
        return new PlayerInput(false, false, false, false, true, false, 0f, 0f, false, false,
                false, 0, WorldConstants.NO_HOTBAR_SELECT, false);
    }

    // --- eating ---

    @Test
    void eatingRestoresFoodAndDecrementsStack() {
        giveBread(3);
        world.add(player, new Hunger(5, 0f));
        world.add(player, eating());

        system.update(world, DT);

        Foods.Food bread = Foods.byId(WorldConstants.ITEM_BREAD);
        assertEquals(5 + bread.foodRestore(), hunger().food());
        assertTrue(hunger().saturation() > 0f);
        assertEquals(2, world.get(player, Inventory.class).orElseThrow()
                .slots()[0].count(), "one bread consumed");
    }

    @Test
    void eatingDoesNothingWhenHoldingABlock() {
        Inventory inv = Inventories.add(Inventories.empty(),
                new ItemStack(WorldConstants.BLOCK_STONE, 4)).inventory();
        world.add(player, inv);
        world.add(player, new Hotbar(0));
        world.add(player, new Hunger(5, 0f));
        world.add(player, eating());

        system.update(world, DT);

        assertEquals(5, hunger().food());
        assertEquals(4, world.get(player, Inventory.class).orElseThrow().slots()[0].count());
    }

    // --- starvation ---

    @Test
    void emptyFoodDealsStarvationDamage() {
        world.add(player, new Hunger(0, 0f));
        advance(WorldConstants.STARVE_INTERVAL + 0.1f);
        assertTrue(health() < WorldConstants.MAX_HEALTH);
    }

    // --- regen coupling ---

    @Test
    void wellFedRegeneratesHealthAndSpendsHunger() {
        world.add(player, new Health(WorldConstants.MAX_HEALTH - 4, WorldConstants.MAX_HEALTH));
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, 10f));
        advance(WorldConstants.REGEN_INTERVAL + 0.1f);

        assertTrue(health() > WorldConstants.MAX_HEALTH - 4, "well-fed player regenerates");
        assertTrue(hunger().saturation() < 10f, "regen spends hunger");
    }

    @Test
    void lowFoodDoesNotRegenerateHealth() {
        world.add(player, new Health(WorldConstants.MAX_HEALTH - 4, WorldConstants.MAX_HEALTH));
        world.add(player, new Hunger(WorldConstants.REGEN_FOOD_THRESHOLD - 1, 10f));
        advance(WorldConstants.REGEN_INTERVAL * 3f);
        assertEquals(WorldConstants.MAX_HEALTH - 4, health());
    }

    @Test
    void regenWaitsForPostDamageDelay() {
        world.add(player, new Health(WorldConstants.MAX_HEALTH - 4, WorldConstants.MAX_HEALTH));
        world.add(player, new Hunger(WorldConstants.MAX_FOOD, 10f));
        world.add(player, new DamageTimers(0f, 0f, 0f)); // just took damage
        advance(WorldConstants.REGEN_INTERVAL + 0.1f);
        assertEquals(WorldConstants.MAX_HEALTH - 4, health(), "no regen inside the delay");
    }

    private void giveBread(int count) {
        Inventory inv = Inventories.add(Inventories.empty(),
                new ItemStack(WorldConstants.ITEM_BREAD, count)).inventory();
        world.add(player, inv);
        world.add(player, new Hotbar(0));
    }
}
