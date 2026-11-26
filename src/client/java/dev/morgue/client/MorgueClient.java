package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.modules.combat.AimAssistModule;
import dev.morgue.client.modules.combat.ClickAssistModule;
import dev.morgue.client.modules.combat.AutoCrystalModule;
import dev.morgue.client.modules.combat.AnchorMacroModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.ui.KillSoundDetector;
import dev.morgue.client.ui.UiSoundManager;
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
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;
    private static final Map<KeyMapping, Runnable> MODULE_TOGGLE_BINDINGS = new LinkedHashMap<>();
    private static final MaceHitterModule MACE_HITTER = new MaceHitterModule();
    private static final KillAuraModule KILL_AURA = new KillAuraModule();
    private static final AimAssistModule AIM_ASSIST = new AimAssistModule();
    private static final ClickAssistModule CLICK_ASSIST = new ClickAssistModule();
    private static final AutoTotemModule AUTO_TOTEM = new AutoTotemModule();
    private static final InventoryHelperModule INVENTORY_HELPER = new InventoryHelperModule();
    private static final AutoEatModule AUTO_EAT = new AutoEatModule();
    private static final SprintModule SPRINT = new SprintModule();
    private static final SafeWalkModule SAFE_WALK = new SafeWalkModule();
    private static final AutoCrystalModule AUTO_CRYSTAL = new AutoCrystalModule();
    private static final AnchorMacroModule ANCHOR_MACRO = new AnchorMacroModule();

    @Override
    public void onInitializeClient() {
        MorgueHud.register();
        UiSoundManager.register();
        KillSoundDetector.register();
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
        registerModuleToggle(category, "mace_hitter", InputConstants.KEY_M,
                () -> setMaceHitterEnabled(!MACE_HITTER.isEnabled()));
        registerModuleToggle(category, "kill_aura", InputConstants.KEY_K,
                () -> setKillAuraEnabled(!KILL_AURA.isEnabled()));
        registerModuleToggle(category, "auto_totem", InputConstants.KEY_V,
                () -> setAutoTotemEnabled(!AUTO_TOTEM.isEnabled()));
        registerModuleToggle(category, "aim_assist", InputConstants.UNKNOWN.getValue(),
                () -> setAimAssistEnabled(!AIM_ASSIST.isEnabled()));
        registerModuleToggle(category, "click_assist", InputConstants.UNKNOWN.getValue(),
                () -> setClickAssistEnabled(!CLICK_ASSIST.isEnabled()));
        registerModuleToggle(category, "auto_sprint", InputConstants.UNKNOWN.getValue(),
                () -> setSprintEnabled(!SPRINT.isEnabled()));
        registerModuleToggle(category, "safe_walk", InputConstants.UNKNOWN.getValue(),
                () -> setSafeWalkEnabled(!SAFE_WALK.isEnabled()));
        registerModuleToggle(category, "inventory_helper", InputConstants.UNKNOWN.getValue(),
                () -> setInventoryHelperEnabled(!INVENTORY_HELPER.isEnabled()));
        registerModuleToggle(category, "auto_eat", InputConstants.UNKNOWN.getValue(),
                () -> setAutoEatEnabled(!AUTO_EAT.isEnabled()));
        registerModuleToggle(category, "auto_crystal", InputConstants.UNKNOWN.getValue(),
                () -> setAutoCrystalEnabled(!AUTO_CRYSTAL.isEnabled()));
        registerModuleToggle(category, "anchor_macro", InputConstants.UNKNOWN.getValue(),
                () -> setAnchorMacroEnabled(!ANCHOR_MACRO.isEnabled()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (var binding : MODULE_TOGGLE_BINDINGS.entrySet()) {
                while (binding.getKey().consumeClick()) {
                    if (client.gui.screen() == null) {
                        binding.getValue().run();
                        MorgueConfig.save();
                    }
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
            AUTO_CRYSTAL.tick(client);
            AIM_ASSIST.tick(client);
            CLICK_ASSIST.tick(client);
            AUTO_TOTEM.tick(client);
            INVENTORY_HELPER.tick(client);
            AUTO_EAT.tick(client);
            SPRINT.tick(client);
            SAFE_WALK.tick(client);
            ANCHOR_MACRO.tick(client);
        });
    }

    private static void registerModuleToggle(KeyMapping.Category category, String id,
                                              int defaultKey, Runnable toggleAction) {
        KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_" + id, InputConstants.Type.KEYBOARD, defaultKey, category
        ));
        MODULE_TOGGLE_BINDINGS.put(mapping, toggleAction);
    }

    public static void setMaceHitterEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = MACE_HITTER.isEnabled() != enabled;
        MACE_HITTER.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.MACE_HITTER, enabled);
    }

    public static void setKillAuraEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = KILL_AURA.isEnabled() != enabled;
        KILL_AURA.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.KILL_AURA, enabled);
    }

    public static void setAutoTotemEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = AUTO_TOTEM.isEnabled() != enabled;
        AUTO_TOTEM.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.AUTO_TOTEM, enabled);
    }

    public static void setSprintEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = SPRINT.isEnabled() != enabled;
        SPRINT.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.AUTO_SPRINT, enabled);
    }

    public static SprintModule sprint() { return SPRINT; }

    public static void setInventoryHelperEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = INVENTORY_HELPER.isEnabled() != enabled;
        INVENTORY_HELPER.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.INVENTORY_HELPER, enabled);
    }

    public static InventoryHelperModule inventoryHelper() { return INVENTORY_HELPER; }

    public static void setAutoEatEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = AUTO_EAT.isEnabled() != enabled;
        AUTO_EAT.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.AUTO_EAT, enabled);
    }

    public static AutoEatModule autoEat() { return AUTO_EAT; }

    public static void setSafeWalkEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = SAFE_WALK.isEnabled() != enabled;
        SAFE_WALK.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.SAFE_WALK, enabled);
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
        boolean changed = AIM_ASSIST.isEnabled() != enabled;
        AIM_ASSIST.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.AIM_ASSIST, enabled);
    }

    public static AimAssistModule aimAssist() { return AIM_ASSIST; }

    public static void setClickAssistEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = CLICK_ASSIST.isEnabled() != enabled;
        CLICK_ASSIST.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.CLICK_ASSIST, enabled);
    }

    public static ClickAssistModule clickAssist() { return CLICK_ASSIST; }

    public static MaceHitterModule maceHitter() {
        return MACE_HITTER;
    }

    public static AutoTotemModule autoTotem() {
        return AUTO_TOTEM;
    }

    public static void setAutoCrystalEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = AUTO_CRYSTAL.isEnabled() != enabled;
        AUTO_CRYSTAL.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.AUTO_CRYSTAL, enabled);
    }

    public static AutoCrystalModule autoCrystal() { return AUTO_CRYSTAL; }

    public static void setAnchorMacroEnabled(boolean enabled) {
        if (enabled && !MorgueIntegrity.isActivationAllowed()) {
            System.err.println("[MORGUE] Module activation blocked: package integrity is not verified.");
            return;
        }
        boolean changed = ANCHOR_MACRO.isEnabled() != enabled;
        ANCHOR_MACRO.setEnabled(enabled);
        if (changed) ClientNotifications.postModuleToggle(Minecraft.getInstance(),
                ClientNotifications.Module.ANCHOR_MACRO, enabled);
    }

    public static AnchorMacroModule anchorMacro() { return ANCHOR_MACRO; }
}
