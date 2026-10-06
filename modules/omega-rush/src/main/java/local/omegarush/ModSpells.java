package local.omegarush;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.*;
public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,OmegaMod.ID);
    public static final RegistryObject<AbstractSpell> OMEGA_RUSH = SPELLS.register("omega_rush",OmegaSpell::new);
    private ModSpells() {}
}
