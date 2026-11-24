package dev.morgue.client.modules.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Shared, visible combat rotation used by MORGUE's combat modules.
 * Rotation is eased and capped per tick to avoid abrupt camera snapping.
 */
public final class CombatRotation {
    private static final float MAX_STEP_DEGREES = 40.0F;
    private static final float ALIGNMENT_TOLERANCE = 7.0F;

    private CombatRotation() {
    }

    /**
     * Turns toward a target and returns true once the player is aligned closely
     * enough for an attack attempt. Speed is clamped to [0.1, 1.0].
     */
    public static boolean rotateToward(Player player, LivingEntity target, double speed) {
        if (player == null || target == null) return false;

        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));

        float factor = (float) Math.max(0.1, Math.min(1.0, speed));
        float yawError = wrapDegrees(targetYaw - player.getYRot());
        float pitchError = targetPitch - player.getXRot();
        float maxStep = MAX_STEP_DEGREES * factor;

        float nextYaw = player.getYRot() + clamp(yawError * factor, -maxStep, maxStep);
        float nextPitch = player.getXRot() + clamp(pitchError * factor, -maxStep, maxStep);
        nextPitch = clamp(nextPitch, -90.0F, 90.0F);

        player.setYRot(nextYaw);
        player.setYHeadRot(nextYaw);
        player.setXRot(nextPitch);

        return Math.abs(wrapDegrees(targetYaw - nextYaw)) <= ALIGNMENT_TOLERANCE
                && Math.abs(targetPitch - nextPitch) <= ALIGNMENT_TOLERANCE;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }
}
