#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif

    #if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)

    #ifdef OIT_ACCUMULATE
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
    #endif

    return color;
}

#if defined(IS_GUI) && !defined(IS_GRAYSCALE)
layout(location = 4) flat in ivec3 coinflipMarker;
#include <minecraft:globals.glsl>
#include <nextcoinflip:duel.glsl>
#endif

void main() {
    #ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
    #else
    vec4 texColor = texture(Sampler0, texCoord0);
    #endif

    #if defined(IS_GUI) && !defined(IS_GRAYSCALE)
    if (coinflipMarker.r != 0) {
        vec2 corner = texCoord0 * 8.0 - 1.0;
        vec4 scene;
        int startTick = coinflipMarker.g * 256 + coinflipMarker.b;
        float age = clamp(mod(GameTime * 24000.0 - float(startTick) + 12000.0, 24000.0) - 12000.0, 0.0, 80.0);
        if (coinflipMarker.r < 224) {
            int flags = coinflipMarker.r - 208;
            float span = (flags & 8) != 0 ? 90.0 : 60.0;
            scene = cfPortrait(vec2(30.0 + (corner.x - 0.5) * span, corner.y * 100.0), flags, age);
        } else {
            scene = cfCoin(corner * vec2(96.0, 88.0), age, coinflipMarker.r - 235);
        }
        if (scene.a == 0.0) discard;
        fragColor = scene;
        return;
    }
    #endif

    vec4 color = texColor * vertexColor * ColorModulator;

    if (color.a < 0.1) {
        discard;
    }

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
