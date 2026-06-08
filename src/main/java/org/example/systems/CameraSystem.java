package org.example.systems;

import org.example.components.CameraComponent;
import org.example.components.Position;
import org.example.components.RenderCamera;
import org.example.components.Rotation;
import org.example.ecs.Entity;
import org.example.ecs.GameSystem;
import org.example.ecs.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CameraSystem implements GameSystem {

    private final float aspectRatio;

    public CameraSystem(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Override
    public void update(World world, float dt) {
        for (int eid : world.query(Position.class, Rotation.class, CameraComponent.class)) {
            Entity entity = new Entity(eid);
            Position pos = world.get(entity, Position.class).orElseThrow();
            Rotation rot = world.get(entity, Rotation.class).orElseThrow();
            CameraComponent cam = world.get(entity, CameraComponent.class).orElseThrow();
            world.add(entity, new RenderCamera(buildView(pos, rot), buildProjection(cam)));
        }
    }

    private Matrix4f buildView(Position pos, Rotation rot) {
        float yawRad   = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());
        float cosPitch = (float) Math.cos(pitchRad);
        Vector3f forward = new Vector3f(
            (float)  Math.sin(yawRad) * cosPitch,
            (float)  Math.sin(pitchRad),
            (float) -Math.cos(yawRad) * cosPitch
        );
        Vector3f eye = new Vector3f(pos.x(), pos.y(), pos.z());
        return new Matrix4f().lookAt(eye, new Vector3f(eye).add(forward), new Vector3f(0, 1, 0));
    }

    private Matrix4f buildProjection(CameraComponent cam) {
        return new Matrix4f().perspective(
            (float) Math.toRadians(cam.fovDegrees()),
            aspectRatio,
            cam.nearPlane(),
            cam.farPlane()
        );
    }
}
