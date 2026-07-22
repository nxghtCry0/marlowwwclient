#version 330

uniform vec4 Bounds;
uniform float RadiusTopLeft;
uniform float RadiusTopRight;
uniform float RadiusBottomLeft;
uniform float RadiusBottomRight;
uniform float Smoothness;
uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 size, float cornerRadius) {
    return length(max(abs(p) - size + vec2(cornerRadius), 0.0)) - cornerRadius;
}

float variableRoundedBoxSDF(vec2 p, vec2 size, vec4 cornerRadii) {
    vec2 d = abs(p) - size;
    vec4 cornerDistances = vec4(
        RadiusBottomLeft,
        RadiusBottomRight,
        RadiusTopRight,
        RadiusTopLeft
    );

    float radius = mix(
        mix(cornerDistances[0], cornerDistances[1], step(0.0, p.x)),
        mix(cornerDistances[3], cornerDistances[2], step(0.0, p.x)),
        step(0.0, p.y)
    );

    return length(max(d + vec2(radius), 0.0)) - radius;
}

void main() {
    vec2 location = Bounds.xy;
    vec2 toXY = Bounds.zw;
    vec2 size = (toXY - location) * 0.5f;

    vec2 normalizedCoords = (gl_FragCoord.xy - location) / (toXY - location);
    vec2 centerCoords = gl_FragCoord.xy - location - size;

    float distance = variableRoundedBoxSDF(centerCoords, size - 1.0, vec4(RadiusTopLeft, RadiusTopRight, RadiusBottomRight, RadiusBottomLeft));

    vec4 color = mix(
        mix(color1, color2, normalizedCoords.y),
        mix(color3, color4, normalizedCoords.y),
        normalizedCoords.x
    );

    float smoothedAlpha = 1.0f - smoothstep(0.0f, Smoothness, distance);

    fragColor = vec4(color.rgb, smoothedAlpha * color.a);
}