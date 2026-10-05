package com.crimson_susanoo.entity;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.CrimsonSounds;
import com.crimson_susanoo.ServerConfig;
import com.crimson_susanoo.animation.ClientAnimationClock;
import com.crimson_susanoo.animation.SmoothKeyframeEasing;
import com.github.L_Ender.cataclysm.init.ModEffect;
import com.github.L_Ender.cataclysm.init.ModParticle;
import com.crimson_susanoo.spell.CrimsonSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class CrimsonEntity extends Mob implements GeoEntity {
    private static final int DEATH_ANIMATION_TICKS = 32;
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MANIFESTING = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MANIFEST_TICKS = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> MANIFEST_START = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> VISUAL_START = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> COLLAPSE = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GROUNDED = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MOVING = SynchedEntityData.defineId(CrimsonEntity.class, EntityDataSerializers.BOOLEAN);
    private final ClientAnimationClock manifestClock = new ClientAnimationClock();
    private final ClientAnimationClock actionClock = new ClientAnimationClock();
    private int movingGraceTicks;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerId;
    private int ageTicks;
    private int actionTicks;
    private int recoveryTicks;
    private int nextCleave;
    private int nextCrescent;
    private int nextSlash;
    private int fadeTicks;
    private int pathFailureTicks;
    private boolean dismissing;
    private boolean cooldownApplied;
    private LivingEntity attackTarget;
    private boolean hunting;
    private boolean patrolling;
    private int nextPatrol = 80;

    public CrimsonEntity(EntityType<? extends CrimsonEntity> type, Level level) {
        super(type, level);
        this.moveControl = new CrimsonMoveControl(this);
        this.xpReward = 0;
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new CrimsonNavigation(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 450)
                .add(Attributes.ARMOR, 18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ACTION, 0);
        entityData.define(FADE, 0);
        entityData.define(MANIFESTING, false);
        entityData.define(MANIFEST_TICKS, 60);
        entityData.define(MANIFEST_START, 0L);
        entityData.define(VISUAL_START, 0L);
        entityData.define(COLLAPSE, false);
        entityData.define(FOLLOWING, false);
        entityData.define(GROUNDED, false);
        entityData.define(MOVING, false);
    }

    public void setOwner(ServerPlayer player) {
        ownerId = player.getUUID();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(ServerConfig.HEALTH.get());
        getAttribute(Attributes.ARMOR).setBaseValue(ServerConfig.ARMOR.get());
        setHealth(getMaxHealth());
    }

    public boolean isOwnedBy(ServerPlayer player) { return player.getUUID().equals(ownerId); }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        if (ownerId == null || getServer() == null) return null;
        return getServer().getPlayerList().getPlayer(ownerId);
    }

    public boolean isDismissing() { return dismissing; }
    public boolean isManifesting() { return entityData.get(MANIFESTING); }
    public void startManifesting() { startManifesting(60); }
    public void startManifesting(int remainingCastTicks) {
        entityData.set(MANIFEST_TICKS, Math.max(1, remainingCastTicks));
        entityData.set(MANIFEST_START, level().getGameTime());
        entityData.set(MANIFESTING, true);
    }
    public double getManifestAnimationTick(double partialTick) {
        double elapsed = level().isClientSide
                ? manifestClock.elapsed(entityData.get(MANIFEST_START), level().getGameTime(), tickCount, partialTick)
                : Math.max(0, level().getGameTime() - entityData.get(MANIFEST_START)
                    + Math.max(0, Math.min(1, partialTick)));
        // Hold the final pose until the authoritative activation packet arrives.
        return Math.min(59.999, elapsed * 60.0 / entityData.get(MANIFEST_TICKS));
    }
    public void activate() { entityData.set(MANIFESTING, false); ageTicks = 0; }
    public int getAction() { return entityData.get(ACTION); }
    public int getFade() { return entityData.get(FADE); }
    public boolean isManaCollapsing() { return entityData.get(COLLAPSE); }
    public boolean isVisuallyGrounded() { return entityData.get(GROUNDED); }
    public boolean isVisuallyMoving() { return entityData.get(MOVING); }

    public double getActionAnimationTick(double partialTick) {
        int length = switch (getAction()) { case 1 -> 23; case 2 -> 26; case 3 -> 32; default -> 0; };
        return visualElapsed(partialTick, length);
    }

    public double getEndingAnimationTick(double partialTick) {
        return visualElapsed(partialTick, 30);
    }

    private double visualElapsed(double partialTick, int length) {
        if (length == 0) return 0;
        double elapsed = level().isClientSide
                ? actionClock.elapsed(entityData.get(VISUAL_START), level().getGameTime(), tickCount, partialTick)
                : level().getGameTime() - entityData.get(VISUAL_START)
                    + Math.max(0, Math.min(1, partialTick));
        return Math.max(0, Math.min(length - 0.001, elapsed));
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return isAlive() && !isManifesting() && !dismissing; }
    @Override public void push(Entity other) { /* The guardian holds its ground in crowds. */ }
    @Override public void push(double x, double y, double z) { }

    public void setHunting(boolean value) {
        hunting = value;
        patrolling = false;
        attackTarget = null;
        getNavigation().stop();
    }

    public boolean isHunting() { return hunting; }

    private boolean inPursuitRange(ServerPlayer owner, LivingEntity target) {
        // Retain an acquired target beyond the acquisition boundary to avoid edge chatter.
        double ownerRange = hunting || target == attackTarget ? 32 : 22;
        return target != null && distanceToSqr(target) <= 32 * 32
                && owner.distanceToSqr(target) <= ownerRange * ownerRange;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isManifesting()) return false;
        Entity attacker = source.getEntity();
        if (attacker == null && source.getDirectEntity() instanceof Projectile projectile)
            attacker = projectile.getOwner();
        ServerPlayer owner = getOwnerPlayer();
        // Reject friendly damage before vanilla records a retaliation target or
        // starts hurt animation. Friends need no scoreboard team in a co-op pack.
        if (attacker != null && (attacker.getUUID().equals(ownerId)
                || !ServerConfig.FRIENDLY_FIRE.get() && (isPlayerCompanion(attacker)
                || owner != null && (owner.isAlliedTo(attacker)
                || attacker.isAlliedTo(owner))))) return false;
        boolean damaged = super.hurt(source, amount);
        if (damaged && !level().isClientSide) level().playSound(null, blockPosition(), CrimsonSounds.HURT, SoundSource.HOSTILE, 1, 0.6F);
        return damaged;
    }

    @Override
    public void tick() {
        double beforeX = getX(), beforeZ = getZ();
        super.tick();
        if (level().isClientSide) {
            clientAura();
            return;
        }
        // Remote clients interpolate mob positions; their local velocity/ground
        // flags are not a reliable animation gate. Send the server's support and
        // actual travel, retaining a brief pause between path updates.
        entityData.set(GROUNDED, onGround());
        if (Math.hypot(getX() - beforeX, getZ() - beforeZ) > .01) movingGraceTicks = 3;
        else movingGraceTicks = Math.max(0, movingGraceTicks - 1);
        entityData.set(MOVING, movingGraceTicks > 0);
        if (isDeadOrDying()) return;
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (isManifesting()) {
            ServerPlayer caster = getOwnerPlayer();
            if (caster == null || !caster.isAlive() || caster.level() != level()
                    || !MagicData.getPlayerMagicData(caster).isCasting()
                    || !CrimsonSusanoo.SPELL.get().getSpellId().equals(MagicData.getPlayerMagicData(caster).getCastingSpellId())) {
                if (caster != null) CrimsonSpell.clearOwnerMarker(caster);
                discard();
            }
            return;
        }
        if (dismissing) {
            if (++fadeTicks >= 30) discard();
            else entityData.set(FADE, fadeTicks);
            return;
        }
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            dismiss("owner_invalid");
            return;
        }
        if (!advanceLifetime(owner)) return;
        if (ageTicks % 120 == 0) level().playSound(null, blockPosition(), CrimsonSounds.AMBIENT_AURA, SoundSource.HOSTILE, 0.65F, 0.48F);
        tickCombat(owner, serverLevel);
    }

    private void tickCombat(ServerPlayer owner, ServerLevel serverLevel) {
        if (actionTicks > 0) {
            advanceAttackStep(owner, serverLevel);
            actionTicks--;
            if (actionTicks == 0) executeAttack(owner);
        } else if (recoveryTicks > 0) {
            if (ageTicks % 5 == 0) selectTarget(owner);
            if (--recoveryTicks == 0) entityData.set(ACTION, 0);
        } else {
            entityData.set(ACTION, 0);
            if (ageTicks % 5 == 0) selectTarget(owner);
            if (ageTicks % 5 == 0) selectAttack(owner);
        }
        if (ageTicks % 20 == 0) reposition(owner, serverLevel);
        if (Boolean.getBoolean("crimson_susanoo.tracePursuit") && ageTicks % 5 == 0) {
            var path = getNavigation().getPath();
            var move = getMoveControl();
            CrimsonSusanoo.LOGGER.info("Pursuit tick={} entity={} pos={} yaw={} action={} windup={} recovery={} target={} ownerDistance={} pathNode={} pathLength={} nextNode={} wanted={} wantedPos={} speed={}",
                    ageTicks, getId(), position(), getYRot(), getAction(), actionTicks, recoveryTicks,
                    attackTarget == null ? "none" : attackTarget.getId() + ":" + attackTarget.position(),
                    distanceTo(owner), path == null ? -1 : path.getNextNodeIndex(),
                    path == null ? 0 : path.getNodeCount(),
                    path == null || path.isDone() ? "none" : path.getNextEntityPos(this),
                    move.hasWanted(), new Vec3(move.getWantedX(), move.getWantedY(), move.getWantedZ()), getSpeed());
        }
    }

    private boolean advanceLifetime(ServerPlayer owner) {
        ageTicks++;
        if (ageTicks >= ServerConfig.DURATION.get() * 20) {
            dismiss("duration");
            return false;
        }
        if (ageTicks % 20 == 0) {
            if (!chargeMana(owner, ServerConfig.UPKEEP.get().floatValue())) {
                CrimsonSusanoo.LOGGER.debug("Mana upkeep failed for {}", owner.getUUID());
                dismiss("mana_collapse");
                return false;
            }
            CrimsonSusanoo.LOGGER.debug("Mana upkeep paid for {}", owner.getUUID());
        }
        return true;
    }

    public static boolean chargeMana(ServerPlayer player, float amount) {
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() + 0.0001F < amount) return false;
        magic.setMana(Math.max(0, magic.getMana() - amount));
        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        return true;
    }

    private void selectTarget(ServerPlayer owner) {
        LivingEntity oldTarget = attackTarget;
        LivingEntity preferred = owner.getLastHurtByMob();
        if (owner.tickCount - owner.getLastHurtByMobTimestamp() < 100
                && canHarm(owner, preferred) && inPursuitRange(owner, preferred)) { setAttackTarget(preferred, oldTarget); return; }
        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32),
                e -> canHarm(owner, e) && inPursuitRange(owner, e)
                        && (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == owner));
        LivingEntity selected = nearby.stream().filter(e -> e instanceof Mob mob && mob.getTarget() == owner)
                .min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
        if (selected == null) {
            preferred = owner.getLastHurtMob();
            if (owner.tickCount - owner.getLastHurtMobTimestamp() < 100
                    && canHarm(owner, preferred) && inPursuitRange(owner, preferred)) selected = preferred;
        }
        if (selected == null) {
            preferred = getLastHurtByMob();
            if (canHarm(owner, preferred) && inPursuitRange(owner, preferred)) selected = preferred;
        }
        if (selected == null) selected = nearby.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setAttackTarget(selected, oldTarget);
    }

    private void setAttackTarget(LivingEntity selected, LivingEntity previous) {
        attackTarget = selected;
        if (selected != previous) {
            getNavigation().stop();
            patrolling = false;
        }
        if (previous != selected) CrimsonSusanoo.LOGGER.debug("Guardian {} target changed to {}", getUUID(),
                selected == null ? "none" : selected.getUUID().toString());
    }

    public boolean canHarm(ServerPlayer owner, @Nullable Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity == this || entity == owner) return false;
        // An explicit friendly-fire opt-in still respects the server/team PvP rules.
        if (entity instanceof Player player && !owner.canHarmPlayer(player)) return false;
        if (entity instanceof CrimsonEntity other) {
            ServerPlayer otherOwner = other.getOwnerPlayer();
            if (otherOwner != null && !owner.canHarmPlayer(otherOwner)) return false;
        }
        if (!ServerConfig.FRIENDLY_FIRE.get()) {
            if (isPlayerCompanion(entity)) return false;
            if (owner.isAlliedTo(entity) || entity.isAlliedTo(owner)) return false;
        }
        return true;
    }

    private static boolean isPlayerCompanion(Entity entity) {
        return entity instanceof Player || entity instanceof CrimsonEntity
                || entity instanceof TamableAnimal tame && tame.isTame() && tame.getOwnerUUID() != null
                || entity instanceof IMagicSummon summon && summon.getSummoner() instanceof Player;
    }

    public boolean canStrike(ServerPlayer owner, @Nullable Entity entity) {
        if (!canHarm(owner, entity)) return false;
        if (entity == attackTarget || entity instanceof Enemy) return true;
        return entity instanceof Mob mob && (mob.getTarget() == owner || mob.getTarget() == this);
    }

    private void selectAttack(ServerPlayer owner) {
        if (!canHarm(owner, attackTarget) || !inPursuitRange(owner, attackTarget)) return;
        double distance = distanceTo(attackTarget);
        // A clear ranged response can reach an owner-bound threat before a long chase.
        // If the lane, cooldown or mana disallows it, keep pursuing without waiting.
        // Defense must yield to the same six-block melee window as ordinary combat.
        // A five-block threshold stranded ready attacks behind an already-finished path.
        if (threatensOwner(owner, attackTarget) && distance > 6.0) {
            if (distance > 8 && distance <= 16 && attackTarget.distanceToSqr(owner) <= 64
                    && ageTicks >= nextCrescent && canAfford(owner, ServerConfig.CRESCENT_MANA.get())
                    && hasCrescentLane(attackTarget)) {
                beginAttack(2, 14);
                return;
            }
            getNavigation().moveTo(attackTarget, 1.45);
            return;
        }
        boolean bossLike = attackTarget.getMaxHealth() >= 100 || attackTarget.getArmorValue() >= 16;
        boolean needsBrand = attackTarget.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()) == null;
        boolean grouped = level().getEntitiesOfClass(LivingEntity.class, attackTarget.getBoundingBox().inflate(4),
                e -> canStrike(owner, e)).size() >= 2;
        if (bossLike && needsBrand && distance <= 16 && ageTicks >= nextCrescent
                && canAfford(owner, ServerConfig.CRESCENT_MANA.get()) && hasCrescentLane(attackTarget)) {
            beginAttack(2, 14);
        } else if (distance <= 6 && grouped && ageTicks >= nextCleave) {
            beginAttack(1, 11);
        } else if (distance <= 6 && bossLike && ageTicks >= nextSlash && canAfford(owner, ServerConfig.SLASH_MANA.get())) {
            beginAttack(3, 20);
        } else if (distance <= 6 && ageTicks >= nextCleave) {
            beginAttack(1, 11);
        } else if (distance > 6 && distance <= 16 && ageTicks >= nextCrescent
                && canAfford(owner, ServerConfig.CRESCENT_MANA.get()) && hasCrescentLane(attackTarget)) {
            beginAttack(2, 14);
        } else if (distance > 5 && distance <= 32) {
            getNavigation().moveTo(attackTarget, 1.3);
        } else if (distance <= 5) {
            // A previous chase must not keep driving through the target during cooldown.
            getNavigation().stop();
        }
    }

    private boolean threatensOwner(ServerPlayer owner, LivingEntity target) {
        return target instanceof Mob mob && mob.getTarget() == owner
                || target == owner.getLastHurtByMob()
                && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100;
    }

    private boolean hasCrescentLane(LivingEntity target) {
        Vec3 start = position().add(0, 1.8, 0);
        Vec3 end = new Vec3(target.getX(), start.y, target.getZ());
        return level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void advanceAttackStep(ServerPlayer owner, ServerLevel level) {
        if (!canHarm(owner, attackTarget) || !inPursuitRange(owner, attackTarget)) return;
        // Like Ignis, track during anticipation, then commit the final strike direction.
        if (actionTicks > 4) faceAttackTarget();
        if (getAction() == 2 || actionTicks > 8 || actionTicks <= 2
                || distanceToSqr(attackTarget) <= 4.5 * 4.5) return;
        Vec3 direction = new Vec3(-Math.sin(Math.toRadians(getYRot())), 0,
                Math.cos(Math.toRadians(getYRot())));
        // Six raised-cosine velocity samples, approximately .016,.12,.224,.224,.12,.016.
        // Same .72-block maximum stride, with eased launch and braking.
        double stepLength = .12 * (1 - Math.cos(Math.PI * (8 - actionTicks + .5) / 3));
        stepLength = Math.min(stepLength, Math.max(0, distanceTo(attackTarget) - 4.5));
        Vec3 displacement = direction.scale(stepLength);
        Vec3 next = position().add(displacement);
        // Each short step checks the whole guardian volume and supported ground.
        // Never charge through walls, liquid or off an arena edge.
        if (level.getBlockState(BlockPos.containing(next).below()).isSolidRender(level, BlockPos.containing(next).below())
                && SafePlacement.isClear(level, this, next)) {
            move(net.minecraft.world.entity.MoverType.SELF, displacement);
        }
    }

    private void faceAttackTarget() {
        if (attackTarget == null) return;
        Vec3 offset = attackTarget.position().subtract(position());
        if (offset.horizontalDistanceSqr() > .0001) {
            float desired = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
            // Even a rear target can be reached within Cleave's tracking window,
            // without a one-tick half-turn. Final four strike ticks remain committed.
            float heading = getYRot() + Mth.clamp(Mth.wrapDegrees(desired - getYRot()), -24, 24);
            setYRot(heading);
            setYBodyRot(heading);
            setYHeadRot(heading);
        }
        // Head/body are set together above; a queued look command would outlive the strike.
    }

    private boolean canAfford(ServerPlayer owner, int cost) {
        return MagicData.getPlayerMagicData(owner).getMana() >= cost;
    }

    private void beginAttack(int type, int windup) {
        entityData.set(VISUAL_START, level().getGameTime());
        entityData.set(ACTION, type);
        actionTicks = windup;
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        faceAttackTarget();
        if (type == 3) level().playSound(null, blockPosition(), CrimsonSounds.SLASH_WINDUP, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    private void executeAttack(ServerPlayer owner) {
        int action = entityData.get(ACTION);
        if (action == 1) {
            nextCleave = ageTicks + (int) (ServerConfig.CLEAVE_COOLDOWN.get() * 20);
            cleave(owner);
        } else if (action == 2 && canHarm(owner, attackTarget)
                && chargeMana(owner, ServerConfig.CRESCENT_MANA.get())) {
            nextCrescent = ageTicks + (int) (ServerConfig.CRESCENT_COOLDOWN.get() * 20);
            CrimsonSusanoo.LOGGER.debug("Crescent mana paid: {} owner={}", ServerConfig.CRESCENT_MANA.get(), owner.getUUID());
            crescent(owner);
        } else if (action == 3 && canHarm(owner, attackTarget)
                && distanceToSqr(attackTarget) <= 8 * 8
                && chargeMana(owner, ServerConfig.SLASH_MANA.get())) {
            nextSlash = ageTicks + (int) (ServerConfig.SLASH_COOLDOWN.get() * 20);
            CrimsonSusanoo.LOGGER.debug("Slash mana paid: {} owner={}", ServerConfig.SLASH_MANA.get(), owner.getUUID());
            slash(owner);
        }
        // Let the blade follow through and settle before movement resumes.
        recoveryTicks = 12;
    }

    private float scaledDamage(ServerPlayer owner, double base) {
        return (float) (base * CrimsonSusanoo.SPELL.get().getSpellPower(1, owner) * ServerConfig.DAMAGE_MULTIPLIER.get());
    }

    private void cleave(ServerPlayer owner) {
        Vec3 facing = getLookAngle().normalize();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6), e -> canStrike(owner, e));
        for (LivingEntity enemy : targets) {
            Vec3 offset = enemy.position().subtract(position());
            if (offset.lengthSqr() > 36 || offset.normalize().dot(facing) < 0.5) continue;
            if (enemy.hurt(CrimsonSusanoo.SPELL.get().getDamageSource(this, owner).get(), scaledDamage(owner, ServerConfig.CLEAVE_DAMAGE.get()))) {
                enemy.setSecondsOnFire(4);
                enemy.knockback(ServerConfig.CLEAVE_KNOCKBACK.get(), getX() - enemy.getX(), getZ() - enemy.getZ());
            }
        }
        if (level() instanceof ServerLevel serverLevel) {
            // Keep the low sweep legible against Solas bloom without covering the blade.
            DustParticleOptions trail = new DustParticleOptions(new Vector3f(1, 0.32F, 0.025F), .65F);
            DustParticleOptions hotEdge = new DustParticleOptions(new Vector3f(1, .72F, .16F), .35F);
            double heading = Math.atan2(facing.z, facing.x);
            for (int i = -6; i <= 6; i++) {
                double angle = heading + i * Math.PI / 18;
                serverLevel.sendParticles(trail, getX() + Math.cos(angle) * 5, getY() + 1.25,
                        getZ() + Math.sin(angle) * 5, 1, 0.04, 0.04, 0.04, 0.006);
                if (i % 3 == 0) serverLevel.sendParticles(hotEdge,
                        getX() + Math.cos(angle) * 5, getY() + 1.25,
                        getZ() + Math.sin(angle) * 5, 1, 0.025, 0.025, 0.025, 0.003);
            }
        }
        level().playSound(null, blockPosition(), CrimsonSounds.CLEAVE, SoundSource.HOSTILE, 2, 0.55F);
    }

    private void crescent(ServerPlayer owner) {
        if (attackTarget == null) return;
        Vec3 direction = attackTarget.position().subtract(position()).normalize();
        CrimsonWave wave = CrimsonSusanoo.WAVE.get().create(level());
        if (wave == null) return;
        wave.setup(owner, this, direction, scaledDamage(owner, ServerConfig.CRESCENT_DAMAGE.get()));
        wave.moveTo(getX() + direction.x * 2, getY() + 1.8, getZ() + direction.z * 2);
        level().addFreshEntity(wave);
        level().playSound(null, blockPosition(), CrimsonSounds.CRESCENT, SoundSource.HOSTILE, 2, 0.55F);
    }

    private void slash(ServerPlayer owner) {
        if (attackTarget == null || distanceToSqr(attackTarget) > 8 * 8) return;
        Vec3 impact = attackTarget.position();
        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(4), e -> canStrike(owner, e));
        for (LivingEntity enemy : targets) {
            boolean direct = enemy == attackTarget;
            float damage = scaledDamage(owner, ServerConfig.SLASH_DAMAGE.get() * (direct ? 1 : 0.5));
            if (enemy.hurt(CrimsonSusanoo.SPELL.get().getDamageSource(this, owner).get(), damage)) {
                Vec3 source = direct ? position() : impact;
                Vec3 away = enemy.position().subtract(source).multiply(1, 0, 1);
                if (away.horizontalDistanceSqr() < 0.0001) {
                    away = new Vec3(-Math.sin(Math.toRadians(getYRot())), 0,
                            Math.cos(Math.toRadians(getYRot())));
                }
                enemy.knockback(2.0, -away.x, -away.z);
            }
        }
        if (level() instanceof ServerLevel serverLevel) {
            DustParticleOptions shock = new DustParticleOptions(new Vector3f(1, 0.09F, 0.02F), 2.4F);
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI / 12;
                serverLevel.sendParticles(shock, impact.x + Math.cos(angle) * 4, impact.y + 0.2,
                        impact.z + Math.sin(angle) * 4, 2, 0.1, 0.1, 0.1, 0.02);
            }
            // Ignis's eight-frame fire burst stays at the contact point for 7-10 ticks.
            // Its factory interprets X speed as size, so zero speed is intentional.
            serverLevel.sendParticles(ModParticle.IGNIS_EXPLODE.get(), impact.x, impact.y + 0.8,
                    impact.z, 1, 0, 0, 0, 0);
        }
        level().playSound(null, BlockPos.containing(impact), CrimsonSounds.SLASH_IMPACT, SoundSource.HOSTILE, 2, 0.65F);
    }

    private void reposition(ServerPlayer owner, ServerLevel serverLevel) {
        // Keep the planted attack pose through both windup and recovery.
        if (actionTicks > 0 || recoveryTicks > 0) {
            entityData.set(FOLLOWING, false);
            getNavigation().stop();
            return;
        }
        double distance = distanceTo(owner);
        if (canHarm(owner, attackTarget) && inPursuitRange(owner, attackTarget)
                && distance < Math.max(ServerConfig.TELEPORT_DISTANCE.get(), ServerConfig.COMBAT_LEASH.get())) {
            entityData.set(FOLLOWING, false);
            pathFailureTicks = 0;
            return;
        }
        boolean following = distance > ServerConfig.FOLLOW_RADIUS.get();
        entityData.set(FOLLOWING, following);
        if (!following) {
            // A nearby owner does not cancel a path toward the current combat target.
            if (!canHarm(owner, attackTarget)) patrol(owner, serverLevel);
            pathFailureTicks = 0;
            return;
        }
        if (distance >= ServerConfig.TELEPORT_DISTANCE.get() || pathFailureTicks >= 5) {
            if (teleportNear(owner, serverLevel)) { entityData.set(FOLLOWING, false); pathFailureTicks = 0; return; }
        }
        if (!getNavigation().moveTo(owner, 1.0)) pathFailureTicks++;
        else pathFailureTicks = 0;
    }

    private void patrol(ServerPlayer owner, ServerLevel level) {
        if (patrolling && !getNavigation().isDone()) return;
        getNavigation().stop();
        patrolling = false;
        if (ageTicks < nextPatrol) return;
        nextPatrol = ageTicks + 80 + random.nextInt(61);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 3 + random.nextDouble() * 3;
            BlockPos near = BlockPos.containing(owner.getX() + Math.cos(angle) * radius,
                    owner.getY(), owner.getZ() + Math.sin(angle) * radius);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos feet = near.above(dy);
                Vec3 goal = Vec3.atBottomCenterOf(feet);
                if (distanceToSqr(goal) < 4 || distanceToSqr(goal) > 81
                        || !level.getBlockState(feet.below()).isSolidRender(level, feet.below())
                        || !SafePlacement.isClear(level, this, goal)) continue;
                var path = getNavigation().createPath(feet, 0);
                if (path != null && path.canReach() && getNavigation().moveTo(path, .65)) {
                    patrolling = true;
                    return;
                }
            }
        }
    }

    private boolean teleportNear(ServerPlayer owner, ServerLevel level) {
        for (int ring = 5; ring <= 11; ring += 2) {
            for (int direction = 0; direction < 8; direction++) {
                double angle = direction * Math.PI / 4;
                int x = (int) Math.floor(owner.getX() + Math.cos(angle) * ring);
                int z = (int) Math.floor(owner.getZ() + Math.sin(angle) * ring);
                BlockPos ground = BlockPos.containing(x, owner.getY() - 2, z);
                for (int y = -2; y <= 3; y++) {
                    BlockPos feet = ground.above(y + 2);
                    if (!level.getBlockState(feet.below()).isSolidRender(level, feet.below())) continue;
                    Vec3 position = Vec3.atBottomCenterOf(feet);
                    if (!SafePlacement.isClear(level, this, position)) continue;
                    teleportTo(position.x, position.y, position.z);
                    getNavigation().stop();
                    CrimsonSusanoo.LOGGER.debug("Repositioned guardian of {}", owner.getUUID());
                    return true;
                }
            }
        }
        return false;
    }

    public void dismiss(String reason) {
        if (dismissing) return;
        if (isManifesting()) {
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null) CrimsonSpell.clearOwnerMarker(owner);
            discard();
            return;
        }
        dismissing = true;
        entityData.set(VISUAL_START, level().getGameTime());
        entityData.set(COLLAPSE, "mana_collapse".equals(reason));
        entityData.set(FADE, 1);
        getNavigation().stop();
        attackTarget = null;
        ServerPlayer owner = getOwnerPlayer();
        if (owner != null) {
            CrimsonSpell.clearOwnerMarker(owner);
            applyCooldown(owner);
        }
        CrimsonSusanoo.LOGGER.debug("Crimson Susanoo dismissed: {} owner={}", reason, ownerId);
        level().playSound(null, blockPosition(), entityData.get(COLLAPSE) ? CrimsonSounds.MANA_COLLAPSE : CrimsonSounds.DISMISS,
                SoundSource.HOSTILE, 1.5F, 0.5F);
    }

    private void applyCooldown(ServerPlayer owner) {
        if (cooldownApplied) return;
        cooldownApplied = true;
        // Scrolls normally bypass Iron's cooldowns; this ultimate always starts one.
        MagicHelper.MAGIC_MANAGER.addCooldown(owner, CrimsonSusanoo.SPELL.get(), CastSource.SPELLBOOK);
        MagicData.getPlayerMagicData(owner).getPlayerCooldowns().syncToPlayer(owner);
    }

    @Override public void die(DamageSource source) {
        if (!level().isClientSide) entityData.set(VISUAL_START, level().getGameTime());
        ServerPlayer owner = getOwnerPlayer();
        if (owner != null) {
            CrimsonSpell.clearOwnerMarker(owner);
            applyCooldown(owner);
        }
        super.die(source);
        CrimsonSusanoo.LOGGER.debug("Guardian {} died, owner={}", getUUID(), ownerId);
        level().playSound(null, blockPosition(), CrimsonSounds.DEATH, SoundSource.HOSTILE, 2, 0.6F);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_ANIMATION_TICKS && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && !cooldownApplied && !isManifesting()) {
            ServerPlayer owner = getOwnerPlayer();
            if (owner != null) {
                CrimsonSpell.clearOwnerMarker(owner);
                applyCooldown(owner);
            }
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        var controller = new AnimationController<CrimsonEntity>(this, "main", 3, state -> {
            // Pack cast-speed modifiers also shorten the preview's lifetime.
            // Start immediately and fit all three visual seconds into that lifetime.
            state.getController().setAnimationSpeed(isManifesting() ? 60.0 / entityData.get(MANIFEST_TICKS) : 1.0);
            // Timed states must enter at the server's current phase, including for a new tracker.
            boolean timed = isManifesting() || isDeadOrDying() || getFade() > 0
                    || getAction() != 0;
            state.getController().transitionLength(timed ? 0 : 3);
            if (isManifesting()) return state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.manifest"));
            if (isDeadOrDying()) return state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.death"));
            if (getFade() > 0 && isManaCollapsing()) return state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.collapse"));
            if (getFade() > 0) return state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.dismiss"));
            // Damage still applies, but cannot interrupt a committed sword swing.
            if (hurtTime > 0 && getAction() == 0) return state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.hurt"));
            return switch (getAction()) {
                case 1 -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.cleave"));
                case 2 -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.crescent"));
                case 3 -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.guardian.slash"));
                default -> state.setAndContinue(isVisuallyMoving()
                        ? RawAnimation.begin().thenLoop(entityData.get(FOLLOWING)
                                ? "animation.guardian.follow" : "animation.guardian.walk")
                        : RawAnimation.begin().thenLoop("animation.guardian.idle"));
            };
        }) {
            @Override protected double adjustTick(double tick) {
                double localTick = super.adjustTick(tick);
                // GeckoLib polls the next queued clip at transition tick zero.
                // Preserve that handoff before seeking the running clip to server time.
                if (getAnimationState() == AnimationController.State.TRANSITIONING) return localTick;
                double partialTick = tick - Math.floor(tick);
                if (isManifesting()) return getManifestAnimationTick(partialTick);
                if (isDeadOrDying() || getFade() > 0) return getEndingAnimationTick(partialTick);
                if (getAction() != 0) return getActionAnimationTick(partialTick);
                return localTick;
            }
        };
        var easing = new SmoothKeyframeEasing();
        controller.setOverrideEasingTypeFunction(entity -> easing.prepare(controller.getCurrentAnimation()));
        controllers.add(controller);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    private void clientAura() {
        if (isManifesting()) {
            clientManifestAura();
            return;
        }
        if (tickCount % 2 != 0) return;
        int count = getFade() > 0 ? 7 : 3;
        DustParticleOptions red = new DustParticleOptions(new Vector3f(0.75F, 0.03F, 0.08F), 1.1F);
        DustParticleOptions orange = new DustParticleOptions(new Vector3f(1.0F, 0.42F, 0.04F), 0.8F);
        for (int i = 0; i < count; i++) {
            double x = getX() + (random.nextDouble() - 0.5) * 3.5;
            double y = getY() + random.nextDouble() * 7;
            double z = getZ() + (random.nextDouble() - 0.5) * 3.5;
            level().addParticle(i % 3 == 0 ? orange : red, x, y, z, 0, 0.025 + random.nextDouble() * 0.035, 0);
        }
        for (int i = 0; i < 4; i++) {
            double angle = tickCount * 0.14 + i * Math.PI / 2;
            double radius = 2.15 + 0.3 * Math.sin(tickCount * 0.08 + i);
            double height = 0.8 + (tickCount * 0.11 + i * 1.7) % 6.5;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            level().addParticle(i == 0 ? orange : red, x, getY() + height, z,
                    -Math.sin(angle) * 0.025, 0.055, Math.cos(angle) * 0.025);
        }
        if (getFade() == 0) {
            Vec3 forward = visualForward();
            Vec3 right = visualRight(forward);
            // Sparse sparks peel off the rear mantle; the main fire stays attached.
            if (tickCount % 12 == 0 && random.nextFloat() < .65F) {
                double angle = random.nextDouble() * Math.PI;
                double rear = Math.sin(angle);
                Vec3 ember = position().add(right.scale(Math.cos(angle) * 2.05))
                        .add(forward.scale(-.35 - rear * 1.15))
                        .add(0, 6.65 + rear * .55, 0);
                Vec3 drift = forward.scale(-.035 - random.nextDouble() * .025)
                        .add(right.scale((random.nextDouble() - .5) * .03));
                level().addParticle(ParticleRegistry.EMBER_PARTICLE.get(), ember.x, ember.y, ember.z,
                        drift.x, .04 + random.nextDouble() * .035, drift.z);
            }
            // Katana heat and ripples are rendered from its animated bone. A second,
            // estimated body-space blade trail drifted away when wrists or torso moved.
        }
        if (tickCount % 10 == 0) {
            for (int i = 0; i < 10; i++) {
                double angle = (i + tickCount * 0.04) * Math.PI * 2 / 10;
                double radius = 2.7 + 0.18 * Math.sin(tickCount * 0.15);
                level().addParticle(i % 3 == 0 ? orange : red,
                        getX() + Math.cos(angle) * radius, getY() + 0.12,
                        getZ() + Math.sin(angle) * radius, 0, 0.015, 0);
            }
        }
        if (getFade() > 0 && getFade() < 26) {
            DustParticleOptions eyes = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.12F), 1.4F);
            Vec3 forward = visualForward();
            Vec3 side = visualRight(forward).scale(0.42);
            Vec3 eyeCenter = position().add(forward.scale(0.9)).add(0, 6.35, 0);
            level().addParticle(eyes, eyeCenter.x - side.x, eyeCenter.y, eyeCenter.z - side.z, 0, 0.02, 0);
            level().addParticle(eyes, eyeCenter.x + side.x, eyeCenter.y, eyeCenter.z + side.z, 0, 0.02, 0);
        }
    }

    // GeckoLib renders with the body's yaw, which can stay fixed while the
    // entity's head/look yaw changes. Anchor face and blade effects to the
    // same body angle as the geometry, including while standing still.
    private Vec3 visualForward() {
        double yaw = Math.toRadians(yBodyRot);
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private static Vec3 visualRight(Vec3 forward) {
        return new Vec3(forward.z, 0, -forward.x);
    }

    private void clientManifestAura() {
        // Keep particle stages aligned with the animation for modified cast speeds.
        int phase = (int) getManifestAnimationTick(0);
        DustParticleOptions crimson = new DustParticleOptions(new Vector3f(0.82F, 0.015F, 0.055F), 1.45F);
        DustParticleOptions ember = new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.025F), 1.2F);
        DustParticleOptions heart = new DustParticleOptions(new Vector3f(1.0F, 0.76F, 0.12F), 0.9F);
        if (tickCount % 5 == 0) {
            double radius = 3.0 + 0.25 * Math.sin(phase * 0.2);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6 + phase * 0.035;
                level().addParticle(i % 4 == 0 ? ember : crimson,
                        getX() + Math.cos(angle) * radius, getY() + 0.16,
                        getZ() + Math.sin(angle) * radius, 0, 0.025, 0);
            }
        }
        if (tickCount % 6 == 0 && phase >= 10) {
            for (int ring = 0; ring < 2; ring++) {
                double height = ring == 0 ? 2.7 : 5.3;
                double radius = ring == 0 ? 2.4 : 2.15;
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4 + phase * (ring == 0 ? 0.07 : -0.08);
                    level().addParticle(i % 3 == 0 ? ember : crimson,
                            getX() + Math.cos(angle) * radius, getY() + height,
                            getZ() + Math.sin(angle) * radius,
                            -Math.sin(angle) * 0.035, 0.03, Math.cos(angle) * 0.035);
                }
            }
        }
        if (tickCount % 2 != 0) return;
        double pillarHeight = Math.min(8.0, 0.8 + phase * 0.12);
        for (int i = 0; i < 4; i++) {
            double angle = phase * 0.16 + i * Math.PI / 2;
            double height = random.nextDouble() * pillarHeight;
            double radius = 0.65 + 0.22 * Math.sin(height * 1.8 + phase * 0.13);
            Vec3 rising = new Vec3(getX() + Math.cos(angle) * radius,
                    getY() + height, getZ() + Math.sin(angle) * radius);
            if (i == 0) {
                level().addParticle(ParticleRegistry.EMBER_PARTICLE.get(), rising.x, rising.y, rising.z, 0, 0.06, 0);
            } else {
                level().addParticle(ember, rising.x, rising.y, rising.z,
                        -Math.sin(angle) * 0.035, 0.12, Math.cos(angle) * 0.035);
            }
        }
        for (int i = 0; i < 5; i++) {
            double angle = phase * 0.2 + i * Math.PI * 2 / 5;
            double height = ((phase * 0.19 + i * 1.45) % 7.4);
            double radius = 1.1 + 0.16 * height + 0.2 * Math.sin(phase * 0.13);
            level().addParticle(i % 2 == 0 ? crimson : ember,
                    getX() + Math.cos(angle) * radius, getY() + height,
                    getZ() + Math.sin(angle) * radius,
                    -Math.sin(angle) * 0.04, 0.075, Math.cos(angle) * 0.04);
        }
        if (phase >= 12) {
            double crownHeight = Math.min(6.75, 3.5 + (phase - 12) * 0.105);
            for (int i = 0; i < 3; i++) {
                double angle = phase * 0.24 + i * Math.PI * 2 / 3;
                level().addParticle(ember,
                        getX() + Math.cos(angle) * 1.9, getY() + crownHeight,
                        getZ() + Math.sin(angle) * 1.9, 0, 0.09, 0);
            }
        }
        if (phase >= 40) {
            Vec3 forward = visualForward();
            Vec3 right = visualRight(forward);
            for (int i = 0; i < 3; i++) {
                double distance = 1.3 + random.nextDouble() * 4.1;
                Vec3 blade = position().add(right.scale(1.45 + Math.max(0, distance - 1.3) * 0.26))
                        .add(forward.scale(distance))
                        .add(0, 2.25 + distance * 0.11, 0);
                level().addParticle(i == 0 ? heart : ember, blade.x, blade.y, blade.z,
                        0, 0.07, 0);
                if (i == 0) {
                    level().addParticle(new FlameStrikeParticleOptions((float) forward.x, 0,
                                    (float) forward.z, false, false, 0.3F),
                            blade.x, blade.y, blade.z, 0, 0.06, 0);
                }
            }
        }
        if (phase >= 52) {
            double radius = 2.2 + (phase - 52) * 0.35;
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                level().addParticle(i % 2 == 0 ? heart : ember,
                        getX() + Math.cos(angle) * radius, getY() + 0.3,
                        getZ() + Math.sin(angle) * radius,
                        Math.cos(angle) * 0.12, 0.04, Math.sin(angle) * 0.12);
            }
        }
    }
}
