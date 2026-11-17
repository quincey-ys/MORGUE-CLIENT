package dev.morgue.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.config.MorgueConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public final class MorgueScreen extends Screen {
    private static final int BLACK = 0xC8050914;
    private static final int PANEL = 0xD00B1220;
    private static final int PANEL_LIGHT = 0xE1121D30;
    private static final int BORDER = 0xB34A6388;
    private static final int WHITE = 0xFFE8F0FF;
    private static final int MUTED = 0xFF91A5C1;
    private static final int RED = 0xFF80AFFF;
    private static final int RED_SOFT = 0xFF315A91;
    private static final int BUTTON_IDLE = 0xF00A1220;
    private static final int BUTTON_HOVER = 0xFF17263D;
    private static final List<String> CATEGORIES = List.of(
            "Combat", "Movement", "Player", "Render", "World", "Misc", "Privacy", "Config"
    );

    private static final Map<String, List<String>> MODULES = new LinkedHashMap<>();
    private static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();


    static {
        MODULES.put("Combat", List.of("AI Brain", "Mace Hitter", "KillAura", "Aim Assist", "Click Assist"));
        MODULES.put("Movement", List.of("Sprint", "Safe Walk", "Movement Settings"));
        MODULES.put("Player", List.of("Auto Totem", "Inventory Helper", "Item Use"));
        MODULES.put("Render", List.of("HUD Editor", "Module List", "Visual Settings"));
        MODULES.put("World", List.of("World Info", "Environment", "World Settings"));
        MODULES.put("Misc", List.of("Resource Pack Manager", "Notifications", "Client Utilities", "Credits"));
        MODULES.put("Privacy", List.of("Privacy Dashboard", "Build Integrity", "Screenshot Privacy", "Client Metadata"));
        MODULES.put("Config", List.of("Profiles", "Keybinds", "Theme"));
        MODULES.values().forEach(list -> list.forEach(name -> ENABLED.put(name, false)));
    }

    private String selectedCategory = "Combat";
    private String selectedModule;
    private boolean entityPage;
    private int entityPageIndex;
    private List<EntityOption> cachedEntityOptions;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private EditBox searchBox;
    private String searchQuery = "";
    private boolean draggingPanel;
    private boolean resizingPanel;
    private int moduleScrollOffset;
    private int moduleTotalCount;
    private int moduleVisibleCount;
    private String profileStatus = "Profiles are stored locally in the MORGUE config folder.";
    private String pendingKeybindModule;
    private boolean moduleScrollDragging;
    private int moduleScrollDragOffset;
    private int moduleListTop;
    private int moduleListBottom;
    private static final int MODULE_ROW_HEIGHT = 36;

    private static float panelOpacity = 1.0F;
    private static float controlOpacity = 1.0F;

    private int settingsScrollOffset;
    private int settingsScrollMaxOffset;
    private boolean settingsScrollDragging;
    private int settingsScrollDragOffset;
    private int activeOpacitySlider = -1;
    private final Map<AbstractWidget, SettingSlider> settingSliders = new java.util.IdentityHashMap<>();
    private SettingSlider activeSettingSlider;

    public MorgueScreen() {
        super(Component.literal("MORGUE-CLIENT"));
    }

    @Override
    protected void init() {
        clearWidgets();
        settingSliders.clear();
        activeSettingSlider = null;

        int maxWidth = Math.max(320, width - 24);
        int maxHeight = Math.max(300, height - 24);
        if (panelWidth == 0 || panelHeight == 0) {
            panelWidth = Math.min(720, maxWidth);
            panelHeight = Math.min(440, maxHeight);
            panelX = (width - panelWidth) / 2;
            panelY = (height - panelHeight) / 2;
        } else {
            panelWidth = Math.max(Math.min(560, maxWidth), Math.min(panelWidth, maxWidth));
            panelHeight = Math.max(Math.min(360, maxHeight), Math.min(panelHeight, maxHeight));
            panelX = Math.max(12, Math.min(panelX, width - panelWidth - 12));
            panelY = Math.max(12, Math.min(panelY, height - panelHeight - 12));
        }

        int sidebarX = panelX + 16;
        int sidebarY = panelY + 88;
        for (String category : CATEGORIES) {
            addRenderableWidget(Button.builder(Component.literal(category), button -> {
                selectedCategory = category;
                selectedModule = null;
                searchQuery = "";
                entityPage = false;
                moduleScrollOffset = 0;
                settingsScrollOffset = 0;
                rebuildWidgets();
            }).bounds(sidebarX, sidebarY, 116, 24).build());
            sidebarY += 29;
        }

        if (selectedModule == null) {
            searchBox = new EditBox(font, panelX + 150, panelY + 102,
                    Math.max(120, panelWidth - 285), 24, Component.literal("Search modules"));
            searchBox.setValue(searchQuery);
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal("FILTER"), button -> {
                searchQuery = searchBox.getValue().trim();
                rebuildWidgets();
            }).bounds(panelX + panelWidth - 120, panelY + 101, 88, 26).build());
            buildModuleList();
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
        } else if ("Visual Settings".equals(selectedModule) || "Theme".equals(selectedModule)) {
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
        } else {
            buildPlaceholderSettings();
        }

        layoutSettingsViewport();
    }

    private List<String> filteredModules() {
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        return MODULES.getOrDefault(selectedCategory, List.of()).stream()
                .filter(module -> query.isEmpty()
                        || module.toLowerCase(java.util.Locale.ROOT).contains(query))
                .toList();
    }

    private static boolean isViewOnlyModule(String module) {
        return "AI Brain".equals(module) || "Build Integrity".equals(module)
                || "Credits".equals(module) || "World Info".equals(module)
                || "Client Metadata".equals(module) || "Module List".equals(module)
                || "Environment".equals(module) || "Visual Settings".equals(module)
                || "Notifications".equals(module) || "Client Utilities".equals(module)
                || "Screenshot Privacy".equals(module) || "World Settings".equals(module)
                || "HUD Editor".equals(module);
    }

    private void buildModuleList() {
        int moduleX = panelX + 150;
        moduleListTop = panelY + 140;
        moduleListBottom = panelY + panelHeight - 48;
        int buttonHeight = 28;
        int moduleWidth = Math.max(150, panelWidth - 182);
        List<String> filtered = filteredModules();

        moduleTotalCount = filtered.size();
        moduleVisibleCount = Math.max(1, (moduleListBottom - moduleListTop) / MODULE_ROW_HEIGHT);
        int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
        moduleScrollOffset = Math.max(0, Math.min(moduleScrollOffset, maxOffset));

        int end = Math.min(moduleTotalCount, moduleScrollOffset + moduleVisibleCount);
        for (int i = moduleScrollOffset; i < end; i++) {
            String module = filtered.get(i);
            int moduleY = moduleListTop + (i - moduleScrollOffset) * MODULE_ROW_HEIGHT;
            boolean viewOnly = isViewOnlyModule(module);
            boolean enabled = isModuleEnabled(module);
            String label;
            if (module.equals(pendingKeybindModule)) {
                label = "PRESS A KEY OR MOUSE BUTTON  /  ESC TO CANCEL  /  "
                        + module.toUpperCase(java.util.Locale.ROOT);
            } else {
                String state = viewOnly ? "INFO" : (enabled ? "ON" : "OFF");
                String keyLabel = MorgueClient.isModuleKeybindSupported(module)
                        ? "   [" + MorgueClient.moduleKeybind(module).getTranslatedKeyMessage().getString() + "]"
                        : "";
                label = state + "   " + module + keyLabel;
            }

            addRenderableWidget(Button.builder(Component.literal(label), button -> {
                if (viewOnly) {
                    selectedModule = module;
                    entityPage = false;
                    settingsScrollOffset = 0;
                } else {
                    toggleModule(module);
                }
                rebuildWidgets();
            }).bounds(moduleX, moduleY, moduleWidth, buttonHeight).build());
        }

        if (moduleTotalCount == 0) {
            addRenderableWidget(Button.builder(Component.literal("NO MATCHING MODULES"),
                    button -> {}).bounds(moduleX, moduleListTop, moduleWidth, buttonHeight).build());
        }
    }

    private String moduleAt(double mouseX, double mouseY) {
        if (selectedModule != null || entityPage || moduleVisibleCount <= 0
                || mouseX < panelX + 150 || mouseX > panelX + panelWidth - 26
                || mouseY < moduleListTop || mouseY >= moduleListBottom) {
            return null;
        }
        int row = (int) ((mouseY - moduleListTop) / MODULE_ROW_HEIGHT);
        if (row < 0 || row >= moduleVisibleCount) {
            return null;
        }
        int index = moduleScrollOffset + row;
        List<String> filtered = filteredModules();
        return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
    }

    private int moduleScrollTrackX() {
        return panelX + panelWidth - 17;
    }

    private int moduleScrollThumbHeight() {
        int trackHeight = Math.max(1, moduleListBottom - moduleListTop);
        return Math.max(22, trackHeight * moduleVisibleCount / Math.max(1, moduleTotalCount));
    }

    private int moduleScrollThumbY() {
        int trackHeight = Math.max(1, moduleListBottom - moduleListTop);
        int thumbHeight = moduleScrollThumbHeight();
        int maxOffset = Math.max(1, moduleTotalCount - moduleVisibleCount);
        return moduleListTop + (trackHeight - thumbHeight) * moduleScrollOffset / maxOffset;
    }

    private void setModuleScrollFromMouseY(double mouseY) {
        int trackHeight = Math.max(1, moduleListBottom - moduleListTop);
        int thumbHeight = moduleScrollThumbHeight();
        int travel = Math.max(1, trackHeight - thumbHeight);
        int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
        double fraction = (mouseY - moduleScrollDragOffset - moduleListTop) / travel;
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        moduleScrollOffset = (int) Math.round(fraction * maxOffset);
        rebuildWidgets();
    }

    private void buildInventoryHelperSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.inventoryHelper();
        addSettingButton(x, y, toggleLabel("Inventory Refill", module.isEnabled()),
                () -> MorgueClient.setInventoryHelperEnabled(!module.isEnabled()));
        addValueSlider(x, y + 36, panelWidth / 2 - 170, "Refill At", 1, 16, " items", true,
                () -> module.getRefillAt(), value -> module.setRefillAt((int) Math.round(value)));
        addRenderableWidget(Button.builder(Component.literal(
                "Swaps matching low-count hotbar stacks with main-inventory stacks."),
                button -> {}).bounds(x, y + 76, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla container input; item components must match."),
                button -> {}).bounds(x, y + 112, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildSafeWalkSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.safeWalk();
        addSettingButton(x, y, toggleLabel("Safe Walk", module.isEnabled()),
                () -> MorgueClient.setSafeWalkEnabled(!module.isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically crouches when movement projects beyond a supported block edge."),
                button -> {}).bounds(x, y + 38, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Your physical sneak key takes priority over automatic edge protection."),
                button -> {}).bounds(x, y + 76, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildSprintSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.sprint();
        addSettingButton(x, y, toggleLabel("Auto Sprint", module.isEnabled()),
                () -> MorgueClient.setSprintEnabled(!module.isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically sprints while horizontal movement is detected."),
                button -> {}).bounds(x, y + 38, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses normal player sprint state; it does not alter packets or bypass server rules."),
                button -> {}).bounds(x, y + 76, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildAimAssistSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.aimAssist();
        addSettingButton(x, y, toggleLabel("Visible Aim Assist", module.isEnabled()),
                () -> MorgueClient.setAimAssistEnabled(!module.isEnabled()));
        addValueSlider(x, y + 36, panelWidth / 2 - 170, "Range", 1.0, 6.0, " blocks", false,
                module::getRange, module::setRange);
        addValueSlider(x, y + 72, panelWidth / 2 - 170, "FOV", 15.0, 180.0, " deg", false,
                module::getFov, module::setFov);
        addValueSlider(x, y + 108, panelWidth / 2 - 170, "Smoothing", 0.05, 1.0, "", false,
                module::getSmoothing, value -> module.setSmoothing((float) value));
        addRenderableWidget(Button.builder(Component.literal(
                "Only assists while you hold attack, targets visible players, and rotates visibly."),
                button -> {}).bounds(x, y + 150, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Automatically disables its steering while Mace Hitter or KillAura is enabled."),
                button -> {}).bounds(x, y + 186, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildClickAssistSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.clickAssist();
        addSettingButton(x, y, toggleLabel("Click Assist", module.isEnabled()),
                () -> MorgueClient.setClickAssistEnabled(!module.isEnabled()));
        addValueSlider(x, y + 36, panelWidth / 2 - 170, "Click Rate", 1, 20, " CPS", true,
                () -> module.getClicksPerSecond(), value -> module.setClicksPerSecond((int) Math.round(value)));
        addRenderableWidget(Button.builder(Component.literal(
                "Repeats your held attack input against the entity under your crosshair."),
                button -> {}).bounds(x, y + 78, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla attack cooldown and shared combat arbitration; no packet spoofing."),
                button -> {}).bounds(x, y + 114, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildMaceHitterSettings() {
        MaceHitterModule module = MorgueClient.maceHitter();
        int leftX = panelX + 150;
        int rightX = panelX + panelWidth / 2 + 12;
        int y = panelY + 108;
        int row = 0;

        addValueSlider(leftX, y + row++ * 27, panelWidth / 2 - 170, "Range", 1.0, 6.0, " blocks", false,
                module::getRange, module::setRange);
        addValueSlider(leftX, y + row++ * 27, panelWidth / 2 - 170, "FOV", 15.0, 360.0, " deg", false,
                module::getFov, module::setFov);
        addValueSlider(leftX, y + row++ * 27, panelWidth / 2 - 170, "Minimum Fall", 0.0, 5.0, " blocks", false,
                module::getMinimumFallDistance, module::setMinimumFallDistance);
        addValueSlider(leftX, y + row++ * 27, panelWidth / 2 - 170, "Attack Delay", 0, 200, " ms", true,
                () -> module.getAttackDelayMillis(), value -> module.setAttackDelayMillis((int) Math.round(value)));
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
            settingsScrollOffset = 0;
            rebuildWidgets();
        }).bounds(leftX, y + row * 27, panelWidth / 2 - 170, 24).build());

        addBackButton();
    }

    private void buildEntitySelector() {
        List<EntityOption> options = entityOptions();
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
            }).bounds(panelX + 150, panelY + 102, panelWidth - 176, 26).build());

            int pageSize = 16;
            int start = entityPageIndex * pageSize;
            int end = Math.min(options.size(), start + pageSize);
            int leftX = panelX + 150;
            int rightX = panelX + panelWidth / 2 + 12;
            for (int i = start; i < end; i++) {
                EntityOption option = options.get(i);
                int visibleIndex = i - start;
                int columnX = visibleIndex < 8 ? leftX : rightX;
                int rowY = panelY + 138 + (visibleIndex % 8) * 27;
                boolean enabled = selected.contains(option.id());
                addRenderableWidget(Button.builder(Component.literal((enabled ? "[x] " : "[ ] ") + option.name()),
                        button -> {
                            module.setEntityTypeSelected(option.id(), !enabled);
                            module.setTargetMode(dev.morgue.client.modules.combat.KillAuraModule.TargetMode.CUSTOM);
                            MorgueConfig.save();
                            rebuildWidgets();
                        }).bounds(columnX, rowY, panelWidth / 2 - 170, 23).build());
            }

            if (entityPageIndex > 0) {
                addRenderableWidget(Button.builder(Component.literal("< PREVIOUS"), button -> {
                    entityPageIndex--;
                    rebuildWidgets();
                }).bounds(leftX, panelY + panelHeight - 88, 104, 26).build());
            }
            if (end < options.size()) {
                addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                    entityPageIndex++;
                    rebuildWidgets();
                }).bounds(rightX + 80, panelY + panelHeight - 88, 90, 26).build());
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
        }).bounds(panelX + 150, panelY + 102, panelWidth - 176, 26).build());

        int pageSize = 16;
        int start = entityPageIndex * pageSize;
        int end = Math.min(options.size(), start + pageSize);
        int leftX = panelX + 150;
        int rightX = panelX + panelWidth / 2 + 12;
        for (int i = start; i < end; i++) {
            EntityOption option = options.get(i);
            int visibleIndex = i - start;
            int columnX = visibleIndex < 8 ? leftX : rightX;
            int rowY = panelY + 138 + (visibleIndex % 8) * 27;
            boolean enabled = selected.contains(option.id());
            addRenderableWidget(Button.builder(Component.literal((enabled ? "[x] " : "[ ] ") + option.name()),
                    button -> {
                        module.setEntityTypeSelected(option.id(), !enabled);
                        module.setTargetMode(MaceHitterModule.TargetMode.CUSTOM);
                        MorgueConfig.save();
                        rebuildWidgets();
                    }).bounds(columnX, rowY, panelWidth / 2 - 170, 23).build());
        }

        if (entityPageIndex > 0) {
            addRenderableWidget(Button.builder(Component.literal("< PREVIOUS"), button -> {
                entityPageIndex--;
                rebuildWidgets();
            }).bounds(leftX, panelY + panelHeight - 88, 104, 26).build());
        }
        if (end < options.size()) {
            addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                entityPageIndex++;
                rebuildWidgets();
            }).bounds(rightX + 80, panelY + panelHeight - 88, 90, 26).build());
        }
        addBackButton();
    }

    private void buildKillAuraSettings() {
        var module = MorgueClient.killAura();
        int leftX = panelX + 150;
        int rightX = panelX + panelWidth / 2 + 12;
        int y = panelY + 110;
        int leftRow = 0;
        int rightRow = 0;

        addValueSlider(leftX, y + leftRow++ * 27, panelWidth / 2 - 170, "Range", 1.0, 6.0, " blocks", false,
                module::getRange, module::setRange);
        addValueSlider(leftX, y + leftRow++ * 27, panelWidth / 2 - 170, "FOV", 15.0, 360.0, " deg", false,
                module::getFov, module::setFov);
        addValueSlider(leftX, y + leftRow++ * 27, panelWidth / 2 - 170, "Attack Delay", 0, 200, " ms", true,
                () -> module.getAttackDelayMillis(), value -> module.setAttackDelayMillis((int) Math.round(value)));
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
        }).bounds(leftX, y + leftRow * 27, panelWidth / 2 - 170, 26).build());

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
        addValueSlider(leftX, y + (leftRow + 1) * 27, panelWidth / 2 - 170, "Minimum Target Age", 0, 200, " ticks", true,
                () -> module.getMinimumAgeTicks(), value -> module.setMinimumAgeTicks((int) Math.round(value)));

        addBackButton();
    }

    private void buildAiBrainSettings() {
        int x = panelX + 150;
        int y = panelY + 108;
        var assessment = dev.morgue.client.ai.TargetIntelligence.getLatestAssessment();

        String activeCombat = MorgueClient.maceHitter().isEnabled() && MorgueClient.killAura().isEnabled()
                ? "Mace Hitter priority / KillAura fallback"
                : MorgueClient.maceHitter().isEnabled() ? "Mace Hitter"
                : MorgueClient.killAura().isEnabled() ? "KillAura" : "No combat module active";
        addRenderableWidget(Button.builder(Component.literal("ENGINE: LOCAL TARGET INTELLIGENCE  /  READY"),
                button -> {}).bounds(x, y, panelWidth - 176, 27).build());
        addRenderableWidget(Button.builder(Component.literal("ACTIVE CONTROLLER: " + activeCombat),
                button -> {}).bounds(x, y + 34, panelWidth - 176, 27).build());

        if (assessment == null) {
            addRenderableWidget(Button.builder(Component.literal("CURRENT TARGET: Waiting for a valid target"),
                    button -> {}).bounds(x, y + 68, panelWidth - 176, 27).build());
            addRenderableWidget(Button.builder(Component.literal("ASSESSMENT: No candidate passed the active filters"),
                    button -> {}).bounds(x, y + 102, panelWidth - 176, 27).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("CURRENT TARGET: " + assessment.targetName()),
                    button -> {}).bounds(x, y + 68, panelWidth - 176, 27).build());
            addRenderableWidget(Button.builder(Component.literal("PRIORITY: " + assessment.priority()
                    + "  /  DISTANCE: " + format(assessment.distance())),
                    button -> {}).bounds(x, y + 102, panelWidth - 176, 27).build());
            addRenderableWidget(Button.builder(Component.literal("THREAT SCORE: "
                    + String.format(java.util.Locale.ROOT, "%.2f", assessment.threatScore())
                    + "  /  HEALTH: " + format(assessment.health())),
                    button -> {}).bounds(x, y + 136, panelWidth - 176, 27).build());
            addRenderableWidget(Button.builder(Component.literal("REASON: " + assessment.reason()),
                    button -> {}).bounds(x, y + 170, panelWidth - 176, 27).build());
        }

        addRenderableWidget(Button.builder(Component.literal("DECISION MODEL: deterministic local heuristics; no cloud AI"),
                button -> {}).bounds(x, y + 208, panelWidth - 176, 27).build());
        addRenderableWidget(Button.builder(Component.literal("CLEAR LAST ASSESSMENT"), button -> {
            dev.morgue.client.ai.TargetIntelligence.clearAssessment();
            rebuildWidgets();
        }).bounds(x, y + 242, panelWidth - 176, 27).build());

        addBackButton();
    }

    private void buildAutoTotemSettings() {
        var module = MorgueClient.autoTotem();
        int leftX = panelX + 150;
        int rightX = panelX + panelWidth / 2 + 12;
        int y = panelY + 112;

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
        addValueSlider(leftX, y + 72, panelWidth / 2 - 170, "Switch Delay", 0, 250, " ms", true,
                () -> module.getSwitchDelayMillis(), value -> module.setSwitchDelayMillis((int) Math.round(value)));
        addValueSlider(leftX, y + 108, panelWidth / 2 - 170, "Activation Health", 0, 20, " HP", false,
                () -> module.getActivationHealth(), value -> module.setActivationHealth((float) value));
        addSettingButton(rightX, y, toggleLabel("Pause While Using Item", module.isPauseWhileUsingItem()),
                () -> module.setPauseWhileUsingItem(!module.isPauseWhileUsingItem()));
        addSettingButton(rightX, y + 72, toggleLabel("Predict Recent Damage", module.isPredictRecentDamage()),
                () -> module.setPredictRecentDamage(!module.isPredictRecentDamage()));
        addSettingButton(rightX, y + 108, toggleLabel("Predict Fall Damage", module.isPredictFallDamage()),
                () -> module.setPredictFallDamage(!module.isPredictFallDamage()));
        addSettingButton(rightX, y + 144, toggleLabel("Restore Displaced Offhand", module.isRestoreDisplacedOffhand()),
                () -> module.setRestoreDisplacedOffhand(!module.isRestoreDisplacedOffhand()));
        addSettingButton(rightX, y + 180, toggleLabel("Predict Nearby Threats", module.isPredictNearbyThreats()),
                () -> module.setPredictNearbyThreats(!module.isPredictNearbyThreats()));

        addRenderableWidget(Button.builder(Component.literal("AUTO TOTEM STATUS: " + (module.isEnabled() ? "ON" : "OFF")),
                button -> {
                    MorgueClient.setAutoTotemEnabled(!module.isEnabled());
                    ENABLED.put("Auto Totem", MorgueClient.autoTotem().isEnabled());
                    MorgueConfig.save();
                    rebuildWidgets();
                }).bounds(rightX, y + 36, panelWidth / 2 - 170, 27).build());

        addBackButton();
    }

    private void buildMovementSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        addSettingButton(x, y, toggleLabel("Auto Sprint", MorgueClient.sprint().isEnabled()),
                () -> MorgueClient.setSprintEnabled(!MorgueClient.sprint().isEnabled()));
        addSettingButton(x, y + 36, toggleLabel("Safe Walk", MorgueClient.safeWalk().isEnabled()),
                () -> MorgueClient.setSafeWalkEnabled(!MorgueClient.safeWalk().isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Both options use normal client movement state and respect the server's movement rules."),
                button -> {}).bounds(x, y + 78, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildEnabledModuleList() {
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
        addRenderableWidget(Button.builder(Component.literal("WORLD RULES ARE CONTROLLED BY THE WORLD OWNER OR SERVER."),
                button -> {}).bounds(x, y, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("MORGUE DOES NOT OVERRIDE SERVER GAME RULES OR DIFFICULTY."),
                button -> {}).bounds(x, y + 36, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("USE VANILLA WORLD OPTIONS FOR PERMITTED LOCAL SETTINGS."),
                button -> {}).bounds(x, y + 72, width, 28).build());
        addBackButton();
    }

    private void buildAutoEatSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.autoEat();
        addSettingButton(x, y, toggleLabel("Auto Eat", module.isEnabled()),
                () -> MorgueClient.setAutoEatEnabled(!module.isEnabled()));
        addValueSlider(x, y + 36, panelWidth / 2 - 170, "Hunger Threshold", 1, 19, " points", true,
                () -> module.getHungerThreshold(), value -> module.setHungerThreshold((int) Math.round(value)));
        addRenderableWidget(Button.builder(Component.literal(
                "Selects the most nutritious food already in your hotbar."),
                button -> {}).bounds(x, y + 78, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Uses vanilla item-use input and restores your previous hotbar slot after eating."),
                button -> {}).bounds(x, y + 114, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildItemUseInfo() {
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
        addRenderableWidget(Button.builder(Component.literal("ITEM USE: VANILLA INPUT ONLY"),
                button -> {}).bounds(x, y, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("NO AUTOMATIC FOOD, POTION, OR PROJECTILE USE IS ENABLED."),
                button -> {}).bounds(x, y + 36, width, 28).build());
        addRenderableWidget(Button.builder(Component.literal("THIS PAGE IS RESERVED FOR A FUTURE USER-CONFIGURABLE ITEM-USE MODULE."),
                button -> {}).bounds(x, y + 72, width, 28).build());
        addBackButton();
    }

    private void buildHudEditorInfo() {
        int x = panelX + 150;
        int y = panelY + 112;
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
                button -> {}).bounds(x, y + 114, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Overlay positions are fixed to the upper corners; drag-and-drop editing is not implemented."),
                button -> {}).bounds(x, y + 150, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildNotificationsSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        addSettingButton(x, y, toggleLabel("Local Chat Notifications",
                dev.morgue.client.ClientNotifications.isEnabled()),
                () -> dev.morgue.client.ClientNotifications.setEnabled(
                        !dev.morgue.client.ClientNotifications.isEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Notifications are local chat messages for AutoTotem and Inventory Helper actions."),
                button -> {}).bounds(x, y + 40, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "No notification content is sent to a remote service."),
                button -> {}).bounds(x, y + 78, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildVisualSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
        addRenderableWidget(Button.builder(Component.literal(
                "DRAG THE SLIDERS TO ADJUST THE TRANSPARENCY OF THE CLIENT UI."),
                button -> {}).bounds(x, y, width, 28).build());
        addBackButton();
    }

    private void buildWorldInfo() {
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
        String[] details = {
                "CLIENT: MORGUE-CLIENT",
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
        int x = panelX + 150;
        int y = panelY + 108;
        int contentWidth = panelWidth - 176;
        String[] credits = {
                "PROJECT LEAD  /  quincey-ys",
                "AI DEVELOPMENT SUPPORT  /  OpenAI ChatGPT",
                "MINECRAFT JAVA  /  Mojang Studios, Microsoft",
                "FABRIC LOADER + API  /  FabricMC",
                "BUILD TOOLS  /  Gradle, Fabric Loom, ProGuard",
                "SBOM TOOLING  /  CycloneDX Gradle plugin",
                "RESEARCH  /  Meteor, LiquidBounce, Wurst",
                "RESEARCH  /  Baritone, Anomaly, BleachHack",
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
        int x = panelX + 150;
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
        }).bounds(x, panelY + 116, panelWidth - 176, 30).build());

        addRenderableWidget(Button.builder(Component.literal("Manage local packs and normal client selections."),
                button -> {}).bounds(x, panelY + 156, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Server-required packs remain controlled by the server."),
                button -> {}).bounds(x, panelY + 192, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildPrivacyDashboard() {
        int x = panelX + 150;
        addRenderableWidget(Button.builder(Component.literal("MORGUE does not send a custom mod-list handshake."),
                button -> {}).bounds(x, panelY + 116, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Servers can still inspect normal gameplay and network behavior."),
                button -> {}).bounds(x, panelY + 152, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("No anti-cheat evasion, fake responses, or required-pack bypass is active."),
                button -> {}).bounds(x, panelY + 188, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildIntegrityDashboard() {
        int x = panelX + 150;
        int y = panelY + 112;
        addRenderableWidget(Button.builder(Component.literal(
                "PACKAGE STATUS: " + dev.morgue.client.security.MorgueIntegrity.getStatus()), button -> {})
                .bounds(x, y, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "DETAIL: " + dev.morgue.client.security.MorgueIntegrity.getDetails()), button -> {})
                .bounds(x, y + 36, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "SCOPE: all packaged JAR entries except this manifest"), button -> {})
                .bounds(x, y + 72, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "LIMIT: local tamper check, not a cryptographic signature"), button -> {})
                .bounds(x, y + 108, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("RUN CHECK AGAIN"), button -> {
            dev.morgue.client.security.MorgueIntegrity.verify();
            rebuildWidgets();
        }).bounds(x, y + 152, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildKeybindSettings() {
        int x = panelX + 150;
        addRenderableWidget(Button.builder(Component.literal("OPEN MINECRAFT KEYBIND SETTINGS"), button -> {
            minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options));
        }).bounds(x, panelY + 116, panelWidth - 176, 30).build());
        addRenderableWidget(Button.builder(Component.literal("MORGUE defaults: Right Shift / M / K / V"),
                button -> {}).bounds(x, panelY + 156, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("MIDDLE-CLICK ANY TOGGLEABLE MODULE ROW TO CHANGE ITS KEYBIND."),
                button -> {}).bounds(x, panelY + 192, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("PRESS A KEY OR MOUSE BUTTON; ESC CANCELS. BINDS SAVE TO MINECRAFT CONTROLS."),
                button -> {}).bounds(x, panelY + 228, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void buildProfileSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        int width = panelWidth - 176;
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
        int x = panelX + 150;
        addRenderableWidget(Button.builder(Component.literal("This module is listed in the ClickGUI prototype."),
                button -> {}).bounds(x, panelY + 112, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Gameplay behavior is not connected yet."),
                button -> {}).bounds(x, panelY + 148, panelWidth - 176, 28).build());
        addBackButton();
    }

    private void addValueSlider(int x, int y, int width, String label, double min, double max,
                                String suffix, boolean integer, DoubleSupplier getter, DoubleConsumer setter) {
        AbstractWidget placeholder = Button.builder(Component.empty(), button -> {})
                .bounds(x, y, width, 27).build();
        addRenderableWidget(placeholder);
        settingSliders.put(placeholder,
                new SettingSlider(placeholder, label, min, max, suffix, integer, getter, setter));
    }

    private void updateSettingSlider(SettingSlider slider, double mouseX) {
        int left = slider.widget().getX() + 8;
        int right = slider.widget().getX() + slider.widget().getWidth() - 8;
        double fraction = (mouseX - left) / Math.max(1.0, right - left);
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        double value = slider.minimum() + fraction * (slider.maximum() - slider.minimum());
        if (slider.integer()) {
            value = Math.round(value);
        }
        slider.setter().accept(value);
    }

    private void drawSettingSliders(GuiGraphicsExtractor graphics) {
        for (SettingSlider slider : settingSliders.values()) {
            AbstractWidget widget = slider.widget();
            int x = widget.getX();
            int y = widget.getY();
            int width = widget.getWidth();
            double value = slider.getter().getAsDouble();
            double fraction = (value - slider.minimum())
                    / Math.max(0.0001, slider.maximum() - slider.minimum());
            fraction = Math.max(0.0, Math.min(1.0, fraction));
            int left = x + 8;
            int right = x + width - 8;
            int trackY = y + 19;
            int thumb = left + (int) Math.round((right - left) * fraction);
            String shownValue = slider.integer()
                    ? Long.toString(Math.round(value)) : format(value);
            graphics.text(font, displayText(slider.label() + "  " + shownValue + slider.suffix()),
                    x + 8, y + 2, WHITE);
            graphics.fill(left, trackY, right, trackY + 4, withOpacity(0xFF17263A, controlOpacity));
            graphics.fill(left, trackY + 1, thumb, trackY + 3, withOpacity(RED_SOFT, controlOpacity));
            graphics.fill(left, trackY + 2, thumb, trackY + 3, withOpacity(RED, controlOpacity));
            graphics.outline(left, trackY - 1, right - left, 6, 0xA05C7CA3);
            graphics.fill(thumb - 3, trackY - 4, thumb + 4, trackY + 8, 0xFF9DCBFF);
            graphics.fill(thumb - 1, trackY - 2, thumb + 2, trackY + 6, 0xFF203A59);
        }
    }

    private void addSettingButton(int x, int y, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> {
            action.run();
            MorgueConfig.save();
            rebuildWidgets();
        }).bounds(x, y, panelWidth / 2 - 170, 27).build());
    }

    private void addBackButton() {
        addRenderableWidget(Button.builder(Component.literal("< BACK TO MODULES"), button -> {
            selectedModule = null;
            entityPage = false;
            settingsScrollOffset = 0;
            rebuildWidgets();
        }).bounds(panelX + 150, panelY + panelHeight - 56, 160, 22).build());
    }

    private static boolean isModuleEnabled(String module) {
        return MorgueClient.isModuleKeybindSupported(module)
                ? MorgueClient.isModuleEnabled(module)
                : ENABLED.getOrDefault(module, false);
    }

    private void toggleModule(String module) {
        if (MorgueClient.isModuleKeybindSupported(module)) {
            MorgueClient.setModuleEnabled(module, !MorgueClient.isModuleEnabled(module));
        } else {
            ENABLED.put(module, !isModuleEnabled(module));
            MorgueConfig.save();
        }
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

    public static float getPanelOpacity() {
        return panelOpacity;
    }

    public static void setPanelOpacity(float value) {
        panelOpacity = clampOpacity(value);
    }

    public static float getControlOpacity() {
        return controlOpacity;
    }

    public static void setControlOpacity(float value) {
        controlOpacity = clampOpacity(value);
    }

    private static float clampOpacity(float value) {
        return Math.max(0.20F, Math.min(1.0F, value));
    }

    private static int withOpacity(int argb, float opacity) {
        int alpha = (argb >>> 24) & 0xFF;
        int adjusted = Math.max(0, Math.min(255, Math.round(alpha * opacity)));
        return (argb & 0x00FFFFFF) | (adjusted << 24);
    }

    private boolean isOpacitySettingsPage() {
        return "Visual Settings".equals(selectedModule) || "Theme".equals(selectedModule);
    }

    private boolean isScrollableSettingsPage() {
        return selectedModule != null && !entityPage && !isOpacitySettingsPage();
    }

    private int settingsViewportTop() {
        return panelY + 106;
    }

    private int settingsViewportBottom() {
        return panelY + panelHeight - 58;
    }

    private int settingsViewportLeft() {
        return panelX + 144;
    }

    private int settingsViewportRight() {
        return panelX + panelWidth - 22;
    }

    private boolean isBackButton(AbstractWidget widget) {
        return widget.getMessage().getString().startsWith("< BACK TO MODULES")
                || (widget.getX() == panelX + 150 && widget.getY() == panelY + panelHeight - 56
                && widget.getWidth() == 160 && widget.getHeight() == 22);
    }

    private boolean isSettingsContentWidget(AbstractWidget widget) {
        return widget.getX() >= panelX + 140 && !isBackButton(widget);
    }

    private void layoutSettingsViewport() {
        settingsScrollMaxOffset = 0;
        if (!isScrollableSettingsPage()) {
            settingsScrollOffset = 0;
            return;
        }

        int contentBottom = settingsViewportTop();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget.visible
                    && isSettingsContentWidget(widget)) {
                contentBottom = Math.max(contentBottom, widget.getY() + widget.getHeight());
            }
        }

        settingsScrollMaxOffset = Math.max(0, contentBottom - settingsViewportBottom());
        settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, settingsScrollMaxOffset));
        if (settingsScrollOffset > 0) {
            for (var child : children()) {
                if (child instanceof AbstractWidget widget && widget.visible
                        && isSettingsContentWidget(widget)) {
                    widget.setY(widget.getY() - settingsScrollOffset);
                }
            }
        }
    }

    private int settingsScrollTrackX() {
        return panelX + panelWidth - 16;
    }

    private int settingsScrollThumbHeight() {
        int track = Math.max(1, settingsViewportBottom() - settingsViewportTop());
        return Math.max(22, track * track / Math.max(1, track + settingsScrollMaxOffset));
    }

    private int settingsScrollThumbY() {
        int track = Math.max(1, settingsViewportBottom() - settingsViewportTop());
        int thumb = settingsScrollThumbHeight();
        int travel = Math.max(1, track - thumb);
        return settingsViewportTop() + travel * settingsScrollOffset
                / Math.max(1, settingsScrollMaxOffset);
    }

    private void setSettingsScrollFromMouseY(double mouseY) {
        int track = Math.max(1, settingsViewportBottom() - settingsViewportTop());
        int thumb = settingsScrollThumbHeight();
        int travel = Math.max(1, track - thumb);
        double fraction = (mouseY - settingsScrollDragOffset - settingsViewportTop()) / travel;
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        settingsScrollOffset = (int) Math.round(fraction * settingsScrollMaxOffset);
        rebuildWidgets();
    }

    private int opacitySliderLeft() {
        return panelX + 184;
    }

    private int opacitySliderRight() {
        return panelX + panelWidth - 62;
    }

    private int opacitySliderY(int slider) {
        return panelY + 168 + slider * 60;
    }

    private boolean isOpacitySliderAt(double mouseX, double mouseY, int slider) {
        int y = opacitySliderY(slider);
        return mouseX >= opacitySliderLeft() - 6 && mouseX <= opacitySliderRight() + 6
                && mouseY >= y - 7 && mouseY <= y + 15;
    }

    private void updateOpacitySlider(double mouseX, int slider) {
        double fraction = (mouseX - opacitySliderLeft())
                / Math.max(1.0, opacitySliderRight() - opacitySliderLeft());
        float value = (float) (0.20 + Math.max(0.0, Math.min(1.0, fraction)) * 0.80);
        if (slider == 0) {
            panelOpacity = value;
        } else {
            controlOpacity = value;
        }
    }

    private void drawOpacitySliders(GuiGraphicsExtractor graphics) {
        if (!isOpacitySettingsPage()) {
            return;
        }
        graphics.text(font, displayText("PANEL OPACITY  " + Math.round(panelOpacity * 100) + "%"),
                opacitySliderLeft(), opacitySliderY(0) - 23, WHITE);
        graphics.text(font, displayText("BUTTON OPACITY  " + Math.round(controlOpacity * 100) + "%"),
                opacitySliderLeft(), opacitySliderY(1) - 23, WHITE);

        for (int i = 0; i < 2; i++) {
            int y = opacitySliderY(i);
            float value = i == 0 ? panelOpacity : controlOpacity;
            int left = opacitySliderLeft();
            int right = opacitySliderRight();
            int thumb = left + Math.round((right - left) * ((value - 0.20F) / 0.80F));
            graphics.fill(left, y, right, y + 8, 0xFF17263A);
            graphics.fill(left, y + 1, thumb, y + 7, RED_SOFT);
            graphics.fill(left, y + 3, thumb, y + 5, RED);
            graphics.outline(left, y - 1, right - left, 10, 0xB35C7CA3);
            graphics.fill(thumb - 4, y - 4, thumb + 5, y + 13, 0xFF9DCBFF);
            graphics.fill(thumb - 2, y - 2, thumb + 3, y + 11, 0xFF203A59);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep the live world visible; MORGUE draws only its own translucent panels.
    }

    private static Component displayText(String value) {
        // Use ordinary ASCII/Unicode text with Minecraft's default font.
        // Bold styling gives the labels weight without unsupported mathematical
        // double-struck glyphs, which render as boxed symbols on many clients.
        return Component.literal(value).withStyle(style -> style.withBold(true));
    }

    private void extractCustomButton(GuiGraphicsExtractor graphics, AbstractWidget widget, String rawLabel,
                                      int mouseX, int mouseY) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        long now = System.nanoTime();
        double pulse = (Math.sin(now / 310_000_000.0) + 1.0) * 0.5;
        boolean selected = rawLabel.equals(selectedCategory);
        boolean activeState = rawLabel.startsWith("LIVE") || rawLabel.startsWith("ON")
                || rawLabel.contains(": ON");
        boolean hovered = widget.isHoveredOrFocused() || widget.isMouseOver(mouseX, mouseY);
        boolean enabled = widget.isActive();

        int background = !enabled ? 0xB0070D18
                : hovered ? BUTTON_HOVER
                : selected ? 0xD0132340
                : activeState ? 0xC5122035
                : BUTTON_IDLE;
        int accent = selected || activeState ? RED
                : hovered ? 0xFFB5D2FF
                : 0xFF587DAA;
        int textColor = !enabled ? 0xFF71849F
                : selected || activeState ? 0xFFE1EEFF
                : WHITE;

        // Layered glass plate, cut corners, and a restrained animated edge.
        graphics.fill(x, y, x + w, y + h, withOpacity(background, controlOpacity));
        graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1,
                withOpacity(hovered ? 0x263D5E87 : 0x182A4263, controlOpacity));
        graphics.fill(x, y + 2, x + 3, y + h - 2, accent);
        graphics.fill(x + 3, y, x + w - 5, y + 1, 0x806C86A8);
        graphics.fill(x + 4, y + h - 1, x + w - 2, y + h, 0xA0030812);
        graphics.fill(x + w - 5, y + 4, x + w - 4, y + h - 4,
                selected || activeState ? RED_SOFT : 0xFF263B59);
        graphics.fill(x, y, x + 8, y + 1, accent);
        graphics.fill(x + w - 10, y + h - 1, x + w, y + h, accent);

        if (hovered) {
            int sweepWidth = Math.max(12, w / 4);
            int travel = Math.max(1, w - sweepWidth - 8);
            int sweepX = x + 4 + (int) (Math.floorMod(now / 5_000_000L, travel));
            graphics.fill(sweepX, y + 2, Math.min(x + w - 4, sweepX + sweepWidth), y + h - 2, 0x247DB7FF);
            graphics.fill(x + 3, y + 1, x + w - 4, y + 2, 0xA0A8CEFF);
            graphics.outline(x - 1, y - 1, w + 2, h + 2,
                    0x50000000 | ((int) (90 + pulse * 100) << 16) | 0x004E78B0);
        }

        if (selected || activeState) {
            int beaconAlpha = (int) (150 + pulse * 105);
            graphics.fill(x + w - 12, y + h / 2 - 3, x + w - 6, y + h / 2 + 3,
                    (beaconAlpha << 24) | 0x007FB5FF);
            graphics.fill(x + w - 17, y + h / 2 - 1, x + w - 14, y + h / 2 + 1, 0xB5B5D5FF);
        } else {
            graphics.fill(x + w - 11, y + h / 2 - 1, x + w - 7, y + h / 2 + 1,
                    hovered ? 0xFFB9D8FF : 0xFF54708F);
        }

        int textLeft = x + 10;
        EntityOption targetOption = entityPage ? entityOptionForLabel(rawLabel) : null;
        if (targetOption != null) {
            ItemStack icon = entityItemStack(targetOption);
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 5, y + Math.max(2, (h - 16) / 2));
                textLeft = x + 27;
            }
        }

        String visibleLabel = rawLabel;
        Component label = displayText(visibleLabel);
        int maxTextWidth = Math.max(8, x + w - textLeft - 18);
        while (font.width(label) > maxTextWidth && visibleLabel.length() > 4) {
            visibleLabel = visibleLabel.substring(0, visibleLabel.length() - 2) + "…";
            label = displayText(visibleLabel);
        }
        int textY = y + Math.max(2, (h - 10) / 2);
        graphics.text(font, label, textLeft, textY, textColor);
    }


    private List<EntityOption> entityOptions() {
        if (cachedEntityOptions != null) {
            return cachedEntityOptions;
        }

        List<EntityOption> options = new ArrayList<>();
        options.add(new EntityOption("Players", "minecraft:player"));
        List<EntityOption> registeredLivingTypes = new ArrayList<>();

        // Every non-MISC category is a mob category. Include the special
        // living entity types that Minecraft places in MISC as well.
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if ("minecraft:player".equals(id)) {
                return;
            }
            boolean specialLivingType = "minecraft:armor_stand".equals(id)
                    || "minecraft:mannequin".equals(id);
            if (type.getCategory() == MobCategory.MISC && !specialLivingType) {
                return;
            }

            String name = type.getDescription().getString();
            if (name == null || name.isBlank()) {
                name = id.substring(id.indexOf(':') + 1).replace('_', ' ');
            }
            if (!id.startsWith("minecraft:")) {
                name += " [" + id + "]";
            }
            registeredLivingTypes.add(new EntityOption(name, id));
        });

        registeredLivingTypes.sort(Comparator
                .comparing(EntityOption::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EntityOption::id));
        options.addAll(registeredLivingTypes);
        options.add(new EntityOption("Modded / Other", "morgue:other"));
        cachedEntityOptions = List.copyOf(options);
        return cachedEntityOptions;
    }

    private EntityOption entityOptionForLabel(String rawLabel) {
        String name = rawLabel.replaceFirst("^\\[[ xX]\\]\\s*", "");
        for (EntityOption option : entityOptions()) {
            if (option.name().equals(name)) {
                return option;
            }
        }
        return null;
    }

    private ItemStack entityItemStack(EntityOption option) {
        String itemId;
        if ("minecraft:player".equals(option.id())) {
            itemId = "minecraft:player_head";
        } else if ("minecraft:wither".equals(option.id())) {
            itemId = "minecraft:wither_skeleton_skull";
        } else if ("minecraft:ender_dragon".equals(option.id())) {
            itemId = "minecraft:dragon_head";
        } else if ("minecraft:armor_stand".equals(option.id())
                || "minecraft:mannequin".equals(option.id())) {
            itemId = "minecraft:armor_stand";
        } else if ("morgue:other".equals(option.id())) {
            itemId = "minecraft:barrier";
        } else {
            itemId = option.id() + "_spawn_egg";
        }
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void drawAnimatedPanelEffects(GuiGraphicsExtractor graphics) {
        long now = System.nanoTime();
        double pulse = (Math.sin(now / 420_000_000.0) + 1.0) * 0.5;
        int contentLeft = panelX + 140;
        int contentRight = panelX + panelWidth - 10;
        int contentTop = panelY + 78;
        int contentBottom = panelY + panelHeight - 38;

        // Technical grid behind the controls, kept deliberately faint.
        for (int x = panelX + 12; x < panelX + panelWidth - 10; x += 24) {
            graphics.fill(x, panelY + 5, x + 1, panelY + panelHeight - 5, 0x183E6492);
        }
        for (int y = panelY + 84; y < panelY + panelHeight - 38; y += 22) {
            graphics.fill(contentLeft, y, contentRight, y + 1, 0x183E6492);
        }

        // A moving scanline adds life without dimming the Minecraft world.
        int scanRange = Math.max(1, contentBottom - contentTop);
        int scanY = contentTop + (int) Math.floorMod(now / 42_000_000L, scanRange);
        graphics.fill(contentLeft, scanY, contentRight, scanY + 1, 0x247FB8FF);
        graphics.fill(contentLeft, scanY + 1, contentRight, scanY + 2, 0x0C7FB8FF);

        // Moving top-edge signal and pulsing corner brackets.
        int sweepRange = Math.max(1, panelWidth - 54);
        int sweepX = panelX + 12 + (int) Math.floorMod(now / 7_000_000L, sweepRange);
        graphics.fill(sweepX, panelY + 2, Math.min(panelX + panelWidth - 12, sweepX + 34),
                panelY + 4, 0xD08CBFFF);
        int bracketAlpha = (int) (110 + pulse * 100);
        int bracket = (bracketAlpha << 24) | 0x0085B8FF;
        graphics.fill(panelX + 7, panelY + 8, panelX + 22, panelY + 9, bracket);
        graphics.fill(panelX + 7, panelY + 8, panelX + 8, panelY + 21, bracket);
        graphics.fill(panelX + panelWidth - 22, panelY + 8, panelX + panelWidth - 7, panelY + 9, bracket);
        graphics.fill(panelX + panelWidth - 8, panelY + 8, panelX + panelWidth - 7, panelY + 21, bracket);

        // Tiny diagnostic equalizer in the footer.
        for (int i = 0; i < 9; i++) {
            int barHeight = 2 + (int) ((Math.sin(now / 150_000_000.0 + i * 0.72) + 1.0) * 3.0);
            int barX = panelX + panelWidth - 186 + i * 5;
            graphics.fill(barX, panelY + panelHeight - 12 - barHeight,
                    barX + 2, panelY + panelHeight - 12,
                    i < 3 ? 0xD080B5FF : 0x806E8FB7);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                withOpacity(BLACK, panelOpacity));
        graphics.fill(panelX + 1, panelY + 3, panelX + panelWidth - 1, panelY + panelHeight - 1,
                withOpacity(PANEL, panelOpacity));
        graphics.outline(panelX, panelY, panelWidth, panelHeight, BORDER);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, RED);
        graphics.fill(panelX + panelWidth - 92, panelY + 3, panelX + panelWidth - 16,
                panelY + 4, RED_SOFT);
        drawAnimatedPanelEffects(graphics);

        graphics.fill(panelX + 1, panelY + 4, panelX + 132, panelY + 72,
                withOpacity(PANEL_LIGHT, panelOpacity));
        graphics.text(font, displayText("M"), panelX + 20, panelY + 14, WHITE);
        graphics.text(font, displayText("MORGUE"), panelX + 49, panelY + 17, WHITE);
        graphics.text(font, displayText("CLIENT  /  26.3"), panelX + 49, panelY + 32, MUTED);

        graphics.fill(panelX + 145, panelY + 18, panelX + panelWidth - 16, panelY + 69,
                withOpacity(PANEL_LIGHT, panelOpacity));
        graphics.fill(panelX + 145, panelY + 18, panelX + 148, panelY + 69, RED_SOFT);
        String heading = selectedModule == null ? selectedCategory.toUpperCase() : selectedModule.toUpperCase();
        graphics.text(font, displayText(heading), panelX + 161, panelY + 27, WHITE);
        graphics.text(font, displayText(selectedModule == null
                ? "INDEPENDENT CLIENT  //  BUILD 0.1.0"
                : entityPage ? "TARGET ENTITY FILTER" : "MODULE SETTINGS"), panelX + 161, panelY + 43, MUTED);
        long headerTime = System.nanoTime();
        int headerPulse = (int) (150 + (Math.sin(headerTime / 300_000_000.0) + 1.0) * 52.0);
        graphics.fill(panelX + panelWidth - 43, panelY + 34, panelX + panelWidth - 37, panelY + 40,
                (headerPulse << 24) | 0x004C9FFF);
        graphics.text(font, displayText("ONLINE"), panelX + panelWidth - 91, panelY + 33, 0xFF9FCBFF);

        graphics.fill(panelX + 136, panelY + 82, panelX + 137, panelY + panelHeight - 16, BORDER);
        graphics.text(font, displayText(selectedModule == null ? "MODULES" : "CONFIGURATION"),
                panelX + 151, panelY + 86, MUTED);

        graphics.fill(panelX + 16, panelY + panelHeight - 34, panelX + panelWidth - 16,
                panelY + panelHeight - 33, BORDER);
        graphics.fill(panelX + panelWidth - 14, panelY + panelHeight - 3,
                panelX + panelWidth - 3, panelY + panelHeight - 2, RED);
        graphics.fill(panelX + panelWidth - 9, panelY + panelHeight - 8,
                panelX + panelWidth - 3, panelY + panelHeight - 3, BORDER);
        graphics.text(font, displayText("MORGUE-CLIENT"), panelX + 17,
                panelY + panelHeight - 24, MUTED);
        graphics.text(font, displayText("LMB TOGGLE  •  RMB SETTINGS  •  MMB BIND"),
                panelX + panelWidth - 244, panelY + panelHeight - 24, MUTED);

        // Suppress vanilla captions; custom MORGUE controls are painted above the
        // widget layer. Scrollable pages are rendered in two passes so the content
        // viewport can be clipped without clipping the sidebar or fixed Back button.
        Map<AbstractWidget, String> buttonLabels = new java.util.IdentityHashMap<>();
        Map<AbstractWidget, Boolean> originalVisibility = new java.util.IdentityHashMap<>();
        Map<AbstractWidget, Float> originalAlpha = new java.util.IdentityHashMap<>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !(widget instanceof EditBox)) {
                buttonLabels.put(widget, widget.getMessage().getString());
                originalVisibility.put(widget, widget.visible);
                originalAlpha.put(widget, widget.getAlpha());
                widget.setMessage(Component.empty());
                widget.setAlpha(0.0F);
            }
        }

        if (isScrollableSettingsPage()) {
            for (var child : children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.visible = originalVisibility.getOrDefault(widget, widget.visible)
                            && isSettingsContentWidget(widget);
                }
            }
            graphics.enableScissor(settingsViewportLeft(), settingsViewportTop(),
                    settingsViewportRight(), settingsViewportBottom());
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.disableScissor();

            for (var child : children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.visible = originalVisibility.getOrDefault(widget, widget.visible)
                            && !isSettingsContentWidget(widget);
                }
            }
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            for (var child : children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.visible = originalVisibility.getOrDefault(widget, widget.visible);
                }
            }
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        for (var entry : originalAlpha.entrySet()) {
            entry.getKey().setAlpha(entry.getValue());
        }

        // Draw scrolling settings controls inside the clipped viewport first.
        if (isScrollableSettingsPage()) {
            graphics.enableScissor(settingsViewportLeft(), settingsViewportTop(),
                    settingsViewportRight(), settingsViewportBottom());
            for (var entry : buttonLabels.entrySet()) {
                AbstractWidget widget = entry.getKey();
                String rawLabel = entry.getValue();
                widget.setMessage(Component.literal(rawLabel));
                if (isSettingsContentWidget(widget) && !settingSliders.containsKey(widget)) {
                    extractCustomButton(graphics, widget, rawLabel, mouseX, mouseY);
                }
            }
            drawSettingSliders(graphics);
            graphics.disableScissor();

            // The sidebar and Back button are fixed UI and must never be clipped.
            for (var entry : buttonLabels.entrySet()) {
                AbstractWidget widget = entry.getKey();
                String rawLabel = entry.getValue();
                widget.setMessage(Component.literal(rawLabel));
                if (!isSettingsContentWidget(widget)
                        || isBackButton(widget) || widget.getX() < panelX + 140) {
                    extractCustomButton(graphics, widget, rawLabel, mouseX, mouseY);
                }
            }
        } else {
            for (var entry : buttonLabels.entrySet()) {
                AbstractWidget widget = entry.getKey();
                String rawLabel = entry.getValue();
                widget.setMessage(Component.literal(rawLabel));
                if (!settingSliders.containsKey(widget)) {
                    extractCustomButton(graphics, widget, rawLabel, mouseX, mouseY);
                }
            }
            drawSettingSliders(graphics);
        }

        drawOpacitySliders(graphics);

        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                if (widget instanceof EditBox) {
                    int ex = widget.getX();
                    int ey = widget.getY();
                    int ew = widget.getWidth();
                    int eh = widget.getHeight();
                    boolean searchHovered = widget.isMouseOver(mouseX, mouseY);
                    graphics.outline(ex - 1, ey - 1, ew + 2, eh + 2,
                            searchHovered ? RED : 0xB86C8FB8);
                    graphics.fill(ex + 2, ey + eh - 2, ex + ew - 2, ey + eh - 1,
                            searchHovered ? 0xFF9BC8FF : 0xA049719F);
                    graphics.fill(ex + 6, ey + 2, ex + 7, ey + eh - 2,
                            searchHovered ? 0xFF9BC8FF : 0xA049719F);
                    continue;
                }
            }
        }

        if (isScrollableSettingsPage() && settingsScrollMaxOffset > 0) {
            int trackX = settingsScrollTrackX();
            int thumbY = settingsScrollThumbY();
            int thumbHeight = settingsScrollThumbHeight();
            graphics.fill(trackX, settingsViewportTop(), trackX + 2, settingsViewportBottom(), 0x805778A2);
            graphics.fill(trackX - 2, thumbY, trackX + 4, thumbY + thumbHeight,
                    settingsScrollDragging ? 0xFFB8D5FF : RED);
            graphics.fill(trackX - 1, thumbY + 2, trackX + 3, thumbY + thumbHeight - 2, 0xFF27496F);
        }

        // Drag handle and track for the module-list viewport.
        if (selectedModule == null && moduleTotalCount > moduleVisibleCount && moduleVisibleCount > 0) {
            int trackX = moduleScrollTrackX();
            int thumbY = moduleScrollThumbY();
            int thumbHeight = moduleScrollThumbHeight();
            graphics.fill(trackX, moduleListTop, trackX + 2, moduleListBottom, 0x805778A2);
            graphics.fill(trackX - 2, thumbY, trackX + 4, thumbY + thumbHeight,
                    moduleScrollDragging ? 0xFFB8D5FF : RED);
            graphics.fill(trackX - 1, thumbY + 2, trackX + 3, thumbY + thumbHeight - 2, 0xFF27496F);
        }
    }

    private int hoveredSearchBorder(AbstractWidget widget, int mouseX, int mouseY) {
        return widget.isMouseOver(mouseX, mouseY) ? RED : 0xFF6586AC;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isScrollableSettingsPage() && settingsScrollMaxOffset > 0
                && mouseX >= settingsViewportLeft() && mouseX <= settingsViewportRight()
                && mouseY >= settingsViewportTop() && mouseY <= settingsViewportBottom()
                && verticalAmount != 0.0) {
            settingsScrollOffset = Math.max(0, Math.min(settingsScrollMaxOffset,
                    settingsScrollOffset - (int) Math.round(verticalAmount * 28.0)));
            rebuildWidgets();
            return true;
        }
        if (selectedModule == null && moduleTotalCount > moduleVisibleCount
                && mouseX >= panelX + 150 && mouseX <= panelX + panelWidth - 20
                && mouseY >= moduleListTop && mouseY <= moduleListBottom
                && verticalAmount != 0.0) {
            int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
            int step = Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
            moduleScrollOffset = Math.max(0, Math.min(maxOffset,
                    moduleScrollOffset - (verticalAmount > 0 ? step : -step)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (pendingKeybindModule != null) {
            MorgueClient.setModuleKeybind(pendingKeybindModule,
                    InputConstants.Type.MOUSE.getOrCreate(event.button()));
            if (minecraft != null) {
                minecraft.options.save();
            }
            pendingKeybindModule = null;
            rebuildWidgets();
            return true;
        }

        if (event.button() == 0) {
            for (SettingSlider slider : settingSliders.values()) {
                AbstractWidget widget = slider.widget();
                if (mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight()
                        && (!isScrollableSettingsPage()
                        || (mouseY >= settingsViewportTop() && mouseY <= settingsViewportBottom()))) {
                    activeSettingSlider = slider;
                    updateSettingSlider(slider, mouseX);
                    return true;
                }
            }
        }

        if (event.button() == 0 && isOpacitySettingsPage()) {
            for (int slider = 0; slider < 2; slider++) {
                if (isOpacitySliderAt(mouseX, mouseY, slider)) {
                    activeOpacitySlider = slider;
                    updateOpacitySlider(mouseX, slider);
                    return true;
                }
            }
        }

        if (event.button() == 0 && isScrollableSettingsPage() && settingsScrollMaxOffset > 0
                && mouseX >= settingsScrollTrackX() - 5 && mouseX <= settingsScrollTrackX() + 6
                && mouseY >= settingsViewportTop() && mouseY <= settingsViewportBottom()) {
            int thumbY = settingsScrollThumbY();
            int thumbHeight = settingsScrollThumbHeight();
            settingsScrollDragging = true;
            settingsScrollDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                    ? (int) mouseY - thumbY : thumbHeight / 2;
            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                setSettingsScrollFromMouseY(mouseY);
            }
            return true;
        }

        if (isScrollableSettingsPage() && mouseX >= settingsViewportLeft()
                && mouseX <= settingsViewportRight()
                && (mouseY < settingsViewportTop() || mouseY > settingsViewportBottom())
                && !(mouseX >= panelX + 145 && mouseX <= panelX + 320
                && mouseY >= panelY + panelHeight - 56 && mouseY <= panelY + panelHeight - 34)) {
            return true;
        }

        if (event.button() == 0 && selectedModule == null
                && moduleTotalCount > moduleVisibleCount
                && mouseX >= moduleScrollTrackX() - 5 && mouseX <= moduleScrollTrackX() + 6
                && mouseY >= moduleListTop && mouseY <= moduleListBottom) {
            int thumbY = moduleScrollThumbY();
            int thumbHeight = moduleScrollThumbHeight();
            moduleScrollDragging = true;
            moduleScrollDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                    ? (int) mouseY - thumbY : thumbHeight / 2;
            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                setModuleScrollFromMouseY(mouseY);
            }
            return true;
        }

        String module = moduleAt(mouseX, mouseY);
        if (event.button() == 1 && module != null) {
            selectedModule = module;
            entityPage = false;
            settingsScrollOffset = 0;
            pendingKeybindModule = null;
            rebuildWidgets();
            return true;
        }
        if (event.button() == 2 && module != null && MorgueClient.isModuleKeybindSupported(module)) {
            pendingKeybindModule = module;
            rebuildWidgets();
            return true;
        }

        if (event.button() == 1) {
            if (mouseX >= panelX + panelWidth - 18 && mouseY >= panelY + panelHeight - 18) {
                resizingPanel = true;
                return true;
            }

            if (mouseX >= panelX + 140 && mouseX <= panelX + panelWidth - 12
                    && mouseY >= panelY + 3 && mouseY <= panelY + 68) {
                draggingPanel = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pendingKeybindModule != null) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                pendingKeybindModule = null;
                rebuildWidgets();
                return true;
            }

            MorgueClient.setModuleKeybind(pendingKeybindModule, InputConstants.getKey(event));
            if (minecraft != null) {
                minecraft.options.save();
            }
            pendingKeybindModule = null;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == 0 && activeSettingSlider != null) {
            updateSettingSlider(activeSettingSlider, event.x());
            return true;
        }
        if (event.button() == 0 && activeOpacitySlider >= 0) {
            updateOpacitySlider(event.x(), activeOpacitySlider);
            return true;
        }
        if (event.button() == 0 && settingsScrollDragging) {
            setSettingsScrollFromMouseY(event.y());
            return true;
        }
        if (event.button() == 0 && moduleScrollDragging) {
            setModuleScrollFromMouseY(event.y());
            return true;
        }
        if (event.button() == 1 && draggingPanel) {
            panelX = Math.max(12, Math.min(width - panelWidth - 12, panelX + (int) Math.round(deltaX)));
            panelY = Math.max(12, Math.min(height - panelHeight - 12, panelY + (int) Math.round(deltaY)));
            rebuildWidgets();
            return true;
        }
        if (event.button() == 1 && resizingPanel) {
            int availableWidth = Math.max(280, width - panelX - 12);
            int availableHeight = Math.max(260, height - panelY - 12);
            int minimumWidth = Math.min(560, availableWidth);
            int minimumHeight = Math.min(360, availableHeight);
            panelWidth = Math.max(minimumWidth,
                    Math.min(availableWidth, panelWidth + (int) Math.round(deltaX)));
            panelHeight = Math.max(minimumHeight,
                    Math.min(availableHeight, panelHeight + (int) Math.round(deltaY)));
            rebuildWidgets();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingPanel = false;
        resizingPanel = false;
        moduleScrollDragging = false;
        settingsScrollDragging = false;
        if (activeSettingSlider != null) {
            activeSettingSlider = null;
            MorgueConfig.save();
            return true;
        }
        if (activeOpacitySlider >= 0) {
            activeOpacitySlider = -1;
            MorgueConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SettingSlider(AbstractWidget widget, String label, double minimum, double maximum,
                                  String suffix, boolean integer, DoubleSupplier getter, DoubleConsumer setter) {
    }

    private record EntityOption(String name, String id) {
    }
}
