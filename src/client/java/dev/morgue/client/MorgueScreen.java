package dev.morgue.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MorgueScreen extends Screen {
    private static final int BLACK = 0xFF080808;
    private static final int PANEL = 0xFF111111;
    private static final int PANEL_LIGHT = 0xFF191919;
    private static final int BORDER = 0xFF303030;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFF888888;
    private static final int RED = 0xFF8E2525;

    private static final List<String> CATEGORIES = List.of(
            "Combat", "Movement", "Player", "Render", "World", "Misc", "Config"
    );

    private static final Map<String, List<String>> MODULES = new LinkedHashMap<>();
    private static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();

    static {
        MODULES.put("Combat", List.of("Aim Assist", "Click Assist", "Hit Settings"));
        MODULES.put("Movement", List.of("Sprint", "Safe Walk", "Movement Settings"));
        MODULES.put("Player", List.of("Inventory Helper", "Item Use", "Player Settings"));
        MODULES.put("Render", List.of("HUD Editor", "Module List", "Visual Settings"));
        MODULES.put("World", List.of("World Info", "Environment", "World Settings"));
        MODULES.put("Misc", List.of("Notifications", "Client Utilities", "General Settings"));
        MODULES.put("Config", List.of("Profiles", "Keybinds", "Theme"));
        MODULES.values().forEach(list -> list.forEach(name -> ENABLED.put(name, false)));
    }

    private String selectedCategory = "Combat";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public MorgueScreen() {
        super(Component.literal("MORGUE-CLIENT"));
    }

    @Override
    protected void init() {
        clearWidgets();

        panelWidth = Math.min(620, width - 32);
        panelHeight = Math.min(370, height - 32);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int sidebarX = panelX + 16;
        int sidebarY = panelY + 88;
        for (String category : CATEGORIES) {
            addRenderableWidget(Button.builder(Component.literal(category), button -> {
                selectedCategory = category;
                rebuildWidgets();
            }).bounds(sidebarX, sidebarY, 116, 24).build());
            sidebarY += 29;
        }

        int moduleX = panelX + 150;
        int moduleY = panelY + 104;
        for (String module : MODULES.getOrDefault(selectedCategory, List.of())) {
            addRenderableWidget(Button.builder(
                    Component.literal((ENABLED.getOrDefault(module, false) ? "ON   " : "OFF   ") + module),
                    button -> {
                        ENABLED.put(module, !ENABLED.getOrDefault(module, false));
                        rebuildWidgets();
                    }
            ).bounds(moduleX, moduleY, panelWidth - 170, 30).build());
            moduleY += 39;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xB9000000);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BLACK);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, BORDER);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, RED);

        graphics.fill(panelX + 1, panelY + 1, panelX + 132, panelY + 72, PANEL);
        graphics.text(font, Component.literal("M"), panelX + 20, panelY + 14, WHITE);
        graphics.text(font, Component.literal("MORGUE"), panelX + 49, panelY + 17, WHITE);
        graphics.text(font, Component.literal("CLIENT  /  26.3"), panelX + 49, panelY + 32, MUTED);

        graphics.fill(panelX + 145, panelY + 18, panelX + panelWidth - 16, panelY + 69, PANEL);
        graphics.text(font, Component.literal(selectedCategory.toUpperCase()), panelX + 161, panelY + 27, WHITE);
        graphics.text(font, Component.literal("INDEPENDENT CLIENT  //  BUILD 0.1.0"),
                panelX + 161, panelY + 43, MUTED);

        graphics.fill(panelX + 136, panelY + 82, panelX + 137, panelY + panelHeight - 16, BORDER);
        graphics.text(font, Component.literal("MODULES"), panelX + 151, panelY + 86, MUTED);

        graphics.fill(panelX + 16, panelY + panelHeight - 34, panelX + panelWidth - 16,
                panelY + panelHeight - 33, BORDER);
        graphics.text(font, Component.literal("MORGUE-CLIENT"), panelX + 17,
                panelY + panelHeight - 24, MUTED);
        graphics.text(font, Component.literal("RIGHT SHIFT  •  CLOSE"),
                panelX + panelWidth - 153, panelY + panelHeight - 24, MUTED);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
