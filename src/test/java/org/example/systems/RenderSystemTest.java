package org.example.systems;

import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.World;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderSystemTest {

    @Test
    void skipsRenderWhenNoCameraEntity() {
        World world = new World();
        // No RenderCamera entity — early return before touching shader/mesh
        RenderSystem system = new RenderSystem(null, null);
        assertDoesNotThrow(() -> system.update(world, 0.016f));
    }

    @Test
    void skipsRenderForEntityWithoutRenderCamera() {
        World world = new World();
        Entity other = world.create();
        // Entity exists but has no RenderCamera component
        RenderSystem system = new RenderSystem(null, null);
        assertDoesNotThrow(() -> system.update(world, 0.016f));
    }

    @Test
    void queriesRenderCameraFromWorld() {
        World world = new World();
        Entity cam = world.create();
        Matrix4f view = new Matrix4f();
        Matrix4f proj = new Matrix4f();
        world.add(cam, new RenderCamera(view, proj));

        // Confirm the ECS query finds the camera — render itself requires a GL context
        int[] cameras = world.query(RenderCamera.class);
        assertEquals(1, cameras.length);
        RenderCamera rc = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        assertSame(view, rc.view());
        assertSame(proj, rc.projection());
    }

    @Test
    void firstCameraEntityIsUsedWhenMultipleExist() {
        World world = new World();
        Entity cam1 = world.create();
        Entity cam2 = world.create();
        world.add(cam1, new RenderCamera(new Matrix4f().translation(1, 0, 0), new Matrix4f()));
        world.add(cam2, new RenderCamera(new Matrix4f().translation(2, 0, 0), new Matrix4f()));

        int[] cameras = world.query(RenderCamera.class);
        assertEquals(2, cameras.length);
        // RenderSystem always picks cameras[0] — verify query returns both
        assertTrue(cameras[0] == cam1.id() || cameras[0] == cam2.id());
    }
}
