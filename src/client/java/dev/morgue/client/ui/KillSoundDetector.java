package dev.morgue.client.ui;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * Detects server death messages whose attacker argument identifies the local player.
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
        if (!isLocalPlayerKiller(message, player)
                && (!allowPlainTextFallback || !isPlainTextKillByLocalPlayer(plainText, player))) return;

        long now = System.currentTimeMillis();
        if (plainText.equals(lastMessage) && now - lastMessageAt < 900L) return;
        lastMessage = plainText;
        lastMessageAt = now;
        UiSoundManager.play(UiSoundManager.Effect.KILL);
    }

    private static boolean isLocalPlayerKiller(Component component, Player player) {
        if (component.getContents() instanceof TranslatableContents translated
                && translated.getKey().startsWith("death.")) {
            Object[] arguments = translated.getArgs();
            // Argument zero is the victim; later arguments include the attacker.
            for (int i = 1; i < arguments.length; i++) {
                if (matchesPlayerName(arguments[i], player)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (isLocalPlayerKiller(sibling, player)) return true;
        }
        return false;
    }

    private static boolean matchesPlayerName(Object argument, Player player) {
        String candidate;
        if (argument instanceof Component component) candidate = component.getString();
        else if (argument instanceof String value) candidate = value;
        else return false;

        String username = player.getName().getString();
        String displayName = player.getDisplayName().getString();
        return candidate.equalsIgnoreCase(username) || candidate.equalsIgnoreCase(displayName);
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
