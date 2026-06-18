package org.example.world;

// Tier of tool material. Higher miningLevel can harvest blocks that require it; speedMultiplier
// scales the damage-per-hit applied when breaking a block with the correct tool kind.
public enum ToolMaterial {
    WOOD    (1, 2.0f),
    STONE   (2, 4.0f),
    IRON    (3, 6.0f),
    GOLD    (1, 12.0f), // fast but low tier (cannot harvest iron-required blocks)
    DIAMOND (4, 8.0f);

    private final int   miningLevel;
    private final float speedMultiplier;

    ToolMaterial(int miningLevel, float speedMultiplier) {
        this.miningLevel     = miningLevel;
        this.speedMultiplier = speedMultiplier;
    }

    public int   miningLevel()     { return miningLevel; }
    public float speedMultiplier() { return speedMultiplier; }
}
