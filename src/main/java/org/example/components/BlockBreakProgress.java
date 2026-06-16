package org.example.components;

// Tracks how far the player has dug into the block they are currently breaking. Lives on the player
// entity; resets when the targeted voxel changes and clears once the block is destroyed.
public record BlockBreakProgress(int x, int y, int z, int damage) {

    public boolean targets(int wx, int wy, int wz) {
        return x == wx && y == wy && z == wz;
    }
}
