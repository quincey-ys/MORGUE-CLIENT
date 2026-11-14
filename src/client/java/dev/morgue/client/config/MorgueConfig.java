package dev.morgue.client.config;

import dev.morgue.client.MorgueClient;
import dev.morgue.client.modules.combat.KillAuraModule;
import dev.morgue.client.modules.combat.MaceHitterModule;
import dev.morgue.client.modules.player.AutoTotemModule;
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

        AutoTotemModule totem = MorgueClient.autoTotem();
        totem.setEnabled(bool(values, "totem.enabled", false));
        totem.setMode(enumValue(values, "totem.mode", AutoTotemModule.Mode.class,
                AutoTotemModule.Mode.SMART));
        totem.setSearchPriority(enumValue(values, "totem.searchPriority",
                AutoTotemModule.SearchPriority.class, AutoTotemModule.SearchPriority.HOTBAR_FIRST));
        totem.setSwitchDelayMillis(integer(values, "totem.switchDelayMillis", 0));
        totem.setPauseWhileUsingItem(bool(values, "totem.pauseWhileUsingItem", true));

        KillAuraModule aura = MorgueClient.killAura();
        aura.setEnabled(bool(values, "aura.enabled", false));
        aura.setRange(decimal(values, "aura.range", 3.2));
        aura.setFov(decimal(values, "aura.fov", 180.0));
        aura.setAttackDelayMillis(integer(values, "aura.attackDelayMillis", 0));
        aura.setRotate(bool(values, "aura.rotate", true));
        aura.setAutoSword(bool(values, "aura.autoSword", true));
        aura.setSwitchBack(bool(values, "aura.switchBack", true));
        aura.setShieldBreak(bool(values, "aura.shieldBreak", false));
        aura.setIgnoreWalls(bool(values, "aura.ignoreWalls", false));
        aura.setTeamsCheck(bool(values, "aura.teamsCheck", true));
        aura.setFovCheck(bool(values, "aura.fovCheck", true));
        aura.setTargetMode(enumValue(values, "aura.targetMode", KillAuraModule.TargetMode.class,
                KillAuraModule.TargetMode.PLAYERS));
        aura.setPriority(enumValue(values, "aura.priority", KillAuraModule.Priority.class,
                KillAuraModule.Priority.CLOSEST));

        // Both combat modules can be configured, but only one may own attacks at a time.
        if (mace.isEnabled() && aura.isEnabled()) {
            aura.setEnabled(false);
        }
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

        AutoTotemModule totem = MorgueClient.autoTotem();
        put(values, "totem.enabled", totem.isEnabled());
        put(values, "totem.mode", totem.getMode());
        put(values, "totem.searchPriority", totem.getSearchPriority());
        put(values, "totem.switchDelayMillis", totem.getSwitchDelayMillis());
        put(values, "totem.pauseWhileUsingItem", totem.isPauseWhileUsingItem());

        KillAuraModule aura = MorgueClient.killAura();
        put(values, "aura.enabled", aura.isEnabled());
        put(values, "aura.range", aura.getRange());
        put(values, "aura.fov", aura.getFov());
        put(values, "aura.attackDelayMillis", aura.getAttackDelayMillis());
        put(values, "aura.rotate", aura.isRotate());
        put(values, "aura.autoSword", aura.isAutoSword());
        put(values, "aura.switchBack", aura.isSwitchBack());
        put(values, "aura.shieldBreak", aura.isShieldBreak());
        put(values, "aura.ignoreWalls", aura.isIgnoreWalls());
        put(values, "aura.teamsCheck", aura.isTeamsCheck());
        put(values, "aura.fovCheck", aura.isFovCheck());
        put(values, "aura.targetMode", aura.getTargetMode());
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
