package com.crimson_susanoo;

import com.crimson_susanoo.entity.CrimsonEntity;
import com.crimson_susanoo.entity.CrimsonWave;
import com.crimson_susanoo.spell.CrimsonSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(CrimsonSusanoo.MODID)
public final class CrimsonSusanoo {
    public static final String MODID = "crimson_susanoo";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    private static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, MODID);

    public static final RegistryObject<EntityType<CrimsonEntity>> GUARDIAN = ENTITIES.register("guardian",
            () -> EntityType.Builder.of(CrimsonEntity::new, MobCategory.MISC)
                    .sized(4.5F, 8.0F).clientTrackingRange(96).updateInterval(2)
                    .fireImmune().build(MODID + ":guardian"));
    public static final RegistryObject<EntityType<CrimsonWave>> WAVE = ENTITIES.register("inferno_crescent",
            () -> EntityType.Builder.<CrimsonWave>of(CrimsonWave::new, MobCategory.MISC)
                    .sized(3.6F, 3.0F).clientTrackingRange(64).updateInterval(1)
                    .build(MODID + ":inferno_crescent"));
    public static final RegistryObject<AbstractSpell> SPELL = SPELLS.register("crimson_susanoo", CrimsonSpell::new);

    public CrimsonSusanoo() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        SPELLS.register(bus);
        bus.addListener(this::registerAttributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        if (Boolean.getBoolean("crimson_susanoo.diagnostics")) {
            MinecraftForge.EVENT_BUS.register(new ServerDiagnostics());
        }
        if (Boolean.getBoolean("crimson_susanoo.bossEncounter")) {
            MinecraftForge.EVENT_BUS.register(new ServerBossDiagnostics());
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GUARDIAN.get(), CrimsonEntity.createAttributes().build());
    }
}
