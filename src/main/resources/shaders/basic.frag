#version 330 core

in vec2 vUV;
in vec3 vTint;
in float vLight;
out vec4 fragColor;

uniform sampler2D uAtlas;

// Caves and interiors stay dim but never pure black; full skylight/blocklight reaches full bright.
const float MIN_LIGHT = 0.12;

void main() {
    vec4 texel = texture(uAtlas, vUV);
    float brightness = mix(MIN_LIGHT, 1.0, vLight);
    fragColor = vec4(texel.rgb * vTint * brightness, texel.a);
}
