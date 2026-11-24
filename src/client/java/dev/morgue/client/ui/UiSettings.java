package dev.morgue.client.ui;

/** Persisted presentation preferences for the MORGUE ClickGUI. */
public final class UiSettings {
    private static boolean animationsEnabled = true;
    private static boolean soundsEnabled = true;
    private static boolean killSoundEnabled = true;

    private UiSettings() {}
    public static boolean isAnimationsEnabled() { return animationsEnabled; }
    public static void setAnimationsEnabled(boolean enabled) { animationsEnabled = enabled; }
    public static boolean isSoundsEnabled() { return soundsEnabled; }
    public static void setSoundsEnabled(boolean enabled) { soundsEnabled = enabled; }
    public static boolean isKillSoundEnabled() { return killSoundEnabled; }
    public static void setKillSoundEnabled(boolean enabled) { killSoundEnabled = enabled; }
}
