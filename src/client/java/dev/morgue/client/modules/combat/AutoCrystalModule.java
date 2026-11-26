package dev.morgue.client.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * BlackOut-inspired crystal placement and detonation logic, adapted to MORGUE's
 * Minecraft 26.3 client APIs. It intentionally uses ordinary game-mode item
 * use and entity attacks instead of Meteor-specific packet/mixin helpers.
 */
public final class AutoCrystalModule {
    private boolean enabled;
    private boolean place = true;
    private boolean explode = true;
    private boolean pauseWhileUsingItem = true;
    private boolean instantPlace = true;
    private boolean onlyOwn;
    private boolean inhibit = true;
    private boolean autoSwitch = true;
    private boolean ignoreTerrain;
    private boolean performanceMode;

    private double range = 5.0;
    private int placeSpeed = 10;
    private int slowSpeed = 2;
    private int explodeSpeed = 4;
    private double slowDamage = 3.0;
    private double minPlaceDamage = 4.0;
    private double maxSelfPlaceDamage = 8.0;
    private double minPlaceRatio = 1.4;
    private double maxAllyPlaceDamage = 8.0;
    private double minAllyPlaceRatio = 2.0;
    private double minExplodeDamage = 2.5;
    private double maxSelfExplodeDamage = 9.0;
    private double minExplodeRatio = 1.1;
    private double maxAllyExplodeDamage = 12.0;
    private double minAllyExplodeRatio = 2.0;
    private double forcePop = 1.0;
    private double antiSelfPop = 1.0;
    private double antiAllyPop = 1.0;
    private long placeDelayMillis;
    private long crystalAgeMillis;

    private long nextPlaceAt;
    private long nextExplodeAt;
    private int ticksEnabled;
    private int lastPlaceTick = Integer.MIN_VALUE;
    private long lastActionAt;
    private BlockPos lastPlacePosition;

    // Crystal block positions are one block above their obsidian/bedrock base.
    private final Map<BlockPos, Long> ownCrystals = new HashMap<>();
    private final Map<BlockPos, Long> existedAt = new HashMap<>();
    private final Map<Integer, Long> inhibitedIds = new HashMap<>();
    private Placement lastPlacement;

    public void tick(Minecraft client) {
        if (!enabled || client == null || client.player == null || client.level == null
                || client.gameMode == null || client.gui.screen() != null) {
            return;
        }

        Player player = client.player;
        if (player.isSpectator() || player.isDeadOrDying()
                || (pauseWhileUsingItem && player.isUsingItem())) {
            return;
        }

        long now = System.currentTimeMillis();
        ticksEnabled++;
        expireTransientState(client, now);

        List<Player> nearbyPlayers = collectPlayers(client);
        if (nearbyPlayers.size() <= 1) {
            lastPlacement = null;
            return;
        }

        // Existing crystals are evaluated first, matching BlackOut's explode-first
        // pass. This prevents a fresh placement from taking priority over a crystal
        // that is already available to detonate.
        if (explode && now >= nextExplodeAt) {
            CrystalTarget bestCrystal = findBestCrystal(client, nearbyPlayers, now);
            if (bestCrystal != null && CombatActionCoordinator.tryClaim(client)) {
                client.gameMode.attack(player, bestCrystal.crystal());
                player.swing(InteractionHand.MAIN_HAND,
                        player.getMainHandItem().getAttackAnimation(), true);
                inhibitedIds.put(bestCrystal.crystal().getId(),
                        now + Math.max(500L, 1000L / Math.max(1, explodeSpeed)));
                existedAt.remove(bestCrystal.crystal().blockPosition());
                ownCrystals.remove(bestCrystal.crystal().blockPosition());
                nextExplodeAt = now + 1000L / Math.max(1, explodeSpeed);
                lastActionAt = now;
                return;
            }
        }

        if (!place || now < nextPlaceAt) {
            return;
        }
        if (!instantPlace && placeDelayMillis > 0 && now - lastActionAt < placeDelayMillis) {
            return;
        }

        Placement bestPlacement = findBestPlacement(client, nearbyPlayers);
        lastPlacement = bestPlacement;
        if (bestPlacement == null || !hasCrystalAvailable(player)) {
            return;
        }
        if (ticksEnabled - lastPlaceTick < 1) {
            return;
        }
        if (!CombatActionCoordinator.tryClaim(client)) {
            return;
        }

        if (placeCrystal(client, bestPlacement, now)) {
            lastPlaceTick = ticksEnabled;
            lastPlacePosition = bestPlacement.base();
            long baseTime = now;
            BlockPos crystalPos = bestPlacement.base().above();
            ownCrystals.put(crystalPos, baseTime);
            existedAt.put(crystalPos, ticksEnabled * 1L);
            int actualSpeed = bestPlacement.damage().enemyDamage() <= slowDamage
                    ? slowSpeed : placeSpeed;
            nextPlaceAt = now + 1000L / Math.max(1, actualSpeed);
            lastActionAt = now;
        }
    }

    private List<Player> collectPlayers(Minecraft client) {
        Player self = client.player;
        AABB scan = self.getBoundingBox().inflate(12.0);
        List<Player> players = client.level.getEntitiesOfClass(
                Player.class, scan,
                candidate -> candidate.isAlive() && !candidate.isRemoved()
                        && !candidate.isSpectator()
        );
        if (!players.contains(self)) players.add(self);
        return players;
    }

    private Placement findBestPlacement(Minecraft client, List<Player> players) {
        Player self = client.player;
        int radius = (int) Math.ceil(range);
        BlockPos origin = self.blockPosition();
        Placement best = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos base = origin.offset(dx, dy, dz);
                    if (!isCrystalBase(client, base)) continue;

                    Vec3 hitPoint = new Vec3(base.getX() + 0.5,
                            base.getY() + 1.0, base.getZ() + 0.5);
                    if (self.getEyePosition().distanceToSqr(hitPoint) > range * range) continue;

                    AABB crystalBox = new AABB(base.getX(), base.getY() + 1, base.getZ(),
                            base.getX() + 1, base.getY() + 3, base.getZ() + 1);
                    if (hasBlockingEntity(client, crystalBox, self)) continue;

                    DamageResult damage = evaluateDamage(client, hitPoint, players);
                    if (!passesPlaceDamage(damage)) continue;

                    Placement candidate = new Placement(base, damage);
                    if (isBetterPlacement(candidate, best)) best = candidate;
                }
            }
        }
        return best;
    }

    private boolean isCrystalBase(Minecraft client, BlockPos base) {
        var state = client.level.getBlockState(base);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) return false;
        return client.level.getBlockState(base.above()).isAir()
                && client.level.getBlockState(base.above(2)).isAir();
    }

    private boolean hasBlockingEntity(Minecraft client, AABB box, Player self) {
        return !client.level.getEntitiesOfClass(
                Entity.class, box,
                entity -> entity != self && entity.isAlive()
        ).isEmpty();
    }

    private DamageResult evaluateDamage(Minecraft client, Vec3 explosion, List<Player> players) {
        Player self = client.player;
        double selfDamage = crystalDamage(client, self, self.getBoundingBox(), explosion);
        double bestEnemyDamage = 0.0;
        double bestAllyDamage = 0.0;
        double enemyHealth = 0.0;
        double allyHealth = 0.0;

        for (Player candidate : players) {
            if (candidate == self || !candidate.isAlive()) continue;
            double damage = crystalDamage(client, candidate, candidate.getBoundingBox(), explosion);
            double health = candidate.getHealth() + candidate.getAbsorptionAmount();

            if (self.isAlliedTo(candidate)) {
                if (damage > bestAllyDamage) {
                    bestAllyDamage = damage;
                    allyHealth = health;
                }
            } else if (damage > bestEnemyDamage) {
                bestEnemyDamage = damage;
                enemyHealth = health;
            }
        }

        return new DamageResult(bestEnemyDamage, bestAllyDamage, selfDamage,
                enemyHealth, allyHealth, self.getHealth() + self.getAbsorptionAmount());
    }

    /**
     * BlackOut's damage model uses vanilla crystal strength, exposure sampling,
     * difficulty and armor reduction. This implementation keeps the same core
     * shape while using MORGUE's mapped 26.3 world/entity methods.
     */
    private double crystalDamage(Minecraft client, Player target, AABB box, Vec3 explosion) {
        double strength = 12.0;
        Vec3 feet = new Vec3((box.minX + box.maxX) * 0.5, box.minY,
                (box.minZ + box.maxZ) * 0.5);
        double normalizedDistance = feet.distanceTo(explosion) / strength;
        if (normalizedDistance > 1.0) return 0.0;

        double exposure = exposure(client, target, explosion, box);
        double impact = (1.0 - normalizedDistance) * exposure;
        double damage = (impact * impact + impact) * 3.5 * strength + 1.0;

        Difficulty difficulty = client.level.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) return 0.0;
        if (difficulty == Difficulty.EASY) damage = Math.min(damage / 2.0 + 1.0, damage);
        else if (difficulty == Difficulty.HARD) damage *= 1.5;

        double armor = target.getArmor();
        double toughness = target.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        double divisor = 2.0 + toughness / 4.0;
        double effectiveArmor = Math.max(armor * 0.2,
                Math.min(20.0, armor - damage / divisor));
        damage *= 1.0 - effectiveArmor / 25.0;
        return Math.max(0.0, damage);
    }

    private double exposure(Minecraft client, Player target, Vec3 explosion, AABB box) {
        if (ignoreTerrain) return 1.0;

        int visible = 0;
        int total = 0;
        int samples = performanceMode ? 2 : 3;
        // Normal mode samples a 3 x 3 x 3 grid. Performance mode uses a
        // 2 x 2 x 2 grid to reduce raycasts during dense crystal fights.
        for (int ix = 0; ix < samples; ix++) {
            for (int iy = 0; iy < samples; iy++) {
                for (int iz = 0; iz < samples; iz++) {
                    double fx = (ix + 0.5) / samples;
                    double fy = (iy + 0.5) / samples;
                    double fz = (iz + 0.5) / samples;
                    Vec3 sample = new Vec3(
                            box.minX + (box.maxX - box.minX) * fx,
                            box.minY + (box.maxY - box.minY) * fy,
                            box.minZ + (box.maxZ - box.minZ) * fz
                    );
                    HitResult trace = client.level.clip(new ClipContext(
                            explosion, sample,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
                    if (trace.getType() == HitResult.Type.MISS) visible++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0 : (double) visible / total;
    }

    private boolean passesPlaceDamage(DamageResult damage) {
        if (damage.enemyDamage() <= 0.0) return false;
        if (damage.selfHealth() > 0.0
                && damage.selfDamage() * antiSelfPop >= damage.selfHealth()) return false;
        if (damage.allyHealth() > 0.0
                && damage.allyDamage() * antiAllyPop >= damage.allyHealth()) return false;

        boolean forcePopEnemy = damage.enemyHealth() > 0.0
                && damage.enemyDamage() * forcePop >= damage.enemyHealth();
        if (!forcePopEnemy && damage.enemyDamage() < minPlaceDamage) return false;
        if (damage.allyDamage() > maxAllyPlaceDamage) return false;
        if (damage.allyDamage() > 0.0
                && ratio(damage.enemyDamage(), damage.allyDamage()) < minAllyPlaceRatio) return false;
        if (damage.selfDamage() > maxSelfPlaceDamage) return false;
        return damage.selfDamage() <= 0.0
                || ratio(damage.enemyDamage(), damage.selfDamage()) >= minPlaceRatio;
    }

    private boolean passesExplodeDamage(DamageResult damage, boolean own) {
        if (damage.enemyDamage() <= 0.0) return false;

        if (damage.selfHealth() > 0.0
                && damage.selfDamage() * antiSelfPop >= damage.selfHealth()) return false;
        if (damage.allyHealth() > 0.0
                && damage.allyDamage() * antiAllyPop >= damage.allyHealth()) return false;

        boolean forcePopEnemy = damage.enemyHealth() > 0.0
                && damage.enemyDamage() * forcePop >= damage.enemyHealth();
        if (!forcePopEnemy && damage.enemyDamage() < minExplodeDamage) return false;
        if (damage.allyDamage() > maxAllyExplodeDamage) return false;
        if (damage.allyDamage() > 0.0
                && ratio(damage.enemyDamage(), damage.allyDamage()) < minAllyExplodeRatio) return false;
        if (damage.selfDamage() > maxSelfExplodeDamage) return false;
        if (damage.selfDamage() > 0.0
                && ratio(damage.enemyDamage(), damage.selfDamage()) < minExplodeRatio) return false;
        return true;
    }

    private static double ratio(double numerator, double denominator) {
        return numerator / Math.max(denominator, 0.5);
    }

    private boolean isBetterPlacement(Placement candidate, Placement current) {
        if (current == null) return true;
        if (candidate.damage().enemyDamage() != current.damage().enemyDamage()) {
            return candidate.damage().enemyDamage() > current.damage().enemyDamage();
        }
        if (candidate.damage().selfDamage() != current.damage().selfDamage()) {
            return candidate.damage().selfDamage() < current.damage().selfDamage();
        }
        return candidate.base().distSqr(lastPlacePosition == null
                        ? BlockPos.ZERO : lastPlacePosition)
                < current.base().distSqr(lastPlacePosition == null
                        ? BlockPos.ZERO : lastPlacePosition);
    }

    private CrystalTarget findBestCrystal(Minecraft client, List<Player> players, long now) {
        Player self = client.player;
        List<EndCrystal> crystals = client.level.getEntitiesOfClass(
                EndCrystal.class,
                self.getBoundingBox().inflate(12.0),
                crystal -> crystal.isAlive() && !crystal.isRemoved()
                        && self.distanceTo(crystal) <= range + 2.0
                        && self.hasLineOfSight(crystal)
                        && (!onlyOwn || ownCrystals.containsKey(crystal.blockPosition()))
                        && (!inhibit || !inhibitedIds.containsKey(crystal.getId()))
        );

        CrystalTarget best = null;
        for (EndCrystal crystal : crystals) {
            Long createdAt = existedAt.get(crystal.blockPosition());
            if (crystalAgeMillis > 0 && createdAt != null
                    && ticksEnabled - createdAt < crystalAgeMillis / 50L) {
                continue;
            }

            DamageResult damage = evaluateDamage(client, crystal.position(), players);
            if (!passesExplodeDamage(damage, ownCrystals.containsKey(crystal.blockPosition()))) {
                continue;
            }

            CrystalTarget candidate = new CrystalTarget(crystal, damage);
            if (best == null
                    || candidate.damage().enemyDamage() > best.damage().enemyDamage()
                    || (candidate.damage().enemyDamage() == best.damage().enemyDamage()
                    && candidate.damage().selfDamage() < best.damage().selfDamage())) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean placeCrystal(Minecraft client, Placement placement, long now) {
        Player player = client.player;
        int previousSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        boolean offhandCrystal = player.getOffhandItem().is(Items.END_CRYSTAL);

        if (offhandCrystal) {
            hand = InteractionHand.OFF_HAND;
        } else {
            int crystalSlot = findHotbarCrystal(player);
            if (crystalSlot < 0) return false;
            if (crystalSlot != previousSlot) {
                if (!autoSwitch) return false;
                player.getInventory().setSelectedSlot(crystalSlot);
            }
        }

        BlockPos base = placement.base();
        Vec3 hitLocation = new Vec3(base.getX() + 0.5,
                base.getY() + 1.0, base.getZ() + 0.5);
        try {
            client.gameMode.useItemOn(player, hand,
                    new BlockHitResult(hitLocation, Direction.UP, base, false));
            player.swing(hand, player.getItemInHand(hand).getAttackAnimation(), true);
            return true;
        } finally {
            if (!offhandCrystal && player.getInventory().getSelectedSlot() != previousSlot) {
                player.getInventory().setSelectedSlot(previousSlot);
            }
        }
    }

    private boolean hasCrystalAvailable(Player player) {
        return player.getOffhandItem().is(Items.END_CRYSTAL)
                || findHotbarCrystal(player) >= 0;
    }

    private static int findHotbarCrystal(Player player) {
        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getItem(selected).is(Items.END_CRYSTAL)) return selected;
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.END_CRYSTAL)) return slot;
        }
        return -1;
    }

    private void expireTransientState(Minecraft client, long now) {
        ownCrystals.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);
        inhibitedIds.entrySet().removeIf(entry -> now >= entry.getValue());

        // Track the first client tick on which every visible crystal appeared,
        // not just crystals placed by this module. This keeps Existed timing
        // meaningful for crystals placed manually or by another player.
        HashSet<BlockPos> visible = new HashSet<>();
        for (EndCrystal crystal : client.level.getEntitiesOfClass(
                EndCrystal.class, client.player.getBoundingBox().inflate(16.0))) {
            BlockPos position = crystal.blockPosition();
            visible.add(position);
            existedAt.putIfAbsent(position, (long) ticksEnabled);
        }
        existedAt.keySet().removeIf(position ->
                !visible.contains(position) && !ownCrystals.containsKey(position));
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            nextPlaceAt = 0L;
            nextExplodeAt = 0L;
            lastPlacement = null;
            ownCrystals.clear();
            existedAt.clear();
            inhibitedIds.clear();
        }
    }

    public boolean isPlaceEnabled() { return place; }
    public void setPlaceEnabled(boolean value) { place = value; }
    public boolean isExplodeEnabled() { return explode; }
    public void setExplodeEnabled(boolean value) { explode = value; }
    public boolean isPauseWhileUsingItem() { return pauseWhileUsingItem; }
    public void setPauseWhileUsingItem(boolean value) { pauseWhileUsingItem = value; }
    public boolean isInstantPlace() { return instantPlace; }
    public void setInstantPlace(boolean value) { instantPlace = value; }
    public boolean isOnlyOwn() { return onlyOwn; }
    public void setOnlyOwn(boolean value) { onlyOwn = value; }
    public boolean isInhibit() { return inhibit; }
    public void setInhibit(boolean value) { inhibit = value; }
    public boolean isAutoSwitch() { return autoSwitch; }
    public void setAutoSwitch(boolean value) { autoSwitch = value; }
    public boolean isIgnoreTerrain() { return ignoreTerrain; }
    public void setIgnoreTerrain(boolean value) { ignoreTerrain = value; }
    public boolean isPerformanceMode() { return performanceMode; }
    public void setPerformanceMode(boolean value) { performanceMode = value; }
    public double getRange() { return range; }
    public void setRange(double value) { range = Math.max(2.0, Math.min(6.0, value)); }
    public int getPlaceSpeed() { return placeSpeed; }
    public void setPlaceSpeed(int value) { placeSpeed = Math.max(1, Math.min(20, value)); }
    public int getSlowSpeed() { return slowSpeed; }
    public void setSlowSpeed(int value) { slowSpeed = Math.max(1, Math.min(20, value)); }
    public int getExplodeSpeed() { return explodeSpeed; }
    public void setExplodeSpeed(int value) { explodeSpeed = Math.max(1, Math.min(20, value)); }
    public double getSlowDamage() { return slowDamage; }
    public void setSlowDamage(double value) { slowDamage = Math.max(0.0, Math.min(20.0, value)); }
    public double getMinPlaceDamage() { return minPlaceDamage; }
    public void setMinPlaceDamage(double value) { minPlaceDamage = Math.max(0.0, Math.min(20.0, value)); }
    public double getMaxSelfPlaceDamage() { return maxSelfPlaceDamage; }
    public void setMaxSelfPlaceDamage(double value) { maxSelfPlaceDamage = Math.max(0.0, Math.min(40.0, value)); }
    public double getMinPlaceRatio() { return minPlaceRatio; }
    public void setMinPlaceRatio(double value) { minPlaceRatio = Math.max(0.0, Math.min(5.0, value)); }
    public double getMaxAllyPlaceDamage() { return maxAllyPlaceDamage; }
    public void setMaxAllyPlaceDamage(double value) { maxAllyPlaceDamage = Math.max(0.0, Math.min(40.0, value)); }
    public double getMinAllyPlaceRatio() { return minAllyPlaceRatio; }
    public void setMinAllyPlaceRatio(double value) { minAllyPlaceRatio = Math.max(0.0, Math.min(5.0, value)); }
    public double getMinExplodeDamage() { return minExplodeDamage; }
    public void setMinExplodeDamage(double value) { minExplodeDamage = Math.max(0.0, Math.min(20.0, value)); }
    public double getMaxSelfExplodeDamage() { return maxSelfExplodeDamage; }
    public void setMaxSelfExplodeDamage(double value) { maxSelfExplodeDamage = Math.max(0.0, Math.min(40.0, value)); }
    public double getMinExplodeRatio() { return minExplodeRatio; }
    public void setMinExplodeRatio(double value) { minExplodeRatio = Math.max(0.0, Math.min(5.0, value)); }
    public double getMaxAllyExplodeDamage() { return maxAllyExplodeDamage; }
    public void setMaxAllyExplodeDamage(double value) { maxAllyExplodeDamage = Math.max(0.0, Math.min(40.0, value)); }
    public double getMinAllyExplodeRatio() { return minAllyExplodeRatio; }
    public void setMinAllyExplodeRatio(double value) { minAllyExplodeRatio = Math.max(0.0, Math.min(5.0, value)); }
    public double getForcePop() { return forcePop; }
    public void setForcePop(double value) { forcePop = Math.max(0.0, Math.min(10.0, value)); }
    public double getAntiSelfPop() { return antiSelfPop; }
    public void setAntiSelfPop(double value) { antiSelfPop = Math.max(0.0, Math.min(10.0, value)); }
    public double getAntiAllyPop() { return antiAllyPop; }
    public void setAntiAllyPop(double value) { antiAllyPop = Math.max(0.0, Math.min(10.0, value)); }
    public long getPlaceDelayMillis() { return placeDelayMillis; }
    public void setPlaceDelayMillis(long value) { placeDelayMillis = Math.max(0L, Math.min(1000L, value)); }
    public long getCrystalAgeMillis() { return crystalAgeMillis; }
    public void setCrystalAgeMillis(long value) { crystalAgeMillis = Math.max(0L, Math.min(1000L, value)); }

    public Placement getLastPlacement() { return lastPlacement; }

    public record Placement(BlockPos base, DamageResult damage) {}
    public record CrystalTarget(EndCrystal crystal, DamageResult damage) {}
    public record DamageResult(double enemyDamage, double allyDamage, double selfDamage,
                               double enemyHealth, double allyHealth, double selfHealth) {}
}
