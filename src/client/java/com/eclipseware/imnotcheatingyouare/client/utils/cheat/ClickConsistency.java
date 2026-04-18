package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Click Consistency / CPS humanisation.
 *
 * Anticheats track:
 *   - CPS variance (too consistent = bot)
 *   - Inter-click timing distribution (should follow a roughly Gaussian curve)
 *   - Burst patterns (clicking exactly every N ms is flagged)
 *
 * This class produces humanised click timings by:
 *   1. Adding Gaussian jitter to the base delay
 *   2. Occasionally inserting a "misclick gap" (simulates human hesitation)
 *   3. Tracking a rolling CPS window so we never exceed a configured cap
 */
public class ClickConsistency {

    private static final int   WINDOW_MS   = 1000;
    private static final Deque<Long> clicks = new ArrayDeque<>();

    private static final double JITTER_MEAN   = 0.0;
    private static final double JITTER_STDDEV = 18.0;

    private static final double MISCLICK_PROB = 0.04;
    private static final int    MISCLICK_EXTRA_MS = 120;

    /**
     * Returns true if it's safe to click right now without exceeding maxCPS
     * and without producing a suspicious timing pattern.
     *
     * @param baseDelayMs  nominal ms between clicks
     * @param maxCPS       hard cap (e.g. 14)
     */
    public static boolean shouldClick(long baseDelayMs, int maxCPS) {
        long now = System.currentTimeMillis();
        pruneWindow(now);

        if (clicks.size() >= maxCPS) return false;

        long lastClick = clicks.isEmpty() ? 0L : clicks.peekLast();
        long elapsed   = now - lastClick;

        double jitter = JITTER_MEAN + JITTER_STDDEV * gaussian();
        long   needed = (long) (baseDelayMs + jitter);

        if (Math.random() < MISCLICK_PROB) needed += MISCLICK_EXTRA_MS;

        if (elapsed < needed) return false;

        clicks.addLast(now);
        return true;
    }

    /** Register an external click (e.g. real mouse press) so CPS tracking stays accurate. */
    public static void registerClick() {
        long now = System.currentTimeMillis();
        pruneWindow(now);
        clicks.addLast(now);
    }

    /** Current CPS in the last second. */
    public static int currentCPS() {
        pruneWindow(System.currentTimeMillis());
        return clicks.size();
    }

    private static void pruneWindow(long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }
    }

    private static double gaussian() {
        double u = Math.random(), v = Math.random();
        return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
    }
}
