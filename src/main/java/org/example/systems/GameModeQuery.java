package org.example.systems;

import org.example.components.GameMode;
import org.example.ecs.Entity;
import org.example.ecs.World;

/**
 * Single-point helpers for querying the current {@link GameMode} of a given entity.
 *
 * <p>Each game system that needs to branch on mode imports exactly these two methods so the
 * creative/survival distinction is never duplicated across systems.
 */
public final class GameModeQuery {

    private GameModeQuery() {}

    /** Returns true when the entity carries a {@link GameMode} set to CREATIVE. */
    public static boolean isCreative(World world, Entity entity) {
        return world.get(entity, GameMode.class)
                .map(GameMode::isCreative)
                .orElse(false);
    }

    /** Returns true when the entity has no {@link GameMode} component or is set to SURVIVAL. */
    public static boolean isSurvival(World world, Entity entity) {
        return !isCreative(world, entity);
    }
}
