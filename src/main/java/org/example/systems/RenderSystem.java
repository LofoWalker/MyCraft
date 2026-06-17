package org.example.systems;

import org.example.components.ChunkMeshComponent;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Frustum;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.example.render.TextureAtlas;
import org.example.render.WaterSort;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;

import java.util.Optional;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public final class RenderSystem implements GameSystem {

    private static final int ATLAS_TEXTURE_UNIT = 0;
    // Initial water-batch capacity; the in-view water chunks at load radius rarely exceed this and
    // the buffers only ever grow (never shrink), so steady state allocates nothing per frame.
    private static final int INITIAL_WATER_CAPACITY = 256;

    private final Shader       shader;
    private final Shader       waterShader;
    private final TextureAtlas atlas;

    // Reused every frame: the per-chunk draw loop must not allocate.
    private final Frustum  frustum        = new Frustum();
    private final Matrix4f viewProjection = new Matrix4f();
    private final Matrix4f model          = new Matrix4f();
    private final Matrix4f invView        = new Matrix4f();

    // Reused water-pass scratch (back-to-front sort). Indices into the parallel arrays below.
    private Mesh[]  waterMeshes = new Mesh[INITIAL_WATER_CAPACITY];
    private float[] waterCenterX = new float[INITIAL_WATER_CAPACITY];
    private float[] waterCenterZ = new float[INITIAL_WATER_CAPACITY];
    private int[]   waterOrder   = new int[INITIAL_WATER_CAPACITY];
    private float[] waterDistance = new float[INITIAL_WATER_CAPACITY];
    // Model translation per batched water chunk, so the sorted draw uses the chunk origin.
    private float[] waterOriginX = new float[INITIAL_WATER_CAPACITY];
    private float[] waterOriginY = new float[INITIAL_WATER_CAPACITY];
    private float[] waterOriginZ = new float[INITIAL_WATER_CAPACITY];

    public RenderSystem(Shader shader, Shader waterShader, TextureAtlas atlas) {
        this.shader      = shader;
        this.waterShader = waterShader;
        this.atlas       = atlas;
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        viewProjection.set(camera.projection()).mul(camera.view());
        frustum.update(viewProjection);

        int waterCount = drawOpaqueAndCollectWater(world, camera);
        drawWaterPass(camera, waterCount);
    }

    private int drawOpaqueAndCollectWater(World world, RenderCamera camera) {
        shader.bind();
        // One atlas bind for the whole chunk pass: every chunk samples the same texture on unit 0.
        atlas.bind(ATLAS_TEXTURE_UNIT);
        shader.setUniform1i("uAtlas", ATLAS_TEXTURE_UNIT);
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());

        int waterCount = 0;
        for (int eid : world.query(ChunkMeshComponent.class, Position.class)) {
            Entity entity = new Entity(eid);
            Position pos = world.get(entity, Position.class).orElseThrow();
            if (!isChunkVisible(pos)) continue;
            ChunkMeshComponent meshes = world.get(entity, ChunkMeshComponent.class).orElseThrow();
            shader.setUniformMatrix4f("uModel", model.translation(pos.x(), pos.y(), pos.z()));
            meshes.opaque().draw();
            waterCount = collectWater(meshes.water(), pos, waterCount);
        }
        shader.unbind();
        return waterCount;
    }

    private int collectWater(Optional<Mesh> water, Position pos, int count) {
        if (water.isEmpty()) return count;
        if (count == waterMeshes.length) growWaterBuffers();
        waterMeshes[count]  = water.get();
        waterOriginX[count] = pos.x();
        waterOriginY[count] = pos.y();
        waterOriginZ[count] = pos.z();
        waterCenterX[count] = pos.x() + WorldConstants.CHUNK_SIZE_XZ * 0.5f;
        waterCenterZ[count] = pos.z() + WorldConstants.CHUNK_SIZE_XZ * 0.5f;
        return count + 1;
    }

    private void drawWaterPass(RenderCamera camera, int waterCount) {
        if (waterCount == 0) return;
        // Camera world position is the translation of the inverse view matrix.
        camera.view().invert(invView);
        float camX = invView.m30();
        float camZ = invView.m32();
        WaterSort.backToFront(waterCount, camX, camZ, waterCenterX, waterCenterZ, waterOrder, waterDistance);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false); // keep depth-test on but stop writing, so water never occludes itself

        waterShader.bind();
        atlas.bind(ATLAS_TEXTURE_UNIT);
        waterShader.setUniform1i("uAtlas", ATLAS_TEXTURE_UNIT);
        waterShader.setUniformMatrix4f("uView",       camera.view());
        waterShader.setUniformMatrix4f("uProjection", camera.projection());
        waterShader.setUniform3f("uWaterTint",
                WorldConstants.WATER_TINT_R, WorldConstants.WATER_TINT_G, WorldConstants.WATER_TINT_B);
        waterShader.setUniform1f("uWaterAlpha", WorldConstants.WATER_ALPHA);

        for (int i = 0; i < waterCount; i++) {
            int c = waterOrder[i];
            waterShader.setUniformMatrix4f("uModel",
                    model.translation(waterOriginX[c], waterOriginY[c], waterOriginZ[c]));
            waterMeshes[c].draw();
        }
        waterShader.unbind();

        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    private void growWaterBuffers() {
        int n = waterMeshes.length * 2;
        waterMeshes   = java.util.Arrays.copyOf(waterMeshes, n);
        waterCenterX  = java.util.Arrays.copyOf(waterCenterX, n);
        waterCenterZ  = java.util.Arrays.copyOf(waterCenterZ, n);
        waterOrder    = java.util.Arrays.copyOf(waterOrder, n);
        waterDistance = java.util.Arrays.copyOf(waterDistance, n);
        waterOriginX  = java.util.Arrays.copyOf(waterOriginX, n);
        waterOriginY  = java.util.Arrays.copyOf(waterOriginY, n);
        waterOriginZ  = java.util.Arrays.copyOf(waterOriginZ, n);
    }

    private boolean isChunkVisible(Position pos) {
        float sx = WorldConstants.CHUNK_SIZE_XZ;
        float h  = WorldConstants.WORLD_HEIGHT;
        return frustum.isBoxVisible(
            pos.x(),      pos.y(),     pos.z(),
            pos.x() + sx, pos.y() + h, pos.z() + sx
        );
    }
}
