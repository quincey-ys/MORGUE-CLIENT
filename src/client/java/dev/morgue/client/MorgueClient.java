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

import java.util.LinkedHashMap;
import java.util.Map;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;
    private static final Map<String, KeyMapping> MODULE_KEYBINDS = new LinkedHashMap<>();
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
        registerModuleKeybind("Mace Hitter", "mace_hitter", InputConstants.KEY_M, category);
        registerModuleKeybind("KillAura", "kill_aura", InputConstants.KEY_K, category);
        registerModuleKeybind("Auto Totem", "auto_totem", InputConstants.KEY_V, category);
        registerModuleKeybind("Aim Assist", "aim_assist", -1, category);
        registerModuleKeybind("Click Assist", "click_assist", -1, category);
        registerModuleKeybind("Sprint", "sprint", -1, category);
        registerModuleKeybind("Safe Walk", "safe_walk", -1, category);
        registerModuleKeybind("Inventory Helper", "inventory_helper", -1, category);
        registerModuleKeybind("Item Use", "item_use", -1, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null) {
                for (Map.Entry<String, KeyMapping> entry : MODULE_KEYBINDS.entrySet()) {
                    while (entry.getValue().consumeClick()) {
                        toggleModule(entry.getKey());
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
            AIM_ASSIST.tick(client);
            CLICK_ASSIST.tick(client);
            AUTO_TOTEM.tick(client);
            INVENTORY_HELPER.tick(client);
            AUTO_EAT.tick(client);
            SPRINT.tick(client);
            SAFE_WALK.tick(client);
        });
    }

    private static void registerModuleKeybind(String module, String id, int defaultKey,
                                               KeyMapping.Category category) {
        KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.module_" + id,
                InputConstants.Type.KEYBOARD,
                defaultKey,
                category
        ));
        MODULE_KEYBINDS.put(module, mapping);
    }

    public static KeyMapping moduleKeybind(String module) {
        return MODULE_KEYBINDS.get(module);
    }

    public static boolean isModuleKeybindSupported(String module) {
        return MODULE_KEYBINDS.containsKey(module);
    }

    public static void setModuleKeybind(String module, InputConstants.Key key) {
        KeyMapping mapping = MODULE_KEYBINDS.get(module);
        if (mapping != null && key != null) {
            mapping.setKey(key);
        }
    }

    public static boolean isModuleEnabled(String module) {
        return switch (module) {
            case "Mace Hitter" -> MACE_HITTER.isEnabled();
            case "KillAura" -> KILL_AURA.isEnabled();
            case "Auto Totem" -> AUTO_TOTEM.isEnabled();
            case "Aim Assist" -> AIM_ASSIST.isEnabled();
            case "Click Assist" -> CLICK_ASSIST.isEnabled();
            case "Sprint" -> SPRINT.isEnabled();
            case "Safe Walk" -> SAFE_WALK.isEnabled();
            case "Inventory Helper" -> INVENTORY_HELPER.isEnabled();
            case "Item Use" -> AUTO_EAT.isEnabled();
            default -> false;
        };
    }

    public static void setModuleEnabled(String module, boolean enabled) {
        switch (module) {
            case "Mace Hitter" -> setMaceHitterEnabled(enabled);
            case "KillAura" -> setKillAuraEnabled(enabled);
            case "Auto Totem" -> setAutoTotemEnabled(enabled);
            case "Aim Assist" -> setAimAssistEnabled(enabled);
            case "Click Assist" -> setClickAssistEnabled(enabled);
            case "Sprint" -> setSprintEnabled(enabled);
            case "Safe Walk" -> setSafeWalkEnabled(enabled);
            case "Inventory Helper" -> setInventoryHelperEnabled(enabled);
            case "Item Use" -> setAutoEatEnabled(enabled);
            default -> {
                return;
            }
        }
        MorgueConfig.save();
    }

    private static void toggleModule(String module) {
        setModuleEnabled(module, !isModuleEnabled(module));
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
