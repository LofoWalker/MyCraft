package org.example.systems;

import org.example.components.BlockEntity;
import org.example.components.Furnace;
import org.example.components.Inventory;
import org.example.components.ItemStack;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.Inventories;
import org.example.world.SmeltingRecipes;
import org.example.world.WorldConstants;

// Simulation-side furnace system. Runs once per simulation tick on every block-entity that carries
// a Furnace component. The 3-slot inventory layout is:
//   slot 0 → input (ore or smeltable item)
//   slot 1 → fuel (wood, coal-equivalent, etc.)
//   slot 2 → output (smelted result)
//
// Tick logic per furnace:
//   1. If fuelTicks > 0, decrement it (fuel is still burning).
//   2. If fuelTicks == 0 and input is smeltable and output can accept the result, consume one fuel
//      item to relight the furnace.
//   3. If the furnace is burning and input is smeltable and output can accept the result, increment
//      cookTicks.  When cookTicks reaches FURNACE_COOK_TIME, produce one output and reset cookTicks.
public final class FurnaceSystem implements GameSystem {

    @Override
    public void update(World world, float dt) {
        int[] furnaceEntities = world.query(BlockEntity.class, Furnace.class, Inventory.class);
        for (int id : furnaceEntities) {
            Entity entity = new Entity(id);
            Furnace  furnace = world.get(entity, Furnace.class).orElseThrow();
            Inventory inv    = world.get(entity, Inventory.class).orElseThrow();

            FurnaceState state = tick(furnace, inv);
            world.add(entity, state.furnace());
            world.add(entity, state.inventory());
        }
    }

    // Advances one simulation tick and returns the updated (Furnace, Inventory) pair.
    // Package-private and static to allow direct testing without a World.
    static FurnaceState tick(Furnace furnace, Inventory inv) {
        ItemStack input  = inv.slots()[0];
        ItemStack fuel   = inv.slots()[1];
        ItemStack output = inv.slots()[2];

        int fuelTicks    = furnace.fuelTicks();
        int fuelTicksMax = furnace.fuelTicksMax();
        int cookTicks    = furnace.cookTicks();

        // 1. Burn down current fuel.
        if (fuelTicks > 0) {
            fuelTicks--;
        }

        // 2. Try to light new fuel when current fuel is exhausted.
        if (fuelTicks == 0 && canSmelt(input, output)) {
            int burnTicks = SmeltingRecipes.fuelBurnTicks(fuel.itemId());
            if (burnTicks > 0) {
                fuelTicks    = burnTicks;
                fuelTicksMax = burnTicks;
                inv = consumeSlot(inv, 1);
                fuel = inv.slots()[1];
            }
        }

        // 3. Cook step: only advance when the furnace is burning.
        if (fuelTicks > 0 && canSmelt(input, output)) {
            cookTicks++;
            if (cookTicks >= WorldConstants.FURNACE_COOK_TIME) {
                cookTicks = 0;
                int resultId = SmeltingRecipes.smeltingResult(input.itemId());
                inv = consumeSlot(inv, 0);
                inv = addToOutput(inv, resultId);
            }
        } else if (fuelTicks == 0) {
            // No fuel: reset cook progress (furnace cools down).
            cookTicks = 0;
        }

        Furnace updated = new Furnace(cookTicks, fuelTicks, fuelTicksMax);
        return new FurnaceState(updated, inv);
    }

    // True when the input can be smelted and the output slot can accept the result.
    private static boolean canSmelt(ItemStack input, ItemStack output) {
        if (input.isEmpty()) return false;
        int resultId = SmeltingRecipes.smeltingResult(input.itemId());
        if (resultId == SmeltingRecipes.NO_RESULT) return false;
        return canAcceptOutput(output, resultId);
    }

    private static boolean canAcceptOutput(ItemStack output, int resultId) {
        if (output.isEmpty()) return true;
        if (output.itemId() != resultId) return false;
        return output.count() < WorldConstants.MAX_STACK;
    }

    private static Inventory consumeSlot(Inventory inv, int slotIndex) {
        ItemStack[] slots = inv.slots().clone();
        ItemStack current = slots[slotIndex];
        int left = current.count() - 1;
        slots[slotIndex] = left > 0
                ? new ItemStack(current.itemId(), left, current.durability())
                : ItemStack.EMPTY;
        return new Inventory(slots);
    }

    private static Inventory addToOutput(Inventory inv, int resultId) {
        ItemStack[] slots = inv.slots().clone();
        ItemStack output = slots[2];
        if (output.isEmpty()) {
            slots[2] = new ItemStack(resultId, 1);
        } else {
            slots[2] = new ItemStack(resultId, output.count() + 1);
        }
        return new Inventory(slots);
    }

    // Immutable result pair returned by tick().
    record FurnaceState(Furnace furnace, Inventory inventory) {}
}
