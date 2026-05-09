#version 330

// `#moj_import` is Minecraft's GLSL preprocessor extension. IDEs without
// Mojang's preprocessor may flag it as "expected PP_END" — that warning is
// not real; the game's shader loader resolves these imports at runtime.
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 vUv;
in vec4 vColor;
in float vSphericalDist;
in float vCylindricalDist;

out vec4 fragColor;

void main() {
    float pulse = 0.90 + 0.10 * sin(GameTime * 1800.0);

    float flow = 0.82 + 0.18 * sin(vUv.y * 9.0 - GameTime * 2400.0);

    float alpha = vColor.a * 0.88;
    vec3 col = vColor.rgb * pulse * flow;

    float fog = total_fog_value(vSphericalDist, vCylindricalDist, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(col, alpha) * ColorModulator * (1.0 - fog);
}
