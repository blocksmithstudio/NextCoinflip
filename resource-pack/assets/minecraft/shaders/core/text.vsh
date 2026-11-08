#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

#if defined(IS_GUI) && !defined(IS_GRAYSCALE)
uniform sampler2D Sampler0;
layout(location = 4) flat out ivec3 coinflipMarker;
#endif

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
#if defined(IS_GUI) && !defined(IS_GRAYSCALE)
    coinflipMarker = ivec3(Color.rgb * 255.0 + 0.5);
    bool portrait = coinflipMarker.r >= 208 && coinflipMarker.r <= 223;
    bool coin = coinflipMarker.r >= 235 && coinflipMarker.r <= 237;
    vec2 corner = UV0 * 8.0 - 1.0;
    vec2 rounded = round(corner);
    bool skinQuad = (portrait || coin) && coinflipMarker.g < 94 && all(equal(textureSize(Sampler0, 0), ivec2(64)))
            && all(lessThanEqual(abs(corner - rounded), vec2(0.001)))
            && all(greaterThanEqual(rounded, vec2(0.0))) && all(lessThanEqual(rounded, vec2(1.0)));
    if (skinQuad) {
        if (portrait && texelFetch(Sampler0, ivec2(54, 20), 0).a < 0.5
                && texelFetch(Sampler0, ivec2(46, 52), 0).a < 0.5) {
            coinflipMarker.r |= 2;
        }
        float aspect = abs(ProjMat[1][1] / ProjMat[0][0]);
        float fit = min(1.0, aspect / 1.35);
        vec2 halfSize = portrait ? vec2(0.60 / aspect, 1.0) * 0.96 : vec2(96.0 / (88.0 * aspect), 1.0) * 0.58;
        if (portrait && (coinflipMarker.r & 8) != 0) halfSize.x *= 1.5;
        float side = (coinflipMarker.r & 1) == 0 ? -0.56 : 0.56;
        vec2 screen = vec2(portrait ? side : 0.0, 0.0) + (corner * vec2(2.0, -2.0) + vec2(-1.0, 1.0)) * halfSize * fit;
        gl_Position.xy = screen * gl_Position.w;
    } else {
        coinflipMarker = ivec3(0);
    }
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;
}
