package dev.morgue.client;

import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.config.MorgueConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MorgueScreen extends Screen {
    private static final int BLACK = 0xFF080808;
    private static final int PANEL = 0xFF111111;
    private static final int PANEL_LIGHT = 0xFF191919;
    private static final int BORDER = 0xFF303030;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFF888888;
    private static final int RED = 0xFF8E2525;

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
        MODULES.put("Misc", List.of("Resource Pack Manager", "Notifications", "Client Utilities"));
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
        } else if ("Mace Hitter".equals(selectedModule)) {
            buildMaceHitterSettings();
        } else if ("Auto Totem".equals(selectedModule)) {
            buildAutoTotemSettings();
        } else if ("KillAura".equals(selectedModule)) {
            buildKillAuraSettings();
        } else if ("AI Brain".equals(selectedModule)) {
            buildAiBrainSettings();
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
        int moduleY = panelY + 140;
        String query = searchQuery.toLowerCase(java.util.Locale.ROOT);
        int visibleCount = 0;
        for (String module : MODULES.getOrDefault(selectedCategory, List.of())) {
            if (!query.isEmpty() && !module.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                continue;
            }
            boolean viewOnly = "AI Brain".equals(module) || "Build Integrity".equals(module);
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
            ).bounds(moduleX, moduleY, Math.max(150, panelWidth - 275), 28).build());

            addRenderableWidget(Button.builder(Component.literal("SETTINGS"), button -> {
                selectedModule = module;
                entityPage = false;
                rebuildWidgets();
            }).bounds(panelX + panelWidth - 112, moduleY, 88, 28).build());

            moduleY += 36;
            visibleCount++;
        }

        if (visibleCount == 0) {
            addRenderableWidget(Button.builder(Component.literal("NO MATCHING MODULES"),
                    button -> {}).bounds(moduleX, moduleY, Math.max(150, panelWidth - 275), 28).build());
        }
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

        addSettingButton(leftX, y + leftRow++ * 34, "Range: " + format(module.getRange()) + "  (+0.2)",
                () -> module.setRange(module.getRange() >= 6.0 ? 1.0 : module.getRange() + 0.2));
        addSettingButton(leftX, y + leftRow++ * 34, "FOV: " + format(module.getFov()) + "  (+15)",
                () -> module.setFov(module.getFov() >= 360.0 ? 15.0 : module.getFov() + 15.0));
        addSettingButton(leftX, y + leftRow++ * 34, "Attack Delay: " + module.getAttackDelayMillis() + " ms",
                () -> module.setAttackDelayMillis(module.getAttackDelayMillis() >= 200 ? 0 : module.getAttackDelayMillis() + 10));
        addSettingButton(leftX, y + leftRow++ * 34, "Priority: " + module.getPriority(),
                () -> module.setPriority(switch (module.getPriority()) {
                    case CLOSEST -> dev.morgue.client.modules.combat.KillAuraModule.Priority.LOWEST_HEALTH;
                    case LOWEST_HEALTH -> dev.morgue.client.modules.combat.KillAuraModule.Priority.THREAT;
                    case THREAT -> dev.morgue.client.modules.combat.KillAuraModule.Priority.CLOSEST;
                }));
        addSettingButton(leftX, y + leftRow++ * 34, "Targets: " + module.getTargetMode(),
                () -> module.setTargetMode(switch (module.getTargetMode()) {
                    case PLAYERS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.HOSTILE_MOBS;
                    case HOSTILE_MOBS -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.ALL_LIVING;
                    case ALL_LIVING -> dev.morgue.client.modules.combat.KillAuraModule.TargetMode.PLAYERS;
                }));

        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Rotation", module.isRotate()),
                () -> module.setRotate(!module.isRotate()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Auto Sword", module.isAutoSword()),
                () -> module.setAutoSword(!module.isAutoSword()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Switch Back", module.isSwitchBack()),
                () -> module.setSwitchBack(!module.isSwitchBack()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Shield Break", module.isShieldBreak()),
                () -> module.setShieldBreak(!module.isShieldBreak()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Ignore Walls", module.isIgnoreWalls()),
                () -> module.setIgnoreWalls(!module.isIgnoreWalls()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("FOV Check", module.isFovCheck()),
                () -> module.setFovCheck(!module.isFovCheck()));
        addSettingButton(rightX, y + rightRow++ * 34, toggleLabel("Team Check", module.isTeamsCheck()),
                () -> module.setTeamsCheck(!module.isTeamsCheck()));

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
        addSettingButton(rightX, y, toggleLabel("Pause While Using Item", module.isPauseWhileUsingItem()),
                () -> module.setPauseWhileUsingItem(!module.isPauseWhileUsingItem()));

        addRenderableWidget(Button.builder(Component.literal("AUTO TOTEM STATUS: " + (module.isEnabled() ? "ON" : "OFF")),
                button -> {
                    MorgueClient.setAutoTotemEnabled(!module.isEnabled());
                    ENABLED.put("Auto Totem", MorgueClient.autoTotem().isEnabled());
                    MorgueConfig.save();
                    rebuildWidgets();
                }).bounds(rightX, y + 36, panelWidth / 2 - 170, 27).build());

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
        addRenderableWidget(Button.builder(Component.literal("ACTIVE PROFILE: DEFAULT"),
                button -> {}).bounds(x, panelY + 116, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Settings save automatically to config/morgue-client.properties."),
                button -> {}).bounds(x, panelY + 152, panelWidth - 176, 28).build());
        addRenderableWidget(Button.builder(Component.literal("Named profile import/export is not implemented yet."),
                button -> {}).bounds(x, panelY + 188, panelWidth - 176, 28).build());
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
        String heading = selectedModule == null ? selectedCategory.toUpperCase() : selectedModule.toUpperCase();
        graphics.text(font, Component.literal(heading), panelX + 161, panelY + 27, WHITE);
        graphics.text(font, Component.literal(selectedModule == null
                ? "INDEPENDENT CLIENT  //  BUILD 0.1.0"
                : entityPage ? "TARGET ENTITY FILTER" : "MODULE SETTINGS"), panelX + 161, panelY + 43, MUTED);

        graphics.fill(panelX + 136, panelY + 82, panelX + 137, panelY + panelHeight - 16, BORDER);
        graphics.text(font, Component.literal(selectedModule == null ? "MODULES" : "CONFIGURATION"),
                panelX + 151, panelY + 86, MUTED);

        graphics.fill(panelX + 16, panelY + panelHeight - 34, panelX + panelWidth - 16,
                panelY + panelHeight - 33, BORDER);
        graphics.fill(panelX + panelWidth - 14, panelY + panelHeight - 3,
                panelX + panelWidth - 3, panelY + panelHeight - 2, RED);
        graphics.fill(panelX + panelWidth - 9, panelY + panelHeight - 8,
                panelX + panelWidth - 3, panelY + panelHeight - 3, BORDER);
        graphics.text(font, Component.literal("MORGUE-CLIENT"), panelX + 17,
                panelY + panelHeight - 24, MUTED);
        graphics.text(font, Component.literal("RIGHT SHIFT  •  CLOSE"),
                panelX + panelWidth - 153, panelY + panelHeight - 24, MUTED);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
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
