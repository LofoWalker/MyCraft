package org.example.components;

import org.joml.Matrix4f;

public record RenderCamera(Matrix4f view, Matrix4f projection) {}
