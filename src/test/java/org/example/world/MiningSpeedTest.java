package org.example.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiningSpeedTest {

    @Test
    void correctPickaxeDealsMuchMoreDamageToStoneThanBareHands() {
        int bareHands   = ItemRegistry.damagePerHit(BlockType.STONE, WorldConstants.BLOCK_AIR);
        int withPickaxe = ItemRegistry.damagePerHit(BlockType.STONE, ItemRegistry.PICKAXE_IRON);
        assertTrue(withPickaxe > bareHands,
                "Iron pickaxe should deal more damage to stone than bare hands");
    }

    @Test
    void woodPickaxeDealsMoreDamageThanBareHandsOnStone() {
        int bareHands    = ItemRegistry.damagePerHit(BlockType.STONE, WorldConstants.BLOCK_AIR);
        int woodPickaxe  = ItemRegistry.damagePerHit(BlockType.STONE, ItemRegistry.PICKAXE_WOOD);
        assertTrue(woodPickaxe > bareHands);
    }

    @Test
    void diamondPickaxeFasterThanIronPickaxeOnStone() {
        int iron    = ItemRegistry.damagePerHit(BlockType.STONE, ItemRegistry.PICKAXE_IRON);
        int diamond = ItemRegistry.damagePerHit(BlockType.STONE, ItemRegistry.PICKAXE_DIAMOND);
        assertTrue(diamond >= iron, "Diamond pickaxe should be at least as fast as iron");
    }

    @Test
    void wrongToolKindGivesBareHandDamage() {
        // A shovel is the wrong kind for stone (which needs a pickaxe).
        int shovel   = ItemRegistry.damagePerHit(BlockType.STONE, ItemRegistry.SHOVEL_IRON);
        int bareHand = WorldConstants.BARE_HAND_DAMAGE;
        assertEquals(bareHand, shovel,
                "Using a shovel on stone should give bare-hand damage");
    }

    @Test
    void bareHandsDamageIsAlwaysAtLeastOne() {
        assertTrue(WorldConstants.BARE_HAND_DAMAGE >= 1);
    }

    @Test
    void correctAxeFasterOnWoodThanBareHands() {
        int bareHands = ItemRegistry.damagePerHit(BlockType.WOOD, WorldConstants.BLOCK_AIR);
        int axe       = ItemRegistry.damagePerHit(BlockType.WOOD, ItemRegistry.AXE_STONE);
        assertTrue(axe > bareHands);
    }

    @Test
    void correctShovelFasterOnDirtThanBareHands() {
        int bareHands = ItemRegistry.damagePerHit(BlockType.DIRT, WorldConstants.BLOCK_AIR);
        int shovel    = ItemRegistry.damagePerHit(BlockType.DIRT, ItemRegistry.SHOVEL_IRON);
        assertTrue(shovel > bareHands);
    }

    @Test
    void hittingAirWithToolGivesOneHit() {
        // AIR has no effective tool (NONE); correct kind check fails, falls back to bare-hand damage.
        int damage = ItemRegistry.damagePerHit(BlockType.AIR, ItemRegistry.PICKAXE_IRON);
        assertEquals(WorldConstants.BARE_HAND_DAMAGE, damage);
    }
}
