package dev.morgue.client.modules.player;

import dev.morgue.client.ClientNotifications;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Standalone MORGUE Auto Totem.
 *
 * Inventory changes use Minecraft's normal container-input path. Damage and
 * fall predictions are conservative local estimates, not server damage forecasts.
 */
public final class AutoTotemModule {
    public enum Mode { SMART, SWITCH, PICK_UP }
    public enum SearchPriority { HOTBAR_FIRST, INVENTORY_FIRST }

    private boolean enabled;
    private Mode mode = Mode.SMART;
    private SearchPriority searchPriority = SearchPriority.HOTBAR_FIRST;
    private long switchDelayMillis;
    private long nextAttemptAt;
    private long pendingUntil;
    private boolean pauseWhileUsingItem = true;
    private float activationHealth = 20.0F;
    private boolean predictRecentDamage = true;
    private boolean predictFallDamage = true;
    private boolean predictNearbyThreats = true;
    private boolean restoreDisplacedOffhand = true;
    private float lastEffectiveHealth = -1.0F;
    private float recentDamagePerTick;
    private int restoreMenuSlot = -1;
    private ItemStack displacedOffhand = ItemStack.EMPTY;

    public void tick(Minecraft client) {
        if ((!enabled && restoreMenuSlot < 0) || client == null || client.player == null
                || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()
                || (pauseWhileUsingItem && player.isUsingItem())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextAttemptAt || now < pendingUntil) return;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.TOTEM_OF_UNDYING)) {
            pendingUntil = 0L;
            return;
        }

        // Restore the item displaced by the totem once the totem has been consumed.
        // Only restore if the original inventory slot still contains the exact stack.
        if (offhand.isEmpty() && restoreDisplacedOffhand && restoreMenuSlot >= 0
                && !displacedOffhand.isEmpty()) {
            ItemStack candidate = player.inventoryMenu.getSlot(restoreMenuSlot).getItem();
            if (ItemStack.isSameItemSameComponents(candidate, displacedOffhand)) {
                if (!InventoryActionCoordinator.tryClaim(client)) return;
                client.gameMode.handleContainerInput(
                        player.inventoryMenu.containerId,
                        restoreMenuSlot,
                        Inventory.SLOT_OFFHAND,
                        ContainerInput.SWAP,
                        player
                );
                restoreMenuSlot = -1;
                displacedOffhand = ItemStack.EMPTY;
                pendingUntil = now + 300L;
                ClientNotifications.post(client, "Restored the displaced offhand item.");
                return;
            }
            restoreMenuSlot = -1;
            displacedOffhand = ItemStack.EMPTY;
        } else if (!offhand.isEmpty()) {
            restoreMenuSlot = -1;
            displacedOffhand = ItemStack.EMPTY;
        }

        if (!enabled) return;

        float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();
        if (lastEffectiveHealth >= 0.0F) {
            float damageThisTick = lastEffectiveHealth - effectiveHealth;
            if (damageThisTick > 0.0F) {
                recentDamagePerTick = Math.max(recentDamagePerTick, damageThisTick / 10.0F);
            } else {
                recentDamagePerTick *= 0.88F;
            }
        }
        lastEffectiveHealth = effectiveHealth;

        float predictedHealth = effectiveHealth;
        if (predictRecentDamage) predictedHealth -= recentDamagePerTick * 10.0F;
        if (predictFallDamage && player.fallDistance > 3.0F
                && player.getDeltaMovement().y < 0.0) {
            predictedHealth -= Math.max(0.0F, player.fallDistance - 3.0F);
        }
        if (predictNearbyThreats) {
            int nearbyHostiles = client.level.getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class,
                    player.getBoundingBox().inflate(5.0),
                    entity -> entity != player && entity.isAlive()
                            && !entity.getType().getCategory().isFriendly()
            ).size();
            predictedHealth -= Math.min(4.0F, nearbyHostiles * 1.5F);
        }
        if (activationHealth <= 0.0F || predictedHealth > activationHealth) return;

        int inventoryIndex = findTotem(player);
        if (inventoryIndex < 0) return;

        // InventoryMenu maps hotbar slots to 36..44 and main inventory to 9..35.
        int menuSlot = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
        if (restoreDisplacedOffhand && !offhand.isEmpty()) {
            restoreMenuSlot = menuSlot;
            displacedOffhand = offhand.copy();
        }

        if (!InventoryActionCoordinator.tryClaim(client)) return;
        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                menuSlot,
                Inventory.SLOT_OFFHAND,
                ContainerInput.SWAP,
                player
        );

        ClientNotifications.post(client, "Totem swap requested.");
        nextAttemptAt = now + switchDelayMillis;
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
            if (player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) return slot;
        }
        return -1;
    }

    private int findMainInventoryTotem(Player player) {
        for (int slot = 9; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(Items.TOTEM_OF_UNDYING)) return slot;
        }
        return -1;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) { pendingUntil = 0L; nextAttemptAt = 0L; }
    }
    public void toggle() { setEnabled(!enabled); }
    public Mode getMode() { return mode; }
    public void setMode(Mode value) { mode = value == null ? Mode.SMART : value; }
    public SearchPriority getSearchPriority() { return searchPriority; }
    public void setSearchPriority(SearchPriority value) {
        searchPriority = value == null ? SearchPriority.HOTBAR_FIRST : value;
    }
    public long getSwitchDelayMillis() { return switchDelayMillis; }
    public void setSwitchDelayMillis(long value) {
        switchDelayMillis = Math.max(0L, Math.min(1000L, value));
    }
    public float getActivationHealth() { return activationHealth; }
    public void setActivationHealth(float value) {
        activationHealth = Math.max(0.0F, Math.min(20.0F, value));
    }
    public boolean isPredictRecentDamage() { return predictRecentDamage; }
    public void setPredictRecentDamage(boolean value) { predictRecentDamage = value; }
    public boolean isPredictFallDamage() { return predictFallDamage; }
    public void setPredictFallDamage(boolean value) { predictFallDamage = value; }
    public boolean isPredictNearbyThreats() { return predictNearbyThreats; }
    public void setPredictNearbyThreats(boolean value) { predictNearbyThreats = value; }
    public boolean isRestoreDisplacedOffhand() { return restoreDisplacedOffhand; }
    public void setRestoreDisplacedOffhand(boolean value) {
        restoreDisplacedOffhand = value;
        if (!value) { restoreMenuSlot = -1; displacedOffhand = ItemStack.EMPTY; }
    }
    public boolean isPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public void setPauseWhileUsingItem(boolean value) { pauseWhileUsingItem = value; }
}
