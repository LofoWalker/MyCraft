package org.example.systems;

import org.example.components.ItemEntity;
import org.example.components.ItemStack;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.example.world.BlockType;
import org.example.world.WorldConstants;
import org.joml.Matrix4f;

// Draws each dropped item as a small solid cube tinted with its block's colour, at the item's world
// position. Reuses one cube mesh, the highlight shader (flat uColor) and a single matrix, so the
// per-item loop allocates nothing. Runs after the opaque world pass.
public final class ItemRenderSystem implements GameSystem, AutoCloseable {

    private static final float OPAQUE_ALPHA = 1.0f;

    private final Shader   shader;
    private final Mesh     cube;
    private final Matrix4f model = new Matrix4f();

    public ItemRenderSystem() {
        this.shader = Shader.fromResources("/shaders/highlight.vert", "/shaders/highlight.frag");
        this.cube   = Mesh.createTestCube();
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        shader.bind();
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());
        shader.setUniform1f("uAlpha", OPAQUE_ALPHA);

        for (int eid : world.query(ItemEntity.class, Position.class, ItemStack.class)) {
            drawItem(world, new Entity(eid));
        }
        shader.unbind();
    }

    private void drawItem(World world, Entity item) {
        Position  pos   = world.get(item, Position.class).orElseThrow();
        ItemStack stack = world.get(item, ItemStack.class).orElseThrow();
        float[]   color = BlockType.byId((byte) stack.itemId()).colorSide();

        model.translation(pos.x(), pos.y(), pos.z()).scale(WorldConstants.ITEM_RENDER_SCALE);
        shader.setUniformMatrix4f("uModel", model);
        shader.setUniform3f("uColor", color[0], color[1], color[2]);
        cube.draw();
    }

    @Override
    public void close() {
        cube.close();
        shader.close();
    }
}
