package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.*;
import java.util.List;

/** A single spell slot; form and activation cost belong to the server controller. */
public final class SaiyanSpell extends AbstractSpell {
    public static final ResourceLocation ID = new ResourceLocation(GrandExplosionMod.ID, "saiyan_ascension");
    private static final DefaultConfig CONFIG = new DefaultConfig().setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE).setMaxLevel(1).setCooldownSeconds(0).setAllowCrafting(false).build();
    public SaiyanSpell() { baseManaCost = 0; castTime = 0; baseSpellPower = 0; }
    @Override public ResourceLocation getSpellResource() { return ID; }
    @Override public DefaultConfig getDefaultConfig() { return CONFIG; }
    @Override public CastType getCastType() { return CastType.INSTANT; }
    // Native casting must never pre-charge mana: holding to power down is free even at zero mana.
    @Override public int getManaCost(int level) { return 0; }
    @Override public int getSpellCooldown() { return 0; }
    @Override public boolean checkPreCastConditions(Level level, int n, LivingEntity caster, MagicData data) {
        return caster instanceof net.minecraft.world.entity.player.Player;
    }
    @Override public void onCast(Level level, int n, LivingEntity caster, CastSource source, MagicData data) {
        // Compatibility entry point for native/quick-cast packets. Upgrading still requires a release packet.
        if (caster instanceof ServerPlayer player) SaiyanManager.nativeActivation(player);
    }
    @Override public List<MutableComponent> getUniqueInfo(int n, LivingEntity caster) {
        return List.of(Component.m_237110_("ui.irons_ultimate_explosion.saiyan.cost", SaiyanConfig.SSJ1.cost().get(), SaiyanConfig.SSJ2.cost().get(),SaiyanConfig.SSJ3.cost().get()),
                Component.m_237110_("ui.irons_ultimate_explosion.saiyan.drain", SaiyanConfig.SSJ1.drain().get(), SaiyanConfig.SSJ2.drain().get(),SaiyanConfig.SSJ3.drain().get()),
                Component.m_237110_("ui.irons_ultimate_explosion.saiyan.controls", SaiyanConfig.HOLD.get() / 20.0));
    }
}
