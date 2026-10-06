package local.omegarush;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OmegaConfig {
    public enum Quality { FULL, REDUCED, MINIMAL }
    public static final ForgeConfigSpec SERVER, CLIENT;
    public static final ForgeConfigSpec.IntValue MANA, COOLDOWN, CHARGE, DURATION, BASE_POWER, POWER_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SPEED, STEERING, SPACING, RADIUS;
    public static final ForgeConfigSpec.BooleanValue PVP, OVERLAY, FLASH;
    public static final ForgeConfigSpec.EnumValue<Quality> QUALITY;
    public static final ForgeConfigSpec.DoubleValue VOLUME;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("omegaRush");
        MANA = b.defineInRange("manaCost",200,0,10000);
        COOLDOWN = b.defineInRange("cooldownSeconds",120,0,3600);
        CHARGE = b.comment("Charge length in ticks, adjusted by Iron's casting attributes.").defineInRange("chargeTicks",16,4,60);
        DURATION = b.comment("100–200 ticks is 5–10 seconds at normal TPS.").defineInRange("flightTicks",140,100,200);
        SPEED = b.comment("Blocks per server tick; 0.8 is 16 blocks/second.").defineInRange("flightSpeed",0.8,0.1,2.0);
        STEERING = b.comment("Thundercrash's velocity blending response.").defineInRange("steeringResponsiveness",0.18,0.02,1.0);
        SPACING = b.defineInRange("trailSpacing",2.0,1.0,6.0);
        RADIUS = b.defineInRange("damageRadius",4.05,0.5,6.0);
        BASE_POWER = b.defineInRange("baseSpellPower",12,1,1000);
        POWER_PER_LEVEL = b.defineInRange("spellPowerPerLevel",3,0,1000);
        PVP = b.comment("Also respects server PvP, team rules and protection events.").define("pvpDamage",true);
        b.pop(); SERVER = b.build();
        b = new ForgeConfigSpec.Builder(); b.push("presentation");
        QUALITY = b.defineEnum("particleQuality",Quality.FULL);
        OVERLAY = b.define("rainbowPlayerOverlay",true);
        FLASH = b.comment("Disable bright white burst accents; colored rings remain.").define("brightFlashes",true);
        VOLUME = b.defineInRange("soundVolume",0.7,0.0,2.0);
        b.pop(); CLIENT = b.build();
    }
    private OmegaConfig() {}
}
