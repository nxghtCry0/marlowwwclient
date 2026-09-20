#version 150

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform vec2 uSize;
uniform vec2 uLocation;

uniform float radius;
uniform float Brightness;
uniform float Quality;
in vec2 texCoord;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

vec4 blur() {
    #define TAU 6.28318530718
    #define STEPS 16.0
    #define STEP_SIZE 0.1
    #define STEPS_COUNT 10

    vec2 Radius = Quality / InputResolution.xy;
    vec2 uv = gl_FragCoord.xy / InputResolution.xy;
    vec4 Color = vec4(0.0);
    float totalWeight = 0.0;

    float angleStep = TAU / STEPS;
    vec2 halfSize = uSize / 2.0;
    vec2 center = gl_FragCoord.xy - uLocation - halfSize;
    
    float sdfCenter = roundedBoxSDF(center, halfSize, radius);
    if (sdfCenter > 4.0) {
        return texture(InputSampler, uv) * Brightness;
    }

    for (float d = 0.0; d < TAU; d += angleStep) {
        vec2 dir = vec2(cos(d), sin(d));
        
        for (int i = 1; i <= STEPS_COUNT; i++) {
            float step = float(i) * STEP_SIZE;
            vec2 offset = dir * Radius * step;
            vec2 samplePos = center + offset;
            float sdf = roundedBoxSDF(samplePos, halfSize, radius);
            
            float weight = smoothstep(2.0, -2.0, sdf);
            if (weight > 0.0) {
                Color += texture(InputSampler, uv + offset) * weight;
                totalWeight += weight;
            }
        }
    }

    return (totalWeight > 0.0 ? Color / totalWeight : texture(InputSampler, uv)) * Brightness;
}

void main() {
    vec2 halfSize = uSize / 2.0;
    vec2 centerPos = gl_FragCoord.xy - uLocation - halfSize;
    float sdf = roundedBoxSDF(centerPos, halfSize, radius);
    
    if (sdf > 4.0) {
        discard;
    }
    
    float smoothedAlpha = 1.0 - smoothstep(-2.0, 2.0, sdf);
    fragColor = vec4(blur().rgb, smoothedAlpha);
}