package dev.morgue.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.EnumMap;
import java.util.Map;

/** Client-local playback for MORGUE's bundled UI and kill sound effects. */
public final class UiSoundManager {
    public enum Effect {
        OPEN("ui.open", 0.72F, false),
        CLOSE("ui.close", 0.72F, false),
        HOVER("ui.hover", 0.24F, false),
        CLICK("ui.click", 0.48F, false),
        TOGGLE_ON("ui.toggle_on", 0.62F, false),
        TOGGLE_OFF("ui.toggle_off", 0.62F, false),
        SLIDER("ui.slider", 0.28F, false),
        KILL("ui.kill", 0.9F, true);

        private final String id;
        private final float volume;
        private final boolean killSound;

        Effect(String id, float volume, boolean killSound) {
            this.id = id;
            this.volume = volume;
            this.killSound = killSound;
        }
    }

    private static final Map<Effect, SoundEvent> EVENTS = new EnumMap<>(Effect.class);
    private static boolean registered;

    private UiSoundManager() {}

    public static synchronized void register() {
        if (registered) return;
        for (Effect effect : Effect.values()) {
            Identifier id = Identifier.fromNamespaceAndPath("morgue", effect.id);
            SoundEvent event = SoundEvent.createVariableRangeEvent(id);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
            EVENTS.put(effect, event);
        }
        registered = true;
    }

    public static void play(Effect effect) {
        play(effect, false);
    }

    /** Used only to let the UI-sounds switch play its ON cue while enabling sounds. */
    public static void play(Effect effect, boolean allowWhenUiSoundsDisabled) {
        if (effect == null || !registered) return;
        if (effect.killSound) {
            if (!UiSettings.isKillSoundEnabled()) return;
        } else if (!UiSettings.isSoundsEnabled() && !allowWhenUiSoundsDisabled) {
            return;
        }

        SoundEvent sound = EVENTS.get(effect);
        Minecraft client = Minecraft.getInstance();
        if (sound != null && client != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, effect.volume));
        }
    }
}
