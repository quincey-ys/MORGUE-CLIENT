package dev.morgue.client.ui;

/**
 * Persisted presentation preferences for the MORGUE ClickGUI.
 * Custom sound playback is connected when user-supplied sound assets are added.
 */
public final class UiSettings {
    private static boolean animationsEnabled = true;
    private static boolean soundsEnabled;

    private UiSettings() {
    }

    public static boolean isAnimationsEnabled() {
        return animationsEnabled;
    }

    public static void setAnimationsEnabled(boolean enabled) {
        animationsEnabled = enabled;
    }

    public static boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public static void setSoundsEnabled(boolean enabled) {
        soundsEnabled = enabled;
    }
}
