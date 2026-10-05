package com.crimson_susanoo.spell;

import com.crimson_susanoo.CrimsonSusanoo;
import com.crimson_susanoo.CrimsonSounds;
import com.crimson_susanoo.ServerConfig;
import com.crimson_susanoo.entity.CrimsonEntity;
import com.crimson_susanoo.entity.SafePlacement;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.List;

public final class CrimsonSpell extends AbstractSpell {
    public static final String ACTIVE_ID = "CrimsonSusanooActive";
    public static final String ACTIVE_READY = "CrimsonSusanooFullySummoned";
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CrimsonSusanoo.MODID, "crimson_susanoo");
    private final DefaultConfig defaults = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1).setCooldownSeconds(300).setAllowCrafting(true).build();

    public CrimsonSpell() {
        baseManaCost = 150;
        manaCostPerLevel = 0;
        baseSpellPower = 1;
        spellPowerPerLevel = 0;
        castTime = 80;
    }

    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return defaults; }
    @Override public SchoolType getSchoolType() { return SchoolRegistry.FIRE.get(); }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public int getManaCost(int level) { return ServerConfig.INITIAL_MANA.get(); }
    @Override public int getSpellCooldown() { return ServerConfig.COOLDOWN.get() * 20; }

    @Override public int getEffectiveCastTime(int level, LivingEntity caster) {
        return caster != null && MagicData.getPlayerMagicData(caster).getPlayerRecasts().hasRecastForSpell(this)
                ? 0 : super.getEffectiveCastTime(level, caster);
    }

    // Register the recall manually after manifestation; keeping getRecastCount at
    // its default also preserves Iron's support for the initial consumable scroll.
    private void enableRecall(ServerPlayer owner, int level, CastSource source) {
        MagicData magic = MagicData.getPlayerMagicData(owner);
        // Iron counts the initial summon as the first of the two total casts.
        magic.getPlayerRecasts().addRecast(new RecastInstance(getSpellId(), level, 2,
                ServerConfig.DURATION.get() * 20 + 40, source, null), magic);
    }

    @Override
    public void onRecastFinished(ServerPlayer owner, RecastInstance instance,
                                 RecastResult result, ICastDataSerializable castData) {
        CrimsonEntity guardian = findActive(owner, owner.serverLevel());
        if (guardian != null) guardian.dismiss("recall_" + result.name().toLowerCase(java.util.Locale.ROOT));
        // The guardian owns cooldown application, including scroll summons.
    }

    // Iron displays these on scrolls and spellbook entries. Config values are
    // synced to clients so these numbers follow the world's actual settings.
    @Override
    public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        float power = getSpellPower(1, caster);
        return List.of(
                Component.translatable("ui.crimson_susanoo.cleave", formatDamage(ServerConfig.CLEAVE_DAMAGE.get(), power)),
                Component.translatable("ui.crimson_susanoo.crescent", formatDamage(ServerConfig.CRESCENT_DAMAGE.get(), power), ServerConfig.CRESCENT_MANA.get()),
                Component.translatable("ui.crimson_susanoo.slash", formatDamage(ServerConfig.SLASH_DAMAGE.get(), power), formatDamage(ServerConfig.SLASH_DAMAGE.get() * 0.5, power), ServerConfig.SLASH_MANA.get()),
                Component.translatable("ui.crimson_susanoo.sustain", getManaCost(level), formatValue(ServerConfig.UPKEEP.get()), ServerConfig.DURATION.get()));
    }

    // Match the damage float used by the guardian, with compact tooltip precision.
    private static String formatDamage(double base, float power) {
        return java.math.BigDecimal.valueOf((float) (base * power * ServerConfig.DAMAGE_MULTIPLIER.get()))
                .setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String formatValue(double value) {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    // Iron normally starts cooldown on cast. Pass false and use its cooldown manager at dismissal.
    @Override
    public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean applyCooldown) {
        MagicData data = MagicData.getPlayerMagicData(player);
        // An instant recall can finish a tick after expiry/death cleared its
        // recast. Never turn that late completion into a fresh summon.
        if (data.getPlayerCooldowns().isOnCooldown(this)) return;
        if (data.getPlayerRecasts().hasRecastForSpell(this)) {
            CrimsonEntity guardian = level instanceof ServerLevel serverLevel ? findActive(player, serverLevel) : null;
            if (guardian != null && !guardian.isManifesting()) guardian.dismiss("voluntary");
            return;
        }
        float manaBeforeCast = data.getMana();
        // Iron's scroll source normally ignores mana. This ultimate charges every player cast.
        if (!source.consumesMana() && !CrimsonEntity.chargeMana(player, getManaCost(spellLevel))) return;
        super.castSpell(level, spellLevel, player, source, false);
        if (level instanceof ServerLevel serverLevel
                && (!player.getPersistentData().getBoolean(ACTIVE_READY) || findActive(player, serverLevel) == null)) {
            // Iron's cast event can change the deducted amount; restore the exact pre-cast balance.
            data.setMana(manaBeforeCast);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        }
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!(caster instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return false;
        CrimsonEntity active = findActive(player, serverLevel);
        if (active != null) return !active.isManifesting() && data.getPlayerRecasts().hasRecastForSpell(this);
        return data.getMana() >= getManaCost(spellLevel)
                && !data.getPlayerCooldowns().isOnCooldown(this)
                && active == null;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!(caster instanceof ServerPlayer owner) || !(level instanceof ServerLevel serverLevel)) return;
        CrimsonEntity guardian = findActive(owner, serverLevel);
        if (guardian == null) guardian = createGuardian(owner, serverLevel, 0);
        if (guardian == null) return;
        guardian.activate();
        owner.getPersistentData().putBoolean(ACTIVE_READY, true);
        enableRecall(owner, spellLevel, source);
        impact(serverLevel, guardian.position());
        CrimsonSusanoo.LOGGER.debug("Crimson Susanoo summoned for {} ({})", owner.getGameProfile().getName(), owner.getUUID());
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (!(caster instanceof ServerPlayer owner) || !(level instanceof ServerLevel serverLevel)) return;
        int total = Math.max(1, data.getCastDuration());
        int elapsed = total - data.getCastDurationRemaining();
        if (elapsed <= 0) return;
        if (elapsed >= total / 4 && elapsed % 10 == 0 && findActive(owner, serverLevel) == null) {
            createGuardian(owner, serverLevel, data.getCastDurationRemaining());
        }
        if (elapsed == 1) level.playSound(null, owner.blockPosition(), CrimsonSounds.GATHERING, SoundSource.PLAYERS, 1.4F, 0.5F);
        if (elapsed == total / 4) level.playSound(null, owner.blockPosition(), CrimsonSounds.SKELETON, SoundSource.PLAYERS, 1.3F, 0.5F);
        if (elapsed == total / 2) level.playSound(null, owner.blockPosition(), CrimsonSounds.FULL_MANIFESTATION, SoundSource.PLAYERS, 2, 0.55F);
        if (elapsed == total * 3 / 4) level.playSound(null, owner.blockPosition(), CrimsonSounds.SWORD_CREATION, SoundSource.PLAYERS, 2, 0.55F);
        // The preview entity draws the pillar, spiral, armor and sword effects locally.
        // Only the first-second gathering ring needs a packet before that entity exists.
        if (elapsed % 5 != 0 || elapsed >= total / 4) return;
        DustParticleOptions crimson = new DustParticleOptions(new Vector3f(0.95F, 0.04F, 0.08F), 1.8F);
        double angle = elapsed * 0.38;
        for (int i = 0; i < 6; i++) {
            double a = angle + i * Math.PI / 3;
            serverLevel.sendParticles(crimson, owner.getX() + Math.cos(a) * 2,
                    owner.getY() + 0.3 + elapsed * 0.025, owner.getZ() + Math.sin(a) * 2,
                    1, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private CrimsonEntity createGuardian(ServerPlayer owner, ServerLevel level, int remainingCastTicks) {
        boolean preview = remainingCastTicks > 0;
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (guardian == null) return null;
        Vec3 position = findPosition(level, owner, guardian);
        if (position == null) return null;
        guardian.setOwner(owner);
        guardian.moveTo(position.x, position.y, position.z, owner.getYRot(), 0);
        // LivingEntity renders its body/head yaw independently of Entity's spawn yaw.
        guardian.setYBodyRot(owner.getYRot());
        guardian.setYHeadRot(owner.getYRot());
        guardian.yBodyRotO = owner.getYRot();
        guardian.yHeadRotO = owner.getYRot();
        if (preview) guardian.startManifesting(remainingCastTicks);
        if (!level.addFreshEntity(guardian)) return null;
        owner.getPersistentData().putUUID(ACTIVE_ID, guardian.getUUID());
        owner.getPersistentData().remove(ACTIVE_READY);
        CrimsonSusanoo.LOGGER.debug("Created {} guardian {} for owner {}", preview ? "manifesting" : "active", guardian.getUUID(), owner.getUUID());
        return guardian;
    }

    /** Operator visual checks use the normal safe placement and owner setup without a held cast key. */
    public CrimsonEntity summonForVisualTest(ServerPlayer owner, ServerLevel level) {
        if (findActive(owner, level) != null) return null;
        CrimsonEntity guardian = createGuardian(owner, level, 0);
        if (guardian != null) {
            guardian.activate();
            owner.getPersistentData().putBoolean(ACTIVE_READY, true);
            enableRecall(owner, 1, CastSource.SPELLBOOK);
        }
        return guardian;
    }

    private Vec3 findPosition(ServerLevel level, ServerPlayer owner, CrimsonEntity guardian) {
        for (int radius = 5; radius <= 9; radius += 2) {
            for (int i = 0; i < 8; i++) {
                // Minecraft yaw 0 faces +Z; the polar search starts straight ahead.
                double angle = i * Math.PI / 4 + owner.getYRot() * Math.PI / 180 + Math.PI / 2;
                int x = (int) Math.floor(owner.getX() + radius * Math.cos(angle));
                int z = (int) Math.floor(owner.getZ() + radius * Math.sin(angle));
                for (int deltaY = -2; deltaY <= 3; deltaY++) {
                    BlockPos feet = BlockPos.containing(x, owner.getY() + deltaY, z);
                    if (!level.getBlockState(feet.below()).isSolidRender(level, feet.below())) continue;
                    Vec3 position = Vec3.atBottomCenterOf(feet);
                    if (SafePlacement.isClear(level, guardian, position)) return position;
                }
            }
        }
        return null;
    }

    private void impact(ServerLevel level, Vec3 center) {
        DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.02F), 2.2F);
        for (int i = 0; i < 32; i++) {
            double angle = 2 * Math.PI * i / 32;
            level.sendParticles(red, center.x + Math.cos(angle) * 4, center.y + 0.3,
                    center.z + Math.sin(angle) * 4, 2, 0.1, 0.1, 0.1, 0.05);
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 2, center.z, 80, 2, 3, 2, 0.13);
        level.playSound(null, BlockPos.containing(center), CrimsonSounds.FINAL_IMPACT, SoundSource.PLAYERS, 2.4F, 0.55F);
    }

    public static CrimsonEntity findActive(ServerPlayer player, ServerLevel level) {
        if (!player.getPersistentData().hasUUID(ACTIVE_ID)) {
            clearStaleSummon(player);
            return null;
        }
        UUID id = player.getPersistentData().getUUID(ACTIVE_ID);
        if (level.getEntity(id) instanceof CrimsonEntity guardian && guardian.isOwnedBy(player)
                && guardian.isAlive() && !guardian.isDismissing()) return guardian;
        clearStaleSummon(player);
        return null;
    }

    public static void clearOwnerMarker(ServerPlayer player) {
        player.getPersistentData().remove(ACTIVE_ID);
        player.getPersistentData().remove(ACTIVE_READY);
        var recasts = MagicData.getPlayerMagicData(player).getPlayerRecasts();
        String spellId = CrimsonSusanoo.SPELL.get().getSpellId();
        if (recasts.hasRecastForSpell(spellId)) {
            recasts.removeRecast(recasts.getRecastInstance(spellId), RecastResult.USER_CANCEL);
        }
    }

    private static void clearStaleSummon(ServerPlayer player) {
        boolean fullySummoned = player.getPersistentData().getBoolean(ACTIVE_READY);
        clearOwnerMarker(player);
        if (fullySummoned) {
            MagicData magic = MagicData.getPlayerMagicData(player);
            if (!magic.getPlayerCooldowns().isOnCooldown(CrimsonSusanoo.SPELL.get())) {
                MagicHelper.MAGIC_MANAGER.addCooldown(player, CrimsonSusanoo.SPELL.get(), CastSource.SPELLBOOK);
            }
        }
    }
}
