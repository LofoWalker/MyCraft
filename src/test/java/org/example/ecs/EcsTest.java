package org.example.ecs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EcsTest {

    record Position(float x, float y, float z) {}
    record Velocity(float vx, float vy, float vz) {}
    record Tag() {}

    // ── Création / query ──────────────────────────────────────────────────────

    @Test
    void createNEntitiesAndQuery() {
        World world = new World();
        int n = 200;

        for (int i = 0; i < n; i++) {
            Entity e = world.create();
            world.add(e, new Position(i, 0, 0));
            if (i % 2 == 0) world.add(e, new Velocity(1, 0, 0));
        }

        assertEquals(n,     world.query(Position.class).length);
        assertEquals(n / 2, world.query(Velocity.class).length);
        assertEquals(n / 2, world.query(Position.class, Velocity.class).length);
    }

    @Test
    void queryWithNoMatchingStore() {
        World world = new World();
        assertEquals(0, world.query(Tag.class).length);
    }

    // ── Ajout / suppression de composants ────────────────────────────────────

    @Test
    void addThenRemoveComponent() {
        World world = new World();
        Entity e = world.create();
        world.add(e, new Position(1, 2, 3));

        assertTrue(world.has(e, Position.class));
        assertEquals(1f, world.get(e, Position.class).orElseThrow().x());

        world.remove(e, Position.class);

        assertFalse(world.has(e, Position.class));
        assertTrue(world.get(e, Position.class).isEmpty());
    }

    @Test
    void updateComponentInPlace() {
        World world = new World();
        Entity e = world.create();
        world.add(e, new Position(0, 0, 0));
        world.add(e, new Position(5, 0, 0)); // overwrite
        assertEquals(5f, world.get(e, Position.class).orElseThrow().x());
    }

    @Test
    void removeFromMiddleOfDenseArray() {
        World world = new World();
        Entity[] entities = new Entity[5];
        for (int i = 0; i < 5; i++) {
            entities[i] = world.create();
            world.add(entities[i], new Position(i, 0, 0));
        }
        world.remove(entities[2], Position.class);
        assertEquals(4, world.query(Position.class).length);
        assertFalse(world.has(entities[2], Position.class));
        // remaining entities still readable
        for (int i = 0; i < 5; i++) {
            if (i != 2) assertTrue(world.has(entities[i], Position.class));
        }
    }

    // ── Destroy & recyclage d'IDs ────────────────────────────────────────────

    @Test
    void destroyEntityClearsComponents() {
        World world = new World();
        Entity e = world.create();
        world.add(e, new Position(1, 2, 3));
        world.add(e, new Velocity(1, 0, 0));

        world.destroy(e);

        assertFalse(world.has(e, Position.class));
        assertFalse(world.has(e, Velocity.class));
    }

    @Test
    void entityIdRecycling() {
        World world = new World();
        Entity e1 = world.create();
        int id1 = e1.id();
        world.destroy(e1);
        Entity e2 = world.create();
        assertEquals(id1, e2.id());
    }

    // ── SystemScheduler ───────────────────────────────────────────────────────

    @Test
    void systemSchedulerRunsInOrder() {
        World world = new World();
        int[] order = {0};

        SystemScheduler scheduler = new SystemScheduler();
        scheduler.add((w, dt) -> assertEquals(0, order[0]++));
        scheduler.add((w, dt) -> assertEquals(1, order[0]++));
        scheduler.add((w, dt) -> assertEquals(2, order[0]++));

        scheduler.update(world, 0.016f);
        assertEquals(3, order[0]);
    }

    @Test
    void movementSystemIntegration() {
        World world = new World();
        Entity e = world.create();
        world.add(e, new Position(0, 0, 0));
        world.add(e, new Velocity(3, 0, 0));

        GameSystem moveSystem = (w, dt) -> {
            for (int eid : w.query(Position.class, Velocity.class)) {
                Entity entity = new Entity(eid);
                Position p = w.get(entity, Position.class).orElseThrow();
                Velocity v = w.get(entity, Velocity.class).orElseThrow();
                w.add(entity, new Position(p.x() + v.vx() * dt, p.y() + v.vy() * dt, p.z() + v.vz() * dt));
            }
        };

        SystemScheduler scheduler = new SystemScheduler();
        scheduler.add(moveSystem);
        scheduler.update(world, 1.0f);

        assertEquals(3.0f, world.get(e, Position.class).orElseThrow().x(), 1e-6f);
    }
}
