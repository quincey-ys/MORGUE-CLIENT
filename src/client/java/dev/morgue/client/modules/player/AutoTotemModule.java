package dev.morgue.client.modules.player;

import dev.morgue.client.ClientNotifications;
import dev.morgue.client.config.MorgueConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Standalone MORGUE Auto Totem.
 *
 * Inventory changes use Minecraft's normal container-input path. The initial
 * equip is health-gated; once a totem has been equipped, empty-offhand refills
 * are independent of health. Displaced offhand items are never restored.
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
    private boolean pendingEquip;
    private int pendingSourceMenuSlot = -1;
    private String pendingPlayerUuid = "";
    private boolean pauseWhileUsingItem = true;
    private float activationHealth = 20.0F;
    private boolean predictRecentDamage = true;
    private boolean predictFallDamage = true;
    private boolean predictNearbyThreats = true;
    private boolean keepTotemEquipped = true;
    private boolean refillArmed;
    private String refillPlayerUuid = "";
    private String lastPlayerUuid = "";
    private float lastEffectiveHealth = -1.0F;
    private float recentDamagePerTick;

    public void tick(Minecraft client) {
        if (client == null) return;
        if (client.player == null || client.level == null || client.gameMode == null) {
            lastEffectiveHealth = -1.0F;
            recentDamagePerTick = 0.0F;
            lastPlayerUuid = "";
            return;
        }

        Player player = client.player;
        long now = System.currentTimeMillis();
        String playerUuid = player.getUUID().toString();

        if (!playerUuid.equals(lastPlayerUuid)) {
            lastPlayerUuid = playerUuid;
            lastEffectiveHealth = -1.0F;
            recentDamagePerTick = 0.0F;
        }
        if (pendingEquip && !playerUuid.equals(pendingPlayerUuid)) {
            clearPendingEquip();
        }
        if (refillArmed && !playerUuid.equals(refillPlayerUuid)) {
            disarmRefill(true);
        }

        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.TOTEM_OF_UNDYING)) {
            if (pendingEquip && keepTotemEquipped) {
                armRefill(playerUuid);
            } else if (enabled && keepTotemEquipped && !refillArmed) {
                // Also support players who already had a totem equipped when enabling the module.
                armRefill(playerUuid);
            }
            clearPendingEquip();
            return;
        }

        // If the first swap was processed and the totem popped before this tick,
        // the selected inventory slot will no longer contain that totem. Arm from
        // this observed inventory change so a very fast pop is not missed.
        if (pendingEquip) {
            ItemStack requestedSlot = pendingSourceMenuSlot >= 0 && pendingSourceMenuSlot <= 44
                    ? player.inventoryMenu.getSlot(pendingSourceMenuSlot).getItem() : ItemStack.EMPTY;
            if (!requestedSlot.is(Items.TOTEM_OF_UNDYING)) {
                clearPendingEquip();
                if (keepTotemEquipped) armRefill(playerUuid);
            } else if (now < pendingUntil) {
                return;
            } else {
                clearPendingEquip();
            }
        }

        // A non-empty offhand item always wins over persistent refill. Keep the
        // refill armed so it resumes naturally if the user later empties the slot.
        if (keepTotemEquipped && refillArmed && !offhand.isEmpty()) return;

        if (!enabled || player.isSpectator() || player.isDeadOrDying()
                || (pauseWhileUsingItem && player.isUsingItem()) || client.gui.screen() != null) {
            return;
        }
        if (now < nextAttemptAt || now < pendingUntil) return;

        // Once armed, refill an empty offhand regardless of health or predictions.
        if (keepTotemEquipped && refillArmed) {
            requestTotemSwap(client, player, findTotem(player), now, true);
            return;
        }

        // First equip remains governed by the configured health/prediction threshold.
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

        requestTotemSwap(client, player, findTotem(player), now, false);
    }

    private void requestTotemSwap(Minecraft client, Player player, int inventoryIndex,
                                  long now, boolean refill) {
        if (inventoryIndex < 0) return;
        if (refill && !player.getOffhandItem().isEmpty()) return;

        // InventoryMenu maps hotbar slots to 36..44 and main inventory to 9..35.
        int menuSlot = inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
        if (menuSlot < 9 || menuSlot > 44) return;

        // Revalidate the chosen source immediately before submitting the container action.
        ItemStack source = player.getInventory().getItem(inventoryIndex);
        ItemStack menuStack = player.inventoryMenu.getSlot(menuSlot).getItem();
        if (!source.is(Items.TOTEM_OF_UNDYING) || !menuStack.is(Items.TOTEM_OF_UNDYING)) return;
        if (refill && !player.getOffhandItem().isEmpty()) return;
        if (!InventoryActionCoordinator.tryClaim(client)) return;

        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                menuSlot,
                Inventory.SLOT_OFFHAND,
                ContainerInput.SWAP,
                player
        );

        pendingEquip = true;
        pendingSourceMenuSlot = menuSlot;
        pendingPlayerUuid = player.getUUID().toString();
        nextAttemptAt = now + switchDelayMillis;
        pendingUntil = now + 300L;

        // Keep background refills quiet; the first threshold-triggered equip
        // retains the existing local notification behavior.
        if (!refill) ClientNotifications.post(client, "Totem swap requested.");
    }

    private void armRefill(String playerUuid) {
        if (!keepTotemEquipped || playerUuid == null || playerUuid.isBlank()) return;
        boolean changed = !refillArmed || !playerUuid.equals(refillPlayerUuid);
        refillArmed = true;
        refillPlayerUuid = playerUuid;
        if (changed) MorgueConfig.save();
    }

    private void disarmRefill(boolean persist) {
        boolean changed = refillArmed || !refillPlayerUuid.isEmpty();
        refillArmed = false;
        refillPlayerUuid = "";
        clearPendingEquip();
        if (persist && changed) MorgueConfig.save();
    }

    private void clearPendingEquip() {
        pendingEquip = false;
        pendingSourceMenuSlot = -1;
        pendingPlayerUuid = "";
        pendingUntil = 0L;
    }

    /** Restores only a valid persisted refill state for the same player. */
    public void loadRefillState(boolean armed, String playerUuid) {
        String normalizedUuid = playerUuid == null ? "" : playerUuid.trim();
        refillArmed = armed && keepTotemEquipped && !normalizedUuid.isEmpty();
        refillPlayerUuid = refillArmed ? normalizedUuid : "";
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
    public void setEnabled(boolean value) { enabled = value; }
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
    public boolean isKeepTotemEquipped() { return keepTotemEquipped; }
    public void setKeepTotemEquipped(boolean value) {
        keepTotemEquipped = value;
        if (!value) disarmRefill(false);
    }
    public boolean isRefillArmed() { return refillArmed; }
    public String getRefillPlayerUuid() { return refillPlayerUuid; }
    public boolean isPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public void setPauseWhileUsingItem(boolean value) { pauseWhileUsingItem = value; }
}
