package org.example.world;

import org.example.components.ColliderAABB;
import org.example.components.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AABBCellTest {

    private static final ColliderAABB PLAYER_BOX = new ColliderAABB(0.6f, 1.8f, 0.6f);

    @Test
    void cellInsidePlayerFootprintOverlaps() {
        Position pos = new Position(5.5f, 60f, 5.5f); // feet at y=60, head at 61.8
        assertTrue(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 5, 60, 5));
        assertTrue(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 5, 61, 5));
    }

    @Test
    void cellBesidePlayerDoesNotOverlap() {
        Position pos = new Position(5.5f, 60f, 5.5f);
        assertFalse(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 6, 60, 5));
        assertFalse(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 4, 60, 5));
    }

    @Test
    void cellAbovePlayerHeadDoesNotOverlap() {
        Position pos = new Position(5.5f, 60f, 5.5f); // head reaches 61.8, so cell at y=62 is clear
        assertFalse(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 5, 62, 5));
    }

    @Test
    void cellBelowPlayerFeetDoesNotOverlap() {
        Position pos = new Position(5.5f, 60f, 5.5f);
        assertFalse(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 5, 59, 5));
    }

    @Test
    void cellFlushAgainstFaceDoesNotOverlap() {
        // Feet exactly on a cell boundary: the cell directly below (max == cellTop) is only touching.
        Position pos = new Position(5.5f, 60f, 5.5f);
        assertFalse(AABBCell.playerOverlapsCell(pos, PLAYER_BOX, 5, 59, 5));
    }
}
