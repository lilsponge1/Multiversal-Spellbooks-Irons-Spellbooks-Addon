package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, GrandExplosionMod.ID);
    public static final RegistryObject<AbstractSpell> GRAND_EXPLOSION = SPELLS.register("grand_explosion", GrandExplosionSpell::new);
    public static final RegistryObject<AbstractSpell> THUNDERCRASH = SPELLS.register("thundercrash", ThundercrashSpell::new);
    private ModSpells() {}
}
