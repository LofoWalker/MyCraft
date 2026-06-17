#version 330 core

in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uAtlas;
uniform vec3  uWaterTint;
uniform float uWaterAlpha;
// Global day/night factor in [NIGHT_LIGHT, 1] so water darkens with the rest of the world at night.
uniform float uDayFactor;

void main() {
    vec4 texel = texture(uAtlas, vUV);
    fragColor = vec4(texel.rgb * uWaterTint * uDayFactor, texel.a * uWaterAlpha);
}
