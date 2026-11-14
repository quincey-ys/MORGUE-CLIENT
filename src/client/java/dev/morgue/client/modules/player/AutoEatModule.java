package dev.morgue.client.modules.player;

import dev.morgue.client.ClientNotifications;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Eats the most nutritious food already in the hotbar using vanilla item-use input. */
public final class AutoEatModule {
    private boolean enabled;
    private int hungerThreshold = 16;
    private int originalSlot = -1;
    private int eatingSlot = -1;

    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) return;
        Player player = client.player;

        if (eatingSlot >= 0) {
            if (player.isUsingItem()) return;
            if (player.getInventory().getSelectedSlot() == eatingSlot && originalSlot >= 0) {
                player.getInventory().setSelectedSlot(originalSlot);
            }
            eatingSlot = -1;
            originalSlot = -1;
        }

        if (!enabled || player.isSpectator() || player.isDeadOrDying()
                || player.isUsingItem() || player.getFoodData().getFoodLevel() > hungerThreshold
                || !player.canEat(false)) return;

        int bestSlot = -1;
        int bestNutrition = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            FoodProperties food = stack.get(DataComponents.FOOD);
            if (food != null && food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                bestSlot = slot;
            }
        }
        if (bestSlot < 0) return;

        originalSlot = player.getInventory().getSelectedSlot();
        eatingSlot = bestSlot;
        player.getInventory().setSelectedSlot(bestSlot);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        ClientNotifications.post(client, "Auto Eat started using hotbar food.");
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public int getHungerThreshold() { return hungerThreshold; }
    public void setHungerThreshold(int value) { hungerThreshold = Math.max(1, Math.min(19, value)); }
}
