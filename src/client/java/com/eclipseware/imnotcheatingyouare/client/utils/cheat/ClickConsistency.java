package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClickConsistency {

    private static final int   WINDOW_MS   = 1000;
    private static final Deque<Long> clicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();

    private static final double JITTER_MEAN   = 0.0;
    private static final double JITTER_STDDEV = 18.0;

    private static final double MISCLICK_PROB = 0.04;
    private static final int    MISCLICK_EXTRA_MS = 120;

    private static int clickStreak = 0;
    private static long lastClickTime = 0L;
    private static double startMult = 1.0;
    private static double midMult = 1.0;
    private static double endMult = 1.0;

    private static int rightClickStreak = 0;
    private static long lastRightClickTime = 0L;
    private static double rightStartMult = 1.0;
    private static double rightMidMult = 1.0;
    private static double rightEndMult = 1.0;

    public static boolean shouldClick(long baseDelayMs, int maxCPS) {
        long now = System.currentTimeMillis();
        pruneWindow(now);

        if (clicks.size() >= maxCPS) return false;

        if (now - lastClickTime > 600L) {
            clickStreak = 0;
            startMult = 1.1 + Math.random() * 0.25;
            midMult = 0.85 + Math.random() * 0.15;
            endMult = 1.0 + Math.random() * 0.2;
        }

        double t = Math.min(clickStreak / 8.0, 1.0);
        double bezierMult = (1.0 - t) * (1.0 - t) * startMult + 2.0 * (1.0 - t) * t * midMult + t * t * endMult;

        double jitter = JITTER_MEAN + JITTER_STDDEV * gaussian();
        long   needed = (long) (baseDelayMs * bezierMult + jitter);

        if (Math.random() < MISCLICK_PROB) needed += MISCLICK_EXTRA_MS;

        long lastClick = clicks.isEmpty() ? 0L : clicks.peekLast();
        long elapsed   = now - lastClick;

        if (elapsed < needed) return false;

        clicks.addLast(now);
        lastClickTime = now;
        clickStreak++;
        return true;
    }

    public static void registerClick() {
        long now = System.currentTimeMillis();
        pruneWindow(now);
        clicks.addLast(now);
        lastClickTime = now;
        clickStreak++;
    }

    public static int currentCPS() {
        pruneWindow(System.currentTimeMillis());
        return clicks.size();
    }

    private static void pruneWindow(long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > WINDOW_MS) {
            clicks.pollFirst();
        }
    }

    public static boolean shouldRightClick(long baseDelayMs, int maxCPS) {
        long now = System.currentTimeMillis();
        pruneRightWindow(now);

        if (rightClicks.size() >= maxCPS) return false;

        if (now - lastRightClickTime > 600L) {
            rightClickStreak = 0;
            rightStartMult = 1.1 + Math.random() * 0.25;
            rightMidMult = 0.85 + Math.random() * 0.15;
            rightEndMult = 1.0 + Math.random() * 0.2;
        }

        double t = Math.min(rightClickStreak / 8.0, 1.0);
        double bezierMult = (1.0 - t) * (1.0 - t) * rightStartMult + 2.0 * (1.0 - t) * t * rightMidMult + t * t * rightEndMult;

        double jitter = JITTER_MEAN + JITTER_STDDEV * gaussian();
        long   needed = (long) (baseDelayMs * bezierMult + jitter);

        if (Math.random() < MISCLICK_PROB) needed += MISCLICK_EXTRA_MS;

        long lastClick = rightClicks.isEmpty() ? 0L : rightClicks.peekLast();
        long elapsed   = now - lastClick;

        if (elapsed < needed) return false;

        rightClicks.addLast(now);
        lastRightClickTime = now;
        rightClickStreak++;
        return true;
    }

    private static void pruneRightWindow(long now) {
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > WINDOW_MS) {
            rightClicks.pollFirst();
        }
    }

    private static double gaussian() {
        double u = Math.random(), v = Math.random();
        return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
    }
}
