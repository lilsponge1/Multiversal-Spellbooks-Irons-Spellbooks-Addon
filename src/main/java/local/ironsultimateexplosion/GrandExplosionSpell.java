package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.CameraShakeData;
import io.redspace.ironsspellbooks.api.util.CameraShakeManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class GrandExplosionSpell extends AbstractSpell {
    private static final Map<UUID, TargetLock> TARGETS = new HashMap<>();
    public static final ResourceLocation ID = new ResourceLocation(GrandExplosionMod.ID, "grand_explosion");
    private static final ResourceLocation SPARK_SOUND = new ResourceLocation("minecraft", "block.redstone_torch.burnout");
    private static final ResourceLocation ARC_SOUND = new ResourceLocation("minecraft", "entity.lightning_bolt.impact");
    private static final ResourceLocation IMPACT_SOUND = new ResourceLocation("minecraft", "entity.generic.explode");
    private static final ResourceLocation BALLISTIX_BOOM = new ResourceLocation("ballistix", "nuclearexplosion");
    private static final ResourceLocation ALEX_BOOM = new ResourceLocation("alexscaves", "nuclear_explosion");
    private static final DefaultConfig CONFIG = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(120)
            .setAllowCrafting(false)
            .build();

    public GrandExplosionSpell() {
        baseManaCost = 80;
        manaCostPerLevel = 0;
        baseSpellPower = 12;
        spellPowerPerLevel = 2;
        castTime = 120;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return CONFIG; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getEffectiveCastTime(int level, LivingEntity caster) { return Math.max(80, super.getEffectiveCastTime(level, caster)); }
    @Override public boolean allowLooting() { return false; }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData data, Player player) {
        boolean staffCast = source == CastSource.SWORD && player.m_21205_().m_150930_(ModItems.CINDERSTAR_STAFF.get())
                && io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CONSUME_MANA.get();
        if (source != CastSource.SPELLBOOK && source != CastSource.COMMAND && !staffCast)
            return new CastResult(CastResult.Type.FAILURE, Component.m_237115_("message.irons_ultimate_explosion.spellbook_only"));
        return super.canBeCastedBy(spellLevel, source, data, player);
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (level instanceof ServerLevel server) {
            server.m_6263_(null, caster.m_20185_(), caster.m_20186_(), caster.m_20189_(),
                    SoundRegistry.FIRE_BOMB_CHARGE.get(), SoundSource.PLAYERS, 0.5f, 0.65f);
            if (caster instanceof ServerPlayer player) {
                Vec3 point = RaycastBuilder.begin(level, caster).range(48).checkForBlocks(true).build().m_82450_();
                long seed = server.m_46467_() ^ player.m_20148_().getMostSignificantBits();
                int duration = getEffectiveCastTime(spellLevel, caster);
                long now = server.m_46467_();
                TARGETS.put(player.m_20148_(), new TargetLock(point, server.m_46472_().m_135782_().toString(), seed,
                        now, duration, now + duration + 20, now));
                ExplosionNetwork.charge(server, point, ExplosionConfig.VISUAL_RADIUS.get(), seed, duration);
            }
        }
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData data, boolean interrupted) {
        if (caster instanceof ServerPlayer player && level instanceof ServerLevel server) {
            TargetLock unused = TARGETS.remove(player.m_20148_());
            if (unused != null) ExplosionNetwork.cancel(server, unused.point(), ExplosionConfig.VISUAL_RADIUS.get(), unused.seed());
        }
        super.onServerCastComplete(level, spellLevel, caster, data, interrupted);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.m_46467_();
        if (caster instanceof ServerPlayer player) {
            TargetLock locked = TARGETS.get(player.m_20148_());
            if (locked != null && locked.dimension().equals(server.m_46472_().m_135782_().toString())) {
                long elapsed = now - locked.startedAt();
                if (elapsed >= 0 && elapsed < locked.duration() && now >= locked.nextSparkAt()) {
                    float progress = Math.min(1.0f, elapsed / (float)locked.duration());
                    int interval = Math.max(4, 16 - Math.round(12 * progress));
                    SoundEvent spark = ForgeRegistries.SOUND_EVENTS.getValue(SPARK_SOUND);
                    if (spark != null) server.m_6263_(null, locked.point().f_82479_, locked.point().f_82480_, locked.point().f_82481_,
                            spark, SoundSource.BLOCKS, 8.0f, 0.8f + 0.7f * progress);
                    if (spark != null) server.m_6263_(null, caster.m_20185_(), caster.m_20186_(), caster.m_20189_(),
                            spark, SoundSource.PLAYERS, 1.25f, 0.8f + 0.7f * progress);
                    if (progress >= 0.8f) {
                        SoundEvent arc = ForgeRegistries.SOUND_EVENTS.getValue(ARC_SOUND);
                        if (arc != null) {
                            server.m_6263_(null, locked.point().f_82479_, locked.point().f_82480_, locked.point().f_82481_,
                                    arc, SoundSource.BLOCKS, 6.0f, 1.0f + 0.4f * progress);
                            server.m_6263_(null, caster.m_20185_(), caster.m_20186_(), caster.m_20189_(),
                                    arc, SoundSource.PLAYERS, 0.75f, 1.0f + 0.4f * progress);
                        }
                    }
                    TARGETS.put(player.m_20148_(), new TargetLock(locked.point(), locked.dimension(), locked.seed(),
                            locked.startedAt(), locked.duration(), locked.expiresAt(), now + interval));
                }
            }
        }
        if (now % 5 == 0) {
            double x = caster.m_20185_(), y = caster.m_20186_() + 1.0, z = caster.m_20189_();
            server.m_8767_(ParticleTypes.f_123744_, x, y, z, 8, 0.8, 0.8, 0.8, 0.04);
            server.m_8767_(ParticleTypes.f_123762_, x, y, z, 2, 0.6, 0.7, 0.6, 0.01);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!(level instanceof ServerLevel server)) return;
        TargetLock locked = caster instanceof ServerPlayer player ? TARGETS.remove(player.m_20148_()) : null;
        if (locked != null && (!locked.dimension().equals(server.m_46472_().m_135782_().toString()) ||
                server.m_46467_() > locked.expiresAt())) locked = null;
        boolean playerCast = caster instanceof ServerPlayer && (source == CastSource.SPELLBOOK || source == CastSource.SWORD);
        float spent = 0;
        if (playerCast) {
            ServerPlayer player = (ServerPlayer)caster;
            spent = Math.max(0, SpellEvents.takePreCastMana(player) - data.getMana());
            if (spent < 1 && (!player.m_7500_() || io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_MANA_COST.get())) {
                if (locked != null) ExplosionNetwork.cancel(server, locked.point(), ExplosionConfig.VISUAL_RADIUS.get(), locked.seed());
                return;
            }
        }
        Vec3 point = locked != null ? locked.point() :
                RaycastBuilder.begin(level, caster).range(48).checkForBlocks(true).build().m_82450_();
        boolean regalia = CinderstarArmorItem.hasFullSet(caster);
        double r = regalia ? 80.0 : 60.0;
        AABB area = new AABB(point.f_82479_ - r, point.f_82480_ - r, point.f_82481_ - r,
                point.f_82479_ + r, point.f_82480_ + r, point.f_82481_ + r);
        List<Entity> targets = level.m_6249_(caster, area, e -> e instanceof LivingEntity);
        targets.sort(Comparator.comparingDouble(e -> distanceSquared(e.m_20182_(), point)));
        float center = (float)Math.min(160, ExplosionConfig.DAMAGE_MULTIPLIER.get() *
                (30 + 2 * Math.max(0, Math.min(40, getSpellPower(spellLevel, caster)))
                        + 12 * Math.sqrt(Math.min(spent, 1600) / 100.0)));
        if (regalia) center = Math.min(320, center * 2);
        int count = 0;
        for (Entity entity : targets) {
            if (count++ >= 512) break;
            double dsq = distanceSquared(entity.m_20182_(), point);
            if (dsq > r * r) continue;
            if (entity instanceof Player && !ExplosionConfig.PVP_DAMAGE.get()) continue;
            double distance = Math.sqrt(dsq);
            float damage = (float)(center * Math.max(0.2, 1 - 0.8 * Math.pow(distance / r, 1.5)));
            if (DamageSources.applyDamage(entity, damage, getDamageSource(caster))) {
                double nx = entity.m_20185_() - point.f_82479_, nz = entity.m_20189_() - point.f_82481_;
                double horizontal = Math.max(0.001, Math.sqrt(nx*nx + nz*nz));
                double force = 1.8 * (1 - distance / r);
                Vec3 motion = entity.m_20184_();
                entity.m_20334_(Math.max(-1.8, Math.min(1.8, motion.f_82479_ + nx/horizontal*force)),
                        Math.max(-0.6, Math.min(0.6, motion.f_82480_ + 0.6 * (1 - distance / r))),
                        Math.max(-1.8, Math.min(1.8, motion.f_82481_ + nz/horizontal*force)));
            }
        }
        long seed = locked != null ? locked.seed() : level.m_46467_() ^ caster.m_20148_().getMostSignificantBits();
        if (caster instanceof ServerPlayer player) CraterManager.enqueue(server, player, point, seed);
        ExplosionNetwork.detonate(server, point, ExplosionConfig.VISUAL_RADIUS.get(), seed);
        if (!ModList.get().isLoaded("ballistix"))
            CameraShakeManager.addCameraShake(new CameraShakeData(level, 16, point, 48));
        if (ModList.get().isLoaded("traveloptics")) TravelopticsFlash.show(server, point);
        SoundEvent nuclear = ModList.get().isLoaded("ballistix")
                ? ForgeRegistries.SOUND_EVENTS.getValue(BALLISTIX_BOOM) : null;
        if (nuclear == null && ModList.get().isLoaded("alexscaves"))
            nuclear = ForgeRegistries.SOUND_EVENTS.getValue(ALEX_BOOM);
        if (nuclear != null && ModList.get().isLoaded("ballistix")) {
            server.m_6263_(null, point.f_82479_, point.f_82480_, point.f_82481_, nuclear, SoundSource.BLOCKS, 25.0f, 1.0f);
        } else {
            if (nuclear != null)
                server.m_6263_(null, point.f_82479_, point.f_82480_, point.f_82481_, nuclear, SoundSource.BLOCKS, 8.0f, 1.0f);
            SoundEvent impact = ForgeRegistries.SOUND_EVENTS.getValue(IMPACT_SOUND);
            if (impact != null)
                server.m_6263_(null, point.f_82479_, point.f_82480_, point.f_82481_, impact, SoundSource.BLOCKS, 3.0f, 0.65f);
            server.m_6263_(null, point.f_82479_, point.f_82480_, point.f_82481_, SoundRegistry.FIERY_EXPLOSION.get(),
                    SoundSource.BLOCKS, 2.0f, 0.75f);
        }
        int exhaustion = ExplosionConfig.EXHAUSTION_SECONDS.get();
        if (exhaustion > 0) caster.m_7292_(new MobEffectInstance(MobEffects.f_19597_, exhaustion * 20, 1));
    }

    private static double distanceSquared(Vec3 a, Vec3 b) {
        double x = a.f_82479_ - b.f_82479_, y = a.f_82480_ - b.f_82480_, z = a.f_82481_ - b.f_82481_;
        return x*x + y*y + z*z;
    }

    private record TargetLock(Vec3 point, String dimension, long seed, long startedAt, int duration,
                              long expiresAt, long nextSparkAt) {}
}
