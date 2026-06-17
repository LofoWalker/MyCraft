package org.example.world;

import org.example.components.ColliderAABB;
import org.example.components.Position;

// Pure overlap test between a player's AABB and a unit voxel cell [c, c+1] on each axis. Used to
// veto placing a block inside the player (which would trap them). The player box is centred on its
// position horizontally (half width/depth) and spans [y, y + height] vertically — matching the
// convention used by CollisionSystem.
public final class AABBCell {

    private static final int CELL_SIZE = 1;

    private AABBCell() {}

    public static boolean playerOverlapsCell(Position pos, ColliderAABB box,
                                             int cellX, int cellY, int cellZ) {
        float hw = box.width() / 2f;
        float hd = box.depth() / 2f;

        float minX = pos.x() - hw, maxX = pos.x() + hw;
        float minY = pos.y(),      maxY = pos.y() + box.height();
        float minZ = pos.z() - hd, maxZ = pos.z() + hd;

        return overlaps(minX, maxX, cellX)
            && overlaps(minY, maxY, cellY)
            && overlaps(minZ, maxZ, cellZ);
    }

    // Half-open overlap: cell occupies [c, c+1). Touching exactly at a face (max == c or min == c+1)
    // does not count as overlap, so a block can sit flush against the player without being vetoed.
    private static boolean overlaps(float min, float max, int cell) {
        return max > cell && min < cell + CELL_SIZE;
    }
}
