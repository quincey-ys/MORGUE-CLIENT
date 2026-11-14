package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;

/**
 * Gives the combat modules one shared attack slot per client tick.
 * Mace Hitter is evaluated first; KillAura may act when Mace Hitter has no
 * eligible attack. This prevents duplicate attacks and conflicting rotations.
 */
public final class CombatActionCoordinator {
    private static int claimedTick = Integer.MIN_VALUE;

    private CombatActionCoordinator() {}

    public static boolean tryClaim(Minecraft client) {
        if (client == null || client.player == null) return false;
        int tick = client.player.tickCount;
        if (claimedTick == tick) return false;
        claimedTick = tick;
        return true;
    }
}
