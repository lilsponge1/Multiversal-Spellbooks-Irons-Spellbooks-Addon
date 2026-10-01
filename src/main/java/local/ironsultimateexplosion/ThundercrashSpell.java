package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import java.util.Optional;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ThundercrashSpell extends AbstractSpell {
    public static final ResourceLocation ID = new ResourceLocation(GrandExplosionMod.ID, "thundercrash");
    private static final DefaultConfig CONFIG = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE).setMaxLevel(5).setCooldownSeconds(120).setAllowCrafting(true).build();
    public ThundercrashSpell() { baseManaCost = 150; baseSpellPower = 40; spellPowerPerLevel = 10; castTime = 10; }
    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return CONFIG; }
    @Override public CastType getCastType() { return CastType.LONG; }
    @Override public float getSpellPower(int spellLevel, net.minecraft.world.entity.Entity caster) {
        int level = Math.max(1, spellLevel);
        double configured = ThundercrashConfig.BASE_POWER.get()
                + (double)ThundercrashConfig.POWER_PER_LEVEL.get() * (level - 1);
        double original = baseSpellPower + (double)spellPowerPerLevel * (level - 1);
        // Scale only base progression; Iron's applies all attributes and its configured multiplier once.
        return (float)(super.getSpellPower(level, caster) * configured / original);
    }
    @Override public int getManaCost(int level) { return ThundercrashConfig.MANA.get(); }
    @Override public int getSpellCooldown() { return ThundercrashConfig.COOLDOWN.get() * 20; }
    @Override public int getEffectiveCastTime(int level, LivingEntity caster) { return ThundercrashConfig.CHARGE.get(); }
    @Override public AnimationHolder getCastStartAnimation() { return SpellAnimations.PREPARE_CROSS_ARMS; }
    @Override public Optional<SoundEvent> getCastStartSound() { return Optional.empty(); }
    @Override public Optional<SoundEvent> getCastFinishSound() { return Optional.empty(); }
    @Override public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
        return List.of(Component.m_237110_("ui.irons_ultimate_explosion.thundercrash.damage",
                        String.format(java.util.Locale.ROOT,"%.1f",getSpellPower(level,caster)*ThundercrashConfig.DAMAGE.get())),
                Component.m_237110_("ui.irons_ultimate_explosion.thundercrash.radius",ThundercrashConfig.RADIUS.get()),
                Component.m_237110_("ui.irons_ultimate_explosion.thundercrash.duration",ThundercrashConfig.DURATION.get()/20.0));
    }
    @Override public CastResult canBeCastedBy(int spellLevel, CastSource source, MagicData data, net.minecraft.world.entity.player.Player player) {
        CastResult normal = super.canBeCastedBy(spellLevel, source, data, player);
        if (player instanceof ServerPlayer p && !ThundercrashManager.eligible(p))
            return new CastResult(CastResult.Type.FAILURE, Component.m_237115_("message.irons_ultimate_explosion.thundercrash.unavailable"));
        return normal;
    }
    @Override public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (level.f_46443_) return caster instanceof net.minecraft.world.entity.player.Player;
        return caster instanceof ServerPlayer p && ThundercrashManager.eligible(p) && super.checkPreCastConditions(level, spellLevel, caster, data);
    }
    @Override public void onServerPreCast(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (caster instanceof ServerPlayer p) ThundercrashManager.charge(p, spellLevel);
        super.onServerPreCast(level, spellLevel, caster, data);
    }
    @Override public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof ServerPlayer p) ThundercrashManager.start(p, spellLevel, getSpellPower(spellLevel, caster));
        super.onCast(level, spellLevel, caster, source, data);
    }
    @Override public void onServerCastComplete(Level level, int spellLevel, LivingEntity caster, MagicData data, boolean interrupted) {
        if (interrupted && caster instanceof ServerPlayer p) ThundercrashManager.cancelCharge(p);
        super.onServerCastComplete(level, spellLevel, caster, data, interrupted);
    }
}
