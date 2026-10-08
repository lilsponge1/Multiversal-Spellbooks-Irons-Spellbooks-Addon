package local.ironsultimateexplosion;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SaiyanConfig {
    public record Tier(ForgeConfigSpec.IntValue cost, ForgeConfigSpec.IntValue duration,
            ForgeConfigSpec.DoubleValue drain, ForgeConfigSpec.DoubleValue regen,
            ForgeConfigSpec.DoubleValue power, ForgeConfigSpec.DoubleValue melee,
            ForgeConfigSpec.DoubleValue speed, ForgeConfigSpec.DoubleValue attack,
            ForgeConfigSpec.DoubleValue resistance, ForgeConfigSpec.DoubleValue knockback) {}
    public static Tier SSJ1, SSJ2, SSJ3;
    public static ForgeConfigSpec.DoubleValue SSJ3_HEALTH, SSJ3_CRIT, SSJ3_JUMP, SSJ3_REGEN_CAP;
    public static ForgeConfigSpec.DoubleValue SSJ3_AURA, SSJ3_PARTICLES, SSJ3_SWAY;
    public static ForgeConfigSpec.BooleanValue SSJ3_COMBAT, SSJ3_MUSIC;
    public static ForgeConfigSpec.IntValue HOLD, SHORT;
    public static ForgeConfigSpec.DoubleValue SHAKE_RADIUS, VOLUME, DENSITY;
    public static ForgeConfigSpec.IntValue BUDGET;
    public static ForgeConfigSpec.BooleanValue HEAD, SHAKE, HUD;
    static void server(ForgeConfigSpec.Builder b) {
        b.push("saiyanAscension");
        HOLD = b.defineInRange("powerDownHoldTicks", 60, 20, 200);
        SHORT = b.comment("Release strictly before this many server ticks to ascend; intermediate holds cancel.")
                .defineInRange("shortPressTicks", 20, 2, 30);
        SHAKE_RADIUS = b.defineInRange("nearbyScreenShakeRadius", 12.0, 0.0, 24.0);
        VOLUME = b.defineInRange("transformationSoundVolume", 1.0, 0.0, 4.0);
        SSJ1 = tier(b, "ssj1", 50, 50, 10, .50, .20, .25, .15, .15, .15, .50);
        SSJ2 = tier(b, "ssj2", 75, 60, 17, .25, .35, .40, .25, .25, .25, .80);
        SSJ3 = tier(b, "ssj3", 0, 300, 30, .25, 1, 1, .40, .50, .35, 1);
        b.pop();
    }
    private static Tier tier(ForgeConfigSpec.Builder b, String name, int cost, int time, double drain,
            double regen, double power, double melee, double speed, double attack, double resist, double knockback) {
        b.push(name);
        Tier t = new Tier(b.defineInRange("activationManaCost", cost, 0, 10000),
                b.defineInRange("transformationTicks", time, 10, name.equals("ssj3") ? 1200 : 200),
                b.defineInRange("manaDrainPerSecond", drain, 0.0, 1000.0),
                b.defineInRange("manaRegenMultiplier", regen, 0.0, 1.0),
                b.defineInRange("spellPowerBonus", power, 0.0, 5.0),
                b.defineInRange("meleeDamageBonus", melee, 0.0, 5.0),
                b.defineInRange("movementSpeedBonus", speed, 0.0, 2.0),
                b.defineInRange("attackSpeedBonus", attack, 0.0, 5.0),
                b.defineInRange("damageResistance", resist, 0.0, .90),
                b.comment("Additive knockback resistance: .50 means fifty percentage points.")
                        .defineInRange("knockbackResistance", knockback, 0.0, 1.0));
        if (name.equals("ssj3")) {
            SSJ3_HEALTH=b.defineInRange("maximumHealthBonus",.40,0,3);
            SSJ3_CRIT=b.comment("Uses a native spell-critical attribute, or the pack's shared Apothic critical-damage attribute when available.").defineInRange("criticalDamageBonus",.50,0,3);
            SSJ3_JUMP=b.defineInRange("jumpStrengthBonus",.35,0,1);
            SSJ3_REGEN_CAP=b.comment("Natural regeneration can offset at most this fraction of SSJ3 drain. Instant mana restoration is unaffected.").defineInRange("maximumRegenOffsetFraction",.50,0,.95);
        }
        b.pop(); return t;
    }
    public static Tier tier(SaiyanRules.Form form) { return switch(form) { case SUPER_SAIYAN_3 -> SSJ3; case SUPER_SAIYAN_2 -> SSJ2; default -> SSJ1; }; }
    static void client(ForgeConfigSpec.Builder b) {
        b.push("saiyanAscension");
        DENSITY = b.defineInRange("particleDensity", 1.0, 0.0, 2.0);
        BUDGET = b.defineInRange("particlesPerClientTick", 128, 0, 384);
        HEAD = b.define("goldenHeadSpikes", true);
        SHAKE = b.define("cameraShake", true);
        HUD = b.define("showFormIndicator", true);
        b.push("ssj3");
        SSJ3_AURA=b.defineInRange("auraIntensity",1.0,0.0,2.0);
        SSJ3_PARTICLES=b.defineInRange("particleQuantityMultiplier",1.5,0.0,3.0);
        SSJ3_SWAY=b.defineInRange("hairMotionStrength",1.0,0.0,2.0);
        SSJ3_COMBAT=b.define("combatAndMovementEffects",true);
        SSJ3_MUSIC=b.define("completionMusic",true);
        b.pop();
        b.pop();
    }
    private SaiyanConfig() {}
}
