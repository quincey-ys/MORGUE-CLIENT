package dev.morgue.client.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Shared local target evaluator used by MORGUE combat modules.
 *
 * This is a deterministic heuristic decision layer, not a cloud service or
 * trained model. It centralizes ranking so combat modules can share one
 * explainable target assessment without taking ownership of each other's
 * attack or inventory logic.
 */
public final class TargetIntelligence {
    public enum Priority {
        CLOSEST,
        LOWEST_HEALTH,
        THREAT
    }

    public record Assessment(
            String targetName,
            double distance,
            float health,
            double threatScore,
            Priority priority,
            String reason,
            long updatedAtMillis
    ) {}

    private static volatile Assessment latestAssessment;

    private TargetIntelligence() {}

    public static LivingEntity selectBest(
            Player player,
            List<LivingEntity> candidates,
            Predicate<LivingEntity> allowed,
            Priority priority,
            double range
    ) {
        if (player == null || candidates == null || allowed == null || range <= 0.0) {
            latestAssessment = null;
            return null;
        }

        Priority ranking = priority == null ? Priority.CLOSEST : priority;
        LivingEntity best = null;
        double bestMetric = Double.NEGATIVE_INFINITY;
        double bestDistance = Double.MAX_VALUE;
        double bestThreat = 0.0;

        for (LivingEntity candidate : candidates) {
            if (candidate == null || candidate == player || !candidate.isAlive()
                    || candidate.isRemoved() || !allowed.test(candidate)) {
                continue;
            }

            double distanceSquared = player.distanceToSqr(candidate);
            if (distanceSquared > range * range) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            double threat = threatScore(player, candidate, distance, range);
            double metric = switch (ranking) {
                case CLOSEST -> -distanceSquared;
                case LOWEST_HEALTH -> -candidate.getHealth();
                case THREAT -> threat;
            };

            if (metric > bestMetric || (metric == bestMetric && distanceSquared < bestDistance)) {
                bestMetric = metric;
                best = candidate;
                bestDistance = distanceSquared;
                bestThreat = threat;
            }
        }

        if (best == null) {
            latestAssessment = null;
            return null;
        }

        double distance = Math.sqrt(bestDistance);
        latestAssessment = new Assessment(
                best.getName().getString(),
                distance,
                best.getHealth(),
                bestThreat,
                ranking,
                explain(player, best, ranking),
                System.currentTimeMillis()
        );
        return best;
    }

    private static double threatScore(Player player, LivingEntity candidate, double distance, double range) {
        boolean playerTarget = candidate instanceof Player;
        boolean hostileMob = !candidate.getType().getCategory().isFriendly();

        double score = playerTarget ? 12.0 : hostileMob ? 9.0 : 2.0;
        score += Math.max(0.0, 5.0 * (1.0 - distance / Math.max(0.1, range)));

        Vec3 towardPlayer = player.position().subtract(candidate.position());
        double separation = towardPlayer.length();
        if (separation > 1.0E-6) {
            double approach = candidate.getDeltaMovement().normalize()
                    .dot(towardPlayer.scale(1.0 / separation));
            score += Math.max(-2.0, Math.min(5.0, approach * 5.0));
        }

        ItemStack held = candidate.getMainHandItem();
        if (held.is(Items.MACE)) score += 8.0;
        else if (held.is(Items.NETHERITE_SWORD) || held.is(Items.NETHERITE_AXE)) score += 6.0;
        else if (held.is(Items.DIAMOND_SWORD) || held.is(Items.DIAMOND_AXE)
                || held.is(Items.TRIDENT)) score += 4.5;
        else if (held.is(Items.IRON_SWORD) || held.is(Items.IRON_AXE)) score += 3.0;

        return score;
    }

    private static String explain(Player player, LivingEntity target, Priority priority) {
        if (priority == Priority.CLOSEST) return "Nearest valid target";
        if (priority == Priority.LOWEST_HEALTH) return "Lowest current health";

        String kind = target instanceof Player
                ? "Player"
                : target.getType().getCategory().isFriendly() ? "Living entity" : "Hostile mob";
        Vec3 towardPlayer = player.position().subtract(target.position());
        double separation = towardPlayer.length();
        boolean approaching = separation > 1.0E-6
                && target.getDeltaMovement().normalize().dot(towardPlayer.scale(1.0 / separation)) > 0.25;
        return approaching ? kind + " closing distance" : kind + " threat score";
    }

    public static Assessment getLatestAssessment() {
        return latestAssessment;
    }

    public static void clearAssessment() {
        latestAssessment = null;
    }
}
