package org.example.systems;

import org.example.components.Inventory;
import org.example.components.ItemEntity;
import org.example.components.ItemStack;
import org.example.components.PickupDelay;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.Inventories;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ItemPickupSystemTest {

    private static final float DT = 1f / 60f;

    @Test
    void collectsItemWithinRadiusOnceDelayElapsed() {
        World world = new World();
        Entity player = spawnPlayer(world, Inventories.empty());
        Entity item   = spawnItem(world, 0.5f, WorldConstants.BLOCK_STONE, 1, 0f);
        ItemPickupSystem system = new ItemPickupSystem();

        system.update(world, DT);

        assertFalse(world.has(item, ItemEntity.class), "fully collected item should be destroyed");
        Inventory inv = world.get(player, Inventory.class).orElseThrow();
        assertEquals(1, totalCount(inv, WorldConstants.BLOCK_STONE));
    }

    @Test
    void leavesItemStillOnPickupCooldown() {
        World world = new World();
        Entity player = spawnPlayer(world, Inventories.empty());
        Entity item   = spawnItem(world, 0.5f, WorldConstants.BLOCK_STONE, 1, WorldConstants.ITEM_PICKUP_DELAY);
        ItemPickupSystem system = new ItemPickupSystem();

        system.update(world, DT); // one tick is not enough to expire the delay

        assertTrue(world.has(item, ItemEntity.class));
        assertEquals(0, totalCount(world.get(player, Inventory.class).orElseThrow(), WorldConstants.BLOCK_STONE));
        assertTrue(world.get(item, PickupDelay.class).orElseThrow().seconds() < WorldConstants.ITEM_PICKUP_DELAY);
    }

    @Test
    void ignoresItemOutsidePickupRadius() {
        World world = new World();
        Entity player = spawnPlayer(world, Inventories.empty());
        float far     = WorldConstants.ITEM_PICKUP_RADIUS + 1f;
        Entity item   = spawnItem(world, far, WorldConstants.BLOCK_STONE, 1, 0f);
        ItemPickupSystem system = new ItemPickupSystem();

        system.update(world, DT);

        assertTrue(world.has(item, ItemEntity.class));
        assertEquals(0, totalCount(world.get(player, Inventory.class).orElseThrow(), WorldConstants.BLOCK_STONE));
    }

    @Test
    void fullInventoryLeavesItemOnTheGround() {
        World world = new World();
        Entity player = spawnPlayer(world, fullInventoryOf(WorldConstants.BLOCK_DIRT));
        Entity item   = spawnItem(world, 0.5f, WorldConstants.BLOCK_STONE, 1, 0f);
        ItemPickupSystem system = new ItemPickupSystem();

        system.update(world, DT);

        assertTrue(world.has(item, ItemEntity.class), "no room -> item stays in the world");
        ItemStack remaining = world.get(item, ItemStack.class).orElseThrow();
        assertEquals(WorldConstants.BLOCK_STONE, remaining.itemId());
        assertEquals(1, remaining.count());
    }

    @Test
    void partialPickupKeepsRemainderOnTheGround() {
        World world = new World();
        // Inventory full of stone except a single stack with one free slot of space.
        Inventory inv = fullInventoryOf(WorldConstants.BLOCK_STONE);
        inv = withSlot(inv, 0, new ItemStack(WorldConstants.BLOCK_STONE, WorldConstants.MAX_STACK - 1));
        Entity player = spawnPlayer(world, inv);
        Entity item   = spawnItem(world, 0.5f, WorldConstants.BLOCK_STONE, 5, 0f);
        ItemPickupSystem system = new ItemPickupSystem();

        system.update(world, DT);

        assertTrue(world.has(item, ItemEntity.class));
        assertEquals(4, world.get(item, ItemStack.class).orElseThrow().count(), "only one unit fit");
    }

    private static int totalCount(Inventory inventory, int itemId) {
        int total = 0;
        for (ItemStack slot : inventory.slots()) {
            if (slot.itemId() == itemId) total += slot.count();
        }
        return total;
    }

    private static Inventory fullInventoryOf(int itemId) {
        ItemStack[] slots = new ItemStack[WorldConstants.INVENTORY_SLOTS];
        Arrays.fill(slots, new ItemStack(itemId, WorldConstants.MAX_STACK));
        return new Inventory(slots);
    }

    private static Inventory withSlot(Inventory inventory, int slot, ItemStack stack) {
        ItemStack[] slots = inventory.slots().clone();
        slots[slot] = stack;
        return new Inventory(slots);
    }

    private static Entity spawnPlayer(World world, Inventory inventory) {
        Entity player = world.create();
        world.add(player, new Position(0f, 60f, 0f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0f, 0f, false, false,
                0, WorldConstants.NO_HOTBAR_SELECT));
        world.add(player, inventory);
        return player;
    }

    private static Entity spawnItem(World world, float offset, byte itemId, int count, float delay) {
        Entity item = world.create();
        world.add(item, new ItemEntity());
        world.add(item, new Position(offset, 60f, 0f));
        world.add(item, new ItemStack(itemId, count));
        if (delay > 0f) world.add(item, new PickupDelay(delay));
        return item;
    }
}
