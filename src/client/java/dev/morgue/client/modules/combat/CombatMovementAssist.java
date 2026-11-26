package dev.morgue.client.modules.combat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Optional movement input helpers used while a MORGUE combat module has a valid target.
 * Input changes are restored at the end of the same client tick.
 */
public final class CombatMovementAssist {
    private static KeyMapping forwardKey;
    private static KeyMapping leftKey;
    private static KeyMapping rightKey;
    private static boolean originalForward;
    private static boolean originalLeft;
    private static boolean originalRight;
    private static boolean changedForward;
    private static boolean changedLeft;
    private static boolean changedRight;
    private static int pendingWTapTicks;

    private CombatMovementAssist() {
    }

    public static void beginClientTick(Minecraft client) {
        restoreInput(client);
        if (client == null || client.player == null || client.options == null
                || client.gui.screen() != null) {
            return;
        }

        forwardKey = client.options.keyUp;
        leftKey = client.options.keyLeft;
        rightKey = client.options.keyRight;
        originalForward = forwardKey.isDown();
        originalLeft = leftKey.isDown();
        originalRight = rightKey.isDown();
        changedForward = false;
        changedLeft = false;
        changedRight = false;

        Player player = client.player;
        if (pendingWTapTicks > 0) {
            pendingWTapTicks = 0;
            if (originalForward && player.isSprinting()) {
                forwardKey.setDown(false);
                changedForward = true;
            }
        }

        if (shouldStrafe(client) && (originalForward || client.options.keyDown.isDown())
                && !originalLeft && !originalRight) {
            boolean strafeLeft = ((player.tickCount / 8) & 1) == 0;
            if (strafeLeft) {
                leftKey.setDown(true);
                changedLeft = true;
            } else {
                rightKey.setDown(true);
                changedRight = true;
            }
        }
    }

    public static void endClientTick(Minecraft client) {
        restoreInput(client);
    }

    public static void requestWTap(Minecraft client) {
        if (client == null || client.player == null || client.options == null
                || client.gui.screen() != null) {
            return;
        }
        if (client.options.keyUp.isDown() && client.player.isSprinting()) {
            pendingWTapTicks = 1;
        }
    }

    private static boolean shouldStrafe(Minecraft client) {
        Player player = client.player;
        MaceHitterModule mace = dev.morgue.client.MorgueClient.maceHitter();
        LivingEntity maceTarget = mace.getLastTarget();
        if (mace.isEnabled() && mace.isStrafeAssistEnabled()
                && isUsableTarget(player, maceTarget, mace.getRange())) {
            return true;
        }

        KillAuraModule aura = dev.morgue.client.MorgueClient.killAura();
        LivingEntity auraTarget = aura.getLastTarget();
        return aura.isEnabled() && aura.isStrafeAssistEnabled()
                && isUsableTarget(player, auraTarget, aura.getRange());
    }

    private static boolean isUsableTarget(Player player, LivingEntity target, double range) {
        return target != null && target.isAlive() && !target.isRemoved()
                && player.distanceTo(target) <= range;
    }

    private static void restoreInput(Minecraft client) {
        if (client == null || client.options == null) {
            changedForward = false;
            changedLeft = false;
            changedRight = false;
            return;
        }
        if (changedForward && forwardKey != null) forwardKey.setDown(originalForward);
        if (changedLeft && leftKey != null) leftKey.setDown(originalLeft);
        if (changedRight && rightKey != null) rightKey.setDown(originalRight);
        changedForward = false;
        changedLeft = false;
        changedRight = false;
    }
}
