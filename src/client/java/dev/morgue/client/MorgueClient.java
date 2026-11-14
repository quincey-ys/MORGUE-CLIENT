package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.security.MorgueIntegrity;
import dev.morgue.client.modules.player.AutoTotemModule;
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
    private static final MaceHitterModule MACE_HITTER = new MaceHitterModule();
    private static final KillAuraModule KILL_AURA = new KillAuraModule();
    private static final AutoTotemModule AUTO_TOTEM = new AutoTotemModule();
    private static final SprintModule SPRINT = new SprintModule();
    private static final SafeWalkModule SAFE_WALK = new SafeWalkModule();

    @Override
    public void onInitializeClient() {
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
            AUTO_TOTEM.tick(client);
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

    public static MaceHitterModule maceHitter() {
        return MACE_HITTER;
    }

    public static AutoTotemModule autoTotem() {
        return AUTO_TOTEM;
    }
}
