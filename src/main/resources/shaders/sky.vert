#version 330 core

out vec2 vNdc;

// Fullscreen triangle generated from gl_VertexID — no vertex buffer needed.
void main() {
    vec2 verts[3] = vec2[3](
        vec2(-1.0, -1.0),
        vec2( 3.0, -1.0),
        vec2(-1.0,  3.0)
    );
    vec2 p = verts[gl_VertexID];
    vNdc = p;
    gl_Position = vec4(p, 0.0, 1.0);
}
