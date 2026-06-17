package org.example.systems;

import org.example.components.Inventory;
import org.example.components.ItemEntity;
import org.example.components.ItemStack;
import org.example.components.PickupDelay;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.Inventories;
import org.example.world.WorldConstants;

// Collects dropped items the player walks over. Each tick it ages every drop's PickupDelay, then for
// any collectable drop within ITEM_PICKUP_RADIUS of the player it tries to merge the stack into the
// player's Inventory. Fully absorbed drops are destroyed; a partial pickup (inventory nearly full)
// keeps the remainder on the ground, and a full inventory leaves the drop untouched.
public final class ItemPickupSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        int[] players = world.query(PlayerInput.class, Position.class, Inventory.class);
        if (players.length == 0) return;

        Entity   player = new Entity(players[0]);
        Position eye    = world.get(player, Position.class).orElseThrow();

        for (int eid : world.query(ItemEntity.class, Position.class, ItemStack.class)) {
            Entity item = new Entity(eid);
            if (ageDelay(world, item, dt)) continue;
            if (!withinReach(eye, world.get(item, Position.class).orElseThrow())) continue;
            collect(world, player, item);
        }
    }

    // Counts the delay down; returns true while the drop is still on cooldown (not yet collectable).
    private static boolean ageDelay(World world, Entity item, float dt) {
        PickupDelay delay = world.get(item, PickupDelay.class).orElse(null);
        if (delay == null) return false;
        float left = delay.seconds() - dt;
        if (left > 0f) {
            world.add(item, new PickupDelay(left));
            return true;
        }
        world.remove(item, PickupDelay.class);
        return false;
    }

    private static void collect(World world, Entity player, Entity item) {
        Inventory inventory = world.get(player, Inventory.class).orElseThrow();
        ItemStack stack     = world.get(item, ItemStack.class).orElseThrow();

        Inventories.AddResult result = Inventories.add(inventory, stack);
        if (sameStack(result.remainder(), stack)) return; // inventory full -> leave the drop

        world.add(player, result.inventory());
        if (result.remainder().isEmpty()) world.destroy(item);
        else                              world.add(item, result.remainder());
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return a.itemId() == b.itemId() && a.count() == b.count();
    }

    private static boolean withinReach(Position player, Position item) {
        float dx = player.x() - item.x();
        float dy = player.y() - item.y();
        float dz = player.z() - item.z();
        float r  = WorldConstants.ITEM_PICKUP_RADIUS;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }
}
