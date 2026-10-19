#version 450
layout(binding = 1) uniform Clouds { vec4 ColorModulator; float FogCloudsEnd; };
layout(location = 0) in vec4 vertexColor;
layout(location = 1) in float vertexDistance;
layout(location = 0) out vec4 fragColor;
void main() {
    fragColor = vec4(vertexColor.rgb, vertexColor.a * (1.0 - clamp(vertexDistance / max(FogCloudsEnd, 1.0), 0.0, 1.0)));
}
