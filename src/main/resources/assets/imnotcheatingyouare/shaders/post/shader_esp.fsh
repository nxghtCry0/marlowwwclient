#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform EspConfig {
    float Radius;
    float FillAlpha;
    float GlowStrength;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec4 center = texture(InSampler, texCoord);

    float nearest = 0.0;
    float glow = 0.0;
    vec3 edgeColor = vec3(0.0);
    float edgeWeight = 0.0;

    for (int ring = 1; ring <= 6; ring++) {
        float dist = Radius * float(ring) / 6.0 * 2.0;
        for (int i = 0; i < 12; i++) {
            float angle = 6.2831853 * float(i) / 12.0;
            vec4 s = texture(InSampler, texCoord + vec2(cos(angle), sin(angle)) * dist * oneTexel);
            if (s.a > 0.0) {
                float falloff = 1.0 - float(ring - 1) / 6.0;
                if (dist <= Radius) nearest = max(nearest, s.a);
                glow = max(glow, s.a * falloff);
                edgeColor += s.rgb * s.a;
                edgeWeight += s.a;
            }
        }
    }

    if (center.a > 0.0) {
        fragColor = vec4(center.rgb, FillAlpha * center.a);
        return;
    }

    if (edgeWeight <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 color = edgeColor / edgeWeight;
    float alpha = max(nearest, glow * glow * GlowStrength);
    fragColor = vec4(color, alpha);
}
