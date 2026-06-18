package org.example.world;

import org.example.components.BlockEntity;
import org.example.components.Furnace;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.ecs.Entity;
import org.example.ecs.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Singleton-style manager: maps world positions to block-entity ECS ids.
// Lookup is edge-triggered (player interaction), so a HashMap is acceptable here.
// Furnace block-entities carry: BlockEntity + Furnace + Inventory (3 slots).
// Chest block-entities carry:   BlockEntity + Inventory (CHEST_SLOTS slots).
public final class BlockEntityStore {

    // Shared singleton; callers that need testability pass a fresh instance directly.
    private static final BlockEntityStore INSTANCE = new BlockEntityStore();

    public static BlockEntityStore get() { return INSTANCE; }

    // position key → ECS entity id
    private final Map<Long, Integer> positionToEntity = new HashMap<>();

    // --- Public API ---

    // Returns the ECS entity id for the block entity at (wx, wy, wz), creating one if absent.
    // blockId determines what components are attached (FURNACE vs CHEST).
    public Entity getOrCreate(World world, int wx, int wy, int wz, byte blockId) {
        long key = encodePos(wx, wy, wz);
        Integer existing = positionToEntity.get(key);
        if (existing != null) {
            return new Entity(existing);
        }
        Entity entity = createBlockEntity(world, wx, wy, wz, blockId);
        positionToEntity.put(key, entity.id());
        return entity;
    }

    // Returns an existing block entity, or empty if none exists at this position.
    public Optional<Entity> find(int wx, int wy, int wz) {
        long key = encodePos(wx, wy, wz);
        Integer id = positionToEntity.get(key);
        return id == null ? Optional.empty() : Optional.of(new Entity(id));
    }

    // Called when a block is broken: drops all inventory contents as items and removes the entity.
    public void remove(World world, int wx, int wy, int wz) {
        long key = encodePos(wx, wy, wz);
        Integer id = positionToEntity.remove(key);
        if (id == null) return;

        Entity entity = new Entity(id);
        dropContents(world, entity, wx, wy, wz);
        world.destroy(entity);
    }

    // Resets the store (used for new world sessions).
    public void clear() {
        positionToEntity.clear();
    }

    // --- Helpers ---

    private static Entity createBlockEntity(World world, int wx, int wy, int wz, byte blockId) {
        Entity entity = world.create();
        world.add(entity, new BlockEntity(wx, wy, wz));
        if (blockId == WorldConstants.BLOCK_FURNACE) {
            world.add(entity, Furnace.empty());
            world.add(entity, emptyFurnaceInventory());
        } else if (blockId == WorldConstants.BLOCK_CHEST) {
            world.add(entity, emptyChestInventory());
        }
        return entity;
    }

    private static Inventory emptyFurnaceInventory() {
        ItemStack[] slots = new ItemStack[3];
        Arrays.fill(slots, ItemStack.EMPTY);
        return new Inventory(slots);
    }

    private static Inventory emptyChestInventory() {
        ItemStack[] slots = new ItemStack[WorldConstants.CHEST_SLOTS];
        Arrays.fill(slots, ItemStack.EMPTY);
        return new Inventory(slots);
    }

    private static void dropContents(World world, Entity entity, int wx, int wy, int wz) {
        world.get(entity, Inventory.class).ifPresent(inv -> {
            for (ItemStack stack : inv.slots()) {
                if (!stack.isEmpty()) {
                    org.example.systems.BlockEntityDrops.spawnStack(world, wx, wy, wz, stack);
                }
            }
        });
    }

    // Packs (wx, wy, wz) into a single long. World coords fit in 20 bits each (±512k blocks).
    static long encodePos(int wx, int wy, int wz) {
        return ((long) (wx & 0xFFFFF) << 40)
             | ((long) (wy & 0xFFFFF) << 20)
             |  (long) (wz & 0xFFFFF);
    }
}
