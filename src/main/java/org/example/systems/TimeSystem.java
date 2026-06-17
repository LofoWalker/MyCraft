package org.example.systems;

import org.example.components.TimeOfDay;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.world.WorldConstants;

// Advances the day/night clock during simulation. Stateless: it reads the singleton TimeOfDay and
// writes the wrapped, advanced value back. Rendering systems only read TimeOfDay, never write it.
public final class TimeSystem implements GameSystem {

    private static final float FULL_CIRCLE = (float) (2.0 * Math.PI);

    @Override
    public void update(World world, float dt) {
        int[] clocks = world.query(TimeOfDay.class);
        if (clocks.length == 0) return;

        Entity clock = new Entity(clocks[0]);
        float current = world.get(clock, TimeOfDay.class).orElseThrow().dayFraction();
        world.add(clock, new TimeOfDay(advance(current, dt)));
    }

    // Pure: advance the fraction by dt / DAY_LENGTH_SECONDS and wrap back into [0, 1).
    public static float advance(float dayFraction, float dt) {
        float next = dayFraction + dt / WorldConstants.DAY_LENGTH_SECONDS;
        return wrap01(next);
    }

    private static float wrap01(float value) {
        float wrapped = value % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    // Sun height in [-1, 1] from dayFraction: 0 at dawn (0.0) and dusk (0.5), +1 at noon (0.25),
    // -1 at midnight (0.75). This is the geometry the sky and the global light factor both share.
    public static float sunHeight(float dayFraction) {
        return (float) Math.sin(dayFraction * FULL_CIRCLE);
    }

    // Pure global skylight factor in [NIGHT_LIGHT, 1]: maximal at noon, floored at NIGHT_LIGHT through
    // the whole night, and monotonic across dawn and dusk. Multiplied into chunk brightness so the
    // world darkens at night yet never goes pitch black (torches/interiors stay visible).
    public static float globalLightFactor(float dayFraction) {
        float margin = WorldConstants.DAY_LIGHT_HORIZON_MARGIN;
        float daylight = (sunHeight(dayFraction) + margin) / (1.0f + margin);
        daylight = Math.max(0.0f, Math.min(1.0f, daylight));
        return WorldConstants.NIGHT_LIGHT + (1.0f - WorldConstants.NIGHT_LIGHT) * daylight;
    }
}
