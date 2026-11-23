package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.setting.SettingsManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Hitboxes extends Module {
    public static Hitboxes INSTANCE;

    public Hitboxes() {
        super("Hitboxes", Category.Blatant, "Expands target entity hitboxes for easier combat hits.");
        INSTANCE = this;
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return;
        }
        SettingsManager sm = client.settingsManager;
        sm.rSetting(new Setting("Expand", this, 0.25, 0.01, 1.00, false));
        sm.rSetting(new Setting("Only Weapon", this, false));
        sm.rSetting(new Setting("Target Players", this, true));
        sm.rSetting(new Setting("Target Mobs", this, false));
    }

    public static float getExtraMarginFor(Entity entity) {
        Hitboxes inst = INSTANCE;
        if (inst == null || !inst.isToggled() || entity == null || mc == null || mc.player == null) {
            return 0.0f;
        }
        if (entity == mc.player || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return 0.0f;
        }
        if (inst.getBool("Only Weapon", false)) {
            ItemStack held = mc.player.getMainHandItem();
            if (!ModuleUtils.isHoldingWeapon(held) && !isSpear(held)) {
                return 0.0f;
            }
        }
        if (TargetFilterManager.isFiltered(entity)) {
            return 0.0f;
        }
        if (entity instanceof Player player) {
            if (!inst.getBool("Target Players", true) || AntiBot.isBot(player) || Teams.isTeam(player)) {
                return 0.0f;
            }
        } else if (!inst.getBool("Target Mobs", false)) {
            return 0.0f;
        }
        return (float) inst.getDouble("Expand", 0.25);
    }

    public static Vec3 clampInteractLocation(Entity entity, Vec3 hitLocation) {
        if (entity == null || hitLocation == null) {
            return hitLocation;
        }
        float extra = getExtraMarginFor(entity);
        if (extra <= 0.0f) {
            return hitLocation;
        }
        return clampBoxHitVector(entity.getBoundingBox(), entity.position(), extra, hitLocation);
    }

    public static Vec3 clampBoxHitVector(AABB baseBox, Vec3 entityBase, float extra, Vec3 hitLocation) {
        if (baseBox == null || entityBase == null || hitLocation == null || extra <= 0.0f) {
            return hitLocation;
        }
        double halfW = Math.max(0.05, (baseBox.maxX - baseBox.minX) * 0.5);
        double halfD = Math.max(0.05, (baseBox.maxZ - baseBox.minZ) * 0.5);
        double minRelY = baseBox.minY - entityBase.y;
        double maxRelY = baseBox.maxY - entityBase.y;
        double expHalfW = halfW + extra;
        double expHalfD = halfD + extra;
        double expMaxY = maxRelY + extra;
        double expMinY = minRelY - extra;

        double rx = hitLocation.x;
        if (Math.abs(rx) > halfW && expHalfW > 1.0e-5) {
            double ratio = Math.min(1.0, Math.abs(rx) / expHalfW);
            rx = Math.copySign(halfW * ratio, rx);
        }

        double rz = hitLocation.z;
        if (Math.abs(rz) > halfD && expHalfD > 1.0e-5) {
            double ratio = Math.min(1.0, Math.abs(rz) / expHalfD);
            rz = Math.copySign(halfD * ratio, rz);
        }

        double ry = hitLocation.y;
        if (ry > maxRelY && expMaxY > 1.0e-5) {
            double ratio = Math.min(1.0, ry / expMaxY);
            ry = maxRelY * ratio;
        } else if (ry < minRelY && Math.abs(expMinY) > 1.0e-5) {
            double ratio = Math.min(1.0, ry / expMinY);
            ry = minRelY * ratio;
        }

        return new Vec3(rx, ry, rz);
    }

    private static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        String desc = item.getDescriptionId();
        return desc.toLowerCase().contains("spear");
    }

    private double getDouble(String name, double fallback) {
        Setting s = setting(name);
        return s != null ? s.getValDouble() : fallback;
    }

    private boolean getBool(String name, boolean fallback) {
        Setting s = setting(name);
        return s != null ? s.getValBoolean() : fallback;
    }

    private Setting setting(String name) {
        ImnotcheatingyouareClient client = ImnotcheatingyouareClient.INSTANCE;
        if (client == null || client.settingsManager == null) {
            return null;
        }
        SettingsManager sm = client.settingsManager;
        return sm.getSettingByName(this, name);
    }
}
