#version 330

uniform vec2 Origin;
uniform float Radius;
uniform float StartAngle;
uniform float AngleRange;

in vec4 vertexColor;

out vec4 fragColor;

float circleSDF(vec2 p, float r) {
    return length(p) - r;
}

void main() {
    vec2 ourLocation = gl_FragCoord.xy;
    vec2 dist = ourLocation - Origin;

    float angle = degrees(atan(dist.y, dist.x));
    if (angle < 0.0) {
        angle += 360.0;
    }
    angle = mod(450.0 - angle, 360.0);

    float start = mod(StartAngle, 360.0);
    float end = mod(start + AngleRange, 360.0);

    bool inSegment = false;
    if (AngleRange >= 360.0) {
        inSegment = true;
    } else if (end >= start) {
        inSegment = (angle >= start && angle <= end);
    } else {
        inSegment = (angle >= start || angle <= end);
    }

    float signedDistance = circleSDF(dist, Radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.5, 0.0, signedDistance);

    if (inSegment) {
        fragColor = vec4(vertexColor.rgb, smoothedAlpha * vertexColor.a);
    } else {
        fragColor = vec4(vertexColor.rgb, 0.0);
    }
}
