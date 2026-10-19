#version 450
layout(binding = 0) uniform Transform { mat4 MVP; vec3 ChunkOffset; };
layout(binding = 1) uniform Clouds { vec4 ColorModulator; float FogEnd; };
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float vertexDistance;
void main() {
    gl_Position = MVP * vec4(Position, 1.0);
    vertexDistance = length(Position + ChunkOffset);
    vertexColor = Color * ColorModulator;
}
