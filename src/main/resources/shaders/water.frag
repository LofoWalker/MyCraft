#version 330 core

in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uAtlas;
uniform vec3  uWaterTint;
uniform float uWaterAlpha;

void main() {
    vec4 texel = texture(uAtlas, vUV);
    fragColor = vec4(texel.rgb * uWaterTint, texel.a * uWaterAlpha);
}
