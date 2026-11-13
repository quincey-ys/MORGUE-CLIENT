package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class MorgueClient implements ClientModInitializer {
    private static KeyMapping openClickGui;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("morgue", "main")
        );

        // SDL scancode 229 is Right Shift.
        openClickGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morgue.clickgui",
                InputConstants.Type.KEYSYM,
                229,
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
        });
    }
}
