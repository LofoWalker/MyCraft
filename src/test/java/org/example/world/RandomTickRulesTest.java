package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RandomTickRules}: pure rule table, no ECS, no chunk system.
 */
class RandomTickRulesTest {

    private static final int FULL_LIGHT = WorldConstants.MAX_LIGHT_LEVEL;
    private static final int GOOD_LIGHT = WorldConstants.GRASS_GROWTH_LIGHT_THRESHOLD;
    private static final int LOW_LIGHT  = WorldConstants.GRASS_GROWTH_LIGHT_THRESHOLD - 1;

    // -----------------------------------------------------------------------
    // Dirt → Grass
    // -----------------------------------------------------------------------

    @Test
    void litDirtAdjacentToGrassBecomeGrass() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_DIRT,
                WorldConstants.BLOCK_AIR,  // above is open
                GOOD_LIGHT,
                true                       // has grass neighbour
        );
        assertEquals(WorldConstants.BLOCK_GRASS, result,
                "Dirt with open top, sufficient light, and grass neighbour should become grass");
    }

    @Test
    void dirtWithoutLightDoesNotGrow() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_DIRT,
                WorldConstants.BLOCK_AIR,
                LOW_LIGHT,
                true
        );
        assertEquals(WorldConstants.BLOCK_DIRT, result,
                "Dirt must not grow grass without enough light");
    }

    @Test
    void dirtWithoutGrassNeighbourDoesNotGrow() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_DIRT,
                WorldConstants.BLOCK_AIR,
                FULL_LIGHT,
                false   // no grass neighbour
        );
        assertEquals(WorldConstants.BLOCK_DIRT, result,
                "Dirt must not grow grass without a neighbouring grass block");
    }

    @Test
    void coveredDirtDoesNotGrow() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_DIRT,
                WorldConstants.BLOCK_STONE,  // above is solid
                FULL_LIGHT,
                true
        );
        assertEquals(WorldConstants.BLOCK_DIRT, result,
                "Dirt covered by a solid block must not grow grass");
    }

    // -----------------------------------------------------------------------
    // Grass → Dirt
    // -----------------------------------------------------------------------

    @Test
    void coveredGrassRevertsToDir() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_GRASS,
                WorldConstants.BLOCK_STONE,  // solid cover
                FULL_LIGHT,
                false
        );
        assertEquals(WorldConstants.BLOCK_DIRT, result,
                "Grass covered by a solid block should revert to dirt");
    }

    @Test
    void uncoveredGrassStaysGrass() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_GRASS,
                WorldConstants.BLOCK_AIR,
                FULL_LIGHT,
                false
        );
        assertEquals(WorldConstants.BLOCK_GRASS, result,
                "Uncovered grass must stay as grass");
    }

    // -----------------------------------------------------------------------
    // Unrelated blocks are unchanged
    // -----------------------------------------------------------------------

    @Test
    void stoneIsUnaffectedByRandomTick() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_STONE,
                WorldConstants.BLOCK_AIR,
                FULL_LIGHT,
                true
        );
        assertEquals(WorldConstants.BLOCK_STONE, result,
                "Stone must not be altered by random tick");
    }

    @Test
    void sandIsUnaffectedByRandomTick() {
        byte result = RandomTickRules.evaluate(
                WorldConstants.BLOCK_SAND,
                WorldConstants.BLOCK_AIR,
                FULL_LIGHT,
                true
        );
        assertEquals(WorldConstants.BLOCK_SAND, result,
                "Sand must not be altered by random tick (gravity is a separate system)");
    }
}
