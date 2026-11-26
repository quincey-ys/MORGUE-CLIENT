package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Shared, vanilla-path combat hit timing for MORGUE's combat modules.
 *
 * A hit is only issued when the selected target is actually under the current
 * view ray, the target is pickable and the configured cooldown has elapsed.
 * The attack itself uses MultiPlayerGameMode.attack rather than custom packets.
 */
public final class CombatHitController {
    private static final Random RANDOM = new Random();
    private static long nextClickAtMillis;

    private CombatHitController() {
    }

    public static boolean isReady(Minecraft client, LivingEntity target, double range,
                                  boolean ignoreWalls, boolean ignoreCooldown) {
        if (client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null
                || target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying() || player.isUsingItem()) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now < nextClickAtMillis) {
            return false;
        }
        if (!ignoreCooldown && player.getAttackStrengthScale(0.5F) < 1.0F) {
            return false;
        }

        return isTargetUnderViewRay(client, target, range, ignoreWalls);
    }

    public static boolean isTargetUnderViewRay(Minecraft client, LivingEntity target,
                                                double range, boolean ignoreWalls) {
        if (client == null || client.player == null || client.level == null
                || target == null || !target.isAlive() || !target.isPickable()) {
            return false;
        }

        Player player = client.player;
        double safeRange = Math.max(0.1D, range);
        Vec3 eyes = player.getEyePosition();
        Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 end = eyes.add(direction.scale(safeRange));
        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(safeRange))
                .inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player,
                eyes,
                end,
                searchBox,
                EntitySelector.CAN_BE_PICKED.and(entity -> entity == target),
                safeRange * safeRange
        );
        if (hit == null || hit.getEntity() != target) {
            return false;
        }

        return ignoreWalls || player.hasLineOfSight(target);
    }

    public static boolean attack(Minecraft client, LivingEntity target, double range,
                                 boolean ignoreWalls, boolean ignoreCooldown, boolean wTapEnabled) {
        if (!isReady(client, target, range, ignoreWalls, ignoreCooldown)) {
            return false;
        }
        if (!CombatActionCoordinator.tryClaim(client)) {
            return false;
        }

        Player player = client.player;
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND,
                player.getMainHandItem().getAttackAnimation(), true);

        // AUTISM's click scheduler gates attempts at approximately 5–8 clicks per
        // second, while vanilla attack strength remains the actual damage cooldown.
        int cps = 5 + RANDOM.nextInt(4);
        nextClickAtMillis = System.currentTimeMillis() + Math.round(1000.0D / cps);

        if (wTapEnabled) {
            CombatMovementAssist.requestWTap(client);
        }
        return true;
    }

    public static void reset() {
        nextClickAtMillis = 0L;
    }
}
