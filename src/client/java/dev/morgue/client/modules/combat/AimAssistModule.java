package dev.morgue.client.modules.combat;

import dev.morgue.client.MorgueClient;
import dev.morgue.client.ai.TargetIntelligence;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Visible, manual-input aim assistance. Never runs alongside automated combat modules. */
public final class AimAssistModule {
    private boolean enabled;
    private double range = 4.5;
    private double fov = 90.0;
    private float smoothing = 0.25F;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gui.screen() != null || !client.options.keyAttack.isDown()) return;
        if (MorgueClient.maceHitter().isEnabled() || MorgueClient.killAura().isEnabled()) return;

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) return;
        List<LivingEntity> candidates = client.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                entity -> entity != player && entity instanceof Player && entity.isAlive()
                        && !entity.isRemoved() && !player.isAlliedTo(entity)
                        && player.hasLineOfSight(entity)
        );
        LivingEntity target = TargetIntelligence.selectBest(
                player, candidates, entity -> isWithinFov(player, entity),
                TargetIntelligence.Priority.CLOSEST, range
        );
        if (target == null) return;

        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        player.setYRot(player.getYRot() + wrapDegrees(targetYaw - player.getYRot()) * smoothing);
        player.setXRot(player.getXRot() + wrapDegrees(targetPitch - player.getXRot()) * smoothing);
    }

    private boolean isWithinFov(Player player, LivingEntity target) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        return Math.abs(wrapDegrees(targetYaw - player.getYRot())) <= fov / 2.0;
    }

    private static float wrapDegrees(float value) {
        value %= 360.0F;
        if (value >= 180.0F) value -= 360.0F;
        if (value < -180.0F) value += 360.0F;
        return value;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public double getRange() { return range; }
    public void setRange(double value) { range = Math.max(1.0, Math.min(6.0, value)); }
    public double getFov() { return fov; }
    public void setFov(double value) { fov = Math.max(15.0, Math.min(180.0, value)); }
    public float getSmoothing() { return smoothing; }
    public void setSmoothing(float value) { smoothing = Math.max(0.05F, Math.min(1.0F, value)); }
}
