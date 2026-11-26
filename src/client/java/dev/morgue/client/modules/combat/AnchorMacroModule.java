package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Crosshair-driven respawn-anchor sequence. Hold the normal Use key while
 * looking at a support block or anchor. The Nether is always treated as safe:
 * a fully charged anchor is never activated there.
 */
public final class AnchorMacroModule {
    private boolean enabled;
    private long actionDelayMillis = 180L;
    private long nextActionAt;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null
                || !client.options.keyUse.isDown()
                || !(client.hitResult instanceof BlockHitResult hit)) return;

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) return;
        long now = System.currentTimeMillis();
        if (now < nextActionAt) return;

        BlockPos targetPos = hit.getBlockPos();
        var state = client.level.getBlockState(targetPos);
        int selectedSlot;

        if (state.is(Blocks.RESPAWN_ANCHOR)) {
            int charges = state.getValue(RespawnAnchorBlock.CHARGE);
            if (charges < 4) {
                selectedSlot = findHotbarItem(player, Items.GLOWSTONE);
                if (selectedSlot < 0) return;
            } else {
                // Respawn anchors do not explode in the Nether. Do not activate them there.
                if (client.level.dimension() == Level.NETHER) return;
                selectedSlot = findHotbarNonGlowstone(player);
                if (selectedSlot < 0) return;
            }
        } else {
            selectedSlot = findHotbarItem(player, Items.RESPAWN_ANCHOR);
            if (selectedSlot < 0) return;
        }

        int previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(selectedSlot);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND,
                player.getMainHandItem().getAttackAnimation(), true);
        player.getInventory().setSelectedSlot(previousSlot);
        nextActionAt = now + actionDelayMillis;
    }

    private static int findHotbarItem(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    private static int findHotbarNonGlowstone(Player player) {
        int current = player.getInventory().getSelectedSlot();
        if (!player.getInventory().getItem(current).is(Items.GLOWSTONE)) return current;
        for (int slot = 0; slot < 9; slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()
                    && !player.getInventory().getItem(slot).is(Items.GLOWSTONE)) return slot;
        }
        return -1;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) nextActionAt = 0L;
    }
    public long getActionDelayMillis() { return actionDelayMillis; }
    public void setActionDelayMillis(long value) {
        actionDelayMillis = Math.max(100L, Math.min(1000L, value));
    }
}
