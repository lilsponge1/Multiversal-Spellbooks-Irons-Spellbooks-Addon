package local.kischool;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.*;

public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, KiSchoolMod.ID);
    public static final RegistryObject<AbstractSpell> SAIYAN_ASCENSION = SPELLS.register("saiyan_ascension", SaiyanSpell::new);
    private ModSpells() {}
}
