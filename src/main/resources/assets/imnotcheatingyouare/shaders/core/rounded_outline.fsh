#version 330

uniform vec4 Bounds;
uniform float Radius;
uniform float Smoothness;
uniform float StrokeWidth;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

void main() {
    vec2 location = Bounds.xy;
    vec2 toXY = Bounds.zw;
    vec2 size = (toXY - location) * 0.5f;

    vec2 normalizedCoords = (gl_FragCoord.xy - location) / (toXY - location);
    float distance = roundedBoxSDF(gl_FragCoord.xy - location - size, size, Radius);
    float innerDistance = roundedBoxSDF(gl_FragCoord.xy - location - size, size - vec2(StrokeWidth), Radius - StrokeWidth);

    float strokeAlpha = smoothstep(0.0f, Smoothness, innerDistance) - smoothstep(0.0f, Smoothness, distance);
    vec4 color = mix(
        mix(color1, color2, normalizedCoords.y),
        mix(color3, color4, normalizedCoords.y),
        normalizedCoords.x
    );

    vec4 strokeColorWithAlpha = vec4(color.rgb, strokeAlpha * color.a);

    fragColor = vec4(strokeColorWithAlpha.rgb, strokeColorWithAlpha.a);
}