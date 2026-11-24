package dev.morgue.client.config;

import dev.morgue.client.MorgueClient;
import dev.morgue.client.ClientNotifications;
import dev.morgue.client.MorgueHud;
import dev.morgue.client.ui.UiSettings;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.player.AutoTotemModule;
import dev.morgue.client.modules.movement.SprintModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small dependency-free persistent configuration for MORGUE's first modules.
 */
public final class MorgueConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("morgue-client.properties");

    private MorgueConfig() {
    }

    private static Path profileFile(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("Invalid profile name");
        }
        return FabricLoader.getInstance().getConfigDir()
                .resolve("morgue-client").resolve("profiles").resolve(name + ".properties");
    }

    public static boolean exportProfile(String name) {
        try {
            save();
            Path destination = profileFile(name);
            Files.createDirectories(destination.getParent());
            Files.copy(FILE, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[MORGUE] Could not export profile: " + exception.getMessage());
            return false;
        }
    }

    public static boolean importProfile(String name) {
        try {
            Path source = profileFile(name);
            if (!Files.isRegularFile(source)) return false;
            Files.copy(source, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            load();
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[MORGUE] Could not import profile: " + exception.getMessage());
            return false;
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException exception) {
            System.err.println("[MORGUE] Could not read config: " + exception.getMessage());
            return;
        }

        UiSettings.setAnimationsEnabled(bool(values, "ui.animations.enabled", true));
        UiSettings.setSoundsEnabled(bool(values, "ui.sounds.enabled", false));

        put(values, "ui.animations.enabled", UiSettings.isAnimationsEnabled());
        put(values, "ui.sounds.enabled", UiSettings.isSoundsEnabled());

        MaceHitterModule mace = MorgueClient.maceHitter();
        mace.setEnabled(bool(values, "mace.enabled", false));
        mace.setRange(decimal(values, "mace.range", 3.2));
        mace.setFov(decimal(values, "mace.fov", 180.0));
        mace.setMinimumFallDistance(decimal(values, "mace.minimumFallDistance", 1.5));
        mace.setAttackDelayMillis(integer(values, "mace.attackDelayMillis", 0));
        mace.setRotate(bool(values, "mace.rotate", true));
        mace.setAutoSwap(bool(values, "mace.autoSwap", true));
        mace.setAutoJump(bool(values, "mace.autoJump", false));
        mace.setFallingOnly(bool(values, "mace.fallingOnly", true));
        mace.setShieldStun(bool(values, "mace.shieldStun", false));
        mace.setRequireMace(bool(values, "mace.requireMace", true));
        mace.setIgnoreWalls(bool(values, "mace.ignoreWalls", false));
        mace.setFovCheck(bool(values, "mace.fovCheck", true));
        mace.setTeamsCheck(bool(values, "mace.teamsCheck", true));
        mace.setIgnoreCooldown(bool(values, "mace.ignoreCooldown", false));
        mace.setPriority(enumValue(values, "mace.priority", MaceHitterModule.Priority.class,
                MaceHitterModule.Priority.CLOSEST));
        mace.setTargetMode(enumValue(values, "mace.targetMode", MaceHitterModule.TargetMode.class,
                MaceHitterModule.TargetMode.PLAYERS));

        for (String id : mace.getSelectedEntityTypes()) {
            mace.setEntityTypeSelected(id, false);
        }
        String selectedTypes = values.getProperty("mace.entityTypes", "minecraft:player");
        for (String id : selectedTypes.split(",")) {
            mace.setEntityTypeSelected(id.trim(), true);
        }

        ClientNotifications.setEnabled(bool(values, "notifications.enabled", true));
        MorgueClient.setAimAssistEnabled(bool(values, "combat.aimAssist.enabled", false));
        MorgueClient.setClickAssistEnabled(bool(values, "combat.clickAssist.enabled", false));
        MorgueClient.clickAssist().setClicksPerSecond((int) integer(values, "combat.clickAssist.cps", 8));
        MorgueHud.setModuleListEnabled(bool(values, "hud.moduleList", false));
        MorgueHud.setTargetInfoEnabled(bool(values, "hud.targetInfo", false));
        MorgueHud.setHitMarkerEnabled(bool(values, "hud.hitMarker", false));
        MorgueClient.setSprintEnabled(bool(values, "movement.sprint.enabled", false));
        MorgueClient.setSafeWalkEnabled(bool(values, "movement.safeWalk.enabled", false));
        MorgueClient.setInventoryHelperEnabled(bool(values, "player.inventoryHelper.enabled", false));
        MorgueClient.setAutoEatEnabled(bool(values, "player.autoEat.enabled", false));
        MorgueClient.autoEat().setHungerThreshold((int) integer(values, "player.autoEat.hungerThreshold", 16));
        MorgueClient.inventoryHelper().setRefillAt((int) integer(values, "player.inventoryHelper.refillAt", 1));

        AutoTotemModule totem = MorgueClient.autoTotem();
        totem.setEnabled(bool(values, "totem.enabled", false));
        totem.setMode(enumValue(values, "totem.mode", AutoTotemModule.Mode.class,
                AutoTotemModule.Mode.SMART));
        totem.setSearchPriority(enumValue(values, "totem.searchPriority",
                AutoTotemModule.SearchPriority.class, AutoTotemModule.SearchPriority.HOTBAR_FIRST));
        totem.setSwitchDelayMillis(integer(values, "totem.switchDelayMillis", 0));
        totem.setPauseWhileUsingItem(bool(values, "totem.pauseWhileUsingItem", true));
        totem.setActivationHealth((float) decimal(values, "totem.activationHealth", 20.0));
        totem.setPredictRecentDamage(bool(values, "totem.predictRecentDamage", true));
        totem.setPredictFallDamage(bool(values, "totem.predictFallDamage", true));
        totem.setPredictNearbyThreats(bool(values, "totem.predictNearbyThreats", true));
        totem.setKeepTotemEquipped(bool(values, "totem.keepTotemEquipped", true));
        totem.loadRefillState(bool(values, "totem.refillArmed", false),
                values.getProperty("totem.refillPlayerUuid", ""));

        KillAuraModule aura = MorgueClient.killAura();
        aura.setEnabled(bool(values, "aura.enabled", false));
        aura.setRange(decimal(values, "aura.range", 3.2));
        aura.setFov(decimal(values, "aura.fov", 180.0));
        aura.setAttackDelayMillis(integer(values, "aura.attackDelayMillis", 0));
        aura.setRotationSpeed(decimal(values, "aura.rotationSpeed", 0.55));
        aura.setRotate(bool(values, "aura.rotate", true));
        aura.setAutoSword(bool(values, "aura.autoSword", true));
        aura.setSwitchBack(bool(values, "aura.switchBack", true));
        aura.setShieldBreak(bool(values, "aura.shieldBreak", false));
        aura.setIgnoreWalls(bool(values, "aura.ignoreWalls", false));
        aura.setTeamsCheck(bool(values, "aura.teamsCheck", true));
        aura.setFovCheck(bool(values, "aura.fovCheck", true));
        aura.setIgnoreHurtTime(bool(values, "aura.ignoreHurtTime", false));
        aura.setMinimumAgeTicks((int) integer(values, "aura.minimumAgeTicks", 0));
        aura.setCriticalOnly(bool(values, "aura.criticalOnly", false));
        aura.setPauseOnThrowables(bool(values, "aura.pauseOnThrowables", true));
        aura.setAttackSound(bool(values, "aura.attackSound", false));
        aura.setTargetMode(enumValue(values, "aura.targetMode", KillAuraModule.TargetMode.class,
                KillAuraModule.TargetMode.PLAYERS));
        for (String id : aura.getSelectedEntityTypes()) {
            aura.setEntityTypeSelected(id, false);
        }
        String auraSelectedTypes = values.getProperty("aura.entityTypes", "minecraft:player");
        for (String id : auraSelectedTypes.split(",")) {
            aura.setEntityTypeSelected(id.trim(), true);
        }
        aura.setPriority(enumValue(values, "aura.priority", KillAuraModule.Priority.class,
                KillAuraModule.Priority.CLOSEST));

        // Both modules may remain enabled; CombatActionCoordinator arbitrates attacks.
    }

    public static void save() {
        Properties values = new Properties();

        MaceHitterModule mace = MorgueClient.maceHitter();
        put(values, "mace.enabled", mace.isEnabled());
        put(values, "mace.range", mace.getRange());
        put(values, "mace.fov", mace.getFov());
        put(values, "mace.minimumFallDistance", mace.getMinimumFallDistance());
        put(values, "mace.attackDelayMillis", mace.getAttackDelayMillis());
        put(values, "mace.rotate", mace.isRotate());
        put(values, "mace.autoSwap", mace.isAutoSwap());
        put(values, "mace.autoJump", mace.isAutoJump());
        put(values, "mace.fallingOnly", mace.isFallingOnly());
        put(values, "mace.shieldStun", mace.isShieldStun());
        put(values, "mace.requireMace", mace.isRequireMace());
        put(values, "mace.ignoreWalls", mace.isIgnoreWalls());
        put(values, "mace.fovCheck", mace.isFovCheck());
        put(values, "mace.teamsCheck", mace.isTeamsCheck());
        put(values, "mace.ignoreCooldown", mace.isIgnoreCooldown());
        put(values, "mace.priority", mace.getPriority());
        put(values, "mace.targetMode", mace.getTargetMode());
        put(values, "mace.entityTypes", String.join(",", mace.getSelectedEntityTypes()));

        put(values, "notifications.enabled", ClientNotifications.isEnabled());
        put(values, "combat.aimAssist.enabled", MorgueClient.aimAssist().isEnabled());
        put(values, "combat.clickAssist.enabled", MorgueClient.clickAssist().isEnabled());
        put(values, "combat.clickAssist.cps", MorgueClient.clickAssist().getClicksPerSecond());
        put(values, "hud.moduleList", MorgueHud.isModuleListEnabled());
        put(values, "hud.targetInfo", MorgueHud.isTargetInfoEnabled());
        put(values, "hud.hitMarker", MorgueHud.isHitMarkerEnabled());
        put(values, "movement.sprint.enabled", MorgueClient.sprint().isEnabled());
        put(values, "movement.safeWalk.enabled", MorgueClient.safeWalk().isEnabled());
        put(values, "player.inventoryHelper.enabled", MorgueClient.inventoryHelper().isEnabled());
        put(values, "player.inventoryHelper.refillAt", MorgueClient.inventoryHelper().getRefillAt());
        put(values, "player.autoEat.enabled", MorgueClient.autoEat().isEnabled());
        put(values, "player.autoEat.hungerThreshold", MorgueClient.autoEat().getHungerThreshold());

        AutoTotemModule totem = MorgueClient.autoTotem();
        put(values, "totem.enabled", totem.isEnabled());
        put(values, "totem.mode", totem.getMode());
        put(values, "totem.searchPriority", totem.getSearchPriority());
        put(values, "totem.switchDelayMillis", totem.getSwitchDelayMillis());
        put(values, "totem.pauseWhileUsingItem", totem.isPauseWhileUsingItem());
        put(values, "totem.activationHealth", totem.getActivationHealth());
        put(values, "totem.predictRecentDamage", totem.isPredictRecentDamage());
        put(values, "totem.predictFallDamage", totem.isPredictFallDamage());
        put(values, "totem.predictNearbyThreats", totem.isPredictNearbyThreats());
        put(values, "totem.keepTotemEquipped", totem.isKeepTotemEquipped());
        put(values, "totem.refillArmed", totem.isRefillArmed());
        put(values, "totem.refillPlayerUuid", totem.getRefillPlayerUuid());

        KillAuraModule aura = MorgueClient.killAura();
        put(values, "aura.enabled", aura.isEnabled());
        put(values, "aura.range", aura.getRange());
        put(values, "aura.fov", aura.getFov());
        put(values, "aura.attackDelayMillis", aura.getAttackDelayMillis());
        put(values, "aura.rotationSpeed", aura.getRotationSpeed());
        put(values, "aura.rotate", aura.isRotate());
        put(values, "aura.autoSword", aura.isAutoSword());
        put(values, "aura.switchBack", aura.isSwitchBack());
        put(values, "aura.shieldBreak", aura.isShieldBreak());
        put(values, "aura.ignoreWalls", aura.isIgnoreWalls());
        put(values, "aura.teamsCheck", aura.isTeamsCheck());
        put(values, "aura.fovCheck", aura.isFovCheck());
        put(values, "aura.ignoreHurtTime", aura.isIgnoreHurtTime());
        put(values, "aura.minimumAgeTicks", aura.getMinimumAgeTicks());
        put(values, "aura.criticalOnly", aura.isCriticalOnly());
        put(values, "aura.pauseOnThrowables", aura.isPauseOnThrowables());
        put(values, "aura.attackSound", aura.isAttackSound());
        put(values, "aura.targetMode", aura.getTargetMode());
        put(values, "aura.entityTypes", String.join(",", aura.getSelectedEntityTypes()));
        put(values, "aura.priority", aura.getPriority());

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                values.store(writer, "MORGUE Client settings");
            }
        } catch (IOException exception) {
            System.err.println("[MORGUE] Could not save config: " + exception.getMessage());
        }
    }

    private static void put(Properties values, String key, Object value) {
        values.setProperty(key, String.valueOf(value));
    }

    private static boolean bool(Properties values, String key, boolean fallback) {
        return Boolean.parseBoolean(values.getProperty(key, String.valueOf(fallback)));
    }

    private static long integer(Properties values, String key, long fallback) {
        try {
            return Long.parseLong(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double decimal(Properties values, String key, double fallback) {
        try {
            return Double.parseDouble(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E enumValue(Properties values, String key, Class<E> type, E fallback) {
        String requested = values.getProperty(key, fallback.name()).trim();
        E[] constants = type.getEnumConstants();
        if (constants == null) {
            System.err.println("[MORGUE] Config enum " + type.getName()
                    + " is unavailable; using default for " + key + ".");
            return fallback;
        }
        for (E constant : constants) {
            if (constant.name().equalsIgnoreCase(requested)) {
                return constant;
            }
        }
        return fallback;
    }
}
