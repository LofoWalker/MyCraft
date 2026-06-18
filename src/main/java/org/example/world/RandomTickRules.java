package org.example.world;

/**
 * Pure random-tick rule table. Given a cell and its neighbourhood (block ids + skylight level),
 * returns the block id the cell should become — or the current id if no change is needed.
 *
 * <p>Designed to be testable in isolation: no ECS, no chunk system, no RNG here.
 * The caller (RandomTickSystem) provides the sampled cell and its context.
 *
 * <h3>Rules implemented</h3>
 * <ul>
 *   <li>Dirt → Grass: if the cell above is transparent (not solid) AND skylight ≥ threshold
 *       AND at least one horizontal neighbour is grass.</li>
 *   <li>Grass → Dirt: if the cell above is solid (covered, blocks skylight).</li>
 * </ul>
 */
public final class RandomTickRules {

    private RandomTickRules() {}

    /**
     * Evaluates a single cell and returns the block id it should become.
     *
     * @param currentId   block id of the cell being ticked
     * @param aboveId     block id of the cell directly above
     * @param skylight    skylight level at this cell (0..15)
     * @param hasGrassNeighbour true when at least one horizontal neighbour is BLOCK_GRASS
     * @return the new block id (may equal currentId when no change is needed)
     */
    public static byte evaluate(byte currentId, byte aboveId, int skylight,
                                boolean hasGrassNeighbour) {
        if (currentId == WorldConstants.BLOCK_DIRT) {
            return tryGrowGrass(aboveId, skylight, hasGrassNeighbour);
        }
        if (currentId == WorldConstants.BLOCK_GRASS) {
            return tryCoverGrass(aboveId);
        }
        return currentId;
    }

    private static byte tryGrowGrass(byte aboveId, int skylight, boolean hasGrassNeighbour) {
        boolean aboveIsOpen = !BlockType.byId(aboveId).solid();
        boolean wellLit     = skylight >= WorldConstants.GRASS_GROWTH_LIGHT_THRESHOLD;
        if (aboveIsOpen && wellLit && hasGrassNeighbour) {
            return WorldConstants.BLOCK_GRASS;
        }
        return WorldConstants.BLOCK_DIRT;
    }

    private static byte tryCoverGrass(byte aboveId) {
        if (BlockType.byId(aboveId).solid()) {
            return WorldConstants.BLOCK_DIRT;
        }
        return WorldConstants.BLOCK_GRASS;
    }
}
