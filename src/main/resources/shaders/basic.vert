#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec3 aTint;
layout(location = 3) in float aLight;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

out vec2 vUV;
out vec3 vTint;
out float vLight;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPosition, 1.0);
    vUV = aUV;
    vTint = aTint;
    vLight = aLight;
}
