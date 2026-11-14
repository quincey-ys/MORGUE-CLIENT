package dev.morgue.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Small optional MORGUE HUD panels, rendered through Fabric's HUD layer API. */
public final class MorgueHud {
    private static boolean moduleListEnabled;
    private static boolean targetInfoEnabled;
    private static boolean hitMarkerEnabled;
    private static long hitMarkerUntil;
    private static boolean registered;

    private MorgueHud() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("morgue", "client_hud"),
                (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        int left = 8;
        int top = 8;
        if (moduleListEnabled) {
            String[] lines = {
                    "MORGUE",
                    "MACE: " + state(MorgueClient.maceHitter().isEnabled()),
                    "AURA: " + state(MorgueClient.killAura().isEnabled()),
                    "TOTEM: " + state(MorgueClient.autoTotem().isEnabled()),
                    "SPRINT: " + state(MorgueClient.sprint().isEnabled()),
                    "SAFE WALK: " + state(MorgueClient.safeWalk().isEnabled()),
                    "REFILL: " + state(MorgueClient.inventoryHelper().isEnabled()),
                    "AUTO EAT: " + state(MorgueClient.autoEat().isEnabled()),
                    "AIM ASSIST: " + state(MorgueClient.aimAssist().isEnabled()),
                    "CLICK ASSIST: " + state(MorgueClient.clickAssist().isEnabled())
            };
            int height = lines.length * 12 + 8;
            graphics.fill(left, top, left + 124, top + height, 0xB8050914);
            graphics.fill(left, top, left + 2, top + height, 0xFF80AFFF);
            for (int i = 0; i < lines.length; i++) {
                graphics.text(client.font, Component.literal(lines[i]),
                        left + 7, top + 5 + i * 12, i == 0 ? 0xFFE8F0FF : 0xFF91A5C1);
            }
        }

        if (hitMarkerEnabled && System.nanoTime() < hitMarkerUntil) {
            int centerX = client.getWindow().getGuiScaledWidth() / 2;
            int centerY = client.getWindow().getGuiScaledHeight() / 2;
            for (int i = 0; i < 5; i++) {
                graphics.fill(centerX - 7 + i, centerY - 7 + i,
                        centerX - 4 + i, centerY - 4 + i, 0xFFB8D6FF);
                graphics.fill(centerX + 4 - i, centerY - 7 + i,
                        centerX + 7 - i, centerY - 4 + i, 0xFFB8D6FF);
                graphics.fill(centerX - 7 + i, centerY + 4 - i,
                        centerX - 4 + i, centerY + 7 - i, 0xFFB8D6FF);
                graphics.fill(centerX + 4 - i, centerY + 4 - i,
                        centerX + 7 - i, centerY + 7 - i, 0xFFB8D6FF);
            }
        }

        if (targetInfoEnabled) {
            LivingEntity target = MorgueClient.killAura().getLastTarget();
            if (target != null && target.isAlive() && !target.isRemoved()) {
                int x = client.getWindow().getGuiScaledWidth() - 154;
                int y = 8;
                graphics.fill(x, y, x + 146, y + 38, 0xB8050914);
                graphics.fill(x, y, x + 2, y + 38, 0xFF80AFFF);
                graphics.text(client.font, Component.literal(target.getName().getString()),
                        x + 7, y + 6, 0xFFE8F0FF);
                graphics.text(client.font, Component.literal(String.format(java.util.Locale.ROOT,
                        "HP %.1f / %.1f", target.getHealth(), target.getMaxHealth())),
                        x + 7, y + 21, 0xFF91A5C1);
            }
        }
    }

    private static String state(boolean enabled) { return enabled ? "ON" : "OFF"; }
    public static boolean isModuleListEnabled() { return moduleListEnabled; }
    public static void setModuleListEnabled(boolean value) { moduleListEnabled = value; }
    public static boolean isHitMarkerEnabled() { return hitMarkerEnabled; }
    public static void setHitMarkerEnabled(boolean value) { hitMarkerEnabled = value; }
    public static void triggerHitMarker() {
        if (hitMarkerEnabled) hitMarkerUntil = System.nanoTime() + 180_000_000L;
    }
    public static boolean isTargetInfoEnabled() { return targetInfoEnabled; }
    public static void setTargetInfoEnabled(boolean value) { targetInfoEnabled = value; }
}
