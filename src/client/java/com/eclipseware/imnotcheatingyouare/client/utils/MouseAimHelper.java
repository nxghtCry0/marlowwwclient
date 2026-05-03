package com.eclipseware.imnotcheatingyouare.client.utils;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MouseAimHelper {
    private static double manualDeltaX;
    private static double manualDeltaY;
    
    private static double targetAimRateX;
    private static double targetAimRateY;
    
    private static long lastAimTime;
    private static long lastTickTime;
    private static double deltaTime;
    
    private static double accumulatorX;
    private static double accumulatorY;

    private MouseAimHelper() {}

    public static void addDelta(double dx, double dy) {
        manualDeltaX += dx;
        manualDeltaY += dy;
    }

    public static void setAimRate(double rateX, double rateY) {
        targetAimRateX = rateX;
        targetAimRateY = rateY;
        
        long currentTime = System.currentTimeMillis();
        lastAimTime = currentTime;
        
        if (lastTickTime == 0L) {
            lastTickTime = currentTime;
        }
    }

    public static void clearAimRate() {
        targetAimRateX = 0.0D;
        targetAimRateY = 0.0D;
        accumulatorX = 0.0D;
        accumulatorY = 0.0D;
        lastTickTime = 0L;
        deltaTime = 0.0D;
    }

    public static double pollDX() {
        double currentDelta = manualDeltaX;
        manualDeltaX = 0.0D;
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastTick = (lastTickTime > 0L) ? Math.min(currentTime - lastTickTime, 100L) : 0L;
        lastTickTime = currentTime;
        
        deltaTime = timeSinceLastTick / 50.0D;
        
        if (targetAimRateX != 0.0D && currentTime - lastAimTime <= 100L) {
            float sensitivityMultiplier = getSensitivityMultiplier();
            accumulatorX += targetAimRateX * deltaTime;
            
            long discretePixels = Math.round(accumulatorX / sensitivityMultiplier);
            accumulatorX -= ((float)discretePixels * sensitivityMultiplier);
            currentDelta += discretePixels;
        }
        
        return currentDelta;
    }

    public static double pollDY() {
        double currentDelta = manualDeltaY;
        manualDeltaY = 0.0D;
        
        long currentTime = System.currentTimeMillis();
        
        if (targetAimRateY != 0.0D && currentTime - lastAimTime <= 100L) {
            float sensitivityMultiplier = getSensitivityMultiplier();
            accumulatorY += targetAimRateY * deltaTime;
            
            long discretePixels = Math.round(accumulatorY / sensitivityMultiplier);
            accumulatorY -= ((float)discretePixels * sensitivityMultiplier);
            currentDelta += discretePixels;
        }
        
        return currentDelta;
    }

    private static float getSensitivityMultiplier() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return 0.15F;
        
        float rawSensitivity = mc.options.sensitivity().get().floatValue();
        float f2 = rawSensitivity * 0.6F + 0.2F;
        return f2 * f2 * f2 * 1.2F;
    }

    static {
        manualDeltaX = 0.0D;
        manualDeltaY = 0.0D;
        targetAimRateX = 0.0D;
        targetAimRateY = 0.0D;
        lastAimTime = 0L;
        lastTickTime = 0L;
        accumulatorX = 0.0D;
        accumulatorY = 0.0D;
        deltaTime = 0.0D;
    }
}
