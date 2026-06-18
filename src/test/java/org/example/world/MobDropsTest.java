package org.example.world;

import org.example.components.MobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class MobDropsTest {

    @Test
    void cowDropsLeatherAndBeef() {
        int[] drops = MobDrops.dropIds(MobType.Kind.COW);
        assertEquals(2, drops.length);
        assertContains(drops, WorldConstants.ITEM_LEATHER);
        assertContains(drops, WorldConstants.ITEM_BEEF);
    }

    @Test
    void pigDropsPork() {
        int[] drops = MobDrops.dropIds(MobType.Kind.PIG);
        assertEquals(1, drops.length);
        assertEquals(WorldConstants.ITEM_PORK, drops[0]);
    }

    @Test
    void sheepDropsWool() {
        int[] drops = MobDrops.dropIds(MobType.Kind.SHEEP);
        assertEquals(1, drops.length);
        assertEquals(WorldConstants.ITEM_WOOL, drops[0]);
    }

    @Test
    void chickenDropsFeather() {
        int[] drops = MobDrops.dropIds(MobType.Kind.CHICKEN);
        assertEquals(1, drops.length);
        assertEquals(WorldConstants.ITEM_FEATHER, drops[0]);
    }

    @Test
    void zombieDropsNothing() {
        int[] drops = MobDrops.dropIds(MobType.Kind.ZOMBIE);
        assertEquals(0, drops.length);
    }

    @ParameterizedTest
    @EnumSource(MobType.Kind.class)
    void dropIdsAreNeverNull(MobType.Kind kind) {
        assertNotNull(MobDrops.dropIds(kind));
    }

    @ParameterizedTest
    @EnumSource(value = MobType.Kind.class, names = {"COW", "PIG", "SHEEP", "CHICKEN"})
    void passiveMobsHaveAtLeastOneDrop(MobType.Kind kind) {
        assertTrue(MobDrops.dropIds(kind).length >= 1,
                   kind + " should have at least one drop");
    }

    // --- helper ---

    private static void assertContains(int[] arr, int value) {
        for (int v : arr) {
            if (v == value) return;
        }
        fail("Expected array to contain " + value);
    }
}
