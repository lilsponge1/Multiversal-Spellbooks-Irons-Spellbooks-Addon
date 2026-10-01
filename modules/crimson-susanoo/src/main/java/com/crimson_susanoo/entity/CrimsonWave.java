package com.crimson_susanoo.entity;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.CrimsonSounds;
import com.crimson_susanoo.ServerConfig;
import com.github.L_Ender.cataclysm.init.ModEffect;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CrimsonWave extends Entity {
    private static final EntityDataAccessor<Float> DIRECTION_X = SynchedEntityData.defineId(CrimsonWave.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z = SynchedEntityData.defineId(CrimsonWave.class, EntityDataSerializers.FLOAT);
    private final Set<UUID> hit = new HashSet<>();
    private UUID ownerId;
    private UUID guardianId;
    private Vec3 direction = Vec3.ZERO;
    private float damage;
    private int age;

    public CrimsonWave(EntityType<? extends CrimsonWave> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void setup(ServerPlayer owner, CrimsonEntity guardian, Vec3 vector, float damage) {
        ownerId = owner.getUUID();
        guardianId = guardian.getUUID();
        direction = new Vec3(vector.x, 0, vector.z).normalize();
        entityData.set(DIRECTION_X, (float) direction.x);
        entityData.set(DIRECTION_Z, (float) direction.z);
        this.damage = damage;
    }

    @Override protected void defineSynchedData() {
        entityData.define(DIRECTION_X, 0.0F);
        entityData.define(DIRECTION_Z, 0.0F);
    }
    public Vec3 getVisualDirection() {
        return new Vec3(entityData.get(DIRECTION_X), 0, entityData.get(DIRECTION_Z));
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTrail();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level() || !(serverLevel.getEntity(guardianId) instanceof CrimsonEntity guardian)
                || !guardian.isAlive() || guardian.isDismissing()) {
            discard(); return;
        }
        advance(serverLevel, owner, guardian);
    }

    // Separated from PlayerList lookup so the opt-in server diagnostics can use a fake player.
    void advance(ServerLevel serverLevel, ServerPlayer owner, CrimsonEntity guardian) {
        if (++age > 16) { discard(); return; }
        Vec3 nextPosition = position().add(direction);
        if (serverLevel.clip(new ClipContext(position(), nextPosition, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
            discard();
            return;
        }
        setPos(nextPosition);
        if (age % 5 == 0) serverLevel.playSound(null, blockPosition(), CrimsonSounds.CRESCENT_TRAVEL, SoundSource.HOSTILE, 0.5F, 0.7F);
        AABB sweep = getBoundingBox().inflate(0.2, 0.2, 0.2);
        for (LivingEntity enemy : serverLevel.getEntitiesOfClass(LivingEntity.class, sweep,
                e -> guardian.canStrike(owner, e) && !hit.contains(e.getUUID()))) {
            if (hit.size() >= 8) break;
            Vec3 target = enemy.getBoundingBox().getCenter();
            if (serverLevel.clip(new ClipContext(position(), target, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
            hit.add(enemy.getUUID());
            if (enemy.hurt(CrimsonSusanoo.SPELL.get().getDamageSource(this, owner).get(), damage)) {
                applyBrand(enemy);
                enemy.setSecondsOnFire(4);
                serverLevel.playSound(null, enemy.blockPosition(), CrimsonSounds.CRESCENT_IMPACT, SoundSource.HOSTILE, 1.1F, 0.75F);
            }
        }
    }

    private void clientTrail() {
        // A narrow luminous arc remains readable without building a curtain of dust.
        if ((tickCount & 1) != 0) return;
        Vec3 travel = getVisualDirection();
        if (travel.lengthSqr() < 0.5) return;
        Vec3 side = new Vec3(-travel.z, 0, travel.x);
        DustParticleOptions orange = new DustParticleOptions(new Vector3f(1.0F, 0.32F, 0.025F), 0.65F);
        DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.16F), 0.4F);
        for (int i = -4; i <= 4; i++) {
            double width = i * 0.55;
            double height = 0.2 + 1.2 * Math.abs(i) / 4.0;
            Vec3 point = position().add(side.scale(width)).add(0, height, 0);
            level().addParticle(orange, point.x, point.y, point.z, 0, 0.004, 0);
            if (i % 4 == 0) {
                level().addParticle(gold, point.x, point.y + 0.04, point.z, 0, 0.006, 0);
            }
        }
    }

    private void applyBrand(LivingEntity enemy) {
        MobEffect brand = ModEffect.EFFECTBLAZING_BRAND.get();
        MobEffectInstance existing = enemy.getEffect(brand);
        int maximum = ServerConfig.BRAND_MAX_LEVEL.get() - 1;
        if (existing != null && existing.getAmplifier() > maximum) return;
        int amplifier = existing == null || !ServerConfig.BRAND_STACKING.get()
                ? 0 : Math.min(maximum, existing.getAmplifier() + 1);
        enemy.addEffect(new MobEffectInstance(brand, ServerConfig.BRAND_DURATION.get() * 20, amplifier));
        CrimsonSusanoo.LOGGER.debug("Applied Blazing Brand amplifier {} to {}", amplifier, enemy.getUUID());
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
