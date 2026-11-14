package dev.morgue.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Small opt-in local chat notification helper. Messages never leave the client. */
public final class ClientNotifications {
    private static boolean enabled = true;

    private ClientNotifications() {}

    public static void post(Minecraft client, String message) {
        if (enabled && client != null && message != null && !message.isBlank()) {
            client.showDebugChat(Component.literal("[MORGUE] " + message));
        }
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
}
