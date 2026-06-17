#version 330 core

in vec2 vUV;
in vec3 vTint;
out vec4 fragColor;

uniform sampler2D uAtlas;

void main() {
    vec4 texel = texture(uAtlas, vUV);
    fragColor = vec4(texel.rgb * vTint, texel.a);
}
