package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CrystalAura extends Module {
    private Setting range;
    private Setting placeDelay;
    private Setting breakDelay;
    private Setting silentAim;
    private Setting smoothAim;
    private Setting aimSpeed;
    private Setting antiSuicide;
    private Setting requireHeld;

    private int placeTicks = 0;
    private int breakTicks = 0;
    
    private final Map<BlockPos, Long> recentObby = new HashMap<>();

    public CrystalAura() {
        super("CrystalAura", Category.Crystal, "Advanced Crystal Aura with Anti-Suicide. Use with KillAura for PVP.");
        setSubCategory("Semi-Blatant");

        range = new Setting("Range", this, 4.5, 1.0, 6.0, false);
        placeDelay = new Setting("Place Delay", this, 1.0, 0.0, 10.0, true);
        breakDelay = new Setting("Break Delay", this, 1.0, 0.0, 10.0, true);
        silentAim = new Setting("Silent Aim", this, true);
        smoothAim = new Setting("Smooth Aim", this, true);
        aimSpeed = new Setting("Aim Speed", this, 45.0, 1.0, 180.0, true);
        antiSuicide = new Setting("Anti-Suicide", this, true);
        requireHeld = new Setting("Require held", this, false);
        Setting facePlace = new Setting("Face Place Threshold", this, 10.0, 1.0, 20.0, true);
        Setting silentSwap = new Setting("Silent Swap", this, true);

        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(range);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(placeDelay);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(breakDelay);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(silentAim);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(silentSwap);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(smoothAim);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(aimSpeed);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(antiSuicide);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(requireHeld);
        ImnotcheatingyouareClient.INSTANCE.settingsManager.rSetting(facePlace);
    }

    private int deferredRevertSlot = -1;

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        
        Setting silentSwapS = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Silent Swap");
        boolean silentSwap = silentSwapS != null ? silentSwapS.getValBoolean() : true;
        
        if (deferredRevertSlot != -1) {
            if (silentSwap) {
                ModuleUtils.setServerSlot(deferredRevertSlot);
            } else {
                ModuleUtils.switchToSlot(deferredRevertSlot);
            }
            deferredRevertSlot = -1;
        }

        long now = System.currentTimeMillis();
        recentObby.entrySet().removeIf(entry -> now > entry.getValue());

        if (placeTicks > 0) placeTicks--;
        if (breakTicks > 0) breakTicks--;

        double r = range.getValDouble();
        Player target = getOptimalTarget(r);
        
        // Target is permitted regardless of grounding
        if (breakTicks == 0) {
            List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(r));
            for (EndCrystal crystal : crystals) {
                if (mc.player.distanceTo(crystal) <= r) {
                    if (isLethalToSelf(crystal.blockPosition())) continue;
                    
                    if (!RotationManager.hasLineOfSight(mc.player.getEyePosition(), crystal.position().add(0, 0.5, 0))) continue;
                    
                    if (silentAim.getValBoolean()) {
                        aimAt(crystal.position().add(0, 0.5, 0));
                        Vec3 eyePos = mc.player.getEyePosition();
                        Vec3 looking = Vec3.directionFromRotation(RotationManager.getServerPitch(), RotationManager.getServerYaw());
                        Vec3 to = eyePos.add(looking.scale(r + 0.5));
                        net.minecraft.world.phys.AABB aabb = mc.player.getBoundingBox().expandTowards(looking.scale(r + 0.5)).inflate(1.0D);
                        net.minecraft.world.phys.EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(mc.player, eyePos, to, aabb, (e) -> e == crystal, (r+2) * (r+2));
                        if (hitResult == null || hitResult.getEntity() != crystal) continue; 
                    }
                    
                    mc.gameMode.attack(mc.player, crystal);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    breakTicks = (int) breakDelay.getValDouble();
                    return;
                }
            }
        }

        if (placeTicks == 0) {
            if (requireHeld.getValBoolean() && !isHoldingCrystal()) {
                return;
            }

            int crystalSlot = ModuleUtils.findItemInHotbar(Items.END_CRYSTAL);
            if (crystalSlot == -1) return;

            BlockPos targetPos = findBestPlacement();
            if (targetPos != null) {
                if (silentAim.getValBoolean()) aimAt(Vec3.atCenterOf(targetPos));
                deferredRevertSlot = mc.player.getInventory().getSelectedSlot();
                
                if (silentSwap) ModuleUtils.setServerSlot(crystalSlot);
                else ModuleUtils.switchToSlot(crystalSlot);
                
                ModuleUtils.placeBlockPacket(targetPos, Direction.UP);
                placeTicks = Math.max(1, (int) placeDelay.getValDouble());
            } else {
                int obbySlot = ModuleUtils.findItemInHotbar(Items.OBSIDIAN);
                if (obbySlot != -1) {
                    BlockPos obbyPos = findBestObbyPlacement();
                    if (obbyPos != null) {
                        if (silentAim.getValBoolean()) aimAt(Vec3.atCenterOf(obbyPos.below()));
                        deferredRevertSlot = mc.player.getInventory().getSelectedSlot();
                        
                        if (silentSwap) ModuleUtils.setServerSlot(obbySlot);
                        else ModuleUtils.switchToSlot(obbySlot);
                        
                        ModuleUtils.placeBlockPacket(obbyPos.below(), Direction.UP);
                        recentObby.put(obbyPos.below(), now + 1500);
                        placeTicks = Math.max(1, (int) placeDelay.getValDouble());
                    }
                }
            }
        }
    }

    private boolean isHoldingCrystal() {
        return mc.player.getMainHandItem().is(Items.END_CRYSTAL) || mc.player.getOffhandItem().is(Items.END_CRYSTAL);
    }
    
    private void aimAt(Vec3 pos) {
        float speed = smoothAim.getValBoolean() ? (float) aimSpeed.getValDouble() : 180f;
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), pos);
        RotationManager.keepRotated(rots[0], rots[1], speed, false);
    }
    
    private Player getOptimalTarget(double r) {
        return mc.level.players().stream()
            .filter(p -> p != mc.player && p.isAlive() && !FriendManager.isFriend(p) && !AntiBot.isBot(p) && !Teams.isTeam(p) && mc.player.distanceTo(p) <= r)
            .min(Comparator.comparingDouble(p -> mc.player.distanceToSqr(p) + (p.getHealth() * 2.0)))
            .orElse(null);
    }

    private int getBestWeaponSlot() {
        int bestSlot = -1;
        double bestDamage = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            double damage = 1;
            String name = stack.getItem().toString().toLowerCase();
            if (name.contains("sword")) {
                if (name.contains("netherite")) damage = 8;
                else if (name.contains("diamond")) damage = 7;
                else if (name.contains("iron")) damage = 6;
                else damage = 5;
            } else if (name.contains("axe")) {
                if (name.contains("netherite") || name.contains("diamond")) damage = 9;
                else damage = 7;
            }
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private BlockPos findBestPlacement() {
        double r = range.getValDouble();
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos bestPos = null;
        double bestScore = 0;

        for (int x = (int) -r; x <= r; x++) {
            for (int y = (int) -r; y <= r; y++) {
                for (int z = (int) -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
                    BlockState state = mc.level.getBlockState(pos);
                    
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK) || recentObby.containsKey(pos)) {
                        if (mc.level.isEmptyBlock(pos.above()) && mc.level.isEmptyBlock(pos.above(2))) {
                            if (!RotationManager.hasLineOfSight(mc.player.getEyePosition(), new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5))) continue;
                            if (isLethalToSelf(pos.above())) continue;
                            
                            double damage = calculateDamage(pos.above());
                            if (damage <= 0) continue;
                            
                            double score = damage + (state.is(Blocks.OBSIDIAN) || recentObby.containsKey(pos) ? 2.0 : 0.0);
                            
                            if (score > bestScore) {
                                bestScore = score;
                                bestPos = pos;
                            }
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    private BlockPos findBestObbyPlacement() {
        double r = range.getValDouble();
        Player target = getOptimalTarget(r + 4);
        if (target == null) return null;

        BlockPos bestObby = null;
        double bestScore = 0;
        BlockPos targetFeet = target.blockPosition();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = targetFeet.offset(x, y, z);
                    if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > r * r) continue;
                    
                    if ((mc.level.getBlockState(pos.below()).isSolid() || recentObby.containsKey(pos.below())) && mc.level.getBlockState(pos).canBeReplaced() && 
                        mc.level.isEmptyBlock(pos.above()) && mc.level.isEmptyBlock(pos.above(2))) {
                        
                        if (!RotationManager.hasLineOfSight(mc.player.getEyePosition(), new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))) continue;
                        if (isLethalToSelf(pos.above())) continue;

                        
                        double damage = calcDmgToPlayer(pos.above(), target);
                        if (damage > bestScore) {
                            bestScore = damage;
                            bestObby = pos;
                        }
                    }
                }
            }
        }
        return bestObby;
    }

    private boolean isLethalToSelf(BlockPos crystalPos) {
        if (!antiSuicide.getValBoolean()) return false;
        
        Player target = getOptimalTarget(range.getValDouble() + 2);
        Setting facePlaceSetting = ImnotcheatingyouareClient.INSTANCE.settingsManager.getSettingByName(this, "Face Place Threshold");
        double fpThresh = facePlaceSetting != null ? facePlaceSetting.getValDouble() : 10.0;
        if (target != null && target.getHealth() <= fpThresh) {
            if (mc.player.getHealth() > 8.0f) return false;
        }

        double distSq = mc.player.distanceToSqr(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
        if (distSq > 16.0) return false; 
        
        Vec3 crystalVec = new Vec3(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
        Vec3 playerLegs = mc.player.position().add(0, 0.2, 0);
        
        net.minecraft.world.phys.HitResult result = mc.level.clip(new net.minecraft.world.level.ClipContext(
            crystalVec, playerLegs, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player
        ));
        
        if (result.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            double dmg = (20.0 / Math.max(1, distSq)) * (1.0 - (mc.player.getArmorValue() / 30.0));
            if (dmg >= mc.player.getHealth()) return true;
        }
        return false;
    }

    private double calculateDamage(BlockPos crystalPos) {
        double best = 0;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || FriendManager.isFriend(p) || AntiBot.isBot(p) || Teams.isTeam(p)) continue;
            best = Math.max(best, calcDmgToPlayer(crystalPos, p));
        }
        return best;
    }
    
    private double calcDmgToPlayer(BlockPos crystalPos, Player target) {
        double distSq = target.distanceToSqr(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
        if (distSq > 100) return 0;
        
        Vec3 crystalVec = new Vec3(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
        Vec3 targetBody = target.position().add(0, 1.0, 0);
        Vec3 targetFeet = target.position().add(0, 0.2, 0);
        
        net.minecraft.world.phys.HitResult bodyHit = mc.level.clip(new net.minecraft.world.level.ClipContext(
            crystalVec, targetBody, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, target
        ));
        net.minecraft.world.phys.HitResult feetHit = mc.level.clip(new net.minecraft.world.level.ClipContext(
            crystalVec, targetFeet, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, target
        ));
        
        double exposure = 0.0;
        if (bodyHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) exposure += 0.5;
        if (feetHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) exposure += 0.5;
        
        if (exposure == 0) return 0; 
        
        double rawDamage = (20.0 / Math.max(1, distSq)) * exposure;
        double armorMod = 1.0 - (target.getArmorValue() / 25.0); 
        return rawDamage * armorMod;
    }

    @Override
    public void onDisable() {
        recentObby.clear();
        RotationManager.requestReturn();
    }
}
