package dev.morgue.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/** Local chat feedback. Messages are displayed only in the current client. */
public final class ClientNotifications {
    public enum Module {
        MACE_HITTER("Mace Hitter", "maceHitter"),
        KILL_AURA("KillAura", "killAura"),
        AUTO_TOTEM("Auto Totem", "autoTotem"),
        AIM_ASSIST("Aim Assist", "aimAssist"),
        CLICK_ASSIST("Click Assist", "clickAssist"),
        AUTO_SPRINT("Auto Sprint", "autoSprint"),
        SAFE_WALK("Safe Walk", "safeWalk"),
        INVENTORY_HELPER("Inventory Helper", "inventoryHelper"),
        AUTO_EAT("Auto Eat", "autoEat");

        private final String displayName;
        private final String configKey;

        Module(String displayName, String configKey) {
            this.displayName = displayName;
            this.configKey = configKey;
        }

        public String displayName() { return displayName; }
        public String configKey() { return configKey; }
    }

    private static boolean enabled = true;
    private static boolean moduleFeedbackEnabled = true;
    private static final Map<Module, Boolean> MODULE_FEEDBACK = new EnumMap<>(Module.class);

    static {
        for (Module module : Module.values()) {
            MODULE_FEEDBACK.put(module, true);
        }
    }

    private ClientNotifications() {}

    /** Existing action notifications (for inventory and totem actions). */
    public static void post(Minecraft client, String message) {
        if (enabled && client != null && message != null && !message.isBlank()) {
            client.showDebugChat(Component.literal("[MORGUE] " + message));
        }
    }

    /** Announces an actual module state change when that module's feedback is enabled. */
    public static void postModuleToggle(Minecraft client, Module module, boolean isNowEnabled) {
        if (!moduleFeedbackEnabled || module == null || !isModuleFeedbackEnabled(module)
                || client == null || client.player == null || client.level == null) {
            return;
        }
        client.showDebugChat(Component.literal("[MORGUE] " + module.displayName()
                + (isNowEnabled ? " enabled." : " disabled.")));
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static boolean isModuleFeedbackEnabled() { return moduleFeedbackEnabled; }
    public static void setModuleFeedbackEnabled(boolean value) { moduleFeedbackEnabled = value; }

    public static boolean isModuleFeedbackEnabled(Module module) {
        return module != null && MODULE_FEEDBACK.getOrDefault(module, true);
    }

    public static void setModuleFeedbackEnabled(Module module, boolean value) {
        if (module != null) MODULE_FEEDBACK.put(module, value);
    }
}
