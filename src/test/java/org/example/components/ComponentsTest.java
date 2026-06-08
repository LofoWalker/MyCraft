package org.example.components;

import org.example.ecs.Entity;
import org.example.ecs.World;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentsTest {

    private World world;
    private Entity player;

    @BeforeEach
    void setUp() {
        world  = new World();
        player = world.create();
    }

    @Test
    void positionAttachableAndReadable() {
        world.add(player, new Position(1f, 2f, 3f));
        Position p = world.get(player, Position.class).orElseThrow();
        assertEquals(1f, p.x());
        assertEquals(2f, p.y());
        assertEquals(3f, p.z());
    }

    @Test
    void velocityAttachableAndReadable() {
        world.add(player, new Velocity(0f, -9.81f, 0f));
        Velocity v = world.get(player, Velocity.class).orElseThrow();
        assertEquals(-9.81f, v.y(), 1e-4f);
    }

    @Test
    void rotationAttachableAndReadable() {
        world.add(player, new Rotation(1.57f, 0f));
        Rotation r = world.get(player, Rotation.class).orElseThrow();
        assertEquals(1.57f, r.yaw(), 1e-4f);
        assertEquals(0f, r.pitch());
    }

    @Test
    void cameraComponentAttachable() {
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        CameraComponent cam = world.get(player, CameraComponent.class).orElseThrow();
        assertEquals(70f, cam.fovDegrees());
        assertEquals(0.1f, cam.nearPlane(), 1e-5f);
        assertEquals(1000f, cam.farPlane());
    }

    @Test
    void playerInputAttachable() {
        world.add(player, new PlayerInput(true, false, false, false, false, false, 0.3f, -0.1f));
        PlayerInput input = world.get(player, PlayerInput.class).orElseThrow();
        assertTrue(input.forward());
        assertFalse(input.backward());
        assertEquals(0.3f, input.mouseDeltaX(), 1e-5f);
    }

    @Test
    void gravityAttachable() {
        world.add(player, new Gravity(9.81f));
        Gravity g = world.get(player, Gravity.class).orElseThrow();
        assertEquals(9.81f, g.acceleration(), 1e-4f);
    }

    @Test
    void colliderAABBAttachable() {
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));
        ColliderAABB col = world.get(player, ColliderAABB.class).orElseThrow();
        assertEquals(0.6f, col.width(), 1e-5f);
        assertEquals(1.8f, col.height(), 1e-5f);
        assertEquals(0.6f, col.depth(), 1e-5f);
    }

    @Test
    void chunkComponentAttachable() {
        Entity chunk = world.create();
        world.add(chunk, new ChunkComponent(3, -2));
        ChunkComponent cc = world.get(chunk, ChunkComponent.class).orElseThrow();
        assertEquals(3, cc.x());
        assertEquals(-2, cc.z());
    }

    @Test
    void voxelChunkDataReadWriteCycle() {
        Entity chunk = world.create();
        VoxelChunkData data = VoxelChunkData.empty();
        data.set(1, 0, 2, WorldConstants.BLOCK_GRASS);
        world.add(chunk, data);

        VoxelChunkData stored = world.get(chunk, VoxelChunkData.class).orElseThrow();
        assertEquals(WorldConstants.BLOCK_GRASS, stored.get(1, 0, 2));
        assertEquals(WorldConstants.BLOCK_AIR,   stored.get(0, 0, 0));
    }

    @Test
    void renderCameraAttachableAndReadable() {
        Matrix4f view = new Matrix4f().translation(1f, 2f, 3f);
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9f, 0.1f, 1000f);
        world.add(player, new RenderCamera(view, proj));

        RenderCamera rc = world.get(player, RenderCamera.class).orElseThrow();
        assertEquals(view, rc.view());
        assertEquals(proj, rc.projection());
    }

    @Test
    void renderCameraHoldsDistinctViewAndProjection() {
        Matrix4f view = new Matrix4f();
        Matrix4f proj = new Matrix4f().translation(5f, 0f, 0f);
        RenderCamera rc = new RenderCamera(view, proj);

        assertNotSame(rc.view(), rc.projection());
        assertEquals(0f, rc.view().m30());
        assertEquals(5f, rc.projection().m30());
    }

    @Test
    void renderCameraOverwriteUpdatesMatrices() {
        world.add(player, new RenderCamera(new Matrix4f(), new Matrix4f()));

        Matrix4f view2 = new Matrix4f().translation(9f, 0f, 0f);
        Matrix4f proj2 = new Matrix4f().translation(0f, 9f, 0f);
        world.add(player, new RenderCamera(view2, proj2));

        RenderCamera rc = world.get(player, RenderCamera.class).orElseThrow();
        assertEquals(9f, rc.view().m30(), 1e-5f);
        assertEquals(9f, rc.projection().m31(), 1e-5f);
    }

    @Test
    void allComponentsOnSameEntity() {
        world.add(player, new Position(0, 64, 0));
        world.add(player, new Velocity(0, 0, 0));
        world.add(player, new Rotation(0, 0));
        world.add(player, new CameraComponent(70f, 0.1f, 1000f));
        world.add(player, new PlayerInput(false, false, false, false, false, false, 0, 0));
        world.add(player, new Gravity(9.81f));
        world.add(player, new ColliderAABB(0.6f, 1.8f, 0.6f));

        assertTrue(world.has(player, Position.class));
        assertTrue(world.has(player, Velocity.class));
        assertTrue(world.has(player, Rotation.class));
        assertTrue(world.has(player, CameraComponent.class));
        assertTrue(world.has(player, PlayerInput.class));
        assertTrue(world.has(player, Gravity.class));
        assertTrue(world.has(player, ColliderAABB.class));
        assertEquals(1, world.query(Position.class, Velocity.class, Gravity.class).length);
    }
}
