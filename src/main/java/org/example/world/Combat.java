package org.example.world;

import org.example.components.MobType;
import org.example.components.Velocity;

/**
 * Pure combat maths: melee weapon damage, knockback impulses and the daylight-burn rule for
 * undead mobs. No World, no GL — fully unit-testable.
 */
public final class Combat {

    private Combat() {}

    /**
     * Damage dealt by a left-click melee hit while holding the given item.
     * Swords deal tier-scaled damage; anything else (or an empty hand) deals fist damage.
     */
    public static int weaponDamage(int heldItemId) {
        if (ItemRegistry.isTool(heldItemId) && ItemRegistry.toolKind(heldItemId) == ToolKind.SWORD) {
            return swordDamage(ItemRegistry.toolMaterial(heldItemId));
        }
        return WorldConstants.PLAYER_FIST_DAMAGE;
    }

    private static int swordDamage(ToolMaterial material) {
        return switch (material) {
            case WOOD    -> WorldConstants.SWORD_DAMAGE_WOOD;
            case STONE   -> WorldConstants.SWORD_DAMAGE_STONE;
            case IRON    -> WorldConstants.SWORD_DAMAGE_IRON;
            case GOLD    -> WorldConstants.SWORD_DAMAGE_GOLD;
            case DIAMOND -> WorldConstants.SWORD_DAMAGE_DIAMOND;
        };
    }

    /**
     * Velocity imparted to a target knocked back from (fromX, fromZ): a horizontal push directly
     * away from the attacker plus a fixed upward pop. Replaces the target's current velocity.
     */
    public static Velocity knockback(float fromX, float fromZ, float targetX, float targetZ) {
        float dx = targetX - fromX;
        float dz = targetZ - fromZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 1e-4f) {       // attacker and target coincide: push along +x by convention
            dx = 1f; dz = 0f; dist = 1f;
        }
        float speed = WorldConstants.MOB_KNOCKBACK_SPEED;
        return new Velocity(dx / dist * speed, WorldConstants.MOB_KNOCKBACK_VERTICAL, dz / dist * speed);
    }

    /** Undead (zombie/skeleton) exposed to the open sky during the day catch fire. */
    public static boolean undeadBurns(MobType.Kind kind, float dayFraction, boolean skyExposed) {
        boolean undead = kind == MobType.Kind.ZOMBIE || kind == MobType.Kind.SKELETON;
        boolean daytime = dayFraction < WorldConstants.UNDEAD_BURN_DAY_MAX;
        return undead && daytime && skyExposed;
    }
}
