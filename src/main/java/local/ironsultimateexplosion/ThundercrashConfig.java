package local.ironsultimateexplosion;

import net.minecraftforge.common.ForgeConfigSpec;

/** Settings live alongside the existing config without changing Grand Explosion's keys. */
public final class ThundercrashConfig {
    public static ForgeConfigSpec.IntValue CHARGE, LAUNCH, DURATION, MANA, COOLDOWN, LANDING, BASE_POWER, POWER_PER_LEVEL;
    public static ForgeConfigSpec.DoubleValue SPEED, LIFT, STEERING, RADIUS, DAMAGE, EDGE_DAMAGE, KNOCKBACK, RANGE;
    public static ForgeConfigSpec.BooleanValue GUIDED, PVP;
    public static ForgeConfigSpec.IntValue AURA, PARTICLE_BUDGET, GLOBAL_BUDGET;
    public static ForgeConfigSpec.DoubleValue SPACING, CAST_VOLUME, FLIGHT_VOLUME, IMPACT_VOLUME;
    static void server(ForgeConfigSpec.Builder b) {
        b.push("thundercrash");
        CHARGE = b.defineInRange("chargeTicks", 10, 5, 30);
        LAUNCH = b.defineInRange("launchTicks", 4, 1, 15);
        DURATION = b.defineInRange("flightTicks", 80, 20, 100);
        MANA = b.defineInRange("manaCost", 150, 1, 10000);
        COOLDOWN = b.defineInRange("cooldownSeconds", 120, 1, 3600);
        LANDING = b.defineInRange("landingProtectionTicks", 100, 0, 100);
        SPEED = b.defineInRange("flightSpeed", 0.85, 0.2, 2.5);
        LIFT = b.defineInRange("launchVelocity", 0.35, 0.1, 1.5);
        STEERING = b.defineInRange("steeringResponsiveness", 0.18, 0.02, 1.0);
        RADIUS = b.defineInRange("impactRadius", 6.0, 1.0, 12.0);
        BASE_POWER = b.comment("Level-one power before Iron's spell and Lightning attributes and power multiplier.")
                .defineInRange("baseSpellPower", 40, 1, 10000);
        POWER_PER_LEVEL = b.defineInRange("spellPowerPerLevel", 10, 0, 10000);
        DAMAGE = b.defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        EDGE_DAMAGE = b.comment("Fraction of center damage at the impact radius; damage falls linearly with distance.")
                .defineInRange("edgeDamageFraction", 0.25, 0.0, 1.0);
        KNOCKBACK = b.defineInRange("knockback", 1.5, 0.0, 4.0);
        RANGE = b.defineInRange("directionalRange", 40.0, 20.0, 80.0);
        GUIDED = b.comment("Use camera steering; false locks the launch direction and limits distance.").define("guidedFlight", true);
        PVP = b.comment("Still subject to server PvP and Iron's friendly-fire rules. Terrain is never damaged.").define("pvpDamage", true);
        b.pop();
    }
    static void client(ForgeConfigSpec.Builder b) {
        b.push("thundercrash");
        AURA = b.defineInRange("ascensionHelperCalls", 4, 1, 8);
        SPACING = b.defineInRange("trailSpacing", 0.5, 0.25, 1.0);
        PARTICLE_BUDGET = b.defineInRange("particlesPerCasterTick", 28, 8, 64);
        GLOBAL_BUDGET = b.defineInRange("particlesPerClientTick", 224, 32, 512);
        CAST_VOLUME = b.defineInRange("castVolume", 1.0, 0.0, 4.0);
        FLIGHT_VOLUME = b.defineInRange("flightVolume", 0.7, 0.0, 4.0);
        IMPACT_VOLUME = b.defineInRange("impactVolume", 2.0, 0.0, 4.0);
        b.pop();
    }
    private ThundercrashConfig() {}
}
