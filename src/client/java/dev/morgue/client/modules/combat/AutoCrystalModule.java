package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;
import java.util.List;

/**
 * Client-side crystal helper that uses ordinary Minecraft item-use and attack
 * actions. It does not manipulate packets or attempt to bypass server checks.
 */
public final class AutoCrystalModule {
    private boolean enabled;
    private double range = 5.0;
    private long actionDelayMillis = 250L;
    private long nextActionAt;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) return;

        var player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) return;

        long now = System.currentTimeMillis();
        if (now < nextActionAt) return;

        List<Player> targets = client.level.getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(range),
                target -> target != player && target.isAlive() && !target.isRemoved()
                        && !target.isSpectator() && !player.isAlliedTo(target)
                        && player.hasLineOfSight(target)
        );
        Player target = targets.stream()
                .min(Comparator.comparingDouble(candidate -> player.distanceToSqr(candidate)))
                .orElse(null);
        if (target == null) return;

        List<EndCrystal> crystals = client.level.getEntitiesOfClass(
                EndCrystal.class,
                player.getBoundingBox().inflate(range),
                crystal -> crystal.isAlive() && crystal.distanceToSqr(target) <= 16.0
                        && player.distanceToSqr(crystal) > 9.0
                        && player.hasLineOfSight(crystal)
        );
        if (!crystals.isEmpty()) {
            EndCrystal crystal = crystals.stream()
                    .min(Comparator.comparingDouble(player::distanceToSqr))
                    .orElse(null);
            if (crystal != null && CombatActionCoordinator.tryClaim(client)) {
                client.gameMode.attack(player, crystal);
                player.swing(InteractionHand.MAIN_HAND,
                        player.getMainHandItem().getAttackAnimation(), true);
                nextActionAt = now + actionDelayMillis;
            }
            return;
        }

        int crystalSlot = findHotbarItem(player, Items.END_CRYSTAL);
        if (crystalSlot < 0) return;
        BlockPos base = findCrystalBase(client, player, target);
        if (base == null || !CombatActionCoordinator.tryClaim(client)) return;

        int previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(crystalSlot);
        Vec3 hitLocation = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hitLocation, Direction.UP, base, false));
        player.swing(InteractionHand.MAIN_HAND,
                player.getMainHandItem().getAttackAnimation(), true);
        player.getInventory().setSelectedSlot(previousSlot);
        nextActionAt = now + actionDelayMillis;
    }

    private BlockPos findCrystalBase(Minecraft client, Player player, Player target) {
        BlockPos center = target.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos candidate = center.offset(dx, dy, dz);
                    var state = client.level.getBlockState(candidate);
                    if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;
                    if (!client.level.getBlockState(candidate.above()).isAir()
                            || !client.level.getBlockState(candidate.above(2)).isAir()) continue;

                    Vec3 hit = new Vec3(candidate.getX() + 0.5,
                            candidate.getY() + 1.0, candidate.getZ() + 0.5);
                    double playerDistance = player.getEyePosition().distanceToSqr(hit);
                    double targetDistance = target.distanceToSqr(hit);
                    if (playerDistance > range * range || targetDistance > 16.0) continue;
                    if (targetDistance < bestDistance) {
                        bestDistance = targetDistance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private static int findHotbarItem(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) nextActionAt = 0L;
    }
    public double getRange() { return range; }
    public void setRange(double value) { range = Math.max(2.0, Math.min(6.0, value)); }
    public long getActionDelayMillis() { return actionDelayMillis; }
    public void setActionDelayMillis(long value) {
        actionDelayMillis = Math.max(100L, Math.min(1000L, value));
    }
}
