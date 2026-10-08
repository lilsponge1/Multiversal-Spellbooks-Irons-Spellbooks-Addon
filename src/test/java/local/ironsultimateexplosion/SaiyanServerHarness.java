package local.grandexplosiontest;

/** Runs only Saiyan checks; never starts unrelated destructive spell diagnostics. */
@net.minecraftforge.fml.common.Mod("grand_explosion_test")
public final class SaiyanServerHarness {
    public SaiyanServerHarness() { new SaiyanHarness(); }
}
