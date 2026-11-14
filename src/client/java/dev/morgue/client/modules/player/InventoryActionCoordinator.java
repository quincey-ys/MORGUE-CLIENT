package dev.morgue.client.modules.player;

import net.minecraft.client.Minecraft;

/** Serializes automated inventory actions so modules do not issue conflicting clicks in one tick. */
public final class InventoryActionCoordinator {
    private static int claimedTick = Integer.MIN_VALUE;

    private InventoryActionCoordinator() {}

    public static boolean tryClaim(Minecraft client) {
        if (client == null || client.player == null) return false;
        int tick = client.player.tickCount;
        if (claimedTick == tick) return false;
        claimedTick = tick;
        return true;
    }
}
