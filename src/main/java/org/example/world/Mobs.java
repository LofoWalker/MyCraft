package org.example.world;

import org.example.components.AiState;
import org.example.components.ColliderAABB;
import org.example.components.Gravity;
import org.example.components.Health;
import org.example.components.MobType;
import org.example.components.PathTarget;
import org.example.components.Position;
import org.example.components.Rotation;
import org.example.components.Velocity;
import org.example.ecs.Entity;
import org.example.ecs.World;

/**
 * Assembles the component set that constitutes a mob entity. Contains no
 * spawn-rule logic (biome eligibility, pack sizes, light limits) — those belong
 * in later steps.
 */
public final class Mobs {

    private Mobs() {}

    /**
     * Creates a new mob entity at (x, y, z) with all physics, collision, and AI
     * components pre-populated from the MobType's data.
     */
    public static Entity spawn(World world, MobType.Kind kind, float x, float y, float z) {
        Entity mob = world.create();

        MobType.Kind k = kind;
        world.add(mob, new MobType(k));
        world.add(mob, new Position(x, y, z));
        world.add(mob, new Velocity(0f, 0f, 0f));
        world.add(mob, new Gravity(WorldConstants.GRAVITY));
        world.add(mob, new ColliderAABB(k.width(), k.height(), k.width()));
        world.add(mob, new Rotation(0f, 0f));
        world.add(mob, new Health(k.maxHealth(), k.maxHealth()));
        world.add(mob, new AiState(AiState.Behaviour.IDLE, 0f));
        world.add(mob, new PathTarget(x, y, z));

        return mob;
    }
}
