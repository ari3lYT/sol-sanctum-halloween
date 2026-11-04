package ru.d2.elysium.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Absolute vanilla kill statistics confirm locally observed pumpkin mob deaths. */
public final class Counter {
    public record PendingKill(String uuid, String entityType, boolean glowing, long observedAt) {}
    public boolean initialized;
    public long startedAt;
    public long updatedAt;
    public long ordinary;
    public long unique;
    public Map<String, Long> mobKills = new LinkedHashMap<>();
    public Map<String, Integer> baseline = new LinkedHashMap<>();
    public List<PendingKill> pending = new ArrayList<>();


    public boolean observe(String uuid, String entityType, boolean glowing, long now) {
        if (!initialized || !relevant(entityType) || pending.stream().anyMatch(p -> p.uuid().equals(uuid))) return false;
        pending.add(new PendingKill(uuid, entityType, glowing, now));
        return true;
    }
    public boolean apply(Map<String, Integer> values, boolean firstInSession, boolean reset, long now) {
        Map<String, Integer> incoming = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (relevant(key) && value != null && value >= 0) incoming.put(key, value);
        });
        // Vanilla marks all existing statistics dirty on join. Subsequent packets contain only changes.
        if (firstInSession) for (String key : baseline.keySet()) incoming.putIfAbsent(key, 0);
        boolean changed = !initialized || reset;
        if (!initialized || reset) {
            baseline.putAll(incoming);
            initialized = true;
            if (reset || startedAt == 0) startedAt = now;
            pending.clear();
            if (reset) { ordinary = 0; unique = 0; mobKills.clear(); }
        } else {
            for (var entry : incoming.entrySet()) {
                String type = entry.getKey();
                int value = entry.getValue();
                int previous = baseline.getOrDefault(type, 0);
                long delta = Math.max(0L, (long) value - previous);
                if (value != previous) changed = true;
                baseline.put(type, value);
                if (value < previous) {
                    // Rebase after server rollback. Never turn old stats into candy rewards.
                    pending.removeIf(p -> p.entityType().equals(type));
                }
                if (delta > 0) {
                    mobKills.merge(type, delta, Long::sum);
                    var iterator = pending.iterator();
                    while (iterator.hasNext()) {
                        PendingKill p = iterator.next();
                        if (!p.entityType().equals(type)) continue;
                        if (now - p.observedAt() > 120000 || now < p.observedAt()) { iterator.remove(); changed = true; continue; }
                        if (delta <= 0) continue;
                        if (p.glowing()) unique++; else ordinary++;
                        delta--;
                        iterator.remove();
                        changed = true;
                    }
                }
            }
        }
        changed |= pending.removeIf(p -> now - p.observedAt() > 120000 || now < p.observedAt());
        updatedAt = now;
        return changed;
    }
    public static boolean relevant(String type) {
        return type != null && type.contains(":") && !type.equals("minecraft:zombified_piglin")
                && !type.equals("minecraft:player");
    }
    public long ordinaryCandies() { return ordinary; }
    public long uniqueCandies() { return unique; }
    public long totalKills() { return mobKills.values().stream().mapToLong(Long::longValue).sum(); }
    public long points(Config config) { return ordinary * config.ordinaryPoints + unique * config.uniquePoints; }
}
