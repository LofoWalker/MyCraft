#version 330 core

in vec2 vNdc;
out vec4 fragColor;

uniform mat4  uInvProjection;
uniform mat4  uInvView;
uniform vec3  uSunDir;
uniform float uTime;
// Global day factor in [NIGHT_LIGHT, 1]: drives the day->night colour blend (1 = full day).
uniform float uDayFactor;

const vec3 ZENITH_DAY    = vec3(0.20, 0.45, 0.85);
const vec3 HORIZON_DAY   = vec3(0.75, 0.85, 0.95);
const vec3 GROUND_DAY    = vec3(0.52, 0.58, 0.62);

const vec3 ZENITH_NIGHT  = vec3(0.02, 0.03, 0.08);
const vec3 HORIZON_NIGHT = vec3(0.05, 0.06, 0.12);
const vec3 GROUND_NIGHT  = vec3(0.03, 0.03, 0.05);

// Warm band that peaks at the horizon during dawn/dusk (when the body sits low).
const vec3 DUSK_COLOR    = vec3(0.95, 0.55, 0.30);

const vec3 SUN_COLOR     = vec3(1.00, 0.95, 0.80);
const vec3 MOON_COLOR    = vec3(0.70, 0.75, 0.90);
const vec3 CLOUD_COLOR   = vec3(1.00, 1.00, 1.00);

const float CLOUD_SCALE    = 0.25;
const float CLOUD_SPEED    = 0.015;
const float CLOUD_COVERAGE = 0.45;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// World-space direction of the ray through this pixel, ignoring camera position.
vec3 viewRayDirection() {
    vec4 viewSpace = uInvProjection * vec4(vNdc, 1.0, 1.0);
    viewSpace /= viewSpace.w;
    return normalize(mat3(uInvView) * viewSpace.xyz);
}

void main() {
    vec3 dir = viewRayDirection();
    float day = clamp(uDayFactor, 0.0, 1.0);

    vec3 zenith  = mix(ZENITH_NIGHT,  ZENITH_DAY,  day);
    vec3 horizon = mix(HORIZON_NIGHT, HORIZON_DAY, day);
    vec3 ground  = mix(GROUND_NIGHT,  GROUND_DAY,  day);

    float height = dir.y;
    vec3 color = height > 0.0
        ? mix(horizon, zenith, pow(height, 0.5))
        : mix(horizon, ground, pow(-height, 0.4));

    // Dawn/dusk warmth: strongest when the body is near the horizon (uSunDir.y small) and only in
    // the lower sky. Fades out at full day and full night.
    float lowBody  = 1.0 - clamp(uSunDir.y * 4.0, 0.0, 1.0);
    float twilight = lowBody * day * (1.0 - day) * 4.0;
    float band     = clamp(1.0 - abs(dir.y) * 3.0, 0.0, 1.0);
    color = mix(color, DUSK_COLOR, twilight * band);

    vec3 bodyColor = mix(MOON_COLOR, SUN_COLOR, day);
    float body = max(dot(dir, uSunDir), 0.0);
    color += bodyColor * pow(body, 1024.0) * 3.00;  // disc
    color += bodyColor * pow(body, 12.0)   * 0.25 * (0.4 + 0.6 * day);  // halo

    if (dir.y > 0.0) {
        vec2 plane = dir.xz / dir.y * CLOUD_SCALE + vec2(uTime * CLOUD_SPEED, 0.0);
        float density     = fbm(plane);
        float coverage    = smoothstep(CLOUD_COVERAGE, CLOUD_COVERAGE + 0.3, density);
        float horizonFade = smoothstep(0.0, 0.25, dir.y);
        float lit         = (0.85 + 0.15 * body) * mix(0.25, 1.0, day);
        color = mix(color, CLOUD_COLOR * lit, coverage * horizonFade);
    }

    fragColor = vec4(color, 1.0);
}
