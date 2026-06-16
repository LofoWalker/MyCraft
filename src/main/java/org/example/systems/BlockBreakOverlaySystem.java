package org.example.systems;

import org.example.components.BlockBreakProgress;
import org.example.components.ChunkComponent;
import org.example.components.RenderCamera;
import org.example.components.VoxelChunkData;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.example.world.BlockType;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

// Breaking feedback: draws a dark cube over the block the player is mining, growing more opaque as
// the block takes damage. Reads BlockBreakProgress (sim) plus the block's hardness, and runs after
// the opaque world pass so it blends on top. Reuses one cube mesh; no per-frame allocation.
public final class BlockBreakOverlaySystem implements GameSystem, AutoCloseable {

    private static final float MIN_ALPHA     = 0.15f;
    private static final float MAX_ALPHA     = 0.75f;
    // Slightly larger than the block so its faces sit just outside the block's and win the depth test.
    private static final float OVERLAY_SCALE = 1.02f;

    private final Shader   shader;
    private final Mesh     cube;
    private final Matrix4f model = new Matrix4f();

    public BlockBreakOverlaySystem() {
        this.shader = Shader.fromResources("/shaders/highlight.vert", "/shaders/highlight.frag");
        this.cube   = Mesh.createTestCube();
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        int[] players = world.query(BlockBreakProgress.class);
        if (cameras.length == 0 || players.length == 0) return;

        RenderCamera       camera   = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        BlockBreakProgress progress = world.get(new Entity(players[0]), BlockBreakProgress.class).orElseThrow();
        int   hardness = hardnessAt(world, progress.x(), progress.y(), progress.z());
        float fraction = breakFraction(progress.damage(), hardness);

        drawOverlay(camera, progress, fraction);
    }

    private void drawOverlay(RenderCamera camera, BlockBreakProgress progress, float fraction) {
        model.translation(progress.x() + 0.5f, progress.y() + 0.5f, progress.z() + 0.5f).scale(OVERLAY_SCALE);

        // Blended, depth-tested but non-writing: tints the block without disturbing the depth buffer.
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        shader.bind();
        shader.setUniformMatrix4f("uModel",      model);
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());
        shader.setUniform3f("uColor", 0.05f, 0.05f, 0.05f);
        shader.setUniform1f("uAlpha", overlayAlpha(fraction));
        cube.draw();
        shader.unbind();

        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    private static int hardnessAt(World world, int wx, int wy, int wz) {
        int s  = WorldConstants.CHUNK_SIZE_XZ;
        int cx = Math.floorDiv(wx, s);
        int cz = Math.floorDiv(wz, s);
        for (int eid : world.query(ChunkComponent.class, VoxelChunkData.class)) {
            Entity         entity = new Entity(eid);
            ChunkComponent chunk  = world.get(entity, ChunkComponent.class).orElseThrow();
            if (chunk.x() == cx && chunk.z() == cz) {
                byte block = world.get(entity, VoxelChunkData.class).orElseThrow().get(wx - cx * s, wy, wz - cz * s);
                return BlockType.byId(block).hardness();
            }
        }
        return 0;
    }

    static float breakFraction(int damage, int hardness) {
        if (hardness <= 0) return 1f;
        return Math.min(1f, (float) damage / (float) hardness);
    }

    static float overlayAlpha(float fraction) {
        return MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * fraction;
    }

    @Override
    public void close() {
        cube.close();
        shader.close();
    }
}
