package org.example.worldgen;

import org.example.world.WorldConstants;

// Biome enum: each value describes the ecological zone determined by temperature and humidity.
// Properties drive per-biome terrain amplitude, surface palette and tree density.
public enum Biome {

    PLAINS(
            WorldConstants.BIOME_PLAINS_AMPLITUDE_SCALE,
            0.0,
            WorldConstants.TREE_RARITY_PLAINS
    ),
    FOREST(
            WorldConstants.BIOME_FOREST_AMPLITUDE_SCALE,
            0.0,
            WorldConstants.TREE_RARITY_FOREST
    ),
    DESERT(
            WorldConstants.BIOME_DESERT_AMPLITUDE_SCALE,
            WorldConstants.BIOME_DESERT_BASE_OFFSET,
            WorldConstants.TREE_RARITY_NONE
    ),
    MOUNTAINS(
            WorldConstants.BIOME_MOUNTAIN_AMPLITUDE_SCALE,
            WorldConstants.BIOME_MOUNTAIN_BASE_OFFSET,
            WorldConstants.TREE_RARITY_NONE
    ),
    OCEAN(
            WorldConstants.BIOME_OCEAN_AMPLITUDE_SCALE,
            WorldConstants.BIOME_OCEAN_BASE_OFFSET,
            WorldConstants.TREE_RARITY_NONE
    );

    // Multiplier for the base terrain amplitude (higher = more rugged relief).
    private final double amplitudeScale;
    // Vertical offset added to the base terrain height (negative sinks terrain, positive lifts it).
    private final double baseOffset;
    // 1-in-N columns sprouts a tree; TREE_RARITY_NONE means no trees.
    private final int    treeRarity;

    Biome(double amplitudeScale, double baseOffset, int treeRarity) {
        this.amplitudeScale = amplitudeScale;
        this.baseOffset     = baseOffset;
        this.treeRarity     = treeRarity;
    }

    public double amplitudeScale() { return amplitudeScale; }
    public double baseOffset()     { return baseOffset; }
    public int    treeRarity()     { return treeRarity; }

    // Grass tint colour (RGB) used by the mesher to tint grass/leaf textures per biome.
    // Returns a new array each call; callers in the mesher hot path should cache it once per chunk.
    public float[] grassTint() {
        return switch (this) {
            case PLAINS    -> new float[]{ 0.55f, 0.78f, 0.30f };
            case FOREST    -> new float[]{ 0.28f, 0.62f, 0.22f };
            case DESERT    -> new float[]{ 0.80f, 0.70f, 0.35f }; // yellowish, rarely used (no grass)
            case MOUNTAINS -> new float[]{ 0.60f, 0.70f, 0.45f }; // rocky-green
            case OCEAN     -> new float[]{ 0.30f, 0.65f, 0.50f }; // aqua tinge
        };
    }
}
