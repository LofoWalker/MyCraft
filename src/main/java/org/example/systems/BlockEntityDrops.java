package org.example.systems;

import org.example.components.ColliderAABB;
import org.example.components.Gravity;
import org.example.components.ItemEntity;
import org.example.components.ItemStack;
import org.example.components.PickupDelay;
import org.example.components.Position;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;

import java.util.concurrent.ThreadLocalRandom;

// Spawns dropped-item entities for block-entity contents (furnace/chest breakage).
public final class BlockEntityDrops {

    private BlockEntityDrops() {}

    public static void spawnStack(World world, int wx, int wy, int wz, ItemStack stack) {
        if (stack.isEmpty()) return;

        float size = WorldConstants.ITEM_COLLIDER_SIZE;
        Entity item = world.create();
        world.add(item, new ItemEntity());
        world.add(item, stack);
        world.add(item, new Position(wx + 0.5f, wy + 0.5f, wz + 0.5f));
        world.add(item, new Velocity(scatter(), WorldConstants.ITEM_SPAWN_POP_SPEED, scatter()));
        world.add(item, new Gravity(WorldConstants.GRAVITY));
        world.add(item, new ColliderAABB(size, size, size));
        world.add(item, new PickupDelay(WorldConstants.ITEM_PICKUP_DELAY));
    }

    private static float scatter() {
        float s = WorldConstants.ITEM_SPAWN_SCATTER;
        return ThreadLocalRandom.current().nextFloat() * 2f * s - s;
    }
}
