package dev.morgue.client.ai;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces short, explainable combat suggestions from local game state.
 * This is a rule-based coach, not a trained or remote AI model.
 */
public final class CombatCoach {
    private CombatCoach() {}

    public static List<String> instructions(Player player, TargetIntelligence.Assessment target,
                                            boolean maceHitterEnabled, boolean killAuraEnabled) {
        List<String> advice = new ArrayList<>(4);
        if (player == null) {
            return List.of("Enter a world to receive combat instructions.");
        }

        float health = player.getHealth() + player.getAbsorptionAmount();
        if (health <= 8.0F) {
            advice.add("DEFEND: health is critical; create space and use cover.");
        } else if (health <= 14.0F) {
            advice.add("CAUTION: trade carefully; avoid taking a second hit.");
        } else {
            advice.add("PACE: avoid trading hits when you can reset spacing.");
        }

        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            advice.add("OFFHAND: Totem equipped; keep it there while under pressure.");
        } else {
            advice.add("OFFHAND: no Totem detected; consider one before a risky engage.");
        }

        if (!maceHitterEnabled && !killAuraEnabled) {
            advice.add("MANUAL: no combat controller active; track the target and time attacks.");
        } else if (target == null) {
            advice.add("TARGET: none selected; check range, FOV, entity filters, and team checks.");
        } else {
            double distance = target.distance();
            if (distance < 2.0) {
                advice.add("SPACING: very close; strafe to avoid predictable trades.");
            } else if (distance > 3.2) {
                advice.add("CLOSE: reduce distance without sprinting blindly into a hit.");
            } else {
                advice.add("RANGE: target is in close-combat distance; keep your crosshair settled.");
            }

            if (target.threatScore() >= 20.0) {
                advice.add("THREAT: high pressure; prioritize movement and defensive timing.");
            } else if (target.health() > 0.0F && target.health() <= 5.0F) {
                advice.add("FINISH: target is low; keep pressure while respecting attack cooldown.");
            } else {
                advice.add("READ: watch the target's approach and avoid wasting cooldowns.");
            }
        }

        if (maceHitterEnabled) {
            advice.add("MACE: wait for a clean falling hit; don't force a swing from ground level.");
        } else if (killAuraEnabled) {
            advice.add("AURA: verify target filters and team checks before engaging.");
        }

        return List.copyOf(advice);
    }
}
