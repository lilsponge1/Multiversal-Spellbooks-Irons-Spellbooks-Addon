package local.omegarush;
import net.minecraft.world.effect.*;
import net.minecraftforge.registries.*;
public final class OmegaEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,OmegaMod.ID);
    public static final RegistryObject<MobEffect> FORM=EFFECTS.register("omega_form",()->new MobEffect(MobEffectCategory.BENEFICIAL,0xDC61FA){});
    private OmegaEffects(){}
}
