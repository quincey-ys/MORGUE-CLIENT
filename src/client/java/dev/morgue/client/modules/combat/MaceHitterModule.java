package dev.morgue.client.modules.combat;

import dev.morgue.client.ai.TargetIntelligence;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Original MORGUE port of the user's first Mace Hitter design.
 *
 * This intentionally excludes the later spear reach, double-pop, and Breach
 * weapon-combination rework. Target selection and settings are kept local to
 * MORGUE and do not depend on AUTISM Client classes.
 */
public final class MaceHitterModule {
    public enum Priority {
        CLOSEST,
        LOWEST_HEALTH,
        THREAT
    }

    public enum TargetMode {
        PLAYERS,
        MOBS,
        ALL,
        CUSTOM
    }

    private boolean enabled;
    private boolean rotate = true;
    private boolean autoSwap = true;
    private boolean autoJump;
    private boolean fallingOnly = true;
    private boolean shieldStun;
    private boolean requireMace = true;
    private boolean ignoreWalls;
    private boolean fovCheck = true;
    private boolean teamsCheck = true;
    private boolean ignoreCooldown;
    private double range = 3.2;
    private double fov = 180.0;
    private double minimumFallDistance = 1.5;
    private long attackDelayMillis;
    private Priority priority = Priority.CLOSEST;
    private TargetMode targetMode = TargetMode.PLAYERS;
    private long nextAttackAt;
    private LivingEntity lastTarget;

    // Custom mode starts with players selected. Entity IDs are registry IDs.
    private final Set<String> selectedEntityTypes = new LinkedHashSet<>(Set.of("minecraft:player"));

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        var player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) {
            return;
        }

        Entity target = selectTarget(client);
        lastTarget = target instanceof LivingEntity living ? living : null;
        if (target == null) {
            return;
        }

        if (autoJump && player.onGround()) {
            player.jumpFromGround();
        }

        long now = System.currentTimeMillis();
        if (now < nextAttackAt) {
            return;
        }
        // Holding the mace directly must not be blocked by the vanilla attack-strength
        // meter. Mace Hitter's falling-smash checks and its own Attack Delay setting
        // remain authoritative; the server still decides whether an attack deals damage.
        boolean alreadyHoldingMace = isMace(player.getMainHandItem());
        if (!alreadyHoldingMace && !ignoreCooldown
                && player.getAttackStrengthScale(0.5F) < 1.0F) {
            return;
        }

        int originalSlot = player.getInventory().getSelectedSlot();

        // Shield stun is a separate axe tap, so it can happen even while grounded.
        if (shieldStun && target instanceof Player targetPlayer && targetPlayer.isBlocking()) {
            int axeSlot = findHotbarAxe(player);
            if (axeSlot >= 0) {
                if (!CombatActionCoordinator.tryClaim(client)) return;
                if (rotate) {
                    faceTarget(player, target);
                }
                player.getInventory().setSelectedSlot(axeSlot);
                client.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), true);
                player.getInventory().setSelectedSlot(originalSlot);
                nextAttackAt = now + attackDelayMillis;
                return;
            }
        }

        // Do not equip a mace until a falling smash can actually be attempted.
        if (fallingOnly && (player.fallDistance < minimumFallDistance
                || player.getDeltaMovement().y >= 0.0)) {
            return;
        }

        boolean usingMace = isMace(player.getMainHandItem());
        if (!usingMace && autoSwap) {
            int maceSlot = findHotbarItem(player, Items.MACE);
            if (maceSlot >= 0) {
                player.getInventory().setSelectedSlot(maceSlot);
                usingMace = true;
            }
        }
        if (requireMace && !usingMace) {
            player.getInventory().setSelectedSlot(originalSlot);
            return;
        }

        if (!CombatActionCoordinator.tryClaim(client)) {
            if (autoSwap && originalSlot != player.getInventory().getSelectedSlot()) {
                player.getInventory().setSelectedSlot(originalSlot);
            }
            return;
        }
        if (rotate) {
            faceTarget(player, target);
        }

        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), true);
        if (autoSwap && originalSlot != player.getInventory().getSelectedSlot()) {
            player.getInventory().setSelectedSlot(originalSlot);
        }
        nextAttackAt = now + attackDelayMillis;
    }

    private Entity selectTarget(Minecraft client) {
        var player = client.player;
        List<Entity> candidates = client.level.getEntities(
                player,
                player.getBoundingBox().inflate(range),
                entity -> entity.isAlive() && !entity.isRemoved()
                        && entity.isAttackable() && entity.isPickable()
        );

        return TargetIntelligence.selectBestEntity(
                player,
                candidates,
                candidate -> isTargetTypeAllowed(candidate)
                        && (!teamsCheck || !player.isAlliedTo(candidate))
                        && (ignoreWalls || player.hasLineOfSight(candidate))
                        && (!fovCheck || isWithinFov(player, candidate)),
                toIntelligencePriority(priority),
                range
        );
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

    private boolean isTargetTypeAllowed(Entity entity) {
        if (targetMode == TargetMode.CUSTOM) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            return selectedEntityTypes.contains(id)
                    || (selectedEntityTypes.contains("morgue:other") && !id.startsWith("minecraft:"));
        }
        if (entity instanceof Player) {
            return targetMode == TargetMode.PLAYERS || targetMode == TargetMode.ALL;
        }
        return switch (targetMode) {
            case PLAYERS -> false;
            case MOBS -> entity instanceof LivingEntity;
            case ALL -> true;
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
        double angle = Math.sqrt(yawDifference * yawDifference + pitchDifference * pitchDifference);
        return angle <= fov * 0.5;
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

    private static boolean isMace(ItemStack stack) {
        return stack.is(Items.MACE);
    }

    private static int findHotbarItem(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static int findHotbarAxe(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.WOODEN_AXE) || stack.is(Items.STONE_AXE)
                    || stack.is(Items.IRON_AXE) || stack.is(Items.GOLDEN_AXE)
                    || stack.is(Items.DIAMOND_AXE) || stack.is(Items.NETHERITE_AXE)) {
                return slot;
            }
        }
        return -1;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { this.enabled = !this.enabled; }
    public boolean isRotate() { return rotate; }
    public void setRotate(boolean rotate) { this.rotate = rotate; }
    public boolean isAutoSwap() { return autoSwap; }
    public void setAutoSwap(boolean autoSwap) { this.autoSwap = autoSwap; }
    public boolean isAutoJump() { return autoJump; }
    public void setAutoJump(boolean autoJump) { this.autoJump = autoJump; }
    public boolean isFallingOnly() { return fallingOnly; }
    public void setFallingOnly(boolean fallingOnly) { this.fallingOnly = fallingOnly; }
    public boolean isShieldStun() { return shieldStun; }
    public void setShieldStun(boolean shieldStun) { this.shieldStun = shieldStun; }
    public boolean isRequireMace() { return requireMace; }
    public void setRequireMace(boolean requireMace) { this.requireMace = requireMace; }
    public boolean isIgnoreWalls() { return ignoreWalls; }
    public void setIgnoreWalls(boolean ignoreWalls) { this.ignoreWalls = ignoreWalls; }
    public boolean isFovCheck() { return fovCheck; }
    public void setFovCheck(boolean fovCheck) { this.fovCheck = fovCheck; }
    public boolean isTeamsCheck() { return teamsCheck; }
    public void setTeamsCheck(boolean teamsCheck) { this.teamsCheck = teamsCheck; }
    public boolean isIgnoreCooldown() { return ignoreCooldown; }
    public void setIgnoreCooldown(boolean ignoreCooldown) { this.ignoreCooldown = ignoreCooldown; }
    public double getRange() { return range; }
    public void setRange(double range) { this.range = Math.max(1.0, Math.min(6.0, range)); }
    public double getFov() { return fov; }
    public void setFov(double fov) { this.fov = Math.max(1.0, Math.min(360.0, fov)); }
    public double getMinimumFallDistance() { return minimumFallDistance; }
    public void setMinimumFallDistance(double value) { minimumFallDistance = Math.max(0.0, Math.min(10.0, value)); }
    public long getAttackDelayMillis() { return attackDelayMillis; }
    public void setAttackDelayMillis(long value) { attackDelayMillis = Math.max(0L, Math.min(1000L, value)); }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority == null ? Priority.CLOSEST : priority; }
    public TargetMode getTargetMode() { return targetMode; }
    public void setTargetMode(TargetMode mode) { this.targetMode = mode == null ? TargetMode.PLAYERS : mode; }
    public Set<String> getSelectedEntityTypes() { return Set.copyOf(selectedEntityTypes); }
    public void setEntityTypeSelected(String id, boolean selected) {
        if (id == null || id.isBlank()) return;
        if (selected) selectedEntityTypes.add(id);
        else selectedEntityTypes.remove(id);
    }
    public LivingEntity getLastTarget() { return lastTarget; }
}
