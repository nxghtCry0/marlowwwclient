package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import net.minecraft.world.InteractionHand;

public class AntiAFK extends Module {
    private int idleTicks = 0;
    private int cycleIndex = 0;
    private float lastYaw;
    private float lastPitch;
    private boolean initialized = false;
    private boolean jumpPulsed = false;
    private boolean sneakPulsed = false;

    public AntiAFK() {
        super("AntiAFK", Category.Movement, "Simulates activity to avoid being kicked for idling.");
    }

    @Override
    public void onTick() {
        if (!isToggled() || mc.player == null || mc.options == null) return;

        if (jumpPulsed) {
            mc.options.keyJump.setDown(false);
            jumpPulsed = false;
        }
        if (sneakPulsed) {
            mc.options.keyShift.setDown(false);
            sneakPulsed = false;
        }

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        if (!initialized) {
            lastYaw = yaw;
            lastPitch = pitch;
            initialized = true;
            return;
        }

        boolean cameraMoved = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - lastYaw)) > 0.05f
                || Math.abs(pitch - lastPitch) > 0.05f;
        lastYaw = yaw;
        lastPitch = pitch;

        if (hasManualInput() || cameraMoved) {
            idleTicks = 0;
            return;
        }

        Setting intervalSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Interval (s)");
        int interval = intervalSetting != null ? (int) intervalSetting.getValDouble() : 5;

        if (++idleTicks < interval * 20) return;

        performConfiguredAction();
        idleTicks = 0;
    }

    private boolean hasManualInput() {
        return mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown()
                || mc.options.keyJump.isDown()
                || mc.options.keyShift.isDown()
                || mc.options.keySprint.isDown()
                || mc.options.keyAttack.isDown()
                || mc.options.keyUse.isDown();
    }

    private void performConfiguredAction() {
        Setting modeSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Action");
        String action = modeSetting != null ? modeSetting.getValString() : "Cycle";

        if (action.equals("Cycle")) {
            action = switch (cycleIndex++ % 3) {
                case 1 -> "Jump";
                case 2 -> "Swing";
                default -> "Sneak";
            };
        }

        switch (action) {
            case "Jump" -> jump();
            case "Sneak" -> sneak();
            default -> swing();
        }
    }

    private void swing() {
        if (mc.player != null)
            mc.player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
    }

    private void jump() {
        if (mc.player == null || mc.options == null || !mc.player.onGround()) return;
        mc.options.keyJump.setDown(true);
        jumpPulsed = true;
    }

    private void sneak() {
        if (mc.player == null || mc.options == null) return;
        mc.options.keyShift.setDown(true);
        sneakPulsed = true;
    }

    @Override
    public void onDisable() {
        idleTicks = 0;
        cycleIndex = 0;
        initialized = false;
        if (mc.options != null) {
            if (jumpPulsed) mc.options.keyJump.setDown(false);
            if (sneakPulsed) mc.options.keyShift.setDown(false);
        }
        jumpPulsed = false;
        sneakPulsed = false;
    }
}
