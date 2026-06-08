package org.example.systems;

import org.example.components.CameraComponent;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraSystemTest {

    private World world;
    private Entity player;
    private CameraSystem system;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
        world.add(player, new Position(0f, 0f, 0f));
        world.add(player, new Rotation(0f, 0f));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        system = new CameraSystem(16f / 9f);
    }

    @Test
    void producesRenderCameraComponent() {
        system.update(world, 0.016f);
        assertTrue(world.has(player, RenderCamera.class));
    }

    @Test
    void viewAndProjectionAreNonNull() {
        system.update(world, 0.016f);
        RenderCamera rc = world.get(player, RenderCamera.class).orElseThrow();
        assertNotNull(rc.view());
        assertNotNull(rc.projection());
    }

    @Test
    void entityWithoutCameraComponentIsIgnored() {
        Entity other = world.create();
        world.add(other, new Position(0f, 0f, 0f));
        world.add(other, new Rotation(0f, 0f));
        // No CameraComponent

        system.update(world, 0.016f);

        assertFalse(world.has(other, RenderCamera.class));
    }

    @Test
    void renderCameraUpdatesWhenPositionChanges() {
        system.update(world, 0.016f);
        RenderCamera before = world.get(player, RenderCamera.class).orElseThrow();

        world.add(player, new Position(10f, 5f, 3f));
        system.update(world, 0.016f);
        RenderCamera after = world.get(player, RenderCamera.class).orElseThrow();

        assertNotEquals(before.view(), after.view());
    }

    @Test
    void renderCameraUpdatesWhenRotationChanges() {
        system.update(world, 0.016f);
        RenderCamera before = world.get(player, RenderCamera.class).orElseThrow();

        world.add(player, new Rotation(45f, 20f));
        system.update(world, 0.016f);
        RenderCamera after = world.get(player, RenderCamera.class).orElseThrow();

        assertNotEquals(before.view(), after.view());
    }

    @Test
    void projectionUsesCorrectNearFarPlanes() {
        // Projection[2][2] encodes the near/far ratio — not identity
        system.update(world, 0.016f);
        RenderCamera rc = world.get(player, RenderCamera.class).orElseThrow();
        // For a non-degenerate perspective projection, m22 must be < 0 and ≠ 0
        assertTrue(rc.projection().m22() < 0f);
    }
}
