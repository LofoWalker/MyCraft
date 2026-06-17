#version 330 core

in vec2 vUV;
in vec3 vTint;
in float vLight;
out vec4 fragColor;

uniform sampler2D uAtlas;
// Global day/night skylight factor in [NIGHT_LIGHT, 1] (1 = noon, NIGHT_LIGHT = midnight). Set by
// RenderSystem from TimeOfDay. Approximation: vLight carries sky and block light merged into one
// channel, so we cannot dim only the sky. We instead scale the whole brightness by uDayFactor, which
// is floored at NIGHT_LIGHT — so the world darkens at night yet torches and interiors never go pitch
// black and stay readable. A fully correct split would need two light channels (out of scope here).
uniform float uDayFactor;

// Caves and interiors stay dim but never pure black; full skylight/blocklight reaches full bright.
const float MIN_LIGHT = 0.12;

void main() {
    vec4 texel = texture(uAtlas, vUV);
    float brightness = mix(MIN_LIGHT, 1.0, vLight) * uDayFactor;
    fragColor = vec4(texel.rgb * vTint * brightness, texel.a);
}
