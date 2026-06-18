package org.example.world;

import org.example.components.MobType;

/**
 * Pure, stateless drop table: maps each MobType.Kind to the items that fall when it dies.
 * No game state; every method is deterministic and fully testable without a World.
 */
public final class MobDrops {

    private MobDrops() {}

    /**
     * Returns the item ids that the given mob kind drops on death.
     * Callers spawn one ItemEntity per entry.
     */
    public static int[] dropIds(MobType.Kind kind) {
        return switch (kind) {
            case COW     -> new int[]{ WorldConstants.ITEM_LEATHER, WorldConstants.ITEM_BEEF };
            case PIG     -> new int[]{ WorldConstants.ITEM_PORK };
            case SHEEP   -> new int[]{ WorldConstants.ITEM_WOOL };
            case CHICKEN -> new int[]{ WorldConstants.ITEM_FEATHER };
            // Hostile mobs drop nothing yet (bones/string/gunpowder items are a future step).
            case ZOMBIE, SKELETON, SPIDER, CREEPER -> new int[0];
        };
    }
}
