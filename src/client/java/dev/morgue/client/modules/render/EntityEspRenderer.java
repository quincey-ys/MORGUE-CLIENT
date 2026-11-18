package dev.morgue.client.modules.render;

import dev.morgue.client.MorgueClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Draws depth-tested entity outlines from the current frame's immutable render states.
 * No world scan or persistent entity glowing flag is used.
 */
public final class EntityEspRenderer {
    private static boolean registered;

    private EntityEspRenderer() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        LevelRenderEvents.COLLECT_SUBMITS.register(EntityEspRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        EntityEspModule module = MorgueClient.entityEsp();
        if (!module.isEnabled()) {
            return;
        }

        var levelState = context.levelState();
        if (levelState == null || levelState.entityRenderStates.isEmpty()
                || levelState.cameraRenderState == null) {
            return;
        }

        Vec3 camera = levelState.cameraRenderState.pos;
        if (camera == null) {
            return;
        }

        double rangeSquared = (double) module.getRange() * module.getRange();
        var poseStack = context.poseStack();
        var collector = context.submitNodeCollector();

        for (EntityRenderState state : levelState.entityRenderStates) {
            if (state == null || !module.shouldRender(state)
                    || !Double.isFinite(state.distanceToCameraSq)
                    || state.distanceToCameraSq > rangeSquared) {
                continue;
            }

            float width = saneDimension(state.boundingBoxWidth, 0.2F, 3.0F);
            float height = saneDimension(state.boundingBoxHeight, 0.2F, 4.0F);

            poseStack.pushPose();
            poseStack.translate(state.x - camera.x - width * 0.5,
                    state.y - camera.y, state.z - camera.z - width * 0.5);
            poseStack.scale(width, height, width);
            collector.submitShapeOutline(poseStack, Shapes.block(), RenderTypes.LINES,
                    module.colorFor(state), 2.0F, false);
            poseStack.popPose();
        }
    }

    private static float saneDimension(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
