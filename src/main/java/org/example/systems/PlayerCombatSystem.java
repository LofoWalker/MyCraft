package org.example.systems;

import org.example.components.DamageImmunity;
import org.example.components.Health;
import org.example.components.Hotbar;
import org.example.components.Inventory;
import org.example.components.InventoryScreen;
import org.example.components.MobType;
import org.example.components.PlayerInput;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.Combat;
import org.example.world.WorldConstants;

/**
 * Player melee combat: a fresh left-click strikes the nearest mob in front of the player within
 * attack range, dealing weapon-scaled damage plus knockback. Invincibility frames (shared with the
 * mob's damage clock) prevent a single click — and back-to-back clicks — from draining the mob.
 *
 * <p>Runs in the simulation scheduler before BlockInteractionSystem: when the click lands on a mob
 * the strike is applied here; clicks that hit no mob fall through to block breaking.
 */
public final class PlayerCombatSystem implements GameSystem {

    private boolean attackHeldPreviously;

    @Override
    public void update(World world, float dt) {
        int[] players = world.query(PlayerInput.class, Position.class, Rotation.class);
        if (players.length == 0) return;

        Entity player = new Entity(players[0]);
        if (world.has(player, InventoryScreen.class)) {
            attackHeldPreviously = false;
            return;
        }

        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        boolean attackNow  = input.breakBlock();
        boolean justClicked = attackNow && !attackHeldPreviously;
        attackHeldPreviously = attackNow;
        if (!justClicked) return;

        Position pos = world.get(player, Position.class).orElseThrow();
        Rotation rot = world.get(player, Rotation.class).orElseThrow();

        Entity target = nearestMobInFront(world, pos, rot);
        if (target == null) return;

        applyHit(world, target, Combat.weaponDamage(heldItemId(world, player)), pos.x(), pos.z());
    }

    // Applies damage + knockback to a mob, unless it is still in its post-hit invincibility window.
    // Package-private and static for direct unit testing.
    static void applyHit(World world, Entity mob, int damage, float attackerX, float attackerZ) {
        float immune = world.get(mob, DamageImmunity.class).map(DamageImmunity::seconds).orElse(0f);
        if (immune > 0f) return;

        Health health = world.get(mob, Health.class).orElse(null);
        if (health == null) return;
        world.add(mob, new Health(health.current() - damage, health.max()));
        world.add(mob, new DamageImmunity(WorldConstants.DAMAGE_IMMUNITY_SECONDS));

        world.get(mob, Position.class).ifPresent(p ->
                world.add(mob, Combat.knockback(attackerX, attackerZ, p.x(), p.z())));
    }

    private static Entity nearestMobInFront(World world, Position pos, Rotation rot) {
        float yaw   = (float) Math.toRadians(rot.yaw());
        float pitch = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitch);
        float fx = (float)  (Math.sin(yaw) * cosPitch);
        float fy = (float)   Math.sin(pitch);
        float fz = (float) (-Math.cos(yaw) * cosPitch);

        float eyeX = pos.x();
        float eyeY = pos.y() + WorldConstants.PLAYER_EYE_HEIGHT;
        float eyeZ = pos.z();

        Entity best = null;
        float  bestDist = WorldConstants.PLAYER_ATTACK_RANGE;
        for (int eid : world.query(MobType.class, Position.class, Health.class)) {
            Entity mob = new Entity(eid);
            Position mp = world.get(mob, Position.class).orElseThrow();
            float dx = mp.x() - eyeX;
            float dy = (mp.y() + WorldConstants.MOB_TORSO_HEIGHT) - eyeY;
            float dz = mp.z() - eyeZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > WorldConstants.PLAYER_ATTACK_RANGE || dist < 1e-4f) continue;
            float dot = (dx * fx + dy * fy + dz * fz) / dist;
            if (dot < WorldConstants.PLAYER_ATTACK_AIM_DOT) continue;
            if (dist < bestDist) { bestDist = dist; best = mob; }
        }
        return best;
    }

    private static int heldItemId(World world, Entity player) {
        int slot = world.get(player, Hotbar.class).map(Hotbar::selectedSlot).orElse(0);
        return world.get(player, Inventory.class).map(inv -> inv.slots()[slot].itemId()).orElse(0);
    }
}
