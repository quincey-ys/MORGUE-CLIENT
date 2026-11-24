package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.modules.combat.AimAssistModule;
import dev.morgue.client.modules.combat.ClickAssistModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.security.MorgueIntegrity;
import dev.morgue.client.modules.player.AutoTotemModule;
import dev.morgue.client.modules.player.InventoryHelperModule;
import dev.morgue.client.modules.player.AutoEatModule;
import dev.morgue.client.modules.movement.SprintModule;
import dev.morgue.client.modules.movement.SafeWalkModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;
    private static KeyMapping toggleMaceHitter;
    private static KeyMapping toggleKillAura;
    private static KeyMapping toggleAutoTotem;
    private static KeyMapping toggleMaceHitterMouse4;
    private static KeyMapping toggleKillAuraMouse5;
    private static final MaceHitterModule MACE_HITTER = new MaceHitterModule();
    private static final KillAuraModule KILL_AURA = new KillAuraModule();
    private static final AimAssistModule AIM_ASSIST = new AimAssistModule();
    private static final ClickAssistModule CLICK_ASSIST = new ClickAssistModule();
    private static final AutoTotemModule AUTO_TOTEM = new AutoTotemModule();
    private static final InventoryHelperModule INVENTORY_HELPER = new InventoryHelperModule();
    private static final AutoEatModule AUTO_EAT = new AutoEatModule();
    private static final SprintModule SPRINT = new SprintModule();
    private static final SafeWalkModule SAFE_WALK = new SafeWalkModule();

    @Override
    public void onInitializeClient() {
        MorgueHud.register();
        MorgueIntegrity.verify();
        if (MorgueIntegrity.isActivationAllowed()) {
            MorgueConfig.load();
        } else {
            System.err.println("[MORGUE] Integrity verification failed; gameplay modules remain locked.");
        }
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("morgue", "main")
        );

        openClickGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.clickgui",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT,
                category
        ));
        toggleMaceHitter = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_mace_hitter", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, category
        ));
        toggleKillAura = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_kill_aura", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category
        ));
        toggleAutoTotem = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_auto_totem", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, category
        ));
        // GLFW button indices 3 and 4 are the fourth and fifth mouse buttons.
        toggleMaceHitterMouse4 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_mace_hitter_mouse4", InputConstants.Type.MOUSE, 3, category
        ));
        toggleKillAuraMouse5 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_kill_aura_mouse5", InputConstants.Type.MOUSE, 4, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null) {
                while (toggleMaceHitter.consumeClick()) {
                    setMaceHitterEnabled(!MACE_HITTER.isEnabled());
                    MorgueConfig.save();
                }
                while (toggleKillAura.consumeClick()) {
                    setKillAuraEnabled(!KILL_AURA.isEnabled());
                    MorgueConfig.save();
                }
                while (toggleAutoTotem.consumeClick()) {
                    setAutoTotemEnabled(!AUTO_TOTEM.isEnabled());
                    MorgueConfig.save();
                }
                while (toggleMaceHitterMouse4.consumeClick()) {
                    setMaceHitterEnabled(!MACE_HITTER.isEnabled());
                    MorgueConfig.save();
                }
                while (toggleKillAuraMouse5.consumeClick()) {
                    setKillAuraEnabled(!KILL_AURA.isEnabled());
                    MorgueConfig.save();
                }
            }

            while (openClickGui.consumeClick()) {
                if (client.gui.screen() instanceof MorgueScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new MorgueScreen());
                }
            }

            // Mace Hitter gets first claim when a falling smash is ready.
            MACE_HITTER.tick(client);
            KILL_AURA.tick(client);
            AIM_ASSIST.tick(client);
            CLICK_ASSIST.tick(client);
            AUTO_TOTEM.tick(client);
            INVENTORY_HELPER.tick(client);
            AUTO_EAT.tick(client);
            SPRINT.tick(client);
            SAFE_WALK.tick(client);
        });
    }

    public static void setMaceHitterEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        MACE_HITTER.setEnabled(enabled);
    }

    public static void setKillAuraEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        KILL_AURA.setEnabled(enabled);
    }

    public static void setAutoTotemEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        AUTO_TOTEM.setEnabled(enabled);
    }

    public static void setSprintEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        SPRINT.setEnabled(enabled);
    }

    public static SprintModule sprint() { return SPRINT; }

    public static void setInventoryHelperEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        INVENTORY_HELPER.setEnabled(enabled);
    }

    public static InventoryHelperModule inventoryHelper() { return INVENTORY_HELPER; }

    public static void setAutoEatEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        AUTO_EAT.setEnabled(enabled);
    }

    public static AutoEatModule autoEat() { return AUTO_EAT; }

    public static void setSafeWalkEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        SAFE_WALK.setEnabled(enabled);
    }

    public static SafeWalkModule safeWalk() { return SAFE_WALK; }

    public static KillAuraModule killAura() {
        return KILL_AURA;
    }

    public static void setAimAssistEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        AIM_ASSIST.setEnabled(enabled);
    }

    public static AimAssistModule aimAssist() { return AIM_ASSIST; }

    public static void setClickAssistEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        CLICK_ASSIST.setEnabled(enabled);
    }

    public static ClickAssistModule clickAssist() { return CLICK_ASSIST; }

    public static MaceHitterModule maceHitter() {
        return MACE_HITTER;
    }

    public static AutoTotemModule autoTotem() {
        return AUTO_TOTEM;
    }
}
