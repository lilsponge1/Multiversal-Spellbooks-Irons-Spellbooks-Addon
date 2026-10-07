package local.omegarush;

import net.minecraftforge.common.ForgeConfigSpec;

public final class OmegaConfig {
    public enum Quality { FULL, REDUCED, MINIMAL }
    public static final ForgeConfigSpec SERVER, CLIENT;
    public static final ForgeConfigSpec.IntValue MANA, COOLDOWN, FORM_RUSH_COOLDOWN, CHARGE, DURATION, BASE_POWER, POWER_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SPEED, STEERING, SPACING, RADIUS;
    public static final ForgeConfigSpec.BooleanValue PVP, OVERLAY, FLASH;
    public static final ForgeConfigSpec.EnumValue<Quality> QUALITY;
    public static final ForgeConfigSpec.DoubleValue VOLUME;
    public static final ForgeConfigSpec.IntValue FORM_MANA,FORM_CHARGE,FORM_COOLDOWN,FORM_RESISTANCE,FORM_STRENGTH,FORM_HEARTS;
    public static final ForgeConfigSpec.DoubleValue FORM_UPKEEP,FORM_LIFT,HOVER_SPEED,HOVER_VERTICAL,NATURE_BONUS,BOSS_CHANCE,CHEST_CHANCE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_FORM,AFTERIMAGES;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SCARF_BOSSES;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.push("omegaRush");
        MANA = b.defineInRange("manaCost",200,0,10000);
        COOLDOWN = b.defineInRange("cooldownSeconds",120,0,3600);
        FORM_RUSH_COOLDOWN = b.comment("Rush cooldown while Omega Form is active; normal cooldown reductions still apply.").defineInRange("omegaFormCooldownSeconds",10,0,3600);
        CHARGE = b.comment("Charge length in ticks, adjusted by Iron's casting attributes.").defineInRange("chargeTicks",16,4,60);
        DURATION = b.comment("100–200 ticks is 5–10 seconds at normal TPS.").defineInRange("flightTicks",140,100,200);
        SPEED = b.comment("Blocks per server tick; 0.8 is 16 blocks/second.").defineInRange("flightSpeed",0.8,0.1,2.0);
        STEERING = b.comment("Thundercrash's velocity blending response.").defineInRange("steeringResponsiveness",0.18,0.02,1.0);
        SPACING = b.defineInRange("trailSpacing",2.0,1.0,6.0);
        RADIUS = b.defineInRange("damageRadius",4.05,0.5,6.0);
        BASE_POWER = b.defineInRange("baseSpellPower",12,1,1000);
        POWER_PER_LEVEL = b.defineInRange("spellPowerPerLevel",3,0,1000);
        PVP = b.comment("Also respects server PvP, team rules and protection events.").define("pvpDamage",true);
        REQUIRE_FORM=b.define("requireOmegaForm",true);
        b.pop(); b.push("omegaForm");
        FORM_MANA=b.defineInRange("manaCost",200,0,10000);
        FORM_CHARGE=b.comment("Base ritual clock before its 20% acceleration: 160 becomes 128 effective ticks (6.4 seconds). Casting-speed gear can reduce it to 96 ticks.").defineInRange("chargeTicks",160,160,400);
        FORM_LIFT=b.defineInRange("chargeLiftBlocks",3.5,0.0,8.0);
        FORM_COOLDOWN=b.defineInRange("cooldownSeconds",120,0,3600);
        FORM_UPKEEP=b.defineInRange("manaPerSecond",30.0,0.0,1000.0);
        HOVER_SPEED=b.defineInRange("horizontalBlocksPerSecond",6.0,0.1,20.0);
        HOVER_VERTICAL=b.defineInRange("verticalBlocksPerSecond",4.0,0.1,12.0);
        FORM_RESISTANCE=b.defineInRange("resistanceLevel",3,0,4);
        FORM_STRENGTH=b.defineInRange("strengthLevel",1,0,5);
        FORM_HEARTS=b.defineInRange("absorptionHearts",8,0,40);
        NATURE_BONUS=b.defineInRange("naturePowerBonus",0.25,0.0,5.0);
        b.pop(); b.push("floweryScarf");
        BOSS_CHANCE=b.defineInRange("bossDropChance",0.05,0.0,1.0);
        CHEST_CHANCE=b.defineInRange("chestDropChance",0.002,0.0,1.0);
        SCARF_BOSSES=b.defineListAllowEmpty("bosses",java.util.List.of("irons_spellbooks:dead_king","irons_spellbooks:fire_boss","cataclysm:ender_guardian","cataclysm:the_harbinger","cataclysm:the_leviathan","cataclysm:ancient_remnant","cataclysm:maledictus","cataclysm:scylla"),v->v instanceof String s&&net.minecraft.resources.ResourceLocation.m_135820_(s)!=null);
        b.pop(); SERVER = b.build();
        b = new ForgeConfigSpec.Builder(); b.push("presentation");
        QUALITY = b.defineEnum("particleQuality",Quality.FULL);
        OVERLAY = b.define("rainbowPlayerOverlay",true);
        FLASH = b.comment("Disable bright white burst accents; colored rings remain.").define("brightFlashes",true);
        VOLUME = b.defineInRange("soundVolume",0.7,0.0,2.0);
        AFTERIMAGES=b.define("afterImages",true);
        b.pop(); CLIENT = b.build();
    }
    private OmegaConfig() {}
}
