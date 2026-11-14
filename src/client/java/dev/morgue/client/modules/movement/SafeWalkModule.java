package dev.morgue.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Crouches when the player's current horizontal movement projects beyond a
 * supported block edge. The user's own sneak key always takes precedence.
 */
public final class SafeWalkModule {
    private boolean enabled;

    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return;
        Player player = client.player;
        boolean userSneaking = client.options.keyShift.isDown();
        if (!enabled || player.isSpectator() || player.isDeadOrDying()) {
            if (!userSneaking) player.setShiftKeyDown(false);
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        boolean shouldCrouch = false;
        if (player.onGround() && movement.horizontalDistanceSqr() > 1.0E-5) {
            double projectedX = player.getX() + movement.x * 2.0;
            double projectedZ = player.getZ() + movement.z * 2.0;
            BlockPos support = BlockPos.containing(projectedX, player.getY() - 0.5, projectedZ);
            shouldCrouch = client.level.getBlockState(support).isAir();
        }
        player.setShiftKeyDown(userSneaking || shouldCrouch);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public void toggle() { enabled = !enabled; }
}
