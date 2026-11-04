package ru.d2.elysium.core;

import java.util.HashMap;
import java.util.Map;

/** Server damage packet attribution; all timeouts use player ticks, not wall time. */
public final class Attribution {
    private record Hit(int tick, int pumpkin) {}
    private final Map<String, Hit> hits = new HashMap<>();
    private final Map<String, Integer> seenDeaths = new HashMap<>();

    public void clear() { hits.clear(); seenDeaths.clear(); }
    public void damage(String uuid, String type, int pumpkin, int attackerId, int localPlayerId, int tick) {
        if (!Counter.relevant(type)) return;
        if (attackerId == localPlayerId) hits.put(uuid, new Hit(tick, pumpkin));
        else if (attackerId >= 0) hits.remove(uuid);
    }
    public Counter.PendingKill death(String uuid, String type, int pumpkin, int tick, long now) {
        if (!Counter.relevant(type) || seenDeaths.putIfAbsent(uuid, tick) != null) return null;
        Hit hit = hits.remove(uuid);
        if (hit == null || tick - hit.tick() > 100 || tick < hit.tick()) return null;
        int kind = pumpkin == 0 ? hit.pumpkin() : pumpkin;
        return kind == 0 ? null : new Counter.PendingKill(uuid, type, kind == 2, now);
    }
    public void expire(int tick) {
        hits.values().removeIf(hit -> tick - hit.tick() > 100 || tick < hit.tick());
        seenDeaths.values().removeIf(at -> tick - at > 2400 || tick < at);
    }
}
