package org.example.systems;

import org.example.components.DamageImmunity;
import org.example.components.Health;
import org.example.components.Position;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.Combat;
import org.example.world.ItemRegistry;
import org.example.world.WorldConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Combat maths and the i-frame-gated melee hit application. */
class CombatTest {

    @Test
    void swordDealsMoreDamageThanFist() {
        int fist  = Combat.weaponDamage(0); // empty hand
        int sword = Combat.weaponDamage(ItemRegistry.SWORD_DIAMOND);
        assertTrue(sword > fist, "a diamond sword must out-damage a bare fist");
        assertEquals(WorldConstants.PLAYER_FIST_DAMAGE, fist);
    }

    @Test
    void knockbackPushesAwayFromAttacker() {
        Velocity kb = Combat.knockback(0f, 0f, 1f, 0f); // attacker at origin, target at +x
        assertTrue(kb.x() > 0f, "target is pushed along +x, away from the attacker");
        assertEquals(WorldConstants.MOB_KNOCKBACK_VERTICAL, kb.y(), 1e-4, "fixed upward pop");
    }

    @Test
    void iFramesBlockADoubleHit() {
        World world = new World();
        Entity mob = world.create();
        world.add(mob, new Health(20, 20));
        world.add(mob, new Position(5f, 64f, 5f));

        PlayerCombatSystem.applyHit(world, mob, 6, 0f, 0f);
        int afterFirst = world.get(mob, Health.class).orElseThrow().current();
        assertEquals(14, afterFirst, "first hit deals damage");
        assertTrue(world.get(mob, DamageImmunity.class).orElseThrow().seconds() > 0f, "i-frames set");
        assertTrue(world.has(mob, Velocity.class), "knockback velocity applied");

        PlayerCombatSystem.applyHit(world, mob, 6, 0f, 0f);
        int afterSecond = world.get(mob, Health.class).orElseThrow().current();
        assertEquals(afterFirst, afterSecond, "second hit during i-frames deals no damage");
    }
}
