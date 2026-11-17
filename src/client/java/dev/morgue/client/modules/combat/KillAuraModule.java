package dev.morgue.client.modules.combat;

import dev.morgue.client.ai.TargetIntelligence;
import dev.morgue.client.MorgueHud;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MORGUE-native KillAura implementation.
 *
 * Implemented independently against Minecraft's public client/game APIs;
 * it does not import or depend on AUTISM Client classes.
 */
public final class KillAuraModule {
    public enum TargetMode {
        PLAYERS,
        HOSTILE_MOBS,
        ALL_LIVING,
        CUSTOM
    }

    public enum Priority {
        CLOSEST,
        LOWEST_HEALTH,
        THREAT
    }

    private boolean enabled;
    private boolean rotate = true;
    private boolean autoSword = true;
    private boolean switchBack = true;
    private boolean shieldBreak;
    private boolean ignoreWalls;
    private boolean teamsCheck = true;
    private boolean fovCheck = true;
    private boolean ignoreHurtTime;
    private boolean criticalOnly;
    private boolean pauseOnThrowables = true;
    private boolean attackSound;
    private int minimumAgeTicks;
    private double range = 3.2;
    private double fov = 180.0;
    private long attackDelayMillis;
    private long nextAttackAt;
    private TargetMode targetMode = TargetMode.PLAYERS;
    private Priority priority = Priority.CLOSEST;
    private LivingEntity lastTarget;
    private Entity observedTarget;
    private int observedDamageCooldown;
    private final Set<String> selectedEntityTypes = new LinkedHashSet<>(Set.of("minecraft:player"));

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) {
            return;
        }

        if (pauseOnThrowables && (isThrowable(player.getMainHandItem().getItem())
                || isThrowable(player.getOffhandItem().getItem()))) {
            return;
        }

        Entity target = selectTarget(client);
        lastTarget = target instanceof LivingEntity living ? living : null;
        if (target == null) {
            observedTarget = null;
            observedDamageCooldown = 0;
            return;
        }
        if (target instanceof LivingEntity living) {
            int currentDamageCooldown = living.damageCooldownTime;
            if (observedTarget == target && currentDamageCooldown > observedDamageCooldown + 1) {
                MorgueHud.triggerHitMarker();
            }
            observedDamageCooldown = currentDamageCooldown;
        } else {
            observedDamageCooldown = 0;
        }
        observedTarget = target;

        long now = System.currentTimeMillis();
        if (now < nextAttackAt || player.getAttackStrengthScale(0.5F) < 1.0F
                || (criticalOnly && !canLandVanillaCritical(player))) {
            return;
        }

        int originalSlot = player.getInventory().getSelectedSlot();
        boolean changedSlot = false;

        if (shieldBreak && target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
            int axe = findAxe(player);
            if (axe >= 0) {
                player.getInventory().setSelectedSlot(axe);
                changedSlot = true;
            }
        } else if (autoSword && !isSword(player.getMainHandItem().getItem())) {
            int sword = findSword(player);
            if (sword >= 0) {
                player.getInventory().setSelectedSlot(sword);
                changedSlot = true;
            }
        }

        if (!CombatActionCoordinator.tryClaim(client)) {
            // No attack was made, so always undo a temporary weapon selection.
            if (changedSlot) player.getInventory().setSelectedSlot(originalSlot);
            return;
        }
        if (rotate) {
            faceTarget(player, target);
        }

        client.gameMode.attack(player, target);
        if (attackSound) player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.45F, 1.55F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,
                player.getMainHandItem().getAttackAnimation(), true);

        if (changedSlot && switchBack) {
            player.getInventory().setSelectedSlot(originalSlot);
        }

        nextAttackAt = now + attackDelayMillis;
    }

    private Entity selectTarget(Minecraft client) {
        Player player = client.player;
        List<Entity> candidates = client.level.getEntities(
                player,
                player.getBoundingBox().inflate(range),
                entity -> entity.isAlive() && !entity.isRemoved()
                        && entity.isAttackable() && entity.isPickable()
        );

        return TargetIntelligence.selectBestEntity(
                player,
                candidates,
                candidate -> isAllowed(candidate)
                        && (!(candidate instanceof LivingEntity living)
                            || !ignoreHurtTime || living.damageCooldownTime <= 10)
                        && candidate.tickCount >= minimumAgeTicks
                        && (!teamsCheck || !player.isAlliedTo(candidate))
                        && (ignoreWalls || player.hasLineOfSight(candidate))
                        && (!fovCheck || isWithinFov(player, candidate)),
                toIntelligencePriority(priority),
                range
        );
    }


    private static boolean isThrowable(Item item) {
        return item == Items.SNOWBALL || item == Items.EGG || item == Items.ENDER_PEARL
                || item == Items.FIREWORK_ROCKET || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION;
    }

    /** Uses vanilla player state only; it does not spoof critical hits. */
    private static boolean canLandVanillaCritical(Player player) {
        return !player.onGround() && player.fallDistance > 0.0F
                && !player.isInWater() && !player.isPassenger() && !player.onClimbable();
    }

    private static TargetIntelligence.Priority toIntelligencePriority(Priority value) {
        if (value == null) {
            return TargetIntelligence.Priority.CLOSEST;
        }
        return switch (value) {
            case CLOSEST -> TargetIntelligence.Priority.CLOSEST;
            case LOWEST_HEALTH -> TargetIntelligence.Priority.LOWEST_HEALTH;
            case THREAT -> TargetIntelligence.Priority.THREAT;
        };
    }

    private boolean isAllowed(Entity entity) {
        if (targetMode == TargetMode.CUSTOM) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            return selectedEntityTypes.contains(id)
                    || (selectedEntityTypes.contains("morgue:other") && !id.startsWith("minecraft:"));
        }
        if (entity instanceof Player) {
            return targetMode == TargetMode.PLAYERS;
        }
        return switch (targetMode) {
            case PLAYERS -> false;
            case HOSTILE_MOBS -> entity instanceof LivingEntity
                    && !entity.getType().getCategory().isFriendly();
            case ALL_LIVING -> entity instanceof LivingEntity;
            case CUSTOM -> false;
        };
    }

    private boolean isWithinFov(Player player, Entity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        float yawDifference = wrapDegrees(targetYaw - player.getYRot());
        float pitchDifference = targetPitch - player.getXRot();
        return Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference) <= fov * 0.5;
    }

    private void faceTarget(Player player, Entity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    private static int findSword(Player player) {
        Item[] swords = {Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD};
        for (Item sword : swords) {
            for (int slot = 0; slot < 9; slot++) {
                if (player.getInventory().getItem(slot).is(sword)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private static int findAxe(Player player) {
        Item[] axes = {Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
                Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE};
        for (Item axe : axes) {
            for (int slot = 0; slot < 9; slot++) {
                if (player.getInventory().getItem(slot).is(axe)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private static boolean isSword(Item item) {
        return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD
                || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD
                || item == Items.STONE_SWORD || item == Items.WOODEN_SWORD;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { this.enabled = !this.enabled; }
    public boolean isRotate() { return rotate; }
    public void setRotate(boolean value) { rotate = value; }
    public boolean isAutoSword() { return autoSword; }
    public void setAutoSword(boolean value) { autoSword = value; }
    public boolean isSwitchBack() { return switchBack; }
    public void setSwitchBack(boolean value) { switchBack = value; }
    public boolean isShieldBreak() { return shieldBreak; }
    public void setShieldBreak(boolean value) { shieldBreak = value; }
    public boolean isIgnoreWalls() { return ignoreWalls; }
    public void setIgnoreWalls(boolean value) { ignoreWalls = value; }
    public boolean isTeamsCheck() { return teamsCheck; }
    public void setTeamsCheck(boolean value) { teamsCheck = value; }
    public boolean isFovCheck() { return fovCheck; }
    public void setFovCheck(boolean value) { fovCheck = value; }
    public double getRange() { return range; }
    public void setRange(double value) { range = Math.max(1.0, Math.min(6.0, value)); }
    public double getFov() { return fov; }
    public void setFov(double value) { fov = Math.max(1.0, Math.min(360.0, value)); }
    public long getAttackDelayMillis() { return attackDelayMillis; }
    public void setAttackDelayMillis(long value) { attackDelayMillis = Math.max(0, Math.min(1000, value)); }
    public TargetMode getTargetMode() { return targetMode; }
    public void setTargetMode(TargetMode value) { targetMode = value == null ? TargetMode.PLAYERS : value; }
    public Set<String> getSelectedEntityTypes() { return Set.copyOf(selectedEntityTypes); }
    public void setEntityTypeSelected(String id, boolean selected) {
        if (id == null || id.isBlank()) return;
        if (selected) selectedEntityTypes.add(id);
        else selectedEntityTypes.remove(id);
    }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority value) { priority = value == null ? Priority.CLOSEST : value; }
    public boolean isIgnoreHurtTime() { return ignoreHurtTime; }
    public void setIgnoreHurtTime(boolean value) { ignoreHurtTime = value; }
    public int getMinimumAgeTicks() { return minimumAgeTicks; }
    public void setMinimumAgeTicks(int value) { minimumAgeTicks = Math.max(0, Math.min(12000, value)); }
    public boolean isCriticalOnly() { return criticalOnly; }
    public void setCriticalOnly(boolean value) { criticalOnly = value; }
    public boolean isAttackSound() { return attackSound; }
    public void setAttackSound(boolean value) { attackSound = value; }
    public boolean isPauseOnThrowables() { return pauseOnThrowables; }
    public void setPauseOnThrowables(boolean value) { pauseOnThrowables = value; }
    public LivingEntity getLastTarget() { return lastTarget; }
}
