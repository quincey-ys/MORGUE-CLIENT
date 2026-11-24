package dev.morgue.client;

import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.config.MorgueConfig;
import dev.morgue.client.ui.UiSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
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
    private static final int RED = 0xFF67D6C5;
    private static final int RED_SOFT = 0xFF285E5A;
    private static final int BUTTON_IDLE = 0xF0141C28;
    private static final int BUTTON_HOVER = 0xFF243246;
    private static final FontDescription DISPLAY_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("morgue", "display"));

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
    private EditBox searchBox;
    private String searchQuery = "";
    private int moduleScrollOffset;
    private int moduleTotalCount;
    private int moduleVisibleCount;
    private int moduleColumns = 2;
    private final Map<AbstractWidget, Float> hoverProgress = new IdentityHashMap<>();
    private final ArrayDeque<ClickRipple> clickRipples = new ArrayDeque<>();
    private long openedAtNanos = System.nanoTime();
    private long transitionAtNanos = openedAtNanos;
    private long lastFrameNanos;
    private String transitionKey = "";
    private String profileStatus = "Profiles are stored locally in the MORGUE config folder.";

    public MorgueScreen() {
        super(Component.literal("Morgue Refined"));
    }

    @Override
    protected void init() {
        clearWidgets();
        hoverProgress.clear();

        // Full-screen workspace: no floating window, dragging, or resizing.
        panelX = 0;
        panelY = 0;
        panelWidth = Math.max(320, width);
        panelHeight = Math.max(300, height);
        String nextTransitionKey = selectedCategory + ":" + selectedModule + ":" + entityPage;
        if (!nextTransitionKey.equals(transitionKey)) {
            transitionKey = nextTransitionKey;
            transitionAtNanos = System.nanoTime();
        }

        int sidebarX = panelX + 16;
        int sidebarY = panelY + 112;
        for (String category : CATEGORIES) {
            addRenderableWidget(Button.builder(Component.literal(category), button -> {
                selectedCategory = category;
                selectedModule = null;
                searchQuery = "";
                entityPage = false;
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
        } else if ("UI Effects".equals(selectedModule)) {
            buildUiEffectsSettings();
        } else {
            buildPlaceholderSettings();
        }
    }

    private void buildModuleList() {
        int moduleX = panelX + 158;
        int listTop = panelY + 148;
        int listBottom = panelY + panelHeight - 56;
        int rowHeight = 62;
        int buttonHeight = 48;
        int columnGap = 12;
        int contentWidth = Math.max(240, panelWidth - 190);
        moduleColumns = Math.max(1, Math.min(4, (contentWidth + columnGap) / 360));
        int cardWidth = Math.max(220,
                (contentWidth - columnGap * (moduleColumns - 1)) / moduleColumns);
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        List<String> filtered = MODULES.getOrDefault(selectedCategory, List.of()).stream()
                .filter(module -> query.isEmpty()
                        || module.toLowerCase(java.util.Locale.ROOT).contains(query))
                .toList();

        moduleTotalCount = filtered.size();
        int visibleRows = Math.max(1, (listBottom - listTop) / rowHeight);
        moduleVisibleCount = visibleRows * moduleColumns;
        int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
        moduleScrollOffset = Math.max(0, Math.min(moduleScrollOffset, maxOffset));
        moduleScrollOffset -= moduleScrollOffset % moduleColumns;

        int end = Math.min(moduleTotalCount, moduleScrollOffset + moduleVisibleCount);
        for (int i = moduleScrollOffset; i < end; i++) {
            String module = filtered.get(i);
            int relativeIndex = i - moduleScrollOffset;
            int column = relativeIndex % moduleColumns;
            int row = relativeIndex / moduleColumns;
            int cardX = moduleX + column * (cardWidth + columnGap);
            int moduleY = listTop + row * rowHeight;
            boolean viewOnly = "AI Brain".equals(module) || "Build Integrity".equals(module)
                    || "Credits".equals(module) || "World Info".equals(module)
                    || "Client Metadata".equals(module) || "Module List".equals(module)
                    || "Environment".equals(module) || "Visual Settings".equals(module)
                    || "Notifications".equals(module) || "Client Utilities".equals(module)
                    || "Screenshot Privacy".equals(module) || "World Settings".equals(module)
                    || "Item Use".equals(module) || "HUD Editor".equals(module)
                    || "UI Effects".equals(module);
            String state = viewOnly ? "INFO  " : (isModuleEnabled(module) ? "ON  " : "OFF ");
            addRenderableWidget(Button.builder(Component.literal(state + module), button -> {
                if (viewOnly) {
                    selectedModule = module;
                    entityPage = false;
                } else {
                    toggleModule(module);
                }
                rebuildWidgets();
            }).bounds(cardX, moduleY, cardWidth - 76, buttonHeight).build());

            addRenderableWidget(Button.builder(Component.literal("EDIT"), button -> {
                selectedModule = module;
                entityPage = false;
                rebuildWidgets();
            }).bounds(cardX + cardWidth - 68, moduleY, 68, buttonHeight).build());
        }

        if (moduleTotalCount == 0) {
            addRenderableWidget(Button.builder(Component.literal("NO MATCHING MODULES"),
                    button -> {}).bounds(moduleX, listTop, Math.max(200, contentWidth), buttonHeight).build());
        }
    }

    private void buildInventoryHelperSettings() {
        int x = panelX + 150;
        int y = panelY + 112;
        var module = MorgueClient.inventoryHelper();
        addSettingButton(x, y, toggleLabel("Inventory Refill", module.isEnabled()),
                () -> MorgueClient.setInventoryHelperEnabled(!module.isEnabled()));
        addSettingButton(x, y + 36, "Refill At: " + module.getRefillAt() + " items",
                () -> module.setRefillAt(module.getRefillAt() >= 16 ? 1 : module.getRefillAt() + 1));
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
        addSettingButton(x, y + 36, "Range: " + format(module.getRange()),
                () -> module.setRange(module.getRange() >= 6.0 ? 1.0 : module.getRange() + 0.5));
        addSettingButton(x, y + 72, "FOV: " + format(module.getFov()),
                () -> module.setFov(module.getFov() >= 180.0 ? 15.0 : module.getFov() + 15.0));
        addSettingButton(x, y + 108, "Smoothing: " + format(module.getSmoothing()),
                () -> module.setSmoothing(module.getSmoothing() >= 1.0F ? 0.05F : module.getSmoothing() + 0.05F));
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
        addSettingButton(x, y + 36, "Rate: " + module.getClicksPerSecond() + " CPS",
                () -> module.setClicksPerSecond(module.getClicksPerSecond() >= 20
                        ? 1 : module.getClicksPerSecond() + 1));
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
        }).bounds(leftX + 170, panelY + panelHeight - 42, panelWidth - 360, 24).build());

        addBackButton();
    }

    private void buildEntitySelector() {
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
            int end = Math.min(ENTITY_OPTIONS.size(), start + pageSize);
            int leftX = panelX + 150;
            int rightX = panelX + panelWidth / 2 + 12;
            for (int i = start; i < end; i++) {
                EntityOption option = ENTITY_OPTIONS.get(i);
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
                }).bounds(leftX, panelY + panelHeight - 78, 104, 26).build());
            }
            if (end < ENTITY_OPTIONS.size()) {
                addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                    entityPageIndex++;
                    rebuildWidgets();
                }).bounds(rightX + 80, panelY + panelHeight - 78, 90, 26).build());
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
        int end = Math.min(ENTITY_OPTIONS.size(), start + pageSize);
        int leftX = panelX + 150;
        int rightX = panelX + panelWidth / 2 + 12;
        for (int i = start; i < end; i++) {
            EntityOption option = ENTITY_OPTIONS.get(i);
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
            }).bounds(leftX, panelY + panelHeight - 78, 104, 26).build());
        }
        if (end < ENTITY_OPTIONS.size()) {
            addRenderableWidget(Button.builder(Component.literal("NEXT >"), button -> {
                entityPageIndex++;
                rebuildWidgets();
            }).bounds(rightX + 80, panelY + panelHeight - 78, 90, 26).build());
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
        addSettingButton(leftX, y + (leftRow + 1) * 27, "Minimum Target Age: " + module.getMinimumAgeTicks() + " ticks",
                () -> module.setMinimumAgeTicks(module.getMinimumAgeTicks() >= 200 ? 0 : module.getMinimumAgeTicks() + 20));
        addSettingButton(leftX, y + (leftRow + 2) * 27, "Rotation Speed: " + format(module.getRotationSpeed()),
                () -> module.setRotationSpeed(module.getRotationSpeed() >= 0.95 ? 0.1 : module.getRotationSpeed() + 0.1));

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
        addSettingButton(x, y + 36, "Hunger Threshold: " + module.getHungerThreshold(),
                () -> module.setHungerThreshold(module.getHungerThreshold() >= 19
                        ? 1 : module.getHungerThreshold() + 1));
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
        addRenderableWidget(Button.builder(Component.literal("Keyboard defaults: Right Shift / M / K / V"),
                button -> {}).bounds(x, panelY + 156, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("MOUSE 4 / MOUSE 5: SIDE-BUTTON TOGGLES AVAILABLE"),
                button -> {}).bounds(x, panelY + 194, panelWidth - 176, 28).build());
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

    private void buildUiEffectsSettings() {
        int x = panelX + 158;
        int y = panelY + 148;
        addSettingButton(x, y, toggleLabel("Interface Animations", UiSettings.isAnimationsEnabled()),
                () -> UiSettings.setAnimationsEnabled(!UiSettings.isAnimationsEnabled()));
        addSettingButton(x, y + 38, toggleLabel("Custom UI Sounds", UiSettings.isSoundsEnabled()),
                () -> UiSettings.setSoundsEnabled(!UiSettings.isSoundsEnabled()));
        addRenderableWidget(Button.builder(Component.literal(
                "Custom sound files will be loaded from MORGUE's own sound assets."),
                button -> {}).bounds(x, y + 86, Math.max(260, panelWidth - 190), 32).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Animated feedback includes eased hover states, click ripples, and navigation transitions."),
                button -> {}).bounds(x, y + 124, Math.max(260, panelWidth - 190), 32).build());
        addBackButton();
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
            rebuildWidgets();
        }).bounds(panelX + 150, panelY + panelHeight - 42, 160, 24).build());
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
                                      int mouseX, int mouseY) {
        int x = widget.getX();
        int y = widget.getY();
        int w = widget.getWidth();
        int h = widget.getHeight();
        if (w <= 0 || h <= 0) return;

        boolean selected = rawLabel.equals(selectedCategory);
        boolean activeState = rawLabel.startsWith("ON") || rawLabel.startsWith("INFO")
                || rawLabel.contains(": ON");
        boolean hovered = widget.isHoveredOrFocused() || widget.isMouseOver(mouseX, mouseY);
        boolean enabled = widget.isActive();

        int background = !enabled ? 0xFF101722
                : hovered ? BUTTON_HOVER
                : selected ? 0xFF1C2C3A
                : activeState ? 0xFF172A31
                : BUTTON_IDLE;
        int accent = selected || activeState ? RED : (hovered ? 0xFF9FB3C8 : BORDER);
        int textColor = !enabled ? 0xFF667487 : WHITE;

        graphics.fill(x, y, x + w, y + h, background);
        graphics.outline(x, y, w, h, hovered ? 0xFF7B94AE : BORDER);
        graphics.fill(x, y + 4, x + 3, y + h - 4, accent);
        if (activeState) {
            graphics.fill(x + w - 9, y + h / 2 - 3, x + w - 6, y + h / 2 + 3, RED);
        }
        String visibleLabel = rawLabel;
        Component label = displayText(visibleLabel);
        int maxTextWidth = Math.max(8, w - 24);
        while (font.width(label) > maxTextWidth && visibleLabel.length() > 4) {
            visibleLabel = visibleLabel.substring(0, visibleLabel.length() - 2) + "…";
            label = displayText(visibleLabel);
        }
        int textY = y + Math.max(2, (h - 10) / 2);
        graphics.text(font, label, x + 12, textY, textColor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BLACK);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1,
                panelY + panelHeight - 1, PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, BORDER);

        // Clean card-based layout inspired by Ghost Client's multi-column GUI.
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 72, PANEL_LIGHT);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, RED);
        graphics.text(font, displayText("MORGUE"), panelX + 20, panelY + 18, WHITE);
        graphics.text(font, displayText("CLIENT  /  26.3 FABRIC"), panelX + 20, panelY + 36, MUTED);
        graphics.text(font, displayText("RIGHT SHIFT  ·  CLOSE"),
                panelX + panelWidth - 154, panelY + 29, MUTED);

        graphics.fill(panelX + 140, panelY + 82, panelX + 141,
                panelY + panelHeight - 38, BORDER);
        graphics.text(font, displayText("CATEGORIES"), panelX + 18, panelY + 88, MUTED);

        String heading = selectedModule == null
                ? selectedCategory.toUpperCase(java.util.Locale.ROOT)
                : selectedModule.toUpperCase(java.util.Locale.ROOT);
        graphics.text(font, displayText(heading), panelX + 154, panelY + 88, WHITE);

        graphics.fill(panelX + 16, panelY + panelHeight - 34,
                panelX + panelWidth - 16, panelY + panelHeight - 33, BORDER);
        graphics.text(font, displayText("MORGUE  /  LOCAL CONFIGURATION"),
                panelX + 18, panelY + panelHeight - 24, MUTED);

        Map<AbstractWidget, String> buttonLabels = new java.util.IdentityHashMap<>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !(widget instanceof EditBox)) {
                buttonLabels.put(widget, widget.getMessage().getString());
                widget.setMessage(Component.empty());
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        for (var entry : buttonLabels.entrySet()) {
            AbstractWidget widget = entry.getKey();
            String rawLabel = entry.getValue();
            widget.setMessage(Component.literal(rawLabel));
            extractCustomButton(graphics, widget, rawLabel, mouseX, mouseY);
        }

        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget instanceof EditBox) {
                int ex = widget.getX();
                int ey = widget.getY();
                int ew = widget.getWidth();
                int eh = widget.getHeight();
                boolean searchHovered = widget.isMouseOver(mouseX, mouseY);
                graphics.outline(ex - 1, ey - 1, ew + 2, eh + 2,
                        searchHovered ? RED : BORDER);
                graphics.fill(ex + 2, ey + eh - 2, ex + ew - 2, ey + eh - 1,
                        searchHovered ? RED : 0xFF405065);
            }
        }

        if (selectedModule == null && moduleTotalCount > moduleVisibleCount && moduleVisibleCount > 0) {
            int trackX = panelX + panelWidth - 14;
            int trackTop = panelY + 148;
            int trackBottom = panelY + panelHeight - 52;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(18, trackHeight * moduleVisibleCount / moduleTotalCount);
            int maxOffset = Math.max(1, moduleTotalCount - moduleVisibleCount);
            int thumbY = trackTop + (trackHeight - thumbHeight) * moduleScrollOffset / maxOffset;
            graphics.fill(trackX, trackTop, trackX + 2, trackBottom, 0xFF293747);
            graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, RED);
        }
    }

    private int hoveredSearchBorder(AbstractWidget widget, int mouseX, int mouseY) {
        return widget.isMouseOver(mouseX, mouseY) ? RED : 0xFF6586AC;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedModule == null && moduleTotalCount > moduleVisibleCount
                && mouseX >= panelX + 140 && mouseX <= panelX + panelWidth - 10
                && mouseY >= panelY + 132 && mouseY <= panelY + panelHeight - 42
                && verticalAmount != 0.0) {
            int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
            moduleScrollOffset = Math.max(0, Math.min(maxOffset,
                    moduleScrollOffset - (verticalAmount > 0 ? moduleColumns : -moduleColumns)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && UiSettings.isAnimationsEnabled()) {
            clickRipples.addLast(new ClickRipple(event.x(), event.y(), System.nanoTime()));
            while (clickRipples.size() > 16) clickRipples.removeFirst();
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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

    private record EntityOption(String name, String id) {
    }

    private record ClickRipple(double x, double y, long startedAtNanos) {
    }
}
