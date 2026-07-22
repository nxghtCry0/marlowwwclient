#version 330

uniform vec2 Resolution;  // Size of the color picker
uniform vec2 Position;    // Position of the color picker
uniform float Hue;        // Current hue value
uniform float Alpha;      // Current alpha value

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 uv = (gl_FragCoord.xy - Position) / Resolution;
    
    if (uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0) {
        vec3 hsv = vec3(Hue, uv.x, uv.y);
        vec3 rgb = hsv2rgb(hsv);
        fragColor = vec4(rgb, Alpha);
    } else {
        fragColor = vec4(0.0);
    }
} 