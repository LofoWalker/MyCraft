package org.example.world;

import org.example.components.Inventory;
import org.example.components.ItemEntity;
import org.example.components.ItemStack;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Chest block-entity behaviour: storage, transfer and drop-on-break. */
class ChestTest {

    private World world;

    @BeforeEach
    void setUp() {
        world = new World();
        BlockEntityStore.get().clear();
    }

    @Test
    void newChestHasEmptySlots() {
        Entity chest = BlockEntityStore.get().getOrCreate(world, 1, 2, 3, WorldConstants.BLOCK_CHEST);
        Inventory inv = world.get(chest, Inventory.class).orElseThrow();

        assertEquals(WorldConstants.CHEST_SLOTS, inv.slots().length, "chest has CHEST_SLOTS slots");
        for (ItemStack slot : inv.slots()) {
            assertTrue(slot.isEmpty(), "every slot starts empty");
        }
    }

    @Test
    void itemsTransferIntoChest() {
        Entity chest = BlockEntityStore.get().getOrCreate(world, 1, 2, 3, WorldConstants.BLOCK_CHEST);
        Inventory inv = world.get(chest, Inventory.class).orElseThrow();

        Inventory updated = Inventories.add(inv, new ItemStack(WorldConstants.BLOCK_STONE, 10)).inventory();
        world.add(chest, updated);

        Inventory stored = world.get(chest, Inventory.class).orElseThrow();
        assertEquals(WorldConstants.BLOCK_STONE, stored.slots()[0].itemId());
        assertEquals(10, stored.slots()[0].count(), "stored items persist in the chest");
    }

    @Test
    void breakingChestDropsAllContents() {
        Entity chest = BlockEntityStore.get().getOrCreate(world, 5, 6, 7, WorldConstants.BLOCK_CHEST);
        Inventory inv = world.get(chest, Inventory.class).orElseThrow();
        ItemStack[] slots = inv.slots().clone();
        slots[0] = new ItemStack(WorldConstants.BLOCK_STONE, 5);
        slots[1] = new ItemStack(WorldConstants.BLOCK_IRON, 3);
        world.add(chest, new Inventory(slots));

        BlockEntityStore.get().remove(world, 5, 6, 7);

        assertTrue(BlockEntityStore.get().find(5, 6, 7).isEmpty(), "chest entity removed after break");
        int[] drops = world.query(ItemEntity.class, ItemStack.class);
        assertEquals(2, drops.length, "each non-empty slot drops one item entity");
    }
}
