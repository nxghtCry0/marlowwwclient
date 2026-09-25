package com.eclipseware.imnotcheatingyouare.client.module.impl;

import com.eclipseware.imnotcheatingyouare.client.ImnotcheatingyouareClient;
import com.eclipseware.imnotcheatingyouare.client.module.Category;
import com.eclipseware.imnotcheatingyouare.client.module.Module;
import com.eclipseware.imnotcheatingyouare.client.setting.Setting;
import com.eclipseware.imnotcheatingyouare.client.utils.FriendManager;
import com.eclipseware.imnotcheatingyouare.client.utils.ModuleUtils;
import com.eclipseware.imnotcheatingyouare.client.utils.RotationManager;
import com.eclipseware.imnotcheatingyouare.client.utils.TargetFilterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CrystalAura extends Module {
    private enum Action { NONE, BREAK, PLACE, OBBY }

    private final Setting placeRange;
    private final Setting breakRange;
    private final Setting wallRange;
    private final Setting targetRange;
    private final Setting placeDelay;
    private final Setting breakDelay;
    private final Setting minDamage;
    private final Setting maxSelfDamage;
    private final Setting facePlaceHealth;
    private final Setting antiSuicide;
    private final Setting rotate;
    private final Setting swapMode;
    private final Setting autoObsidian;
    private final Setting placeOneThirteen;
    private final Setting excludeBedrock;
    private final Setting requireHeld;
    private final Setting pauseOnEat;
    private final Setting inhibit;

    private int placeTicks;
    private int breakTicks;
    private Action pending = Action.NONE;
    private BlockPos pendingPos;
    private EndCrystal pendingCrystal;
    private int aimedTicks;
    private Player target;

    private final Map<BlockPos, Integer> placedAt = new HashMap<>();
    private final Map<Integer, Integer> attackedIds = new HashMap<>();

    public CrystalAura() {
        super("CrystalAura", Category.Crystal, "Places and breaks end crystals with real explosion damage math, clean packet order and movement-corrected silent aim.");
        setSubCategory("Semi-Blatant");

        placeRange = new Setting("Place Range", this, 4.5, 1.0, 6.0, false);
        breakRange = new Setting("Break Range", this, 4.5, 1.0, 6.0, false);
        wallRange = new Setting("Walls Range", this, 3.0, 0.0, 6.0, false);
        targetRange = new Setting("Target Range", this, 10.0, 4.0, 16.0, false);
        placeDelay = new Setting("Place Delay", this, 1.0, 0.0, 10.0, true);
        breakDelay = new Setting("Break Delay", this, 1.0, 0.0, 10.0, true);
        minDamage = new Setting("Min Damage", this, 5.0, 0.0, 20.0, false);
        maxSelfDamage = new Setting("Max Self Damage", this, 8.0, 0.0, 20.0, false);
        facePlaceHealth = new Setting("Face Place Health", this, 8.0, 0.0, 20.0, false);
        antiSuicide = new Setting("Anti-Suicide", this, true);
        rotate = new Setting("Rotate", this, true);
        ArrayList<String> swaps = new ArrayList<>();
        swaps.add("Silent");
        swaps.add("Normal");
        swaps.add("None");
        swapMode = new Setting("Swap", this, "Silent", swaps);
        autoObsidian = new Setting("Auto Obsidian", this, true);
        placeOneThirteen = new Setting("1.13+ Place", this, true);
        excludeBedrock = new Setting("Exclude Bedrock", this, false);
        requireHeld = new Setting("Require held", this, false);
        pauseOnEat = new Setting("Pause On Eat", this, true);
        inhibit = new Setting("Inhibit", this, true);

        var sm = ImnotcheatingyouareClient.INSTANCE.settingsManager;
        for (Setting s : new Setting[]{placeRange, breakRange, wallRange, targetRange, placeDelay, breakDelay, minDamage, maxSelfDamage,
                facePlaceHealth, antiSuicide, rotate, swapMode, autoObsidian, placeOneThirteen, excludeBedrock, requireHeld, pauseOnEat, inhibit}) {
            sm.rSetting(s);
        }
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
        RotationManager.requestReturn();
    }

    private void reset() {
        placeTicks = 0;
        breakTicks = 0;
        pending = Action.NONE;
        pendingPos = null;
        pendingCrystal = null;
        aimedTicks = 0;
        target = null;
        placedAt.clear();
        attackedIds.clear();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.gui.screen() != null || mc.player.isDeadOrDying()) return;
        if (pauseOnEat.getValBoolean() && mc.player.isUsingItem()) return;
        if (requireHeld.getValBoolean() && !holdingCrystal()) return;

        int tick = mc.player.tickCount;
        placedAt.values().removeIf(t -> tick - t > 20);
        attackedIds.values().removeIf(t -> tick - t > 10);
        if (placeTicks > 0) placeTicks--;
        if (breakTicks > 0) breakTicks--;

        target = findTarget();

        if (pending != Action.NONE) {
            if (!stillValid()) {
                pending = Action.NONE;
            } else if (aim(aimPoint())) {
                execute();
                return;
            } else {
                return;
            }
        }

        if (target == null) return;

        EndCrystal crystal = breakTicks == 0 ? bestCrystal() : null;
        if (crystal != null) {
            queue(Action.BREAK, null, crystal);
            if (aim(aimPoint())) execute();
            return;
        }

        if (placeTicks == 0 && crystalSlot() != -2) {
            BlockPos place = bestPlacement();
            if (place != null) {
                queue(Action.PLACE, place, null);
                if (aim(aimPoint())) execute();
                return;
            }
            if (autoObsidian.getValBoolean() && ModuleUtils.getObsidianSlot() != -1) {
                BlockPos obby = bestObsidian();
                if (obby != null) {
                    queue(Action.OBBY, obby, null);
                    if (aim(aimPoint())) execute();
                }
            }
        }
    }

    private void queue(Action action, BlockPos pos, EndCrystal crystal) {
        if (pending != action || !java.util.Objects.equals(pos, pendingPos) || crystal != pendingCrystal) aimedTicks = 0;
        pending = action;
        pendingPos = pos;
        pendingCrystal = crystal;
    }

    private boolean stillValid() {
        return switch (pending) {
            case BREAK -> pendingCrystal != null && pendingCrystal.isAlive() && canBreak(pendingCrystal);
            case PLACE -> pendingPos != null && canPlaceCrystal(pendingPos) && inPlaceRange(pendingPos);
            case OBBY -> pendingPos != null && mc.level.getBlockState(pendingPos).canBeReplaced() && inPlaceRange(pendingPos);
            default -> false;
        };
    }

    private Vec3 aimPoint() {
        return switch (pending) {
            case BREAK -> pendingCrystal.getBoundingBox().getCenter();
            case PLACE -> new Vec3(pendingPos.getX() + 0.5, pendingPos.getY() + 1.0, pendingPos.getZ() + 0.5);
            case OBBY -> new Vec3(pendingPos.getX() + 0.5, pendingPos.getY(), pendingPos.getZ() + 0.5);
            default -> mc.player.getEyePosition();
        };
    }

    private boolean aim(Vec3 point) {
        if (!rotate.getValBoolean()) return true;
        float[] rots = ModuleUtils.getRotations(mc.player.getEyePosition(), point);
        RotationManager.keepRotated(rots[0], rots[1], 180f, true);
        float yawDiff = Math.abs(Mth.wrapDegrees(rots[0] - RotationManager.getServerYaw()));
        float pitchDiff = Math.abs(rots[1] - RotationManager.getServerPitch());
        if (yawDiff < 2f && pitchDiff < 2f) return true;
        return ++aimedTicks >= 2;
    }

    private void execute() {
        switch (pending) {
            case BREAK -> doBreak(pendingCrystal);
            case PLACE -> doPlace(pendingPos);
            case OBBY -> doObsidian(pendingPos);
            default -> {
            }
        }
        pending = Action.NONE;
        pendingPos = null;
        pendingCrystal = null;
        aimedTicks = 0;
    }

    private void doBreak(EndCrystal crystal) {
        AutoTotem.triggerInputPause();
        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        attackedIds.put(crystal.getId(), mc.player.tickCount);
        breakTicks = (int) breakDelay.getValDouble();
    }

    private void doPlace(BlockPos base) {
        int slot = crystalSlot();
        if (slot == -1) return;
        Vec3 hitVec = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        if (use(slot, new BlockHitResult(hitVec, Direction.UP, base, false))) {
            placedAt.put(base.above(), mc.player.tickCount);
        }
        placeTicks = (int) placeDelay.getValDouble();
    }

    private void doObsidian(BlockPos pos) {
        int slot = ModuleUtils.getObsidianSlot();
        if (slot == -1) return;
        BlockPos support = pos.below();
        Vec3 hitVec = new Vec3(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
        use(slot, new BlockHitResult(hitVec, Direction.UP, support, false));
        placeTicks = (int) placeDelay.getValDouble();
    }

    private boolean use(int slot, BlockHitResult hit) {
        AutoTotem.triggerInputPause();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        int original = ModuleUtils.getSelectedSlot();
        boolean swapped = false;
        if (slot == 40) {
            hand = InteractionHand.OFF_HAND;
        } else if (slot != original) {
            if (swapMode.getValString().equals("None")) return false;
            ModuleUtils.switchToSlot(slot);
            swapped = true;
        }
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
        if (result.consumesAction()) mc.player.swing(hand, SwingAnimation.DEFAULT, true);
        if (swapped && swapMode.getValString().equals("Silent")) ModuleUtils.switchToSlot(original);
        return result.consumesAction();
    }

    private int crystalSlot() {
        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) return 40;
        if (mc.player.getMainHandItem().is(Items.END_CRYSTAL)) return ModuleUtils.getSelectedSlot();
        if (swapMode.getValString().equals("None")) return -1;
        return ModuleUtils.getCrystalSlot();
    }

    private boolean holdingCrystal() {
        return mc.player.getMainHandItem().is(Items.END_CRYSTAL) || mc.player.getOffhandItem().is(Items.END_CRYSTAL);
    }

    private Player findTarget() {
        double range = targetRange.getValDouble();
        Player best = null;
        double bestScore = Double.MAX_VALUE;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            if (FriendManager.isFriend(p) || TargetFilterManager.isFiltered(p) || AntiBot.isBot(p) || Teams.isTeam(p)) continue;
            double d = mc.player.distanceTo(p);
            if (d > range) continue;
            double score = d * 2.0 + p.getHealth() + p.getAbsorptionAmount();
            if (score < bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private boolean canBreak(EndCrystal crystal) {
        Vec3 center = crystal.getBoundingBox().getCenter();
        double dist = mc.player.getEyePosition().distanceTo(center);
        if (dist > breakRange.getValDouble()) return false;
        if (!RotationManager.hasLineOfSight(mc.player.getEyePosition(), center) && dist > wallRange.getValDouble()) return false;
        return true;
    }

    private EndCrystal bestCrystal() {
        double r = breakRange.getValDouble();
        EndCrystal best = null;
        double bestScore = 0;
        for (EndCrystal crystal : mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(r + 1))) {
            if (!crystal.isAlive() || !canBreak(crystal)) continue;
            if (inhibit.getValBoolean() && attackedIds.containsKey(crystal.getId())) continue;
            Vec3 pos = crystal.position();
            double self = explosionDamage(mc.player, pos);
            if (!selfSafe(self)) continue;
            double dmg = explosionDamage(target, pos);
            boolean ours = placedAt.containsKey(crystal.blockPosition());
            if (!ours && !meetsMin(dmg)) continue;
            double score = dmg - self * 0.5 + (ours ? 2.0 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best = crystal;
            }
        }
        return best;
    }

    private boolean inPlaceRange(BlockPos base) {
        Vec3 top = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        double dist = mc.player.getEyePosition().distanceTo(top);
        if (dist > placeRange.getValDouble()) return false;
        return RotationManager.hasLineOfSight(mc.player.getEyePosition(), top) || dist <= wallRange.getValDouble();
    }

    private boolean canPlaceCrystal(BlockPos base) {
        BlockState state = mc.level.getBlockState(base);
        if (!state.is(Blocks.OBSIDIAN) && !(state.is(Blocks.BEDROCK) && !excludeBedrock.getValBoolean())) return false;
        BlockPos up = base.above();
        if (!mc.level.isEmptyBlock(up)) return false;
        if (!placeOneThirteen.getValBoolean() && !mc.level.isEmptyBlock(up.above())) return false;
        AABB box = new AABB(up).expandTowards(0, 1, 0);
        for (Entity e : mc.level.getEntities((Entity) null, box)) {
            if (e instanceof EndCrystal ec && attackedIds.containsKey(ec.getId())) continue;
            if (e.isAlive()) return false;
        }
        return true;
    }

    private BlockPos bestPlacement() {
        int r = (int) Math.ceil(placeRange.getValDouble());
        BlockPos origin = mc.player.blockPosition();
        BlockPos best = null;
        double bestScore = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos base = origin.offset(x, y, z);
                    if (!canPlaceCrystal(base) || !inPlaceRange(base)) continue;
                    Vec3 explosion = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
                    double self = explosionDamage(mc.player, explosion);
                    if (!selfSafe(self)) continue;
                    double dmg = explosionDamage(target, explosion);
                    if (!meetsMin(dmg)) continue;
                    double score = dmg - self * 0.5;
                    if (score > bestScore) {
                        bestScore = score;
                        best = base;
                    }
                }
            }
        }
        return best;
    }

    private BlockPos bestObsidian() {
        BlockPos feet = target.blockPosition();
        BlockPos best = null;
        double bestScore = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 0; y++) {
                    BlockPos pos = feet.offset(x, y, z);
                    if (!mc.level.getBlockState(pos).canBeReplaced()) continue;
                    BlockState support = mc.level.getBlockState(pos.below());
                    if (support.getCollisionShape(mc.level, pos.below()).isEmpty()) continue;
                    if (!mc.level.isEmptyBlock(pos.above())) continue;
                    if (!inPlaceRange(pos.below())) continue;
                    if (!mc.level.getEntities((Entity) null, new AABB(pos).expandTowards(0, 2, 0)).isEmpty()) continue;
                    Vec3 explosion = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    double self = explosionDamage(mc.player, explosion);
                    if (!selfSafe(self)) continue;
                    double dmg = explosionDamage(target, explosion);
                    if (dmg > bestScore && meetsMin(dmg)) {
                        bestScore = dmg;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    private boolean meetsMin(double dmg) {
        if (target == null) return false;
        double health = target.getHealth() + target.getAbsorptionAmount();
        if (dmg >= health) return true;
        if (health <= facePlaceHealth.getValDouble()) return dmg >= 1.0;
        return dmg >= minDamage.getValDouble();
    }

    private boolean selfSafe(double self) {
        if (self > maxSelfDamage.getValDouble()) return false;
        if (antiSuicide.getValBoolean() && self >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0) return false;
        return true;
    }

    private double explosionDamage(LivingEntity entity, Vec3 explosion) {
        if (entity == null) return 0;
        double diameter = 12.0;
        double dist = Math.sqrt(entity.distanceToSqr(explosion)) / diameter;
        if (dist > 1.0) return 0;
        double exposure = exposure(explosion, entity);
        double impact = (1.0 - dist) * exposure;
        double damage = (impact * impact + impact) / 2.0 * 7.0 * diameter + 1.0;
        damage *= 1.5;
        double armor = entity.getArmorValue();
        double toughness = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        double f = Mth.clamp(armor - damage / (2.0 + toughness / 4.0), armor * 0.2, 20.0);
        damage *= 1.0 - f / 25.0;
        if (entity.hasEffect(MobEffects.RESISTANCE)) {
            int amp = entity.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1;
            damage *= Math.max(0.0, 1.0 - amp * 0.2);
        }
        return Math.max(0.0, damage);
    }

    private double exposure(Vec3 source, Entity entity) {
        AABB box = entity.getBoundingBox();
        double sx = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double sy = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double sz = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double ox = (1.0 - Math.floor(1.0 / sx) * sx) / 2.0;
        double oz = (1.0 - Math.floor(1.0 / sz) * sz) / 2.0;
        int hits = 0;
        int total = 0;
        for (double a = 0; a <= 1.0; a += sx) {
            for (double b = 0; b <= 1.0; b += sy) {
                for (double c = 0; c <= 1.0; c += sz) {
                    Vec3 point = new Vec3(Mth.lerp(a, box.minX, box.maxX) + ox, Mth.lerp(b, box.minY, box.maxY), Mth.lerp(c, box.minZ, box.maxZ) + oz);
                    HitResult hit = mc.level.clip(new ClipContext(point, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
                    if (hit.getType() == HitResult.Type.MISS) hits++;
                    total++;
                }
            }
        }
        return total == 0 ? 0 : (double) hits / total;
    }
}
