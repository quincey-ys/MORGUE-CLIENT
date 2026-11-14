package dev.morgue.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Simple client-side auto-sprint helper using normal player movement state. */
public final class SprintModule {
    private boolean enabled;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null) return;
        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            player.setSprinting(true);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public void toggle() { enabled = !enabled; }
}
