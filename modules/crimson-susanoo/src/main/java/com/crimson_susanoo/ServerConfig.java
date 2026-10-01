package com.crimson_susanoo;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue INITIAL_MANA;
    public static final ForgeConfigSpec.DoubleValue UPKEEP;
    public static final ForgeConfigSpec.IntValue DURATION;
    public static final ForgeConfigSpec.IntValue COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue HEALTH;
    public static final ForgeConfigSpec.DoubleValue ARMOR;
    public static final ForgeConfigSpec.DoubleValue GUARD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_RADIUS;
    public static final ForgeConfigSpec.DoubleValue TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue COMBAT_LEASH;
    public static final ForgeConfigSpec.DoubleValue GUARD_PERCENT;
    public static final ForgeConfigSpec.DoubleValue GUARD_MANA_PER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CLEAVE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CLEAVE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue CLEAVE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue CRESCENT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue CRESCENT_COOLDOWN;
    public static final ForgeConfigSpec.IntValue CRESCENT_MANA;
    public static final ForgeConfigSpec.IntValue BRAND_DURATION;
    public static final ForgeConfigSpec.BooleanValue BRAND_STACKING;
    public static final ForgeConfigSpec.IntValue BRAND_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue SLASH_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SLASH_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SLASH_MANA;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;
    public static final ForgeConfigSpec.BooleanValue BLOCK_GRIEFING;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("crimson_susanoo");
        INITIAL_MANA = BUILDER.defineInRange("initialManaCost", 150, 0, 100000);
        UPKEEP = BUILDER.defineInRange("manaDrainPerSecond", 12.0, 0, 100000);
        DURATION = BUILDER.defineInRange("maxDurationSeconds", 90, 1, 3600);
        COOLDOWN = BUILDER.defineInRange("cooldownSeconds", 300, 0, 36000);
        HEALTH = BUILDER.defineInRange("maxHealth", 450.0, 1, 100000);
        ARMOR = BUILDER.defineInRange("armor", 18.0, 0, 1000);
        GUARD_RADIUS = BUILDER.defineInRange("guardRadius", 12.0, 0, 64);
        FOLLOW_RADIUS = BUILDER.defineInRange("followRadius", 15.0, 2, 64);
        TELEPORT_DISTANCE = BUILDER.defineInRange("teleportDistance", 24.0, 4, 128);
        COMBAT_LEASH = BUILDER.comment("Owner distance allowed during valid combat pursuit; idle follow remains unchanged.")
                .defineInRange("combatLeashDistance", 40.0, 4, 128);
        GUARD_PERCENT = BUILDER.defineInRange("guardDamageRedirectPercent", 0.25, 0, 1);
        GUARD_MANA_PER_DAMAGE = BUILDER.defineInRange("guardManaPerDamage", 0.5, 0, 100);
        CLEAVE_DAMAGE = BUILDER.defineInRange("cleaveDamage", 22.0, 0, 10000);
        CLEAVE_COOLDOWN = BUILDER.defineInRange("cleaveCooldownSeconds", 2.25, 0.05, 3600);
        CLEAVE_KNOCKBACK = BUILDER.comment("Ordinary Cleave knockback strength before target resistance. Lower values keep melee exchanges closer; previous releases used 1.0.")
                .defineInRange("cleaveKnockback", 0.7, 0.0, 4.0);
        CRESCENT_DAMAGE = BUILDER.defineInRange("crescentDamage", 28.0, 0, 10000);
        CRESCENT_COOLDOWN = BUILDER.defineInRange("crescentCooldownSeconds", 8.0, 0.05, 3600);
        CRESCENT_MANA = BUILDER.defineInRange("crescentManaCost", 20, 0, 100000);
        BRAND_DURATION = BUILDER.defineInRange("blazingBrandDurationSeconds", 10, 1, 3600);
        BRAND_STACKING = BUILDER.define("blazingBrandStacking", true);
        BRAND_MAX_LEVEL = BUILDER.defineInRange("blazingBrandMaxLevel", 3, 1, 255);
        SLASH_DAMAGE = BUILDER.defineInRange("wrathfulSlashDamage", 40.0, 0, 10000);
        SLASH_COOLDOWN = BUILDER.defineInRange("wrathfulSlashCooldownSeconds", 14.0, 0.05, 3600);
        SLASH_MANA = BUILDER.defineInRange("wrathfulSlashManaCost", 30, 0, 100000);
        FRIENDLY_FIRE = BUILDER.define("friendlyFire", false);
        // Reserved for future opt-in terrain behavior; this release never edits blocks.
        BLOCK_GRIEFING = BUILDER.define("blockGriefing", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServerConfig() {}
}
