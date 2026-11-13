package dev.morgue.client.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Standalone MORGUE Auto Totem.
 *
 * Inventory changes are sent through Minecraft's normal container-input path.
 * This module does not directly mutate client inventory state or invent packets.
 */
public final class AutoTotemModule {
    public enum Mode {
        SMART,
        SWITCH,
        PICK_UP
    }

    public enum SearchPriority {
        HOTBAR_FIRST,
        INVENTORY_FIRST
    }

    private boolean enabled;
    private Mode mode = Mode.SMART;
    private SearchPriority searchPriority = SearchPriority.HOTBAR_FIRST;
    private long switchDelayMillis;
    private long nextAttemptAt;
    private long pendingUntil;
    private boolean pauseWhileUsingItem = true;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()) {
            return;
        }
        if (pauseWhileUsingItem && player.isUsingItem()) {
            return;
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.TOTEM_OF_UNDYING)) {
            pendingUntil = 0L;
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextAttemptAt || now < pendingUntil) {
            return;
        }

        int inventoryIndex = findTotem(player);
        if (inventoryIndex < 0) {
            return;
        }

        // InventoryMenu maps hotbar slots to 36..44 and main inventory to 9..35.
        int menuSlot = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                menuSlot,
                Inventory.SLOT_OFFHAND,
                ContainerInput.SWAP,
                player
        );

        nextAttemptAt = now + switchDelayMillis;
        // Wait for the authoritative inventory update before retrying.
        pendingUntil = now + 300L;
    }

    private int findTotem(Player player) {
        return switch (mode) {
            case SMART -> findByPriority(player, searchPriority);
            case SWITCH -> findHotbarTotem(player);
            case PICK_UP -> findMainInventoryTotem(player);
        };
    }

    private int findByPriority(Player player, SearchPriority priority) {
        if (priority == SearchPriority.INVENTORY_FIRST) {
            int main = findMainInventoryTotem(player);
            return main >= 0 ? main : findHotbarTotem(player);
        }
        int hotbar = findHotbarTotem(player);
        return hotbar >= 0 ? hotbar : findMainInventoryTotem(player);
    }

    private int findHotbarTotem(Player player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }
        }
        return -1;
    }

    private int findMainInventoryTotem(Player player) {
        for (int slot = 9; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }
        }
        return -1;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            pendingUntil = 0L;
            nextAttemptAt = 0L;
        }
    }
    public void toggle() { setEnabled(!enabled); }
    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode == null ? Mode.SMART : mode; }
    public SearchPriority getSearchPriority() { return searchPriority; }
    public void setSearchPriority(SearchPriority priority) {
        this.searchPriority = priority == null ? SearchPriority.HOTBAR_FIRST : priority;
    }
    public long getSwitchDelayMillis() { return switchDelayMillis; }
    public void setSwitchDelayMillis(long delay) {
        this.switchDelayMillis = Math.max(0L, Math.min(1000L, delay));
    }
    public boolean isPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public void setPauseWhileUsingItem(boolean pause) { this.pauseWhileUsingItem = pause; }
}
