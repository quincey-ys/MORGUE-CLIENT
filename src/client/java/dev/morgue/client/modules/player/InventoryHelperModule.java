package dev.morgue.client.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Refills low-count hotbar stacks from the main inventory through vanilla's
 * normal container input path. It never writes directly into local inventory.
 */
public final class InventoryHelperModule {
    private boolean enabled;
    private int refillAt = 1;
    private long pendingUntil;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) return;
        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying() || System.currentTimeMillis() < pendingUntil) return;

        for (int hotbar = 0; hotbar < 9; hotbar++) {
            ItemStack current = player.getInventory().getItem(hotbar);
            if (current.isEmpty() || current.getMaxStackSize() <= 1 || current.getCount() > refillAt) continue;

            for (int inventory = 9; inventory < 36; inventory++) {
                ItemStack candidate = player.getInventory().getItem(inventory);
                if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(current, candidate)) {
                    client.gameMode.handleContainerInput(
                            player.inventoryMenu.containerId,
                            inventory,
                            hotbar,
                            ContainerInput.SWAP,
                            player
                    );
                    pendingUntil = System.currentTimeMillis() + 350L;
                    return;
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; if (!value) pendingUntil = 0L; }
    public int getRefillAt() { return refillAt; }
    public void setRefillAt(int value) { refillAt = Math.max(1, Math.min(16, value)); }
}
