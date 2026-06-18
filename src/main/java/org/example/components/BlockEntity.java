package org.example.components;

// Tags an ECS entity as a block-entity at a specific world position.
// The block type at (wx, wy, wz) determines which other components are expected:
//   BLOCK_FURNACE → also carries Furnace + Inventory (3 slots: input/fuel/output)
//   BLOCK_CHEST   → also carries Inventory (WorldConstants.CHEST_SLOTS slots)
// Lookup by world position is edge-triggered (player interaction only) so a
// Map<Long, Integer> position→entityId is acceptable here and not a hot path.
public record BlockEntity(int wx, int wy, int wz) {}
