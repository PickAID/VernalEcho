#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 vUv;
out vec4 vColor;
out float vSphericalDist;
out float vCylindricalDist;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vSphericalDist = fog_spherical_distance(Position);
    vCylindricalDist = fog_cylindrical_distance(Position);
    vUv = UV0;
    vColor = Color;
}
