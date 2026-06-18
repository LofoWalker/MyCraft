package org.example.systems;

import org.example.components.MobType;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.example.render.Frustum;
import org.example.render.Mesh;
import org.example.render.Shader;
import org.joml.Matrix4f;

/**
 * Draws each mob as two axis-aligned boxes (body + head) scaled from the
 * MobType's dimensions. Uses the highlight shader (flat-colour, no atlas) so
 * rendering works before a dedicated mob sub-atlas exists. Zero allocation per
 * frame: one reused Matrix4f and one shared cube Mesh.
 *
 * Runs in the render scheduler after the opaque world pass and before the HUD.
 */
public final class EntityRenderSystem implements GameSystem, AutoCloseable {

    // Fraction of total height allocated to the head box.
    private static final float HEAD_HEIGHT_FRACTION = 0.35f;
    private static final float BODY_HEIGHT_FRACTION = 1.0f - HEAD_HEIGHT_FRACTION;

    private static final float MOB_TINT_R = 0.55f;
    private static final float MOB_TINT_G = 0.80f;
    private static final float MOB_TINT_B = 0.55f;
    private static final float HEAD_TINT_R = 0.80f;
    private static final float HEAD_TINT_G = 0.70f;
    private static final float HEAD_TINT_B = 0.55f;
    private static final float OPAQUE_ALPHA = 1.0f;

    private final Shader   shader;
    private final Mesh     cube;
    private final Matrix4f model   = new Matrix4f();
    private final Matrix4f vp      = new Matrix4f();
    private final Frustum  frustum = new Frustum();

    public EntityRenderSystem() {
        this.shader = Shader.fromResources("/shaders/highlight.vert", "/shaders/highlight.frag");
        this.cube   = Mesh.createTestCube();
    }

    @Override
    public void update(World world, float dt) {
        int[] cameras = world.query(RenderCamera.class);
        if (cameras.length == 0) return;

        RenderCamera camera = world.get(new Entity(cameras[0]), RenderCamera.class).orElseThrow();
        vp.set(camera.projection()).mul(camera.view());
        frustum.update(vp);

        shader.bind();
        shader.setUniformMatrix4f("uView",       camera.view());
        shader.setUniformMatrix4f("uProjection", camera.projection());
        shader.setUniform1f("uAlpha", OPAQUE_ALPHA);

        for (int eid : world.query(MobType.class, Position.class)) {
            drawMob(world, new Entity(eid));
        }
        shader.unbind();
    }

    private void drawMob(World world, Entity mob) {
        MobType  mobType = world.get(mob, MobType.class).orElseThrow();
        Position pos     = world.get(mob, Position.class).orElseThrow();
        float    yaw     = world.get(mob, Rotation.class).map(Rotation::yaw).orElse(0f);

        MobType.Kind kind   = mobType.kind();
        float        width  = kind.width();
        float        height = kind.height();

        if (!isVisible(pos, width, height)) return;

        float bodyHeight = height * BODY_HEIGHT_FRACTION;
        float headHeight = height * HEAD_HEIGHT_FRACTION;
        float headSize   = Math.max(width, headHeight);

        // Body: centred on mob AABB footprint, origin at feet.
        float bodyY = pos.y() + bodyHeight * 0.5f;
        buildModel(pos.x(), bodyY, pos.z(), yaw, width, bodyHeight, width);
        shader.setUniform3f("uColor", MOB_TINT_R, MOB_TINT_G, MOB_TINT_B);
        cube.draw();

        // Head: sits on top of the body.
        float headY = pos.y() + bodyHeight + headSize * 0.5f;
        buildModel(pos.x(), headY, pos.z(), yaw, headSize, headSize, headSize);
        shader.setUniform3f("uColor", HEAD_TINT_R, HEAD_TINT_G, HEAD_TINT_B);
        cube.draw();
    }

    /**
     * Populates the reused model matrix with translation + yaw rotation + scale.
     * No allocation occurs: Matrix4f methods mutate the existing instance in-place.
     * Package-private so the non-GL unit test can verify the transform without a GL context.
     */
    void buildModel(float x, float y, float z,
                    float yawDeg, float sx, float sy, float sz) {
        buildModel(model, x, y, z, yawDeg, sx, sy, sz);
    }

    /** Pure math — no OpenGL, testable without a GL context. */
    static void buildModel(Matrix4f out, float x, float y, float z,
                           float yawDeg, float sx, float sy, float sz) {
        out.translation(x, y, z)
           .rotateY((float) Math.toRadians(yawDeg))
           .scale(sx, sy, sz);
    }

    private boolean isVisible(Position pos, float width, float height) {
        float hw = width * 0.5f;
        return frustum.isBoxVisible(
                pos.x() - hw, pos.y(),          pos.z() - hw,
                pos.x() + hw, pos.y() + height, pos.z() + hw);
    }

    @Override
    public void close() {
        cube.close();
        shader.close();
    }
}
