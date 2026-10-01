package local.ironsultimateexplosion;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ExplosionConfig {
    public static final ForgeConfigSpec SERVER;
    public static final ForgeConfigSpec CLIENT;
    public static final ForgeConfigSpec.BooleanValue TERRAIN_DAMAGE;
    public static final ForgeConfigSpec.IntValue CRATER_RADIUS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue PVP_DAMAGE;
    public static final ForgeConfigSpec.IntValue VISUAL_RADIUS;
    public static final ForgeConfigSpec.IntValue PARTICLE_QUALITY;
    public static final ForgeConfigSpec.IntValue EXHAUSTION_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("grandExplosion");
        TERRAIN_DAMAGE = b.comment("Allow a Ballistix-size crater, also requiring Iron's spellGriefing.").define("terrainDamage", true);
        CRATER_RADIUS = b.comment("Fallback crater radius when Ballistix is absent; Ballistix uses its own nuclear size setting.")
                .defineInRange("craterRadius", 45, 0, 128);
        DAMAGE_MULTIPLIER = b.defineInRange("damageMultiplier", 1.0, 0.0, 2.0);
        PVP_DAMAGE = b.comment("Damage other players; the caster is always excluded.").define("pvpDamage", true);
        VISUAL_RADIUS = b.defineInRange("visualRadius", 128, 24, 256);
        EXHAUSTION_SECONDS = b.comment("Slowness after detonation; set to zero to disable.").defineInRange("exhaustionSeconds", 15, 0, 20);
        b.pop();
        ThundercrashConfig.server(b);
        SERVER = b.build();
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        PARTICLE_QUALITY = client.comment("0 low, 1 medium, 2 high particle density").defineInRange("particleQuality", 2, 0, 2);
        ThundercrashConfig.client(client);
        CLIENT = client.build();
    }
    private ExplosionConfig() {}
}
