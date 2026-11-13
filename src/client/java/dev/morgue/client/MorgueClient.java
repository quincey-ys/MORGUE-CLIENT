package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;
    private static final MaceHitterModule MACE_HITTER = new MaceHitterModule();

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("morgue", "main")
        );

        openClickGui = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.morgue.clickgui",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openClickGui.consumeClick()) {
                if (client.gui.screen() instanceof MorgueScreen) {
                    client.gui.setScreen(null);
                } else {
                    client.gui.setScreen(new MorgueScreen());
                }
            }

            MACE_HITTER.tick(client);
        });
    }

    public static MaceHitterModule maceHitter() {
        return MACE_HITTER;
    }
}
