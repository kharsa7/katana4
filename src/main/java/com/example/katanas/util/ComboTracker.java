package com.example.katanas.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side mirror of the client combo counter, so slash particles match the technique
 * the player sees (0 kesa-giri, 1 yoko-giri, 2 gyaku-kesa, 3 tsuki).
 * The client keeps its own counter in KatanaAnimator (they must not share state in single player).
 */
public final class ComboTracker {
    public static final int TECHNIQUES = 4;
    public static final long RESET_TICKS = 24;

    private static final Map<UUID, long[]> STATE = new HashMap<>(); // [lastTime, index]

    private ComboTracker() {}

    public static int next(UUID id, long now) {
        long[] s = STATE.computeIfAbsent(id, k -> new long[]{-1000L, 0L});
        s[1] = (now - s[0] > RESET_TICKS) ? 0 : (s[1] + 1) % TECHNIQUES;
        s[0] = now;
        return (int) s[1];
    }

    private static final Map<UUID, Long> ABILITY = new HashMap<>();

    /** Call when the right-click ability fires, so its arm swing is not counted as a combo hit. */
    public static void abilityUsed(UUID id, long now) {
        ABILITY.put(id, now);
    }

    public static boolean isAbilitySwing(UUID id, long now) {
        Long t = ABILITY.get(id);
        return t != null && now - t <= 3;
    }

    public static void clear() {
        STATE.clear();
        ABILITY.clear();
    }
}
