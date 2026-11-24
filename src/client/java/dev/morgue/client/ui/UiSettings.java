package dev.morgue.client.ui;

/** Persisted presentation preferences for the MORGUE ClickGUI. */
public final class UiSettings {
    private static boolean animationsEnabled = true;
    private static boolean soundsEnabled = true;
    private static boolean killSoundEnabled = true;
    private static int accentIndex = 0;

    private static final int[] ACCENTS = {
            0xFF67D6C5, 0xFFB28DFF, 0xFFFFA36C, 0xFFFF6F91, 0xFF7DB6FF
    };
    private static final String[] ACCENT_NAMES = {
            "Teal", "Violet", "Amber", "Rose", "Sky"
    };

    private UiSettings() {}
    public static boolean isAnimationsEnabled() { return animationsEnabled; }
    public static void setAnimationsEnabled(boolean enabled) { animationsEnabled = enabled; }
    public static boolean isSoundsEnabled() { return soundsEnabled; }
    public static void setSoundsEnabled(boolean enabled) { soundsEnabled = enabled; }
    public static boolean isKillSoundEnabled() { return killSoundEnabled; }
    public static void setKillSoundEnabled(boolean enabled) { killSoundEnabled = enabled; }
    public static int accentColor() { return ACCENTS[Math.floorMod(accentIndex, ACCENTS.length)]; }
    public static String accentName() { return ACCENT_NAMES[Math.floorMod(accentIndex, ACCENT_NAMES.length)]; }
    public static void cycleAccent() { accentIndex = (accentIndex + 1) % ACCENTS.length; }
    public static void setAccentIndex(int index) { accentIndex = Math.floorMod(index, ACCENTS.length); }
    public static int accentIndex() { return accentIndex; }
}
