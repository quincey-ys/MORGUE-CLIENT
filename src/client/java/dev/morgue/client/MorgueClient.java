package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.modules.player.AutoTotemModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;
    private static KeyMapping toggleMaceHitter;
    private static KeyMapping toggleKillAura;
    private static KeyMapping toggleAutoTotem;
    private static final MaceHitterModule MACE_HITTER = new MaceHitterModule();
    private static final KillAuraModule KILL_AURA = new KillAuraModule();
    private static final AutoTotemModule AUTO_TOTEM = new AutoTotemModule();

    @Override
    public void onInitializeClient() {
        MorgueConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("morgue", "main")
        );

        openClickGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.clickgui",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT,
                category
        ));
        toggleMaceHitter = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_mace_hitter", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, category
        ));
        toggleKillAura = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_kill_aura", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category
        ));
        toggleAutoTotem = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.toggle_auto_totem", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.gui.screen() == null) {
                while (toggleMaceHitter.consumeClick()) {
                    MACE_HITTER.toggle();
                    MorgueConfig.save();
                }
                while (toggleKillAura.consumeClick()) {
                    KILL_AURA.toggle();
                    MorgueConfig.save();
                }
                while (toggleAutoTotem.consumeClick()) {
                    AUTO_TOTEM.toggle();
                    MorgueConfig.save();
                }
            }

            while (openClickGui.consumeClick()) {
                if (client.gui.screen() instanceof MorgueScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new MorgueScreen());
                }
            }

            KILL_AURA.tick(client);
            MACE_HITTER.tick(client);
            AUTO_TOTEM.tick(client);
        });
    }

    public static KillAuraModule killAura() {
        return KILL_AURA;
    }

    public static MaceHitterModule maceHitter() {
        return MACE_HITTER;
    }

    public static AutoTotemModule autoTotem() {
        return AUTO_TOTEM;
    }
}
