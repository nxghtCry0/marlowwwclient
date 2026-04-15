package com.eclipseware.imnotcheatingyouare.client.utils.cheat;

public class TimerUtil {
    private long lastMs = 0;
    private long initialTime = 0;

    public TimerUtil() {
        this.lastMs = System.currentTimeMillis();
        this.initialTime = System.currentTimeMillis();
    }

    public boolean hasElapsedTime(long time) {
        return System.currentTimeMillis() - lastMs >= time;
    }

    public boolean hasElapsedTime(long time, boolean reset) {
        boolean elapsed = hasElapsedTime(time);
        if (elapsed && reset) {
            reset();
        }
        return elapsed;
    }

    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMs;
    }

    public long getTimeSinceStart() {
        return System.currentTimeMillis() - initialTime;
    }

    public boolean hasTimeSinceStartElapsed(long time) {
        return System.currentTimeMillis() - initialTime >= time;
    }

    public void resetInitial() {
        this.initialTime = System.currentTimeMillis();
    }
}
