#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 vUv;
in vec4 vColor;
in float vSphericalDist;
in float vCylindricalDist;

out vec4 fragColor;

void main() {
    float ringSoft = sin(vUv.x * 3.14159265);
    float volume = pow(max(ringSoft, 0.0), 0.45);

    float pulse = 0.92 + 0.08 * sin(GameTime * 1800.0);
    float intensity = volume * pulse;

    float alpha = vColor.a * mix(0.65, 1.0, intensity);

    vec3 col = vColor.rgb * (0.55 + 0.6 * intensity);

    float fog = total_fog_value(vSphericalDist, vCylindricalDist, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(col, alpha) * ColorModulator * (1.0 - fog);
}
