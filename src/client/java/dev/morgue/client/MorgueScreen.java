package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.ui.UiSettings;
import dev.morgue.client.ui.UiSoundManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MorgueScreen extends Screen {
    private static final int BLACK = 0xA5000000;
    private static final int PANEL = 0xF00D121B;
    private static final int PANEL_LIGHT = 0xFF141E2B;
    private static final int BORDER = 0xFF2B394B;
    private static final int WHITE = 0xFFF1F5FA;
    private static final int MUTED = 0xFF94A3B8;
    private static int accentColor() { return UiSettings.accentColor(); }
    private static int accentSoftColor() {
        int color = UiSettings.accentColor();
        int r = ((color >>> 16) & 0xFF) / 3;
        int g = ((color >>> 8) & 0xFF) / 3;
        int b = (color & 0xFF) / 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static final int BUTTON_IDLE = 0xF0141C28;
    private static final int BUTTON_HOVER = 0xFF243246;
    private static final FontDescription DISPLAY_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("morgue", "display"));

    private static final String BRAND_SUBTITLE = "CLIENT  /  GHOST-INSPIRED WORKSPACE";
    private static final String OVERVIEW_HINT =
            "ALL CATEGORIES  /  CLICK TO TOGGLE  /  RIGHT-CLICK OR ARROW TO CONFIGURE";
    private static final String CLOSE_HINT = "RIGHT SHIFT  ·  CLOSE";

    // Minecraft 26.3 receives SDL mouse buttons, which are 1-based.
    private static final int MOUSE_BUTTON_LEFT = 1;
    private static final int MOUSE_BUTTON_RIGHT = 3;

    private static final List<String> CATEGORIES = List.of(
            "Combat", "Movement", "Player", "Render", "World", "Misc", "Privacy", "Config"
    );

    private static final Map<String, List<String>> MODULES = new LinkedHashMap<>();
    private static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();

    private static final List<EntityOption> ENTITY_OPTIONS = List.of(
            new EntityOption("Players", "minecraft:player"),
            new EntityOption("Zombie", "minecraft:zombie"),
            new EntityOption("Skeleton", "minecraft:skeleton"),
            new EntityOption("Creeper", "minecraft:creeper"),
            new EntityOption("Spider", "minecraft:spider"),
            new EntityOption("Cave Spider", "minecraft:cave_spider"),
            new EntityOption("Enderman", "minecraft:enderman"),
            new EntityOption("Witch", "minecraft:witch"),
            new EntityOption("Pillager", "minecraft:pillager"),
            new EntityOption("Drowned", "minecraft:drowned"),
            new EntityOption("Husk", "minecraft:husk"),
            new EntityOption("Phantom", "minecraft:phantom"),
            new EntityOption("Blaze", "minecraft:blaze"),
            new EntityOption("Wither Skeleton", "minecraft:wither_skeleton"),
            new EntityOption("Slime", "minecraft:slime"),
            new EntityOption("Magma Cube", "minecraft:magma_cube"),
            new EntityOption("Ghast", "minecraft:ghast"),
            new EntityOption("Piglin", "minecraft:piglin"),
            new EntityOption("Piglin Brute", "minecraft:piglin_brute"),
            new EntityOption("Hoglin", "minecraft:hoglin"),
            new EntityOption("Zoglin", "minecraft:zoglin"),
            new EntityOption("Ravager", "minecraft:ravager"),
            new EntityOption("Guardian", "minecraft:guardian"),
            new EntityOption("Elder Guardian", "minecraft:elder_guardian"),
            new EntityOption("Shulker", "minecraft:shulker"),
            new EntityOption("Vindicator", "minecraft:vindicator"),
            new EntityOption("Evoker", "minecraft:evoker"),
            new EntityOption("Vex", "minecraft:vex"),
            new EntityOption("Warden", "minecraft:warden"),
            new EntityOption("Wither", "minecraft:wither"),
            new EntityOption("Ender Dragon", "minecraft:ender_dragon"),
            new EntityOption("Villager", "minecraft:villager"),
            new EntityOption("Iron Golem", "minecraft:iron_golem"),
            new EntityOption("Wolf", "minecraft:wolf"),
            new EntityOption("Cow", "minecraft:cow"),
            new EntityOption("Pig", "minecraft:pig"),
            new EntityOption("Sheep", "minecraft:sheep"),
            new EntityOption("Chicken", "minecraft:chicken"),
            new EntityOption("Horse", "minecraft:horse"),
            new EntityOption("Other", "morgue:other")
    );

    static {
        MODULES.put("Combat", List.of("AI Brain", "Mace Hitter", "KillAura", "Aim Assist", "Click Assist"));
        MODULES.put("Movement", List.of("Sprint", "Safe Walk", "Movement Settings"));
        MODULES.put("Player", List.of("Auto Totem", "Inventory Helper", "Item Use"));
        MODULES.put("Render", List.of("HUD Editor", "Module List", "Visual Settings"));
        MODULES.put("World", List.of("World Info", "Environment", "World Settings"));
        MODULES.put("Misc", List.of("Resource Pack Manager", "Notifications", "Client Utilities", "Credits"));
        MODULES.put("Privacy", List.of("Privacy Dashboard", "Build Integrity", "Screenshot Privacy", "Client Metadata"));
        MODULES.put("Config", List.of("Profiles", "Keybinds", "Theme", "UI Effects"));
        MODULES.values().forEach(list -> list.forEach(name -> ENABLED.put(name, false)));
    }

    private String selectedCategory = "Combat";
    private String selectedModule;
    private boolean entityPage;
    private int entityPageIndex;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int margin;
    private int headerHeight;
    private int headingY;
    private int contentTop;
    private int footerTop;
    private boolean compactLayout;
    private boolean showBrandSubtitle;
    private boolean showPageSubtitle;
    private int overviewScroll;
    private int overviewContentHeight;
    private int overviewViewportHeight;
    private int settingsScroll;
    private int settingsContentHeight;
    private final java.util.List<OverviewCard> overviewCards = new java.util.ArrayList<>();
    private final Set<AbstractWidget> fixedWidgets =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private EditBox searchBox;
    private String searchQuery = "";
    private int moduleTotalCount;
    private int moduleVisibleCount;
    private final Map<AbstractWidget, Float> hoverProgress = new IdentityHashMap<>();
    private final Map<String, Float> toggleProgress = new HashMap<>();
    private final ArrayDeque<ClickRipple> clickRipples = new ArrayDeque<>();
    private long openedAtNanos = System.nanoTime();
    private long transitionAtNanos = openedAtNanos;
    private long lastFrameNanos;
    private String transitionKey = "";
    private boolean openSoundPlayed;
    private long lastSliderSoundNanos;
    private String profileStatus = "Profiles are stored locally in the MORGUE config folder.";

    public MorgueScreen() {
        super(Component.literal("Morgue Refined"));
    }

    @Override
    protected void init() {
        clearWidgets();
        hoverProgress.clear();
        if (!openSoundPlayed) {
            UiSoundManager.play(UiSoundManager.Effect.OPEN);
            openSoundPlayed = true;
        }

        // Full-screen workspace: no floating window, dragging, or resizing.
        panelX = 0;
        panelY = 0;
        panelWidth = Math.max(1, width);
        panelHeight = Math.max(1, height);
        computeLayout();

        String nextTransitionKey = selectedCategory + ":" + selectedModule + ":" + entityPage;
        if (!nextTransitionKey.equals(transitionKey)) {
            transitionKey = nextTransitionKey;
            transitionAtNanos = System.nanoTime();
            settingsScroll = 0;
        }

        if (selectedModule == null) {
            buildSearchControls();
            overviewCards.clear();
            buildGhostOverview();
        } else if (entityPage) {
            buildEntitySelector();
        } else if ("Sprint".equals(selectedModule)) {
            buildSprintSettings();
        } else if ("Safe Walk".equals(selectedModule)) {
            buildSafeWalkSettings();
        } else if ("Inventory Helper".equals(selectedModule)) {
            buildInventoryHelperSettings();
        } else if ("Movement Settings".equals(selectedModule)) {
            buildMovementSettings();
        } else if ("Module List".equals(selectedModule)) {
            buildEnabledModuleList();
        } else if ("Environment".equals(selectedModule)) {
            buildEnvironmentInfo();
        } else if ("Visual Settings".equals(selectedModule)) {
            buildVisualSettings();
        } else if ("Notifications".equals(selectedModule)) {
            buildNotificationsSettings();
        } else if ("Client Utilities".equals(selectedModule)) {
            buildClientUtilities();
        } else if ("Screenshot Privacy".equals(selectedModule)) {
            buildScreenshotPrivacy();
        } else if ("World Settings".equals(selectedModule)) {
            buildWorldSettings();
        } else if ("Item Use".equals(selectedModule)) {
            buildAutoEatSettings();
        } else if ("HUD Editor".equals(selectedModule)) {
            buildHudEditorInfo();
        } else if ("Aim Assist".equals(selectedModule)) {
            buildAimAssistSettings();
        } else if ("Click Assist".equals(selectedModule)) {
            buildClickAssistSettings();
        } else if ("Mace Hitter".equals(selectedModule)) {
            buildMaceHitterSettings();
        } else if ("Auto Totem".equals(selectedModule)) {
            buildAutoTotemSettings();
        } else if ("KillAura".equals(selectedModule)) {
            buildKillAuraSettings();
        } else if ("World Info".equals(selectedModule)) {
            buildWorldInfo();
        } else if ("Client Metadata".equals(selectedModule)) {
            buildClientMetadata();
        } else if ("AI Brain".equals(selectedModule)) {
            buildAiBrainSettings();
        } else if ("Credits".equals(selectedModule)) {
            buildCredits();
        } else if ("Resource Pack Manager".equals(selectedModule)) {
            buildResourcePackManager();
        } else if ("Privacy Dashboard".equals(selectedModule)) {
            buildPrivacyDashboard();
        } else if ("Build Integrity".equals(selectedModule)) {
            buildIntegrityDashboard();
        } else if ("Keybinds".equals(selectedModule)) {
            buildKeybindSettings();
        } else if ("Profiles".equals(selectedModule)) {
            buildProfileSettings();
        } else if ("Theme".equals(selectedModule)) {
            buildThemeSettings();
        } else if ("UI Effects".equals(selectedModule)) {
            buildUiEffectsSettings();
        } else {
            buildPlaceholderSettings();
        }

        if (selectedModule != null) {
            applySettingsScroll();
        }
    }

    private void computeLayout() {
        margin = Math.max(12, Math.min(22, width / 60));
        compactLayout = height < 640;
        showBrandSubtitle = width - margin * 2 >= 560;
        showPageSubtitle = height >= 320;
        headerHeight = height < 320 ? 50 : compactLayout ? 56 : 68;
        headingY = headerHeight + (showPageSubtitle ? (compactLayout ? 12 : 14) : 14);
        contentTop = headingY + (showPageSubtitle ? 36 : 18);
        int footerHeight = height < 320 ? 24 : 30;
        footerTop = height - footerHeight;
    }

    private int contentWidth() {
        return Math.max(1, width - margin * 2);
    }

    private int overviewViewportBottom() {
        return footerTop - 8;
    }

    private int backButtonY() {
        return footerTop - 32;
    }

    private int settingsViewportBottom() {
        return backButtonY() - 8;
    }

    private int settingsLeftX() {
        return margin + 8;
    }

    private int settingsColumnWidth() {
        return Math.max(120, (contentWidth() - 16 - 12) / 2);
    }

    private int settingsRightX() {
        return settingsLeftX() + settingsColumnWidth() + 12;
    }

    private int settingsWideWidth() {
        return Math.max(160, contentWidth() - 24);
    }

    private int settingsTop() {
        return contentTop + 2;
    }

    private int brandWidth() {
        int brand = font.width(displayText("MORGUE"));
        if (showBrandSubtitle) {
            brand = Math.max(brand, font.width(displayText(BRAND_SUBTITLE)));
        }
        return brand;
    }

    private void buildSearchControls() {
        int searchHeight = height < 320 ? 24 : 26;
        int filterWidth = 58;
        int available = width - margin * 2 - brandWidth() - 16;
        int searchWidth = Math.max(80, Math.min(320, available - filterWidth - 8));
        int controlY = Math.max(6, (headerHeight - searchHeight) / 2);
        int searchX = width - margin - filterWidth - 8 - searchWidth;
        searchBox = new EditBox(font, searchX, controlY, searchWidth, searchHeight,
                Component.literal("Search modules"));
        searchBox.setValue(searchQuery);
        addRenderableWidget(searchBox);
        AbstractWidget filter = Button.builder(Component.literal("FILTER"), button -> {
            searchQuery = searchBox.getValue().trim();
            overviewScroll = 0;
            rebuildWidgets();
        }).bounds(width - margin - filterWidth, controlY, filterWidth, searchHeight).build();
        addRenderableWidget(filter);
        fixedWidgets.add(filter);
    }

    private void applySettingsScroll() {
        int contentBottom = contentTop;
        for (var child : children()) {
            if (!(child instanceof AbstractWidget widget) || fixedWidgets.contains(widget)) continue;
            contentBottom = Math.max(contentBottom, widget.getY() + widget.getHeight());
        }
        settingsContentHeight = Math.max(0, contentBottom - contentTop + 10);
        int viewportHeight = Math.max(1, settingsViewportBottom() - contentTop);
        int maxScroll = Math.max(0, settingsContentHeight - viewportHeight);
        settingsScroll = Math.max(0, Math.min(settingsScroll, maxScroll));
        if (settingsScroll == 0) return;
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !fixedWidgets.contains(widget)) {
                widget.setY(widget.getY() - settingsScroll);
            }
        }
    }

    private void buildGhostOverview() {
        overviewCards.clear();
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<java.util.List<String>> moduleLists = new java.util.ArrayList<>();
        moduleTotalCount = 0;
        moduleVisibleCount = 0;
        for (String category : CATEGORIES) {
            List<String> filtered = MODULES.getOrDefault(category, List.of()).stream()
                    .filter(module -> query.isEmpty()
                            || module.toLowerCase(java.util.Locale.ROOT).contains(query)
                            || category.toLowerCase(java.util.Locale.ROOT).contains(query))
                    .toList();
            if (filtered.isEmpty()) continue;
            names.add(category);
            moduleLists.add(filtered);
            moduleTotalCount += filtered.size();
        }
        moduleVisibleCount = moduleTotalCount;

        int viewportHeight = Math.max(1, overviewViewportBottom() - contentTop);
        overviewViewportHeight = viewportHeight;
        if (names.isEmpty()) {
            overviewContentHeight = 0;
            overviewScroll = 0;
            return;
        }

        int gap = compactLayout ? 8 : 12;
        int columns = Math.max(1, Math.min(4,
                (Math.max(160, contentWidth() - 10) + gap) / (200 + gap)));
        int gridWidth = Math.max(160, contentWidth() - 10);
        int cardWidth = Math.max(150, (gridWidth - gap * (columns - 1)) / columns);
        int rows = (names.size() + columns - 1) / columns;
        int pitch = compactLayout ? 24 : 30;
        int buttonHeight = compactLayout ? 20 : 26;
        int cardHeader = compactLayout ? 26 : 34;

        int[] rowHeights = new int[rows];
        for (int row = 0; row < rows; row++) {
            int maxModules = 0;
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index < names.size()) {
                    maxModules = Math.max(maxModules, moduleLists.get(index).size());
                }
            }
            rowHeights[row] = cardHeader + 4
                    + Math.max(0, maxModules - 1) * pitch + buttonHeight + 8;
        }

        int naturalTotal = gap * (rows - 1);
        for (int rowHeight : rowHeights) naturalTotal += rowHeight;
        if (naturalTotal <= viewportHeight) {
            int extra = viewportHeight - naturalTotal;
            int base = extra / rows;
            int remainder = extra % rows;
            for (int row = 0; row < rows; row++) {
                rowHeights[row] += base + (row < remainder ? 1 : 0);
            }
            overviewContentHeight = viewportHeight;
            overviewScroll = 0;
        } else {
            overviewContentHeight = naturalTotal;
            overviewScroll = Math.max(0, Math.min(overviewScroll,
                    overviewContentHeight - viewportHeight));
        }

        int viewportBottom = overviewViewportBottom();
        int cardY = contentTop - overviewScroll;
        for (int row = 0; row < rows; row++) {
            int cardHeight = rowHeights[row];
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= names.size()) break;
                String category = names.get(index);
                List<String> filtered = moduleLists.get(index);
                int cardX = margin + column * (cardWidth + gap);
                overviewCards.add(new OverviewCard(category, filtered.size(),
                        cardX, cardY, cardWidth, cardHeight));
                int itemY = cardY + cardHeader + 4;
                for (String module : filtered) {
                    if (itemY + buttonHeight >= contentTop && itemY <= viewportBottom) {
                        addOverviewModule(cardX, itemY, cardWidth, buttonHeight, category, module);
                    }
                    itemY += pitch;
                }
            }
            cardY += cardHeight + gap;
        }
    }

    private void addOverviewModule(int cardX, int itemY, int cardWidth, int buttonHeight,
                                   String category, String module) {
        boolean viewOnly = isViewOnlyModule(module);
        String state = viewOnly ? "INFO  " : (isModuleEnabled(module) ? "ON  " : "OFF ");
        addRenderableWidget(Button.builder(Component.literal(state + module), button -> {
            selectedCategory = category;
            if (viewOnly) {
                selectedModule = module;
                entityPage = false;
            } else {
                toggleModule(module);
            }
            rebuildWidgets();
        }).bounds(cardX + 10, itemY, cardWidth - 58, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.literal("›"), button -> {
            selectedCategory = category;
            selectedModule = module;
            entityPage = false;
            rebuildWidgets();
        }).bounds(cardX + cardWidth - 42, itemY, 32, buttonHeight).build());
    }

    private static boolean isViewOnlyModule(String module) {
        return "AI Brain".equals(module) || "Build Integrity".equals(module)
                || "Credits".equals(module) || "World Info".equals(module)
                || "Client Metadata".equals(module) || "Module List".equals(module)
                || "Environment".equals(module) || "Visual Settings".equals(module)
                || "Notifications".equals(module) || "Client Utilities".equals(module)
                || "Screenshot Privacy".equals(module) || "World Settings".equals(module)
                || "Item Use".equals(module) || "HUD Editor".equals(module)
                || "UI Effects".equals(module) || "Theme".equals(module);
    }

    private void buildInventoryHelperSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.inventoryHelper();
        addSettingButton(x, y, toggleLabel("Inventory Refill", module.isEnabled()),
                () -> MorgueClient.setInventoryHelperEnabled(!module.isEnabled()));
        addSettingButton(x, y + 36, "Refill At: " + module.getRefillAt() + " items",
                () -> module.setRefillAt(module.getRefillAt() >= 16 ? 1 : module.getRefillAt() + 1));
        addRenderableWidget(Button.builder(Component.literal(
                "Swaps matching low-count hotbar stacks with main-inventory stacks."),
                button -> {}).bounds(x, y + 76, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla container input; item components must match."),
                button -> {}).bounds(x, y + 112, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildSafeWalkSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.safeWalk();
        addSettingButton(x, y, toggleLabel("Safe Walk", module.isEnabled()),
                () -> MorgueClient.setSafeWalkEnabled(!module.isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically crouches when movement projects beyond a supported block edge."),
                button -> {}).bounds(x, y + 38, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Your physical sneak key takes priority over automatic edge protection."),
                button -> {}).bounds(x, y + 76, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildSprintSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.sprint();
        addSettingButton(x, y, toggleLabel("Auto Sprint", module.isEnabled()),
                () -> MorgueClient.setSprintEnabled(!module.isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically sprints while horizontal movement is detected."),
                button -> {}).bounds(x, y + 38, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses normal player sprint state; it does not alter packets or bypass server rules."),
                button -> {}).bounds(x, y + 76, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildAimAssistSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.aimAssist();
        addSettingButton(x, y, toggleLabel("Visible Aim Assist", module.isEnabled()),
                () -> MorgueClient.setAimAssistEnabled(!module.isEnabled()));
        addSettingButton(x, y + 36, "Range: " + format(module.getRange()),
                () -> module.setRange(module.getRange() >= 6.0 ? 1.0 : module.getRange() + 0.5));
        addSettingButton(x, y + 72, "FOV: " + format(module.getFov()),
                () -> module.setFov(module.getFov() >= 180.0 ? 15.0 : module.getFov() + 15.0));
        addSettingButton(x, y + 108, "Smoothing: " + format(module.getSmoothing()),
                () -> module.setSmoothing(module.getSmoothing() >= 1.0F ? 0.05F : module.getSmoothing() + 0.05F));
        addRenderableWidget(Button.builder(Component.literal(
                "Only assists while you hold attack, targets visible players, and rotates visibly."),
                button -> {}).bounds(x, y + 150, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically disables its steering while Mace Hitter or KillAura is enabled."),
                button -> {}).bounds(x, y + 186, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildClickAssistSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.clickAssist();
        addSettingButton(x, y, toggleLabel("Click Assist", module.isEnabled()),
                () -> MorgueClient.setClickAssistEnabled(!module.isEnabled()));
        addSettingButton(x, y + 36, "Rate: " + module.getClicksPerSecond() + " CPS",
                () -> module.setClicksPerSecond(module.getClicksPerSecond() >= 20
                        ? 1 : module.getClicksPerSecond() + 1));
        addRenderableWidget(Button.builder(Component.literal(
                "Repeats your held attack input against the entity under your crosshair."),
                button -> {}).bounds(x, y + 78, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla attack cooldown and shared combat arbitration; no packet spoofing."),
                button -> {}).bounds(x, y + 114, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildMaceHitterSettings() {
        MaceHitterModule module = MorgueClient.maceHitter();
        int leftX = settingsLeftX();
        int rightX = settingsRightX();
        int y = settingsTop();
        int row = 0;

        addSettingButton(leftX, y + row++ * 27, "Range: " + format(module.getRange()) + "  (+0.2)",
                () -> module.setRange(module.getRange() >= 6.0 ? 1.0 : module.getRange() + 0.2));
        addSettingButton(leftX, y + row++ * 27, "FOV: " + format(module.getFov()) + "  (+15)",
                () -> module.setFov(module.getFov() >= 360.0 ? 15.0 : module.getFov() + 15.0));
        addSettingButton(leftX, y + row++ * 27, "Minimum Fall: " + format(module.getMinimumFallDistance()) + "  (+0.1)",
                () -> module.setMinimumFallDistance(module.getMinimumFallDistance() >= 5.0 ? 0.0 : module.getMinimumFallDistance() + 0.1));
        addSettingButton(leftX, y + row++ * 27, "Attack Delay: " + module.getAttackDelayMillis() + " ms  (+10)",
                () -> module.setAttackDelayMillis(module.getAttackDelayMillis() >= 200 ? 0 : module.getAttackDelayMillis() + 10));
        addSettingButton(leftX, y + row++ * 27, "Priority: " + module.getPriority(),
                () -> module.setPriority(switch (module.getPriority()) {
                    case CLOSEST -> MaceHitterModule.Priority.LOWEST_HEALTH;
                    case LOWEST_HEALTH -> MaceHitterModule.Priority.THREAT;
                    case THREAT -> MaceHitterModule.Priority.CLOSEST;
                }));
        addSettingButton(leftX, y + row++ * 27, "Target Mode: " + module.getTargetMode(),
                () -> module.setTargetMode(nextTargetMode(module.getTargetMode())));

        row = 0;
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Rotation", module.isRotate()),
                () -> module.setRotate(!module.isRotate()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Auto Swap", module.isAutoSwap()),
                () -> module.setAutoSwap(!module.isAutoSwap()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Auto Jump", module.isAutoJump()),
                () -> module.setAutoJump(!module.isAutoJump()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Falling Only", module.isFallingOnly()),
                () -> module.setFallingOnly(!module.isFallingOnly()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Shield Stun", module.isShieldStun()),
                () -> module.setShieldStun(!module.isShieldStun()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Require Mace", module.isRequireMace()),
                () -> module.setRequireMace(!module.isRequireMace()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Ignore Walls", module.isIgnoreWalls()),
                () -> module.setIgnoreWalls(!module.isIgnoreWalls()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("FOV Check", module.isFovCheck()),
                () -> module.setFovCheck(!module.isFovCheck()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Team Check", module.isTeamsCheck()),
                () -> module.setTeamsCheck(!module.isTeamsCheck()));
        addSettingButton(rightX, y + row++ * 27, toggleLabel("Ignore Cooldown", module.isIgnoreCooldown()),
                () -> module.setIgnoreCooldown(!module.isIgnoreCooldown()));

        addRenderableWidget(Button.builder(Component.literal("ENTITY TARGET FILTER  >"), button -> {
            entityPage = true;
            entityPageIndex = 0;
            rebuildWidgets();
        }).bounds(leftX, y + 6 * 27 + 8, settingsColumnWidth(), 26).build());

        addBackButton();
    }

    private void buildEntitySelector() {
        int entityPitch = compactLayout ? 24 : 27;
        int entityHeight = entityPitch - 3;
        int rowsTop = settingsTop() + 36;
        int pageY = rowsTop + 8 * entityPitch + 8;
        int leftX = settingsLeftX();
        int rightX = settingsRightX();

        if ("KillAura".equals(selectedModule)) {
            var module = MorgueClient.killAura();
            Set<String> selected = module.getSelectedEntityTypes();
            addRenderableWidget(Button.builder(Component.literal("TARGET MODE: " + module.getTargetMode()), button -> {
                module.setTargetMode(switch (module.getTargetMode()) {
                    case PLAYERS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.HOSTILE_MOBS;
                    case HOSTILE_MOBS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.ALL_LIVING;
                    case ALL_LIVING -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.CUSTOM;
                    case CUSTOM -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.PLAYERS;
                });
                MorgueConfig.save();
                rebuildWidgets();
            }).bounds(leftX, settingsTop(), settingsWideWidth(), 26).build());

            int pageSize = 16;
            int start = entityPageIndex * pageSize;
            int end = Math.min(ENTITY_OPTIONS.size(), start + pageSize);
            for (int i = start; i < end; i++) {
                EntityOption option = ENTITY_OPTIONS.get(i);
                int visibleIndex = i - start;
                int columnX = visibleIndex < 8 ? leftX : rightX;
                int rowY = rowsTop + (visibleIndex % 8) * entityPitch;
                boolean enabled = selected.contains(option.id());
                addRenderableWidget(Button.builder(Component.literal((enabled ? "[x] " : "[ ] ") + option.name()),
                        button -> {
                            module.setEntityTypeSelected(option.id(), !enabled);
                            module.setTargetMode(dev.morgue.client.modules.combat.KillAuraModule.TargetMode.CUSTOM);
                            MorgueConfig.save();
                            rebuildWidgets();
                        }).bounds(columnX, rowY, settingsColumnWidth(), entityHeight).build());
            }

            if (entityPageIndex > 0) {
                addRenderableWidget(Button.builder(Component.literal("< PREVIOUS"), button -> {
                    entityPageIndex--;
                    rebuildWidgets();
                }).bounds(leftX, pageY, 104, 26).build());
            }
            if (end < ENTITY_OPTIONS.size()) {
                addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                    entityPageIndex++;
                    rebuildWidgets();
                }).bounds(rightX + settingsColumnWidth() - 90, pageY, 90, 26).build());
            }
            addBackButton();
            return;
        }

        MaceHitterModule module = MorgueClient.maceHitter();
        Set<String> selected = module.getSelectedEntityTypes();
        addRenderableWidget(Button.builder(Component.literal("TARGET MODE: " + module.getTargetMode()), button -> {
            module.setTargetMode(nextTargetMode(module.getTargetMode()));
            MorgueConfig.save();
            rebuildWidgets();
        }).bounds(leftX, settingsTop(), settingsWideWidth(), 26).build());

        int pageSize = 16;
        int start = entityPageIndex * pageSize;
        int end = Math.min(ENTITY_OPTIONS.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            EntityOption option = ENTITY_OPTIONS.get(i);
            int visibleIndex = i - start;
            int columnX = visibleIndex < 8 ? leftX : rightX;
            int rowY = rowsTop + (visibleIndex % 8) * entityPitch;
            boolean enabled = selected.contains(option.id());
            addRenderableWidget(Button.builder(Component.literal((enabled ? "[x] " : "[ ] ") + option.name()),
                    button -> {
                        module.setEntityTypeSelected(option.id(), !enabled);
                        module.setTargetMode(MaceHitterModule.TargetMode.CUSTOM);
                        MorgueConfig.save();
                        rebuildWidgets();
                    }).bounds(columnX, rowY, settingsColumnWidth(), entityHeight).build());
        }

        if (entityPageIndex > 0) {
            addRenderableWidget(Button.builder(Component.literal("< PREVIOUS"), button -> {
                entityPageIndex--;
                rebuildWidgets();
            }).bounds(leftX, pageY, 104, 26).build());
        }
        if (end < ENTITY_OPTIONS.size()) {
            addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                entityPageIndex++;
                rebuildWidgets();
            }).bounds(rightX + settingsColumnWidth() - 90, pageY, 90, 26).build());
        }
        addBackButton();
    }

    private void buildKillAuraSettings() {
        var module = MorgueClient.killAura();
        int leftX = settingsLeftX();
        int rightX = settingsRightX();
        int y = settingsTop();
        int leftRow = 0;
        int rightRow = 0;

        addSettingButton(leftX, y + leftRow++ * 27, "Range: " + format(module.getRange()) + "  (+0.2)",
                () -> module.setRange(module.getRange() >= 6.0 ? 1.0 : module.getRange() + 0.2));
        addSettingButton(leftX, y + leftRow++ * 27, "FOV: " + format(module.getFov()) + "  (+15)",
                () -> module.setFov(module.getFov() >= 360.0 ? 15.0 : module.getFov() + 15.0));
        addSettingButton(leftX, y + leftRow++ * 27, "Attack Delay: " + module.getAttackDelayMillis() + " ms",
                () -> module.setAttackDelayMillis(module.getAttackDelayMillis() >= 200 ? 0 : module.getAttackDelayMillis() + 10));
        addSettingButton(leftX, y + leftRow++ * 27, "Priority: " + module.getPriority(),
                () -> module.setPriority(switch (module.getPriority()) {
                    case CLOSEST -> dev.morgue.client.modules.combat.KillAuraModule.Priority.LOWEST_HEALTH;
                    case LOWEST_HEALTH -> dev.morgue.client.modules.combat.KillAuraModule.Priority.THREAT;
                    case THREAT -> dev.morgue.client.modules.combat.KillAuraModule.Priority.CLOSEST;
                }));
        addSettingButton(leftX, y + leftRow++ * 27, "Targets: " + module.getTargetMode(),
                () -> module.setTargetMode(switch (module.getTargetMode()) {
                    case PLAYERS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.HOSTILE_MOBS;
                    case HOSTILE_MOBS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.ALL_LIVING;
                    case ALL_LIVING -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.CUSTOM;
                    case CUSTOM -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.PLAYERS;
                }));

        addRenderableWidget(Button.builder(Component.literal("CONFIGURE ENTITY TARGETS  >"), button -> {
            entityPage = true;
            entityPageIndex = 0;
            rebuildWidgets();
        }).bounds(leftX, y + leftRow * 27, settingsColumnWidth(), 26).build());

        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Rotation", module.isRotate()),
                () -> module.setRotate(!module.isRotate()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Auto Sword", module.isAutoSword()),
                () -> module.setAutoSword(!module.isAutoSword()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Switch Back", module.isSwitchBack()),
                () -> module.setSwitchBack(!module.isSwitchBack()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Shield Break", module.isShieldBreak()),
                () -> module.setShieldBreak(!module.isShieldBreak()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Ignore Walls", module.isIgnoreWalls()),
                () -> module.setIgnoreWalls(!module.isIgnoreWalls()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("FOV Check", module.isFovCheck()),
                () -> module.setFovCheck(!module.isFovCheck()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Team Check", module.isTeamsCheck()),
                () -> module.setTeamsCheck(!module.isTeamsCheck()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Ignore Hurt Time", module.isIgnoreHurtTime()),
                () -> module.setIgnoreHurtTime(!module.isIgnoreHurtTime()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Vanilla Critical Only", module.isCriticalOnly()),
                () -> module.setCriticalOnly(!module.isCriticalOnly()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Pause on Throwables", module.isPauseOnThrowables()),
                () -> module.setPauseOnThrowables(!module.isPauseOnThrowables()));
        addSettingButton(rightX, y + rightRow++ * 27, toggleLabel("Attack Sound", module.isAttackSound()),
                () -> module.setAttackSound(!module.isAttackSound()));
        addSettingButton(leftX, y + (leftRow + 1) * 27, "Minimum Target Age: " + module.getMinimumAgeTicks() + " ticks",
                () -> module.setMinimumAgeTicks(module.getMinimumAgeTicks() >= 200 ? 0 : module.getMinimumAgeTicks() + 20));
        addSettingButton(leftX, y + (leftRow + 2) * 27, "Rotation Speed: " + format(module.getRotationSpeed()),
                () -> module.setRotationSpeed(module.getRotationSpeed() >= 0.95 ? 0.1 : module.getRotationSpeed() + 0.1));

        addBackButton();
    }

    private void buildAiBrainSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var assessment = dev.morgue.client.ai.TargetIntelligence.getLatestAssessment();

        String activeCombat = MorgueClient.maceHitter().isEnabled() && MorgueClient.killAura().isEnabled()
                ? "Mace Hitter priority / KillAura fallback"
                : MorgueClient.maceHitter().isEnabled() ? "Mace Hitter"
                : MorgueClient.killAura().isEnabled() ? "KillAura" : "No combat module active";
        addRenderableWidget(Button.builder(Component.literal("ENGINE: LOCAL TARGET INTELLIGENCE  /  READY"),
                button -> {}).bounds(x, y, settingsWideWidth(), 27).build());
        addRenderableWidget(Button.builder(Component.literal("ACTIVE CONTROLLER: " + activeCombat),
                button -> {}).bounds(x, y + 34, settingsWideWidth(), 27).build());

        if (assessment == null) {
            addRenderableWidget(Button.builder(Component.literal("CURRENT TARGET: Waiting for a valid target"),
                    button -> {}).bounds(x, y + 68, settingsWideWidth(), 27).build());
            addRenderableWidget(Button.builder(Component.literal("ASSESSMENT: No candidate passed the active filters"),
                    button -> {}).bounds(x, y + 102, settingsWideWidth(), 27).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("CURRENT TARGET: " + assessment.targetName()),
                    button -> {}).bounds(x, y + 68, settingsWideWidth(), 27).build());
            addRenderableWidget(Button.builder(Component.literal("PRIORITY: " + assessment.priority()
                    + "  /  DISTANCE: " + format(assessment.distance())),
                    button -> {}).bounds(x, y + 102, settingsWideWidth(), 27).build());
            addRenderableWidget(Button.builder(Component.literal("THREAT SCORE: "
                    + String.format(java.util.Locale.ROOT, "%.2f", assessment.threatScore())
                    + "  /  HEALTH: " + format(assessment.health())),
                    button -> {}).bounds(x, y + 136, settingsWideWidth(), 27).build());
            addRenderableWidget(Button.builder(Component.literal("REASON: " + assessment.reason()),
                    button -> {}).bounds(x, y + 170, settingsWideWidth(), 27).build());
        }

        addRenderableWidget(Button.builder(Component.literal("DECISION MODEL: deterministic local heuristics; no cloud AI"),
                button -> {}).bounds(x, y + 208, settingsWideWidth(), 27).build());
        addRenderableWidget(Button.builder(Component.literal("CLEAR LAST ASSESSMENT"), button -> {
            dev.morgue.client.ai.TargetIntelligence.clearAssessment();
            rebuildWidgets();
        }).bounds(x, y + 242, settingsWideWidth(), 27).build());

        addBackButton();
    }

    private void buildAutoTotemSettings() {
        var module = MorgueClient.autoTotem();
        int leftX = settingsLeftX();
        int rightX = settingsRightX();
        int y = settingsTop();

        addSettingButton(leftX, y, "Mode: " + module.getMode(), () -> {
            module.setMode(switch (module.getMode()) {
                case SMART -> dev.morgue.client.modules.player.AutoTotemModule.Mode.SWITCH;
                case SWITCH -> dev.morgue.client.modules.player.AutoTotemModule.Mode.PICK_UP;
                case PICK_UP -> dev.morgue.client.modules.player.AutoTotemModule.Mode.SMART;
            });
        });
        addSettingButton(leftX, y + 36, "Search Priority: " + module.getSearchPriority(), () -> {
            module.setSearchPriority(module.getSearchPriority()
                    == dev.morgue.client.modules.player.AutoTotemModule.SearchPriority.HOTBAR_FIRST
                    ? dev.morgue.client.modules.player.AutoTotemModule.SearchPriority.INVENTORY_FIRST
                    : dev.morgue.client.modules.player.AutoTotemModule.SearchPriority.HOTBAR_FIRST);
        });
        addSettingButton(leftX, y + 72, "Switch Delay: " + module.getSwitchDelayMillis() + " ms", () -> {
            module.setSwitchDelayMillis(module.getSwitchDelayMillis() >= 250 ? 0 : module.getSwitchDelayMillis() + 25);
        });
        addSettingButton(leftX, y + 108, "Activation Health: " + format(module.getActivationHealth()),
                () -> module.setActivationHealth(module.getActivationHealth() <= 0.0F ? 20.0F
                        : module.getActivationHealth() <= 2.0F ? 0.0F : module.getActivationHealth() - 2.0F));
        addSettingButton(rightX, y, toggleLabel("Pause While Using Item", module.isPauseWhileUsingItem()),
                () -> module.setPauseWhileUsingItem(!module.isPauseWhileUsingItem()));
        addSettingButton(rightX, y + 72, toggleLabel("Predict Recent Damage", module.isPredictRecentDamage()),
                () -> module.setPredictRecentDamage(!module.isPredictRecentDamage()));
        addSettingButton(rightX, y + 108, toggleLabel("Predict Fall Damage", module.isPredictFallDamage()),
                () -> module.setPredictFallDamage(!module.isPredictFallDamage()));
        addSettingButton(rightX, y + 144, toggleLabel("Keep Totem Equipped", module.isKeepTotemEquipped()),
                () -> module.setKeepTotemEquipped(!module.isKeepTotemEquipped()));
        addSettingButton(rightX, y + 180, toggleLabel("Predict Nearby Threats", module.isPredictNearbyThreats()),
                () -> module.setPredictNearbyThreats(!module.isPredictNearbyThreats()));

        addRenderableWidget(Button.builder(Component.literal("AUTO TOTEM STATUS: " + (module.isEnabled() ? "ON" : "OFF")),
                button -> {
                    MorgueClient.setAutoTotemEnabled(!module.isEnabled());
                    ENABLED.put("Auto Totem", MorgueClient.autoTotem().isEnabled());
                    MorgueConfig.save();
                    rebuildWidgets();
                }).bounds(rightX, y + 36, settingsColumnWidth(), 27).build());

        addBackButton();
    }

    private void buildMovementSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        addSettingButton(x, y, toggleLabel("Auto Sprint", MorgueClient.sprint().isEnabled()),
                () -> MorgueClient.setSprintEnabled(!MorgueClient.sprint().isEnabled()));
        addSettingButton(x, y + 36, toggleLabel("Safe Walk", MorgueClient.safeWalk().isEnabled()),
                () -> MorgueClient.setSafeWalkEnabled(!MorgueClient.safeWalk().isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Both options use normal client movement state and respect the server's movement rules."),
                button -> {}).bounds(x, y + 78, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildEnabledModuleList() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        String[] lines = {
                "MACE HITTER: " + (MorgueClient.maceHitter().isEnabled() ? "ON" : "OFF"),
                "KILLAURA: " + (MorgueClient.killAura().isEnabled() ? "ON" : "OFF"),
                "AUTO TOTEM: " + (MorgueClient.autoTotem().isEnabled() ? "ON" : "OFF"),
                "SPRINT: " + (MorgueClient.sprint().isEnabled() ? "ON" : "OFF"),
                "SAFE WALK: " + (MorgueClient.safeWalk().isEnabled() ? "ON" : "OFF"),
                "INVENTORY HELPER: " + (MorgueClient.inventoryHelper().isEnabled() ? "ON" : "OFF")
        };
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            addRenderableWidget(Button.builder(Component.literal(line), button -> {})
                    .bounds(x, y + i * 32, width, 27).build());
        }
        addBackButton();
    }

    private void buildEnvironmentInfo() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        if (minecraft == null || minecraft.level == null) {
            addRenderableWidget(Button.builder(Component.literal("ENVIRONMENT: No world loaded."),
                    button -> {}).bounds(x, y, width, 28).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("RAINING: "
                    + (minecraft.level.isRaining() ? "YES" : "NO")),
                    button -> {}).bounds(x, y, width, 28).build());
            addRenderableWidget(Button.builder(Component.literal("THUNDER: "
                    + (minecraft.level.isThundering() ? "YES" : "NO")),
                    button -> {}).bounds(x, y + 36, width, 28).build());
            addRenderableWidget(Button.builder(Component.literal("WORLD GAME TIME: "
                    + minecraft.level.getGameTime()),
                    button -> {}).bounds(x, y + 72, width, 28).build());
        }
        addBackButton();
    }

    private void buildClientUtilities() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        addRenderableWidget(Button.builder(Component.literal("OPEN RESOURCE PACK MANAGER"), button -> {
            selectedModule = "Resource Pack Manager";
            rebuildWidgets();
        }).bounds(x, y, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("OPEN KEYBIND SETTINGS"), button -> {
            selectedModule = "Keybinds";
            rebuildWidgets();
        }).bounds(x, y + 36, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("OPEN CONFIGURATION PROFILES"), button -> {
            selectedModule = "Profiles";
            rebuildWidgets();
        }).bounds(x, y + 72, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("RUN PACKAGE INTEGRITY CHECK"), button -> {
            dev.morgue.client.security.MorgueIntegrity.verify();
            selectedModule = "Build Integrity";
            rebuildWidgets();
        }).bounds(x, y + 108, width, 28).build());
        addBackButton();
    }

    private void buildScreenshotPrivacy() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        String[] lines = {
                "SCREENSHOT PRIVACY: STANDARD GAME CAPTURE",
                "MORGUE DOES NOT HIDE MODULES OR ALTER SCREENSHOTS",
                "NO SCREEN-SHARE OR STREAMING CONCEALMENT IS ACTIVE",
                "USE YOUR OPERATING SYSTEM'S NORMAL SCREENSHOT CONTROLS",
                "LOCAL CLIENT PRIVACY DOES NOT OVERRIDE SERVER MODERATION"
        };
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            addRenderableWidget(Button.builder(Component.literal(line), button -> {})
                    .bounds(x, y + i * 34, width, 28).build());
        }
        addBackButton();
    }

    private void buildWorldSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        addRenderableWidget(Button.builder(Component.literal("WORLD RULES ARE CONTROLLED BY THE WORLD OWNER OR SERVER."),
                button -> {}).bounds(x, y, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("MORGUE DOES NOT OVERRIDE SERVER GAME RULES OR DIFFICULTY."),
                button -> {}).bounds(x, y + 36, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("USE VANILLA WORLD OPTIONS FOR PERMITTED LOCAL SETTINGS."),
                button -> {}).bounds(x, y + 72, width, 28).build());
        addBackButton();
    }

    private void buildAutoEatSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        var module = MorgueClient.autoEat();
        addSettingButton(x, y, toggleLabel("Auto Eat", module.isEnabled()),
                () -> MorgueClient.setAutoEatEnabled(!module.isEnabled()));
        addSettingButton(x, y + 36, "Hunger Threshold: " + module.getHungerThreshold(),
                () -> module.setHungerThreshold(module.getHungerThreshold() >= 19
                        ? 1 : module.getHungerThreshold() + 1));
        addRenderableWidget(Button.builder(Component.literal(
                "Selects the most nutritious food already in your hotbar."),
                button -> {}).bounds(x, y + 78, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla item-use input and restores your previous hotbar slot after eating."),
                button -> {}).bounds(x, y + 114, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildItemUseInfo() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        addRenderableWidget(Button.builder(Component.literal("ITEM USE: VANILLA INPUT ONLY"),
                button -> {}).bounds(x, y, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("NO AUTOMATIC FOOD, POTION, OR PROJECTILE USE IS ENABLED."),
                button -> {}).bounds(x, y + 36, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("THIS PAGE IS RESERVED FOR A FUTURE USER-CONFIGURABLE ITEM-USE MODULE."),
                button -> {}).bounds(x, y + 72, width, 28).build());
        addBackButton();
    }

    private void buildHudEditorInfo() {
        int x = settingsLeftX();
        int y = settingsTop();
        addSettingButton(x, y, toggleLabel("Module List Overlay",
                dev.morgue.client.MorgueHud.isModuleListEnabled()),
                () -> dev.morgue.client.MorgueHud.setModuleListEnabled(
                        !dev.morgue.client.MorgueHud.isModuleListEnabled()));
        addSettingButton(x, y + 36, toggleLabel("KillAura Target Info",
                dev.morgue.client.MorgueHud.isTargetInfoEnabled()),
                () -> dev.morgue.client.MorgueHud.setTargetInfoEnabled(
                        !dev.morgue.client.MorgueHud.isTargetInfoEnabled()));
        addSettingButton(x, y + 72, toggleLabel("Damage Hit Marker",
                dev.morgue.client.MorgueHud.isHitMarkerEnabled()),
                () -> dev.morgue.client.MorgueHud.setHitMarkerEnabled(
                        !dev.morgue.client.MorgueHud.isHitMarkerEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "The marker appears briefly when the selected target's damage cooldown resets."),
                button -> {}).bounds(x, y + 114, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Overlay positions are fixed to the upper corners; drag-and-drop editing is not implemented."),
                button -> {}).bounds(x, y + 150, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildNotificationsSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        int row = 0;
        addSettingButton(x, y + row++ * 32,
                toggleLabel("Action Chat Notifications", dev.morgue.client.ClientNotifications.isEnabled()),
                () -> dev.morgue.client.ClientNotifications.setEnabled(
                        !dev.morgue.client.ClientNotifications.isEnabled()));
        addSettingButton(x, y + row++ * 32,
                toggleLabel("Module Toggle Feedback (Master)",
                        dev.morgue.client.ClientNotifications.isModuleFeedbackEnabled()),
                () -> dev.morgue.client.ClientNotifications.setModuleFeedbackEnabled(
                        !dev.morgue.client.ClientNotifications.isModuleFeedbackEnabled()));

        for (dev.morgue.client.ClientNotifications.Module module
                : dev.morgue.client.ClientNotifications.Module.values()) {
            addSettingButton(x, y + row++ * 32,
                    toggleLabel(module.displayName() + " Chat Feedback",
                            dev.morgue.client.ClientNotifications.isModuleFeedbackEnabled(module)),
                    () -> dev.morgue.client.ClientNotifications.setModuleFeedbackEnabled(module,
                            !dev.morgue.client.ClientNotifications.isModuleFeedbackEnabled(module)));
        }

        addRenderableWidget(Button.builder(Component.literal(
                "Each module has its own saved preference. The master switch mutes all module-toggle messages."),
                button -> {}).bounds(x, y + row * 32 + 2, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildVisualSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        String[] lines = {
                "ACTIVE THEME: GOTHIC MIDNIGHT BLUE",
                "PANEL: DEEP NAVY / BLACK",
                "ACCENT: ICY STEEL BLUE",
                "TYPOGRAPHY: MATHEMATICAL DOUBLE-STRUCK",
                "ANIMATION: ENABLED",
                "THEME EDITOR: CUSTOM COLOR PICKER NOT IMPLEMENTED"
        };
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            addRenderableWidget(Button.builder(Component.literal(line), button -> {})
                    .bounds(x, y + i * 32, width, 27).build());
        }
        addBackButton();
    }

    private void buildWorldInfo() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            addRenderableWidget(Button.builder(Component.literal("WORLD STATUS: No world is currently loaded."),
                    button -> {}).bounds(x, y, width, 28).build());
        } else {
            var player = minecraft.player;
            var position = player.blockPosition();
            addRenderableWidget(Button.builder(Component.literal("PLAYER POSITION: "
                    + position.getX() + "  /  " + position.getY() + "  /  " + position.getZ()),
                    button -> {}).bounds(x, y, width, 28).build());
            addRenderableWidget(Button.builder(Component.literal("PRECISE POSITION: "
                    + String.format(java.util.Locale.ROOT, "%.2f / %.2f / %.2f",
                    player.getX(), player.getY(), player.getZ())),
                    button -> {}).bounds(x, y + 36, width, 28).build());
            addRenderableWidget(Button.builder(Component.literal("HEALTH: "
                    + format(player.getHealth()) + "  /  " + format(player.getMaxHealth())
                    + "    ABSORPTION: " + format(player.getAbsorptionAmount())),
                    button -> {}).bounds(x, y + 72, width, 28).build());
            addRenderableWidget(Button.builder(Component.literal("WORLD TIME: "
                    + minecraft.level.getGameTime()),
                    button -> {}).bounds(x, y + 108, width, 28).build());
        }
        addBackButton();
    }

    private void buildClientMetadata() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        String[] details = {
                "CLIENT: MORGUE REFINED",
                "TARGET: MINECRAFT JAVA 26.3",
                "RUNTIME: JAVA 25  /  FABRIC LOADER 0.20.1+",
                "INTERFACE: GOTHIC MIDNIGHT BLUE",
                "NETWORK: NORMAL MINECRAFT / FABRIC CONNECTION",
                "PRIVACY: NO CUSTOM TELEMETRY OR REMOTE ACCOUNT SERVICE",
                "NOTE: SERVER MODERATION AND NETWORK INSPECTION STILL APPLY"
        };
        for (int i = 0; i < details.length; i++) {
            String line = details[i];
            addRenderableWidget(Button.builder(Component.literal(line), button -> {})
                    .bounds(x, y + i * 32, width, 27).build());
        }
        addBackButton();
    }

    private void buildCredits() {
        int x = settingsLeftX();
        int y = settingsTop();
        int contentWidth = settingsWideWidth();
        String[] credits = {
                "PROJECT LEAD  /  quincey-ys",
                "AI DEVELOPMENT SUPPORT  /  OpenAI ChatGPT",
                "MINECRAFT JAVA  /  Mojang Studios, Microsoft",
                "FABRIC LOADER + API  /  FabricMC",
                "BUILD TOOLS  /  Gradle, Fabric Loom, ProGuard",
                "SBOM TOOLING  /  CycloneDX Gradle plugin",
                "RESEARCH  /  Meteor, LiquidBounce, Wurst",
                "RESEARCH  /  Baritone, Anomoly, BleachHack",
                "INDEPENDENT IMPLEMENTATION  /  No source intentionally copied"
        };
        for (int i = 0; i < credits.length; i++) {
            String line = credits[i];
            addRenderableWidget(Button.builder(Component.literal(line), button -> {})
                    .bounds(x, y + i * 30, contentWidth, 25).build());
        }
        addBackButton();
    }

    private void buildResourcePackManager() {
        int x = settingsLeftX();
        addRenderableWidget(Button.builder(Component.literal("OPEN VANILLA RESOURCE PACK MANAGER"), button -> {
            minecraft.gui.setScreen(new PackSelectionScreen(
                    minecraft.getResourcePackRepository(),
                    repository -> {
                        minecraft.options.updateResourcePacks(repository);
                        minecraft.gui.setScreen(this);
                    },
                    minecraft.getResourcePackDirectory(),
                    Component.translatable("resourcePack.title")
            ));
        }).bounds(x, settingsTop(), settingsWideWidth(), 30).build());

        addRenderableWidget(Button.builder(Component.literal("Manage local packs and normal client selections."),
                button -> {}).bounds(x, settingsTop() + 40, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal("Server-required packs remain controlled by the server."),
                button -> {}).bounds(x, settingsTop() + 76, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildPrivacyDashboard() {
        int x = settingsLeftX();
        addRenderableWidget(Button.builder(Component.literal("MORGUE does not send a custom mod-list handshake."),
                button -> {}).bounds(x, settingsTop(), settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal("Servers can still inspect normal gameplay and network behavior."),
                button -> {}).bounds(x, settingsTop() + 36, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal("No anti-cheat evasion, fake responses, or required-pack bypass is active."),
                button -> {}).bounds(x, settingsTop() + 72, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildIntegrityDashboard() {
        int x = settingsLeftX();
        int y = settingsTop();
        addRenderableWidget(Button.builder(Component.literal(
                "PACKAGE STATUS: " + dev.morgue.client.security.MorgueIntegrity.getStatus()), button -> {})
                .bounds(x, y, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "DETAIL: " + dev.morgue.client.security.MorgueIntegrity.getDetails()), button -> {})
                .bounds(x, y + 36, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "SCOPE: all packaged JAR entries except this manifest"), button -> {})
                .bounds(x, y + 72, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "LIMIT: local tamper check, not a cryptographic signature"), button -> {})
                .bounds(x, y + 108, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal("RUN CHECK AGAIN"), button -> {
            dev.morgue.client.security.MorgueIntegrity.verify();
            rebuildWidgets();
        }).bounds(x, y + 152, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildKeybindSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        addRenderableWidget(Button.builder(Component.literal("OPEN MINECRAFT KEYBIND SETTINGS"), button -> {
            minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
        }).bounds(x, y, settingsWideWidth(), 30).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Every MORGUE module has its own keybind entry in Controls > Key Binds."),
                button -> {}).bounds(x, y + 40, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Bind any module to a keyboard key, Mouse 4, or Mouse 5. Unbound modules stay off."),
                button -> {}).bounds(x, y + 78, settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Defaults: Right Shift opens the GUI; M / K / V toggle Mace Hitter / KillAura / Auto Totem."),
                button -> {}).bounds(x, y + 116, settingsWideWidth(), 36).build());
        addBackButton();
    }

    private void buildProfileSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        int width = settingsWideWidth();
        addRenderableWidget(Button.builder(Component.literal("SAVE PROFILE SLOT 1"), button -> {
            profileStatus = MorgueConfig.exportProfile("profile-1")
                    ? "Saved profile slot 1." : "Could not save profile slot 1.";
            rebuildWidgets();
        }).bounds(x, y, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD PROFILE SLOT 1"), button -> {
            profileStatus = MorgueConfig.importProfile("profile-1")
                    ? "Loaded profile slot 1." : "Profile slot 1 does not exist or could not be loaded.";
            rebuildWidgets();
        }).bounds(x, y + 31, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal("SAVE PROFILE SLOT 2"), button -> {
            profileStatus = MorgueConfig.exportProfile("profile-2")
                    ? "Saved profile slot 2." : "Could not save profile slot 2.";
            rebuildWidgets();
        }).bounds(x, y + 62, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD PROFILE SLOT 2"), button -> {
            profileStatus = MorgueConfig.importProfile("profile-2")
                    ? "Loaded profile slot 2." : "Profile slot 2 does not exist or could not be loaded.";
            rebuildWidgets();
        }).bounds(x, y + 93, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal("SAVE PROFILE SLOT 3"), button -> {
            profileStatus = MorgueConfig.exportProfile("profile-3")
                    ? "Saved profile slot 3." : "Could not save profile slot 3.";
            rebuildWidgets();
        }).bounds(x, y + 124, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal("LOAD PROFILE SLOT 3"), button -> {
            profileStatus = MorgueConfig.importProfile("profile-3")
                    ? "Loaded profile slot 3." : "Profile slot 3 does not exist or could not be loaded.";
            rebuildWidgets();
        }).bounds(x, y + 155, width, 27).build());
        addRenderableWidget(Button.builder(Component.literal(profileStatus), button -> {})
                .bounds(x, y + 190, width, 28).build());
        addBackButton();
    }

    private void buildPlaceholderSettings() {
        int x = settingsLeftX();
        addRenderableWidget(Button.builder(Component.literal("This module is listed in the ClickGUI prototype."),
                button -> {}).bounds(x, settingsTop(), settingsWideWidth(), 28).build());
        addRenderableWidget(Button.builder(Component.literal("Gameplay behavior is not connected yet."),
                button -> {}).bounds(x, settingsTop() + 36, settingsWideWidth(), 28).build());
        addBackButton();
    }

    private void buildThemeSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        addSettingButton(x, y, "Accent Color: " + UiSettings.accentName() + "  ›",
                () -> UiSettings.cycleAccent());
        addRenderableWidget(Button.builder(Component.literal(
                "Choose a palette inspired by Ghost Client's clean, high-contrast panel styling."),
                button -> {}).bounds(x, y + 42, settingsWideWidth(), 32).build());
        addBackButton();
    }

    private void buildUiEffectsSettings() {
        int x = settingsLeftX();
        int y = settingsTop();
        addSettingButton(x, y, toggleLabel("Interface Animations", UiSettings.isAnimationsEnabled()),
                () -> UiSettings.setAnimationsEnabled(!UiSettings.isAnimationsEnabled()));
        addSettingButton(x, y + 38, toggleLabel("Custom UI Sounds", UiSettings.isSoundsEnabled()),
                () -> UiSettings.setSoundsEnabled(!UiSettings.isSoundsEnabled()));
        addSettingButton(x, y + 76, toggleLabel("Player Kill Sound", UiSettings.isKillSoundEnabled()),
                () -> UiSettings.setKillSoundEnabled(!UiSettings.isKillSoundEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Bundled custom sounds: open, close, hover, click, toggles, slider, and kill."),
                button -> {}).bounds(x, y + 118, settingsWideWidth(), 32).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Ambient music is intentionally not included yet."),
                button -> {}).bounds(x, y + 156, settingsWideWidth(), 32).build());
        addBackButton();
    }

    private void addSettingButton(int x, int y, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> {
            action.run();
            MorgueConfig.save();
            rebuildWidgets();
        }).bounds(x, y, settingsColumnWidth(), 27).build());
    }

    private void addBackButton() {
        AbstractWidget back = Button.builder(Component.literal("< BACK TO MODULES"), button -> {
            selectedModule = null;
            entityPage = false;
            rebuildWidgets();
        }).bounds(settingsLeftX(), backButtonY(), 160, 24).build();
        addRenderableWidget(back);
        fixedWidgets.add(back);
    }

    private static boolean isModuleEnabled(String module) {
        return switch (module) {
            case "Mace Hitter" -> MorgueClient.maceHitter().isEnabled();
            case "KillAura" -> MorgueClient.killAura().isEnabled();
            case "Auto Totem" -> MorgueClient.autoTotem().isEnabled();
            case "Sprint" -> MorgueClient.sprint().isEnabled();
            case "Safe Walk" -> MorgueClient.safeWalk().isEnabled();
            case "Inventory Helper" -> MorgueClient.inventoryHelper().isEnabled();
            case "Item Use" -> MorgueClient.autoEat().isEnabled();
            case "Aim Assist" -> MorgueClient.aimAssist().isEnabled();
            case "Click Assist" -> MorgueClient.clickAssist().isEnabled();
            default -> ENABLED.getOrDefault(module, false);
        };
    }

    private void toggleModule(String module) {
        boolean enabled = !isModuleEnabled(module);
        ENABLED.put(module, enabled);
        if ("Mace Hitter".equals(module)) {
            MorgueClient.setMaceHitterEnabled(enabled);
        } else if ("KillAura".equals(module)) {
            MorgueClient.setKillAuraEnabled(enabled);
        } else if ("Auto Totem".equals(module)) {
            MorgueClient.setAutoTotemEnabled(enabled);
        } else if ("Sprint".equals(module)) {
            MorgueClient.setSprintEnabled(enabled);
        } else if ("Safe Walk".equals(module)) {
            MorgueClient.setSafeWalkEnabled(enabled);
        } else if ("Inventory Helper".equals(module)) {
            MorgueClient.setInventoryHelperEnabled(enabled);
        } else if ("Item Use".equals(module)) {
            MorgueClient.setAutoEatEnabled(enabled);
        } else if ("Aim Assist".equals(module)) {
            MorgueClient.setAimAssistEnabled(enabled);
        } else if ("Click Assist".equals(module)) {
            MorgueClient.setClickAssistEnabled(enabled);
        }
        MorgueConfig.save();
    }

    private static MaceHitterModule.TargetMode nextTargetMode(MaceHitterModule.TargetMode mode) {
        return switch (mode) {
            case PLAYERS -> MaceHitterModule.TargetMode.MOBS;
            case MOBS -> MaceHitterModule.TargetMode.ALL;
            case ALL -> MaceHitterModule.TargetMode.CUSTOM;
            case CUSTOM -> MaceHitterModule.TargetMode.PLAYERS;
        };
    }

    private static String toggleLabel(String label, boolean enabled) {
        return label + ": " + (enabled ? "ON" : "OFF");
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x80060A10);
    }

    private static final FontDescription DOUBLE_STRUCK_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("minecraft", "default"));

    private static Component displayText(String value) {
        return Component.literal(value).withStyle(style -> style.withBold(true));
    }

    private void extractCustomButton(GuiGraphicsExtractor graphics, AbstractWidget widget, String rawLabel,
                                      int mouseX, int mouseY, long now, double frameFactor,
                                      double revealFactor) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();
        if (w <= 0 || h <= 0) return;

        boolean selected = rawLabel.equals(selectedCategory);
        boolean toggle = rawLabel.startsWith("ON  ") || rawLabel.startsWith("OFF ")
                || rawLabel.contains(": ON") || rawLabel.contains(": OFF");
        boolean activeState = rawLabel.startsWith("ON") || rawLabel.startsWith("INFO")
                || rawLabel.contains(": ON");
        boolean hovered = widget.isHoveredOrFocused() || widget.isMouseOver(mouseX, mouseY);
        boolean enabled = widget.isActive();

        float previous = hoverProgress.getOrDefault(widget, 0.0F);
        float target = hovered && enabled ? 1.0F : 0.0F;
        float hover = previous + (target - previous)
                * (UiSettings.isAnimationsEnabled() ? (float) frameFactor : 1.0F);
        hoverProgress.put(widget, hover);
        if (hovered && enabled && previous <= 0.05F) {
            UiSoundManager.play(UiSoundManager.Effect.HOVER);
        }

        int idle = !enabled ? 0xFF101722
                : selected ? 0xFF1C2C3A
                : activeState ? 0xFF172A31
                : BUTTON_IDLE;
        int background = mixColor(0xFF070B12, mixColor(idle, BUTTON_HOVER, hover),
                (float) Math.max(0.0, Math.min(1.0, revealFactor)));
        int accent = mixColor(BORDER,
                selected || activeState ? accentColor() : mixColor(BORDER, 0xFF9FB3C8, hover),
                (float) Math.max(0.0, Math.min(1.0, revealFactor)));
        int textColor = mixColor(0xFF667487, !enabled ? 0xFF667487 : WHITE,
                (float) Math.max(0.0, Math.min(1.0, revealFactor)));

        graphics.fill(x, y, x + w, y + h, background);
        graphics.outline(x - (hover > 0.05F ? 1 : 0), y - (hover > 0.05F ? 1 : 0),
                w + (hover > 0.05F ? 2 : 0), h + (hover > 0.05F ? 2 : 0),
                mixColor(BORDER, 0xFF7B94AE, hover));
        graphics.fill(x, y + 4, x + 3 + Math.round(2 * hover), y + h - 4, accent);

        String visibleLabel = rawLabel;
        Component label = displayText(visibleLabel);
        int textPad = w < 90 ? 6 : 12;
        int switchSpace = toggle && w >= 110 ? 56 : 0;
        int maxTextWidth = Math.max(8, w - textPad * 2 - switchSpace);
        while (font.width(label) > maxTextWidth && visibleLabel.length() > 4) {
            visibleLabel = visibleLabel.substring(0, visibleLabel.length() - 2) + "…";
            label = displayText(visibleLabel);
        }
        int textY = y + Math.max(2, (h - 10) / 2);
        graphics.text(font, label, x + textPad, textY, textColor);

        if (toggle && w >= 110) {
            int trackX = x + w - 48;
            int trackY = y + (h - 16) / 2;
            int trackColor = activeState ? accentSoftColor() : 0xFF293747;
            graphics.fill(trackX, trackY, trackX + 34, trackY + 16, trackColor);
            graphics.outline(trackX, trackY, 34, 16, activeState ? accentColor() : BORDER);
            String toggleKey = rawLabel.startsWith("ON  ") || rawLabel.startsWith("OFF ")
                    ? rawLabel.substring(4) : rawLabel.replaceFirst(": (ON|OFF)$", "");
            float targetToggle = activeState ? 1.0F : 0.0F;
            float previousToggle = toggleProgress.getOrDefault(toggleKey, targetToggle);
            float animatedToggle = previousToggle + (targetToggle - previousToggle)
                    * (UiSettings.isAnimationsEnabled() ? (float) frameFactor : 1.0F);
            toggleProgress.put(toggleKey, animatedToggle);
            int knobX = trackX + 2 + Math.round(animatedToggle * 16);
            graphics.fill(knobX, trackY + 2, knobX + 12, trackY + 14, WHITE);
        }
    }

    private static int mixColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = (int) (((from >>> 24) & 0xFF) * (1.0F - t)
                + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >>> 16) & 0xFF) * (1.0F - t)
                + ((to >>> 16) & 0xFF) * t);
        int g = (int) (((from >>> 8) & 0xFF) * (1.0F - t)
                + ((to >>> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1.0F - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawClickRipples(GuiGraphicsExtractor graphics, long now) {
        if (!UiSettings.isAnimationsEnabled()) {
            clickRipples.clear();
            return;
        }
        while (!clickRipples.isEmpty()
                && now - clickRipples.peekFirst().startedAtNanos() > 520_000_000L) {
            clickRipples.removeFirst();
        }
        for (ClickRipple ripple : clickRipples) {
            float progress = Math.min(1.0F,
                    (now - ripple.startedAtNanos()) / 520_000_000.0F);
            int radius = 5 + Math.round(progress * 28.0F);
            int alpha = Math.max(0, Math.round(170.0F * (1.0F - progress)));
            int color = (alpha << 24) | (accentColor() & 0x00FFFFFF);
            int left = (int) Math.round(ripple.x()) - radius;
            int right = (int) Math.round(ripple.x()) + radius;
            int top = (int) Math.round(ripple.y()) - radius;
            int bottom = (int) Math.round(ripple.y()) + radius;
            graphics.fill(left, top, right, top + 1, color);
            graphics.fill(left, bottom - 1, right, bottom, color);
            graphics.fill(left, top, left + 1, bottom, color);
            graphics.fill(right - 1, top, right, bottom, color);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        double frameSeconds = lastFrameNanos == 0L ? 1.0 / 60.0
                : Math.min(0.05, Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = now;
        double frameFactor = 1.0 - Math.exp(-18.0 * frameSeconds);
        double openProgress = Math.min(1.0, Math.max(0.0,
                (now - openedAtNanos) / 240_000_000.0));
        double navProgress = Math.min(1.0, Math.max(0.0,
                (now - transitionAtNanos) / 170_000_000.0));

        graphics.fill(0, 0, width, height, 0xFF070B12);
        graphics.fill(0, 0, width, headerHeight, PANEL_LIGHT);
        graphics.fill(0, 0, width, 3, accentColor());

        graphics.text(font, displayText("MORGUE"), margin,
                Math.max(6, headerHeight / 2 - 16), WHITE);
        if (showBrandSubtitle) {
            graphics.text(font, displayText(BRAND_SUBTITLE), margin,
                    Math.max(6, headerHeight / 2 + 2), MUTED);
        }

        String heading = selectedModule == null ? "MODULE PANELS"
                : selectedModule.toUpperCase(java.util.Locale.ROOT);
        graphics.text(font, displayText(heading), margin, headingY, WHITE);
        String breadcrumb = selectedModule == null ? OVERVIEW_HINT
                : "MODULE PANELS  /  " + selectedCategory.toUpperCase(java.util.Locale.ROOT)
                + (entityPage ? "  /  ENTITY FILTER" : "  /  MODULE SETTINGS");
        if (showPageSubtitle) {
            drawTruncated(graphics, breadcrumb, margin, headingY + 15, contentWidth(), MUTED);
        }
        int navY = showPageSubtitle ? headingY + 26 : headingY + 12;
        int navWidth = (int) Math.round(contentWidth() * navProgress);
        graphics.fill(margin, navY, margin + navWidth, navY + 2, accentColor());
        int openWidth = (int) Math.round((width - margin * 2) * openProgress);
        graphics.fill(margin, headerHeight - 3, margin + openWidth, headerHeight - 1,
                accentSoftColor());

        double reveal = navProgress * openProgress;
        if (selectedModule == null) {
            int viewBottom = overviewViewportBottom();
            graphics.enableScissor(margin, contentTop, width, viewBottom);
            for (OverviewCard card : overviewCards) {
                drawOverviewCard(graphics, card);
            }
            drawContentButtons(graphics, mouseX, mouseY, now, frameFactor, reveal);
            graphics.disableScissor();
            drawScrollbar(graphics, margin + contentWidth() - 4, contentTop, viewBottom,
                    overviewScroll, overviewContentHeight, overviewViewportHeight);
            if (moduleTotalCount == 0) {
                graphics.text(font, displayText("NO MODULES MATCH  /  CLEAR THE FILTER TO SEE ALL CATEGORIES"),
                        margin + 12, contentTop + 18, MUTED);
            }
        } else {
            int viewBottom = settingsViewportBottom();
            graphics.enableScissor(margin, contentTop, width, viewBottom);
            drawContentButtons(graphics, mouseX, mouseY, now, frameFactor, reveal);
            graphics.disableScissor();
            drawScrollbar(graphics, margin + contentWidth() - 4, contentTop, viewBottom,
                    settingsScroll, settingsContentHeight,
                    Math.max(1, viewBottom - contentTop));
            graphics.fill(margin, backButtonY() - 8, width - margin, backButtonY() - 7, BORDER);
        }

        for (var child : children()) {
            if (!(child instanceof AbstractWidget widget) || widget instanceof EditBox) continue;
            if (!fixedWidgets.contains(widget)) continue;
            extractCustomButton(graphics, widget, widget.getMessage().getString(),
                    mouseX, mouseY, now, frameFactor, reveal);
        }

        if (searchBox != null) {
            searchBox.extractRenderState(graphics, mouseX, mouseY, delta);
            int ex = searchBox.getX();
            int ey = searchBox.getY();
            int ew = searchBox.getWidth();
            int eh = searchBox.getHeight();
            boolean searchHovered = searchBox.isMouseOver(mouseX, mouseY);
            graphics.outline(ex - 1, ey - 1, ew + 2, eh + 2,
                    searchHovered ? accentColor() : BORDER);
            graphics.fill(ex + 2, ey + eh - 2, ex + ew - 2, ey + eh - 1,
                    searchHovered ? accentColor() : 0xFF405065);
        }

        drawFooter(graphics);
        drawClickRipples(graphics, now);
    }

    private void drawContentButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    long now, double frameFactor, double reveal) {
        for (var child : children()) {
            if (!(child instanceof AbstractWidget widget) || widget instanceof EditBox) continue;
            if (fixedWidgets.contains(widget)) continue;
            extractCustomButton(graphics, widget, widget.getMessage().getString(),
                    mouseX, mouseY, now, frameFactor, reveal);
        }
    }

    private void drawOverviewCard(GuiGraphicsExtractor graphics, OverviewCard card) {
        int x = card.x();
        int y = card.y();
        int w = card.width();
        int h = card.height();
        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.outline(x, y, w, h, BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 3, accentColor());
        String title = card.name().toUpperCase(java.util.Locale.ROOT);
        graphics.text(font, displayText(title), x + 10, y + 9, WHITE);
        String count = card.moduleCount() + (card.moduleCount() == 1 ? " MODULE" : " MODULES");
        int countWidth = font.width(displayText(count));
        if (x + 20 + font.width(displayText(title)) + 8 < x + w - 10 - countWidth) {
            graphics.text(font, displayText(count), x + w - 10 - countWidth, y + 9, MUTED);
        }
        int separatorY = y + (compactLayout ? 22 : 28);
        graphics.fill(x + 10, separatorY, x + w - 10, separatorY + 1, BORDER);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom,
                               int scroll, int contentHeight, int viewportHeight) {
        if (contentHeight <= viewportHeight || viewportHeight <= 0) return;
        int trackHeight = Math.max(1, bottom - top);
        int thumbHeight = Math.max(20,
                (int) ((long) trackHeight * viewportHeight / contentHeight));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbY = top + (int) ((long) (trackHeight - thumbHeight) * scroll / maxScroll);
        graphics.fill(x, top, x + 2, bottom, 0xFF293747);
        graphics.fill(x - 1, thumbY, x + 3, thumbY + thumbHeight, accentColor());
    }

    private void drawFooter(GuiGraphicsExtractor graphics) {
        graphics.fill(margin, footerTop, width - margin, footerTop + 1, BORDER);
        int textY = height - 19;
        String left = "MORGUE  /  LOCAL CONFIGURATION";
        graphics.text(font, displayText(left), margin, textY, MUTED);
        int closeWidth = font.width(displayText(CLOSE_HINT));
        int closeX = width - margin - closeWidth;
        graphics.text(font, displayText(CLOSE_HINT), closeX, textY, MUTED);
        String fx = "UI FX  " + (UiSettings.isAnimationsEnabled() ? "ON" : "OFF");
        int fxX = closeX - 14 - font.width(displayText(fx));
        if (fxX > margin + font.width(displayText(left)) + 12) {
            graphics.text(font, displayText(fx), fxX, textY, MUTED);
        }
    }

    private void drawTruncated(GuiGraphicsExtractor graphics, String text, int x, int y,
                               int maxWidth, int color) {
        String shown = text;
        if (font.width(displayText(shown)) > maxWidth) {
            int end = shown.length();
            while (end > 0 && font.width(displayText(shown.substring(0, end) + "…")) > maxWidth) {
                end--;
            }
            shown = shown.substring(0, end) + "…";
        }
        graphics.text(font, displayText(shown), x, y, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int step = compactLayout ? 30 : 42;
        int delta = (int) Math.round(verticalAmount * step);
        if (selectedModule == null) {
            int viewBottom = overviewViewportBottom();
            if (mouseX >= margin && mouseX <= width - margin
                    && mouseY >= contentTop && mouseY <= viewBottom) {
                int viewportHeight = Math.max(1, viewBottom - contentTop);
                int maxScroll = Math.max(0, overviewContentHeight - viewportHeight);
                if (maxScroll > 0) {
                    overviewScroll = Math.max(0, Math.min(maxScroll, overviewScroll - delta));
                    rebuildWidgets();
                    return true;
                }
            }
        } else {
            int viewBottom = settingsViewportBottom();
            if (mouseX >= margin && mouseX <= width - margin
                    && mouseY >= contentTop && mouseY <= viewBottom) {
                int viewportHeight = Math.max(1, viewBottom - contentTop);
                int maxScroll = Math.max(0, settingsContentHeight - viewportHeight);
                if (maxScroll > 0) {
                    settingsScroll = Math.max(0, Math.min(maxScroll, settingsScroll - delta));
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean isInteractiveClick(double mouseX, double mouseY) {
        if (selectedModule == null) {
            if (mouseY < contentTop) {
                if (searchBox != null && searchBox.isMouseOver(mouseX, mouseY)) return true;
                for (AbstractWidget widget : fixedWidgets) {
                    if (widget.isMouseOver(mouseX, mouseY)) return true;
                }
                return false;
            }
            return mouseY <= overviewViewportBottom();
        }
        for (AbstractWidget widget : fixedWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) return true;
        }
        return mouseY >= contentTop && mouseY <= settingsViewportBottom();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isInteractiveClick(event.x(), event.y())) {
            return false;
        }
        if (event.button() == MOUSE_BUTTON_RIGHT && selectedModule == null) {
            for (var child : children()) {
                if (!(child instanceof AbstractWidget widget)
                        || !widget.isMouseOver(event.x(), event.y())) continue;
                String label = widget.getMessage().getString();
                if (label.startsWith("ON  ") || label.startsWith("OFF ") || label.startsWith("INFO  ")) {
                    selectedModule = label.startsWith("INFO  ") ? label.substring(6).trim() : label.substring(4).trim();
                    entityPage = false;
                    rebuildWidgets();
                    return true;
                }
            }
        }
        if (event.button() == MOUSE_BUTTON_LEFT) {
            playClickSoundAt(event);
            if (UiSettings.isAnimationsEnabled()) {
                clickRipples.addLast(new ClickRipple(event.x(), event.y(), System.nanoTime()));
                while (clickRipples.size() > 16) clickRipples.removeFirst();
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void playClickSoundAt(MouseButtonEvent event) {
        for (var child : children()) {
            if (!(child instanceof AbstractWidget widget)
                    || !widget.isMouseOver(event.x(), event.y())) continue;

            String label = widget.getMessage().getString();
            boolean isOnToggle = label.startsWith("ON  ") || label.endsWith(": ON");
            boolean isOffToggle = label.startsWith("OFF ") || label.endsWith(": OFF");
            if (isOnToggle || isOffToggle) {
                boolean enabling = isOffToggle;
                boolean enablingUiSounds = enabling && label.contains("Custom UI Sounds");
                UiSoundManager.play(
                        enabling ? UiSoundManager.Effect.TOGGLE_ON : UiSoundManager.Effect.TOGGLE_OFF,
                        enablingUiSounds
                );
            } else {
                UiSoundManager.play(UiSoundManager.Effect.CLICK);
            }
            return;
        }
        UiSoundManager.play(UiSoundManager.Effect.CLICK);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == MOUSE_BUTTON_LEFT) {
            long now = System.nanoTime();
            if (now - lastSliderSoundNanos >= 90_000_000L) {
                for (var child : children()) {
                    if (child instanceof AbstractWidget widget
                            && widget.isMouseOver(event.x(), event.y())
                            && widget.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("slider")) {
                        UiSoundManager.play(UiSoundManager.Effect.SLIDER);
                        lastSliderSoundNanos = now;
                        break;
                    }
                }
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        if (openSoundPlayed) UiSoundManager.play(UiSoundManager.Effect.CLOSE);
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Open screens receive key events directly; the global key mapping does not
        // see them, so the advertised Right Shift close is handled here.
        if (event.key() == InputConstants.KEY_RSHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void resize(int screenWidth, int screenHeight) {
        super.resize(screenWidth, screenHeight);
        width = screenWidth;
        height = screenHeight;
        computeLayout();
        rebuildWidgets();
    }

    private record OverviewCard(String name, int moduleCount, int x, int y, int width, int height) {
    }

    private record EntityOption(String name, String id) {
    }

    private record ClickRipple(double x, double y, long startedAtNanos) {
    }
}
