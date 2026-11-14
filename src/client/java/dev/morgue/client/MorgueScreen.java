package dev.morgue.client;

import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.config.MorgueConfig;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        MODULES.put("Config", List.of("Profiles", "Keybinds", "Theme"));
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
    private boolean draggingPanel;
    private boolean resizingPanel;
    private int moduleScrollOffset;
    private int moduleTotalCount;
    private int moduleVisibleCount;
    private String profileStatus = "Profiles are stored locally in the MORGUE config folder.";

    public MorgueScreen() {
        super(Component.literal("MORGUE-CLIENT"));
    }

    @Override
    protected void init() {
        clearWidgets();

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
    }

    private void buildModuleList() {
        int moduleX = panelX + 150;
        int listTop = panelY + 140;
        int listBottom = panelY + panelHeight - 48;
        int rowHeight = 36;
        int buttonHeight = 28;
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        List<String> filtered = MODULES.getOrDefault(selectedCategory, List.of()).stream()
                .filter(module -> query.isEmpty()
                        || module.toLowerCase(java.util.Locale.ROOT).contains(query))
                .toList();

        moduleTotalCount = filtered.size();
        moduleVisibleCount = Math.min(4, Math.max(1, (listBottom - listTop - buttonHeight) / rowHeight + 1));
        int maxOffset = Math.max(0, moduleTotalCount - moduleVisibleCount);
        moduleScrollOffset = Math.max(0, Math.min(moduleScrollOffset, maxOffset));

        int end = Math.min(moduleTotalCount, moduleScrollOffset + moduleVisibleCount);
        for (int i = moduleScrollOffset; i < end; i++) {
            String module = filtered.get(i);
            int moduleY = listTop + (i - moduleScrollOffset) * rowHeight;
            boolean viewOnly = "AI Brain".equals(module) || "Build Integrity".equals(module)
                    || "Credits".equals(module) || "World Info".equals(module)
                    || "Client Metadata".equals(module) || "Module List".equals(module)
                    || "Environment".equals(module) || "Visual Settings".equals(module)
                    || "Notifications".equals(module) || "Client Utilities".equals(module)
                    || "Screenshot Privacy".equals(module) || "World Settings".equals(module)
                    || "Item Use".equals(module) || "HUD Editor".equals(module);
            addRenderableWidget(Button.builder(
                    Component.literal((viewOnly ? "LIVE  " : (isModuleEnabled(module) ? "ON   " : "OFF   ")) + module),
                    button -> {
                        if (viewOnly) {
                            selectedModule = module;
                            entityPage = false;
                        } else {
                            toggleModule(module);
                        }
                        rebuildWidgets();
                    }
            ).bounds(moduleX, moduleY, Math.max(150, panelWidth - 275), buttonHeight).build());

            addRenderableWidget(Button.builder(Component.literal("SETTINGS"), button -> {
                selectedModule = module;
                entityPage = false;
                rebuildWidgets();
            }).bounds(panelX + panelWidth - 112, moduleY, 88, buttonHeight).build());
        }

        if (moduleTotalCount == 0) {
            addRenderableWidget(Button.builder(Component.literal("NO MATCHING MODULES"),
                    button -> {}).bounds(moduleX, listTop, Math.max(150, panelWidth - 275), buttonHeight).build());
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

        addBackButton();
    }

    private void buildAiBrainSettings() {
        int x = panelX + 150;
        int y = panelY + 108;
        var assessment = dev.morgue.client.ai.TargetIntelligence.getLatestAssessment();

        String activeCombat = MorgueClient.maceHitter().isEnabled() ? "Mace Hitter"
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
        addRenderableWidget(Button.builder(Component.literal(
                "Overlays use the standard Fabric HUD layer and remain local to your client."),
                button -> {}).bounds(x, y + 78, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal(
                "Positions are currently fixed to the upper corners; drag-and-drop editing is not implemented."),
                button -> {}).bounds(x, y + 114, panelWidth - 176, 28).build());
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
        addRenderableWidget(Button.builder(Component.literal("MORGUE defaults: Right Shift / M / K / V"),
                button -> {}).bounds(x, panelY + 156, panelWidth - 176, 28).build());
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
        // Keep the live world visible; MORGUE draws only its own translucent panels.
    }

    private static final FontDescription DOUBLE_STRUCK_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("minecraft", "default"));

    private static String doubleStruck(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint >= 'A' && codePoint <= 'Z') {
                int index = codePoint - 'A';
                // Mathematical double-struck has legacy letter code points for several capitals.
                int[] capitals = {
                        0x1D538, 0x1D539, 0x2102, 0x1D53B, 0x1D53C, 0x1D53D, 0x1D53E,
                        0x210D, 0x1D53F, 0x1D540, 0x1D541, 0x1D542, 0x1D543, 0x1D544,
                        0x1D546, 0x2119, 0x211A, 0x211D, 0x1D54A, 0x1D54B, 0x1D54C,
                        0x1D54D, 0x1D54E, 0x1D54F, 0x1D550, 0x2124
                };
                result.appendCodePoint(capitals[index]);
            } else if (codePoint >= 'a' && codePoint <= 'z') {
                int index = codePoint - 'a';
                int[] lowercase = {
                        0x1D552, 0x1D553, 0x1D554, 0x1D555, 0x1D556, 0x1D557, 0x1D558,
                        0x1D559, 0x1D55A, 0x1D55B, 0x1D55C, 0x1D55D, 0x1D55E, 0x1D55F,
                        0x1D560, 0x1D561, 0x1D562, 0x1D563, 0x1D564, 0x1D565, 0x1D566,
                        0x1D567, 0x1D568, 0x1D569, 0x1D56A, 0x1D56B
                };
                result.appendCodePoint(lowercase[index]);
            } else if (codePoint >= '0' && codePoint <= '9') {
                result.appendCodePoint(0x1D7D8 + codePoint - '0');
            } else {
                result.appendCodePoint(codePoint);
            }
        }
        return result.toString();
    }

    private static Component displayText(String value) {
        return Component.literal(doubleStruck(value))
                .withStyle(style -> style.withFont(DOUBLE_STRUCK_FONT).withBold(false));
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
        graphics.fill(x, y, x + w, y + h, background);
        graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1, hovered ? 0x263D5E87 : 0x182A4263);
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

        String visibleLabel = rawLabel;
        Component label = displayText(visibleLabel);
        int maxTextWidth = Math.max(8, w - 30);
        while (font.width(label) > maxTextWidth && visibleLabel.length() > 4) {
            visibleLabel = visibleLabel.substring(0, visibleLabel.length() - 2) + "…";
            label = displayText(visibleLabel);
        }
        int textY = y + Math.max(2, (h - 10) / 2);
        graphics.text(font, label, x + 10, textY, textColor);
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
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, BLACK);
        graphics.fill(panelX + 1, panelY + 3, panelX + panelWidth - 1, panelY + panelHeight - 1, PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, BORDER);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 3, RED);
        graphics.fill(panelX + panelWidth - 92, panelY + 3, panelX + panelWidth - 16,
                panelY + 4, RED_SOFT);
        drawAnimatedPanelEffects(graphics);

        graphics.fill(panelX + 1, panelY + 4, panelX + 132, panelY + 72, PANEL_LIGHT);
        graphics.text(font, displayText("M"), panelX + 20, panelY + 14, WHITE);
        graphics.text(font, displayText("MORGUE"), panelX + 49, panelY + 17, WHITE);
        graphics.text(font, displayText("CLIENT  /  26.3"), panelX + 49, panelY + 32, MUTED);

        graphics.fill(panelX + 145, panelY + 18, panelX + panelWidth - 16, panelY + 69, PANEL_LIGHT);
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
        graphics.text(font, displayText("RIGHT SHIFT  •  CLOSE"),
                panelX + panelWidth - 153, panelY + panelHeight - 24, MUTED);

        // Suppress vanilla button captions before Minecraft renders widgets. We restore
        // them immediately afterward so input, accessibility, and click behavior stay intact.
        Map<AbstractWidget, String> buttonLabels = new java.util.IdentityHashMap<>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !(widget instanceof EditBox)) {
                buttonLabels.put(widget, widget.getMessage().getString());
                widget.setMessage(Component.empty());
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Paint MORGUE controls once, above the now-captionless vanilla widget layer.
        for (var entry : buttonLabels.entrySet()) {
            AbstractWidget widget = entry.getKey();
            String rawLabel = entry.getValue();
            widget.setMessage(Component.literal(rawLabel));
            extractCustomButton(graphics, widget, rawLabel, mouseX, mouseY);
        }

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

        // A compact scroll track communicates that more modules are available.
        if (selectedModule == null && moduleTotalCount > moduleVisibleCount && moduleVisibleCount > 0) {
            int trackX = panelX + panelWidth - 17;
            int trackTop = panelY + 140;
            int trackBottom = panelY + panelHeight - 48;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(18, trackHeight * moduleVisibleCount / moduleTotalCount);
            int maxOffset = Math.max(1, moduleTotalCount - moduleVisibleCount);
            int thumbY = trackTop + (trackHeight - thumbHeight) * moduleScrollOffset / maxOffset;
            graphics.fill(trackX, trackTop, trackX + 2, trackBottom, 0x805778A2);
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
            int step = Math.max(1, Math.abs((int) Math.signum(verticalAmount)));
            moduleScrollOffset = Math.max(0, Math.min(maxOffset,
                    moduleScrollOffset - (verticalAmount > 0 ? step : -step)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            double mouseX = event.x();
            double mouseY = event.y();

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
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
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
}
