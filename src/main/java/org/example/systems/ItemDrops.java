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

// Spawns the dropped-item entity left behind when a block is broken. The drop sits at the centre of
// the cleared cell with a small upward pop and random horizontal scatter, then falls and settles
// under the shared physics/collision systems. Air-equivalent blocks (no item) drop nothing.
final class ItemDrops {

    private ItemDrops() {}

    static void spawn(World world, int wx, int wy, int wz, byte blockId) {
        if (blockId == WorldConstants.BLOCK_AIR) return;

        float size = WorldConstants.ITEM_COLLIDER_SIZE;
        Entity item = world.create();
        world.add(item, new ItemEntity());
        world.add(item, new ItemStack(blockId, 1));
        // Position is the collider's horizontal centre and vertical base (see CollisionSystem); the
        // cell centre in x/z and mid-cell in y let the drop settle onto the block below.
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
