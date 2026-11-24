package dev.morgue.client.ui;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * Detects system death messages that identify the local player as the attacker.
 * It does not infer kills from attack animations or predicted damage.
 */
public final class KillSoundDetector {
    private static final List<String> ENGLISH_DEATH_MARKERS = List.of(
            " was slain by ", " was killed by ", " was shot by ", " was blown up by ",
            " was fireballed by ", " was pummeled by ", " was impaled by ", " was skewered by ",
            " was stung by ", " was obliterated by ", " was sniped by ", " was struck by ",
            " was knocked into the void by ", " has been killed by "
    );
    private static String lastMessage;
    private static long lastMessageAt;

    private KillSoundDetector() {}

    public static void register() {
        // Death notices are system/game messages; avoid subscribing to player-chat events
        // so chat moderation and ordinary player chat never trigger the kill cue.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handle(message, true));
    }

    private static void handle(Component message, boolean allowPlainTextFallback) {
        Player player = Minecraft.getInstance().player;
        if (message == null || player == null || !UiSettings.isKillSoundEnabled()) return;

        String plainText = message.getString();
        if (!allowPlainTextFallback || !isPlainTextKillByLocalPlayer(plainText, player)) return;

        long now = System.currentTimeMillis();
        if (plainText.equals(lastMessage) && now - lastMessageAt < 900L) return;
        lastMessage = plainText;
        lastMessageAt = now;
        UiSoundManager.play(UiSoundManager.Effect.KILL);
    }

    private static boolean isPlainTextKillByLocalPlayer(String message, Player player) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        String username = player.getName().getString().toLowerCase(Locale.ROOT);
        String displayName = player.getDisplayName().getString().toLowerCase(Locale.ROOT);
        int nameIndex = lowerMessage.lastIndexOf(username);
        if (nameIndex < 0 && !displayName.equals(username)) {
            nameIndex = lowerMessage.lastIndexOf(displayName);
        }
        if (nameIndex < 0) return false;
        String beforeName = lowerMessage.substring(0, nameIndex);
        return ENGLISH_DEATH_MARKERS.stream().anyMatch(beforeName::contains);
    }
}
