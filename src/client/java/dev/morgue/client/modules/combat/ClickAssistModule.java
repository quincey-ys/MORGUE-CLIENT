package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

/** Repeats the user's held attack input at a fixed CPS, respecting vanilla cooldowns. */
public final class ClickAssistModule {
    private boolean enabled;
    private int clicksPerSecond = 8;
    private long nextClickAt;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null
                || !client.options.keyAttack.isDown()) return;
        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()
                || player.getAttackStrengthScale(0.5F) < 1.0F) return;
        if (!(client.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)
                || !target.isAlive() || target == player || !player.hasLineOfSight(target)) return;

        long now = System.currentTimeMillis();
        if (now < nextClickAt || !CombatActionCoordinator.tryClaim(client)) return;
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), true);
        nextClickAt = now + 1000L / clicksPerSecond;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; if (!value) nextClickAt = 0L; }
    public int getClicksPerSecond() { return clicksPerSecond; }
    public void setClicksPerSecond(int value) { clicksPerSecond = Math.max(1, Math.min(20, value)); }
}
