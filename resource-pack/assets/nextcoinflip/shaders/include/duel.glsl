const float CF_PI = 3.14159265;

vec3 cfTurn(vec3 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(c * p.x + s * p.z, p.y, c * p.z - s * p.x);
}

vec2 cfSkinPixel(vec3 p, vec3 n, vec3 size, vec2 origin) {
    float w = size.x;
    float h = size.y;
    float d = size.z;
    if (n.x < -0.5) return origin + vec2(d - p.z, d + p.y);
    if (n.x > 0.5) return origin + vec2(d + w + p.z, d + p.y);
    if (n.y < -0.5) return origin + vec2(d + p.x, p.z);
    if (n.y > 0.5) return origin + vec2(d + w + p.x, d - p.z);
    if (n.z > 0.5) return origin + vec2(d + p.x, d + p.y);
    return origin + vec2(d + w + d + w - p.x, d + p.y);
}

void cfPart(vec3 ray, vec3 dir, vec3 center, vec3 size, vec2 origin, float padding,
            inout float nearest, inout vec3 pixel) {
    vec3 halfSize = size * 0.5 + padding;
    vec3 from = (center - halfSize - ray) / dir;
    vec3 to = (center + halfSize - ray) / dir;
    vec3 enter = min(from, to);
    vec3 exit = max(from, to);
    float t = max(enter.x, max(enter.y, enter.z));
    if (t >= nearest || t > min(exit.x, min(exit.y, exit.z))) return;

    vec3 normal;
    if (enter.x >= max(enter.y, enter.z)) normal = vec3(-sign(dir.x), 0.0, 0.0);
    else if (enter.y >= enter.z) normal = vec3(0.0, -sign(dir.y), 0.0);
    else normal = vec3(0.0, 0.0, -sign(dir.z));

    vec3 fraction = clamp((ray + dir * t - center) / (2.0 * halfSize) + 0.5, 0.001, 0.999);
    ivec2 uv = ivec2(floor(cfSkinPixel(fraction * size, normal, size, origin)));
    vec4 skin = texelFetch(Sampler0, uv, 0);
    if (skin.a < 0.5) return;
    nearest = t;
    float light = 0.66 + 0.34 * max(0.0, dot(normal, normalize(vec3(-0.42, -0.70, 0.9))));
    pixel = skin.rgb * light;
}

void cfLayeredPart(vec3 ray, vec3 dir, vec3 center, vec3 size, vec2 base, vec2 overlay,
                   float padding, inout float nearest, inout vec3 pixel) {
    cfPart(ray, dir, center, size, overlay, padding, nearest, pixel);
    cfPart(ray, dir, center, size, base, 0.0, nearest, pixel);
}

vec3 cfPitch(vec3 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(p.x, c * p.y - s * p.z, s * p.y + c * p.z);
}

void cfLimb(vec3 ray, vec3 dir, vec3 joint, float width, float pitch, vec2 base, vec2 overlay,
           inout float nearest, inout vec3 pixel) {
    cfLayeredPart(cfPitch(ray - joint, -pitch), cfPitch(dir, -pitch), vec3(0.0, 5.0, 0.0),
            vec3(width, 12.0, 4.0), base, overlay, 0.24, nearest, pixel);
}

vec4 cfCoinFace(vec2 local, float flatten, bool heads, bool lit);

vec4 cfSunburst(vec2 p, float age, bool right) {
    vec2 offset = p - vec2(30.0, 50.0);
    float radius = length(offset);
    float angle = atan(offset.y, offset.x + 0.0001);
    float phase = GameTime * (CF_PI * 2.0 * 240.0);
    float wave = cos(angle * 16.0 - phase);
    float rays = smoothstep(0.08, 0.65, wave);
    float fade = 1.0 - smoothstep(14.0, 44.0, radius);
    fade *= 1.0 - smoothstep(20.0, 26.0, right ? -offset.x : offset.x);
    float shine = 0.5 + 0.5 * cos(radius * 0.34 - phase * 2.0);
    vec3 gold = mix(vec3(1.0, 0.57, 0.11), vec3(1.0, 0.94, 0.55), shine);
    float alpha = smoothstep(0.0, 5.0, age) * fade * rays * (0.37 + 0.21 * shine);
    return vec4(gold, alpha);
}

vec4 cfPortrait(vec2 p, int flags, float age) {
    bool right = (flags & 1) != 0;
    float arm = (flags & 2) != 0 ? 3.0 : 4.0;
    bool revealed = (flags & 4) != 0;
    bool winner = (flags & 8) != 0;
    float jumps = clamp((age - 3.0) / 13.0, 0.0, 3.0);
    float hop = winner ? 2.4 * sin(CF_PI * fract(jumps)) : 0.0;
    if (p.y < 20.0) {
        vec4 badge = cfCoinFace((p - vec2(30.0, 10.5 - hop * 0.90)) * 2.25, 1.0, !right, winner);
        if (badge.a > 0.0) return badge;
    }

    vec2 model = (p - vec2(30.0, 59.5)) / 2.32;
    model.y += hop;
    if (abs(model.x) > 12.0 || model.y < -22.0 || model.y > 17.0) {
        return winner ? cfSunburst(p, age, right) : vec4(0.0);
    }

    float cue = age - (right ? 2.0 : 0.0);
    float guard = smoothstep(1.0, 9.0, cue);
    float swing = smoothstep(9.0, 14.0, cue) * (1.0 - smoothstep(15.0, 23.0, cue));
    float brace = smoothstep(36.0, 46.0, cue);
    float lead = 0.12 + guard * 0.54 + swing * 0.97 + brace * 0.39;
    float rear = 0.07 + guard * 0.45 - swing * 0.24 + brace * 0.21;
    float bow = 0.0;
    if (revealed) {
        float reaction = smoothstep(0.0, 8.0, age);
        float settle = smoothstep(43.0, 55.0, age);
        float raised = mix(2.60 + hop * 0.08, 0.35, settle);
        lead = mix(1.05, winner ? raised : 0.12, reaction);
        rear = mix(0.73, winner ? raised - 0.13 : 0.06, reaction);
        if (!winner) bow = 0.68 * smoothstep(2.0, 12.0, age);
    }

    float yaw = right ? 0.40 : -0.40;
    vec3 ray = cfTurn(vec3(model, 48.0), yaw);
    vec3 dir = cfTurn(vec3(0.0001, 0.0001, -1.0), yaw);
    float nearest = 1e4;
    vec3 skin = vec3(0.0);
    cfLayeredPart(cfPitch(ray - vec3(0, -8, 0), bow), cfPitch(dir, bow), vec3(0, -4, 0),
            vec3(8), vec2(0, 0), vec2(32, 0), 0.45, nearest, skin);
    cfLayeredPart(ray, dir, vec3(0, -2, 0), vec3(8, 12, 4), vec2(16, 16), vec2(16, 32), 0.22, nearest, skin);
    float shoulder = 4.0 + arm * 0.5;
    cfLimb(ray, dir, vec3(-shoulder, -7.0, 0.0), arm, right ? lead : rear, vec2(40, 16), vec2(40, 32), nearest, skin);
    cfLimb(ray, dir, vec3(shoulder, -7.0, 0.0), arm, right ? rear : lead, vec2(32, 48), vec2(48, 48), nearest, skin);
    cfLimb(ray, dir, vec3(-2, 5, 0), 4.0, hop * 0.13, vec2(0, 16), vec2(0, 32), nearest, skin);
    cfLimb(ray, dir, vec3(2, 5, 0), 4.0, -hop * 0.10, vec2(16, 48), vec2(0, 48), nearest, skin);
    if (nearest >= 1e4) return winner ? cfSunburst(p, age, right) : vec4(0.0);
    if (revealed && !winner) {
        skin = mix(skin, vec3(dot(skin, vec3(0.2126, 0.7152, 0.0722))), smoothstep(0.0, 8.0, age));
    }
    return vec4(skin, 1.0);
}

float cfBox(vec2 p, vec2 halfSize) {
    vec2 d = abs(p) - halfSize;
    return max(d.x, d.y);
}

bool cfEmblem(vec2 p, bool heads) {
    if (heads) {
        return min(cfBox(vec2(abs(p.x) - 5.0, p.y), vec2(1.55, 7.0)), cfBox(p, vec2(5.0, 1.5))) < 0.0;
    }
    return min(cfBox(p - vec2(0.0, -5.5), vec2(6.5, 1.5)), cfBox(p - vec2(0.0, 0.7), vec2(1.55, 6.3))) < 0.0;
}

bool cfCoinMask(vec2 pixel, float radius, float corner) {
    vec2 p = abs(pixel);
    return max(p.x, p.y) <= radius && p.x + p.y <= corner;
}

vec4 cfCoinFace(vec2 local, float flatten, bool heads, bool lit) {
    const float texel = 2.05;
    const float depth = 4.0;
    float faceY = local.y + depth * 0.5;
    vec2 pixel = floor(vec2(local.x, faceY / flatten) / texel) + 0.5;
    if (!cfCoinMask(pixel, 8.5, 12.0)) {
        float edgeY = (faceY - clamp(faceY, 0.0, depth)) / flatten;
        vec2 edge = floor(vec2(local.x, edgeY) / texel) + 0.5;
        if (!cfCoinMask(edge, 8.5, 12.0)) return vec4(0.0);
        vec3 rim = mod(floor(local.x / texel), 2.0) == 0.0
                ? vec3(0.53, 0.25, 0.04) : vec3(0.79, 0.43, 0.07);
        return vec4(rim, 1.0);
    }

    if (!cfCoinMask(pixel, 7.5, 11.0)) return vec4(0.27, 0.13, 0.06, 1.0);

    float diagonal = pixel.x + pixel.y;
    bool center = cfCoinMask(pixel, 5.5, 8.0);
    vec3 color = lit ? vec3(1.0, 0.83, 0.13) : vec3(1.0, 0.73, 0.07);
    if (!center) {
        if (diagonal < -4.0) color = vec3(1.0, 0.98, 0.82);
        if (diagonal > 2.0) color = vec3(0.90, 0.46, 0.04);
    } else if (diagonal == -3.0 || diagonal == -2.0) {
        color = vec3(1.0, 0.94, 0.62);
    }
    if (cfEmblem((pixel - vec2(0.0, 1.0)) * texel, heads)) color = vec3(1.0, 0.96, 0.73);
    if (cfEmblem(pixel * texel, heads)) color = vec3(0.60, 0.29, 0.04);
    return vec4(color, 1.0);
}

vec4 cfCoin(vec2 p, float age, int result) {
    bool landed = result != 0;
    float travel = clamp((age - 14.0) / 30.0, 0.0, 1.0);
    float spin = 1.0 - pow(1.0 - travel, 2.0);
    float tilt = landed ? 0.0 : spin * CF_PI * 8.5;
    float flatten = max(abs(cos(tilt)), 0.025);
    float lift = landed ? 0.0 : sin(travel * CF_PI) * 24.0;
    float impact = landed ? exp(-age * 0.18) : 0.0;
    bool heads = landed ? result == 1 : cos(tilt) >= 0.0;
    vec2 local = p - vec2(48.0, 44.0 - lift);
    return cfCoinFace(local / (1.0 + impact * 0.14), flatten, heads, landed);
}
