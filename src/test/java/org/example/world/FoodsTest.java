package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FoodsTest {

    @Test
    void appleAndBreadAreRegisteredFoods() {
        assertTrue(Foods.isFood(WorldConstants.ITEM_APPLE));
        assertTrue(Foods.isFood(WorldConstants.ITEM_BREAD));
    }

    @Test
    void blocksAreNotFoods() {
        assertFalse(Foods.isFood(WorldConstants.BLOCK_STONE));
        assertFalse(Foods.isFood(WorldConstants.BLOCK_AIR));
        assertFalse(Foods.isFood(WorldConstants.BLOCK_TORCH));
    }

    @Test
    void foodIdsLiveOutsideTheBlockRange() {
        assertTrue(WorldConstants.ITEM_APPLE > WorldConstants.MAX_BLOCK_ID);
        assertTrue(WorldConstants.ITEM_BREAD > WorldConstants.MAX_BLOCK_ID);
    }

    @Test
    void byIdReturnsRestoreValues() {
        Foods.Food apple = Foods.byId(WorldConstants.ITEM_APPLE);
        assertEquals(WorldConstants.ITEM_APPLE, apple.itemId());
        assertTrue(apple.foodRestore() > 0);
        assertTrue(apple.saturationRestore() > 0f);
    }

    @Test
    void byIdRejectsNonFood() {
        assertThrows(IllegalArgumentException.class, () -> Foods.byId(WorldConstants.BLOCK_STONE));
    }
}
