package com.eclipseware.imnotcheatingyouare.client.utils;

public class TimerUtil {
    private long prevTime;

    public TimerUtil() {
        this.prevTime = System.currentTimeMillis();
    }

    public boolean hasElapsedTime(long time, boolean reset) {
        if (System.currentTimeMillis() - prevTime >= time) {
            if (reset) reset();
            return true;
        }
        return false;
    }

    public void reset() {
        this.prevTime = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - prevTime;
    }
}