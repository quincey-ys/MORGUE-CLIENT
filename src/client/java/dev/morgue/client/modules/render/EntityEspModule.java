package dev.morgue.client.modules.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.MobCategory;

/** Lightweight configuration for render-state based entity outlines. */
public final class EntityEspModule {
    private boolean enabled;
    private boolean showPlayers = true;
    private boolean showHostileMobs = true;
    private boolean showFriendlyMobs;
    private boolean showOtherEntities;
    private int range = 32;

    public boolean shouldRender(EntityRenderState state) {
        if (state == null || state.isInvisible) {
            return false;
        }
        if (state instanceof AvatarRenderState) {
            return showPlayers;
        }
        if (state.entityType == null) {
            return false;
        }
        MobCategory category = state.entityType.getCategory();
        if (category == MobCategory.MISC) {
            return showOtherEntities;
        }
        return category.isFriendly() ? showFriendlyMobs : showHostileMobs;
    }

    public int colorFor(EntityRenderState state) {
        if (state instanceof AvatarRenderState) {
            return 0xFF79BFFF;
        }
        if (state.entityType == null) {
            return 0xFFFFC878;
        }
        MobCategory category = state.entityType.getCategory();
        if (category == MobCategory.MISC) {
            return 0xFFFFC878;
        }
        return category.isFriendly() ? 0xFF83D9A4 : 0xFFFF7188;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isShowPlayers() { return showPlayers; }
    public void setShowPlayers(boolean value) { showPlayers = value; }
    public boolean isShowHostileMobs() { return showHostileMobs; }
    public void setShowHostileMobs(boolean value) { showHostileMobs = value; }
    public boolean isShowFriendlyMobs() { return showFriendlyMobs; }
    public void setShowFriendlyMobs(boolean value) { showFriendlyMobs = value; }
    public boolean isShowOtherEntities() { return showOtherEntities; }
    public void setShowOtherEntities(boolean value) { showOtherEntities = value; }
    public int getRange() { return range; }
    public void setRange(int value) { range = Math.max(8, Math.min(64, value)); }
}
