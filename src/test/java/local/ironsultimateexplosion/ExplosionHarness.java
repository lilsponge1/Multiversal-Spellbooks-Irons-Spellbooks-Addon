package local.grandexplosiontest;

import ballistix.common.settings.BallistixConstants;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import local.ironsultimateexplosion.ExplosionConfig;
import local.ironsultimateexplosion.CraterManager;
import local.ironsultimateexplosion.GrandExplosionSpell;
import local.ironsultimateexplosion.ModItems;
import local.ironsultimateexplosion.ModSpells;
import local.ironsultimateexplosion.SpellEvents;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.registries.ItemRegistry;

/** Disposable-world-only test mod. Never include in the release JAR. */
@Mod("grand_explosion_test")
public final class ExplosionHarness {
    private ServerLevel craterLevel;
    private Boolean oldTerrain;
    private Boolean oldIronGriefing;
    private Integer oldRadius;
    private BlockPos protectedPos;
    private boolean expectProtection;
    private BlockPos nukeInside;
    private BlockPos nukeOutside;
    private BlockPos nukeChest;
    private BlockPos nukeBedrock;
    private ServerPlayer audioPlayer;
    private int audioTicks;
    private int audioDuration;
    public ExplosionHarness() {
        new ThunderstarHarness();
        new ThundercrashHarness();
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::protectBlock);
        MinecraftForge.EVENT_BUS.addListener(this::tickAudio);
    }

    private void protectBlock(BlockEvent.BreakEvent event) {
        if (protectedPos != null && protectedPos.equals(event.getPos())) event.setCanceled(true);
    }

    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_givebook")
                .requires(source -> source.m_6761_(2))
                .executes(ctx -> {
                    ItemStack book = new ItemStack(ItemRegistry.WIMPY_SPELL_BOOK.get());
                    ISpellContainerMutable spells = ISpellContainer.create(1, true, true).mutableCopy();
                    if (!spells.addSpell(ModSpells.GRAND_EXPLOSION.get(), 5, false)) return 0;
                    ISpellContainer.set(book, spells.toImmutable());
                    return ctx.getSource().m_230896_().m_150109_().m_36054_(book) ? 1 : 0;
                  }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("thundercrash_givebook")
                .requires(source -> source.m_6761_(2))
                .executes(ctx -> {
                    ItemStack book = new ItemStack(ItemRegistry.WIMPY_SPELL_BOOK.get());
                    ISpellContainerMutable spells = ISpellContainer.create(1, true, true).mutableCopy();
                    if (!spells.addSpell(ModSpells.THUNDERCRASH.get(), 1, false)) return 0;
                    ISpellContainer.set(book, spells.toImmutable());
                    return ctx.getSource().m_230896_().m_150109_().m_36054_(book) ? 1 : 0;
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { run(ctx.getSource().m_81372_()); return 1; }
                    catch (Throwable error) {
                        System.out.println("GRAND_TEST_ERROR " + error);
                        error.printStackTrace(System.out);
                        return 0;
                    }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_regalia_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { runRegalia(ctx.getSource().m_81372_()); return 1; }
                    catch (Throwable error) {
                        System.out.println("GRAND_REGALIA_ERROR " + error);
                        error.printStackTrace(System.out);
                        return 0;
                    }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_pvp_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { runPvp(ctx.getSource().m_81372_()); return 1; }
                    catch (Throwable error) {
                        System.out.println("GRAND_PVP_ERROR " + error);
                        error.printStackTrace(System.out);
                        return 0;
                    }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_crater_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { startCrater(ctx.getSource().m_81372_(), false, 3); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_CRATER_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_large_crater_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { startCrater(ctx.getSource().m_81372_(), false, 24); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_CRATER_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_protection_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { startCrater(ctx.getSource().m_81372_(), true, 3); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_CRATER_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_crater_verify")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { verifyCrater(); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_CRATER_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_nuke_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { startNuke(ctx.getSource().m_81372_()); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_NUKE_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_nuke_verify")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { verifyNuke(); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_NUKE_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("grand_explosion_audio_test")
                .requires(source -> source.m_6761_(4))
                .executes(ctx -> {
                    try { startAudio(ctx.getSource().m_230896_()); return 1; }
                    catch (Throwable error) { System.out.println("GRAND_AUDIO_ERROR " + error); error.printStackTrace(System.out); return 0; }
                }));
    }

    private void startAudio(ServerPlayer player) {
        if (audioPlayer != null) throw new IllegalStateException("audio check already running");
        audioPlayer = player;
        audioTicks = 0;
        GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
        audioDuration = spell.getEffectiveCastTime(5, player);
        spell.onServerPreCast(player.m_20193_(), 5, player, MagicData.getPlayerMagicData(player));
        System.out.println("GRAND_AUDIO_START duration=" + audioDuration
                + " sparkRegistered=" + ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation("minecraft:block.redstone_torch.burnout"))
                + " arcRegistered=" + ForgeRegistries.SOUND_EVENTS.containsKey(new ResourceLocation("minecraft:entity.lightning_bolt.impact")));
    }

    private void tickAudio(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || audioPlayer == null) return;
        ServerPlayer player = audioPlayer;
        GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
        if (!player.m_213877_() && audioTicks < audioDuration) {
            spell.onServerCastTick(player.m_20193_(), 5, player, MagicData.getPlayerMagicData(player));
            audioTicks++;
        } else {
            spell.onServerCastComplete(player.m_20193_(), 5, player, MagicData.getPlayerMagicData(player), true);
            System.out.println("GRAND_AUDIO_END ticks=" + audioTicks);
            audioPlayer = null;
        }
    }

    private void startNuke(ServerLevel level) {
        if (craterLevel != null) throw new IllegalStateException("crater test already started");
        craterLevel = level;
        oldTerrain = ExplosionConfig.TERRAIN_DAMAGE.get();
        oldIronGriefing = ServerConfigs.SPELL_GREIFING.get();
        ExplosionConfig.TERRAIN_DAMAGE.set(true);
        ServerConfigs.SPELL_GREIFING.set(true);
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ada00000-0000-0000-0000-000000000002"), "GrandTest"));
        player.m_6034_(0, 64, 0); player.m_146922_(0); player.m_146926_(0);
        level.m_7731_(new BlockPos(0, 65, 12), Blocks.f_50069_.m_49966_(), 3);
        Vec3 point = RaycastBuilder.begin(level, player).range(48).checkForBlocks(true).build().m_82450_();
        BlockPos center = new BlockPos((int)Math.floor(point.f_82479_), (int)Math.floor(point.f_82480_),
                (int)Math.floor(point.f_82481_));
        nukeInside = center.m_7918_(0, 0, 40);
        nukeOutside = center.m_7918_(0, 0, 48);
        nukeChest = center.m_7918_(2, 0, 0);
        nukeBedrock = center.m_7918_(0, -1, 0);
        protectedPos = center.m_7918_(-2, 0, 0);
        for (BlockPos pos : new BlockPos[] {nukeInside, nukeOutside, protectedPos})
            level.m_7731_(pos, Blocks.f_50069_.m_49966_(), 3);
        level.m_7731_(nukeChest, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chest")).m_49966_(), 3);
        level.m_7731_(nukeBedrock, ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:bedrock")).m_49966_(), 3);
        GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
        spell.onCast(level, 1, player, CastSource.COMMAND, MagicData.getPlayerMagicData(player));
        System.out.println("GRAND_NUKE_SETUP center=" + center + " ballistixSize=" + BallistixConstants.EXPLOSIVE_NUCLEAR_SIZE
                + " jobs=" + craterJobs() + " inside=" + nukeInside + " outside=" + nukeOutside);
    }

    private void verifyNuke() {
        if (craterLevel == null || nukeInside == null) throw new IllegalStateException("start nuke test first");
        int jobs = craterJobs();
        boolean insideRemoved = craterLevel.m_8055_(nukeInside).m_60795_();
        boolean outsidePreserved = !craterLevel.m_8055_(nukeOutside).m_60795_();
        boolean chestDestroyed = craterLevel.m_7702_(nukeChest) == null;
        boolean bedrockPreserved = !craterLevel.m_8055_(nukeBedrock).m_60795_();
        boolean protectedPreserved = !craterLevel.m_8055_(protectedPos).m_60795_();
        System.out.println("GRAND_NUKE_RESULT jobs=" + jobs + " insideRemoved=" + insideRemoved
                + " outsidePreserved=" + outsidePreserved + " chestDestroyed=" + chestDestroyed
                + " bedrockPreserved=" + bedrockPreserved + " protectedPreserved=" + protectedPreserved);
        if (jobs == 0) {
            ExplosionConfig.TERRAIN_DAMAGE.set(oldTerrain);
            ServerConfigs.SPELL_GREIFING.set(oldIronGriefing);
            craterLevel = null;
            protectedPos = null;
            nukeInside = nukeOutside = nukeChest = nukeBedrock = null;
        }
    }

    private void startCrater(ServerLevel level, boolean protect, int radius) {
        if (craterLevel != null) throw new IllegalStateException("crater test already started");
        craterLevel = level;
        oldTerrain = ExplosionConfig.TERRAIN_DAMAGE.get();
        oldIronGriefing = ServerConfigs.SPELL_GREIFING.get();
        oldRadius = ExplosionConfig.CRATER_RADIUS.get();
        ExplosionConfig.TERRAIN_DAMAGE.set(true);
        ServerConfigs.SPELL_GREIFING.set(true);
        ExplosionConfig.CRATER_RADIUS.set(radius);
        level.m_7731_(new BlockPos(0, 65, 12), Blocks.f_50069_.m_49966_(), 3);
        level.m_7731_(new BlockPos(1, 65, 12),
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:chest")).m_49966_(), 3);
        level.m_7731_(new BlockPos(0, 64, 12),
                ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:bedrock")).m_49966_(), 3);
        expectProtection = protect;
        protectedPos = protect ? new BlockPos(0, 65, 12) : null;
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ada00000-0000-0000-0000-000000000002"), "GrandTest"));
        player.m_6034_(0, 64, 0); player.m_146922_(0); player.m_146926_(0);
        var target = RaycastBuilder.begin(level, player).range(48).checkForBlocks(true).build().m_82450_();
        int breakResult = ForgeHooks.onBlockBreakEvent(level, player.f_8941_.m_9290_(), player, new BlockPos(0, 65, 12));
        System.out.println("GRAND_CRATER_SETUP target=" + target + " breakResult=" + breakResult
                + " removed=" + player.m_213877_() + " levelMatch=" + (player.m_20193_() == level)
                + " addon=" + ExplosionConfig.TERRAIN_DAMAGE.get() + " iron=" + ServerConfigs.SPELL_GREIFING.get());
        GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
        spell.onCast(level, 1, player, CastSource.COMMAND, MagicData.getPlayerMagicData(player));
        System.out.println("GRAND_CRATER_QUEUED radius=" + radius + " candidates=" + craterCandidates()
                + " chestEntity=" + (level.m_7702_(new BlockPos(1, 65, 12)) != null)
                + " loaded=" + level.m_46749_(new BlockPos(0, 65, 12)) + " jobs=" + craterJobs());
    }

    private void verifyCrater() {
        if (craterLevel == null) throw new IllegalStateException("start crater test first");
        boolean stoneRemoved = craterLevel.m_8055_(new BlockPos(0, 65, 12)).m_60795_();
        boolean chestPreserved = craterLevel.m_7702_(new BlockPos(1, 65, 12)) != null;
        boolean bedrockPreserved = !craterLevel.m_8055_(new BlockPos(0, 64, 12)).m_60795_();
        System.out.println("GRAND_CRATER_RESULT protected=" + expectProtection + " stoneRemoved=" + stoneRemoved + " chestPreserved=" + chestPreserved
                + " bedrockPreserved=" + bedrockPreserved + " jobs=" + craterJobs());
        ExplosionConfig.TERRAIN_DAMAGE.set(oldTerrain);
        ServerConfigs.SPELL_GREIFING.set(oldIronGriefing);
        ExplosionConfig.CRATER_RADIUS.set(oldRadius);
        craterLevel = null;
        protectedPos = null;
    }

    private static int craterJobs() {
        try {
            var field = CraterManager.class.getDeclaredField("JOBS");
            field.setAccessible(true);
            return ((java.util.Deque<?>)field.get(null)).size();
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private static int craterCandidates() {
        try {
            var field = CraterManager.class.getDeclaredField("JOBS");
            field.setAccessible(true);
            var jobs = (java.util.Deque<?>)field.get(null);
            if (jobs.isEmpty()) return 0;
            var positions = jobs.peekFirst().getClass().getDeclaredField("positions");
            positions.setAccessible(true);
            return ((java.util.Deque<?>)positions.get(jobs.peekFirst())).size();
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }

    private void run(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ada00000-0000-0000-0000-000000000002"), "GrandTest"));
        player.m_6034_(0, 64, 0);
        player.m_146922_(0);
        player.m_146926_(0);
        boolean placed = level.m_7731_(new BlockPos(0, 65, 12), Blocks.f_50069_.m_49966_(), 3);
        SpellConfigManager.onDatapackSync(new OnDatapackSyncEvent(level.m_7654_().m_6846_(), null));
        GrandExplosionSpell spell = (GrandExplosionSpell) ModSpells.GRAND_EXPLOSION.get();
        MagicData data = MagicData.getPlayerMagicData(player);
        LivingEntity cow = spawnCow(level, 10);
        float health = cow.m_21223_();
        data.setMana(500);
        float availableMana = data.getMana();
        SpellOnCastEvent paid = new SpellOnCastEvent(player, spell.getSpellId(), 1,
                spell.getManaCost(1), spell.getSchoolType(), CastSource.SPELLBOOK);
        System.out.println("GRAND_TEST_SETUP id=" + paid.getSpellId() + " expected=" + GrandExplosionSpell.ID
                + " mana=" + data.getMana() + " placed=" + placed + " entity=" + paid.getEntity().getClass().getName());
        SpellEvents.onSpellCast(paid);
        System.out.println("GRAND_TEST_COST " + paid.getManaCost());
        data.setMana(Math.max(0, data.getMana() - paid.getManaCost()));
        spell.onServerPreCast(level, 1, player, data);
        spell.onServerCastTick(level, 1, player, data);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, data);
        boolean drained = data.getMana() == 0 && paid.getManaCost() == (int)Math.ceil(availableMana);
        boolean damaged = cow.m_21223_() < health;
        boolean terrainSafe = !level.m_8055_(new BlockPos(0, 65, 12)).m_60795_();
        System.out.println("GRAND_TEST_PAID drained=" + drained + " damage=" + damaged
                + " terrainOff=" + terrainSafe + " mana=" + data.getMana());

        LivingEntity second = spawnCow(level, 10);
        float secondHealth = second.m_21223_();
        data.setMana(0);
        SpellOnCastEvent empty = new SpellOnCastEvent(player, spell.getSpellId(), 1,
                spell.getManaCost(1), spell.getSchoolType(), CastSource.SPELLBOOK);
        SpellEvents.onSpellCast(empty);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, data);
        System.out.println("GRAND_TEST_EMPTY fizzled=" + (second.m_21223_() == secondHealth)
                + " manaCost=" + empty.getManaCost());
        var mainhand = ModItems.CINDERSTAR_STAFF.get().m_7167_(EquipmentSlot.MAINHAND);
        var offhand = ModItems.CINDERSTAR_STAFF.get().m_7167_(EquipmentSlot.OFFHAND);
        boolean fire = mainhand.get(AttributeRegistry.FIRE_SPELL_POWER.get()).stream()
                .anyMatch(m -> m.m_22218_() == 0.30 && m.m_22217_() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE);
        boolean cooldown = mainhand.get(AttributeRegistry.COOLDOWN_REDUCTION.get()).stream()
                .anyMatch(m -> m.m_22218_() == 0.10 && m.m_22217_() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE);
        boolean unequipped = offhand.get(AttributeRegistry.FIRE_SPELL_POWER.get()).isEmpty()
                && offhand.get(AttributeRegistry.COOLDOWN_REDUCTION.get()).isEmpty();
        System.out.println("GRAND_TEST_STAFF fire=" + fire + " cooldown=" + cooldown + " offhandClean=" + unequipped);
        boolean recipe = level.m_7654_().m_129894_()
                .m_44043_(new ResourceLocation("irons_ultimate_explosion:cinderstar_staff")).isPresent();
        boolean staffTag = new ItemStack(ModItems.CINDERSTAR_STAFF.get()).m_204117_(TagKey.m_203882_(
                Registries.f_256913_, new ResourceLocation("irons_spellbooks:staff")));
        System.out.println("GRAND_TEST_ASSETS recipe=" + recipe + " staffTag=" + staffTag);
        cow.m_146870_(); second.m_146870_();
    }

    private static LivingEntity spawnCow(ServerLevel level, double z) {
        Entity cow = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft:cow")).m_20615_(level);
        cow.m_6034_(0, 64, z);
        level.m_7967_(cow);
        return (LivingEntity)cow;
    }

    private void runRegalia(ServerLevel level) {
        boolean oldTerrain = ExplosionConfig.TERRAIN_DAMAGE.get();
        boolean oldPvp = ExplosionConfig.PVP_DAMAGE.get();
        FakePlayer caster = FakePlayerFactory.get(level,
                new GameProfile(UUID.fromString("ada00000-0000-0000-0000-000000000003"), "RegaliaTest"));
        LivingEntity baseline = null, far = null, boosted = null;
        try {
            ExplosionConfig.TERRAIN_DAMAGE.set(false);
            ExplosionConfig.PVP_DAMAGE.set(true);
            caster.m_6034_(0, 64, 0);
            caster.m_146922_(0); caster.m_146926_(0);
            level.m_7731_(new BlockPos(0, 65, 12), Blocks.f_50069_.m_49966_(), 3);
            GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
            MagicData data = MagicData.getPlayerMagicData(caster);
            baseline = spawnDurableCow(level, 10);
            far = spawnDurableCow(level, 82);
            float selfHealth = caster.m_21223_();
            spell.onCast(level, 5, caster, CastSource.COMMAND, data);
            float baselineDamage = 500 - baseline.m_21223_();
            boolean farSafeWithoutSet = far.m_21223_() == 500;
            baseline.m_146870_(); baseline = null;
            caster.m_8061_(EquipmentSlot.HEAD, new ItemStack(ModItems.CINDERSTAR_HAT.get()));
            caster.m_8061_(EquipmentSlot.CHEST, new ItemStack(ModItems.CINDERSTAR_ROBE.get()));
            caster.m_8061_(EquipmentSlot.LEGS, new ItemStack(ModItems.CINDERSTAR_LEGGINGS.get()));
            caster.m_8061_(EquipmentSlot.FEET, new ItemStack(ModItems.CINDERSTAR_BOOTS.get()));
            boosted = spawnDurableCow(level, 10);
            spell.onCast(level, 5, caster, CastSource.COMMAND, data);
            float boostedDamage = 500 - boosted.m_21223_();
            boolean farHitWithSet = far.m_21223_() < 500;
            boolean casterSafe = caster.m_21223_() == selfHealth;
            System.out.println("GRAND_REGALIA_RESULT fullSet=" + local.ironsultimateexplosion.CinderstarArmorItem.hasFullSet(caster)
                    + " baseDamage=" + baselineDamage + " boostedDamage=" + boostedDamage
                    + " farSafeWithoutSet=" + farSafeWithoutSet + " farHitWithSet=" + farHitWithSet
                    + " casterSafe=" + casterSafe);
            boolean recipes = java.util.stream.Stream.of("hat", "robe", "leggings", "boots")
                    .allMatch(part -> level.m_7654_().m_129894_().m_44043_(
                            new ResourceLocation("irons_ultimate_explosion:cinderstar_" + part)).isPresent());
            boolean casterAttributes = ModItems.CINDERSTAR_HAT.get().m_7167_(EquipmentSlot.HEAD)
                    .get(AttributeRegistry.FIRE_SPELL_POWER.get()).stream().anyMatch(m -> m.m_22218_() == 0.10);
            System.out.println("GRAND_REGALIA_ASSETS recipes=" + recipes + " fireAttribute=" + casterAttributes);
        } finally {
            ExplosionConfig.TERRAIN_DAMAGE.set(oldTerrain);
            ExplosionConfig.PVP_DAMAGE.set(oldPvp);
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                caster.m_8061_(slot, ItemStack.f_41583_);
            if (baseline != null) baseline.m_146870_();
            if (far != null) far.m_146870_();
            if (boosted != null) boosted.m_146870_();
        }
    }

    private static LivingEntity spawnDurableCow(ServerLevel level, double z) {
        LivingEntity cow = spawnCow(level, z);
        cow.m_21051_(Attributes.f_22276_).m_22100_(500);
        cow.m_21153_(500);
        return cow;
    }

    private static ServerPlayer damageTestPlayer(ServerLevel level, String name, String id) {
        ServerPlayer player = new ServerPlayer(level.m_7654_(), level,
                new GameProfile(UUID.fromString(id), name));
        // Borrow Forge's inert packet handler, while retaining ServerPlayer's
        // normal PvP and damage behavior. FakePlayer itself is invulnerable.
        FakePlayer connectionOwner = FakePlayerFactory.get(level,
                new GameProfile(UUID.nameUUIDFromBytes((name + "Connection").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        name + "Conn"));
        player.f_8906_ = connectionOwner.f_8906_;
        return player;
    }

    private static void clearSpawnProtection(ServerPlayer player) {
        try {
            var field = ServerPlayer.class.getDeclaredField("f_8921_");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not clear synthetic player's spawn protection", error);
        }
    }

    private void runPvp(ServerLevel level) {
        boolean oldPvp = ExplosionConfig.PVP_DAMAGE.get();
        boolean oldTerrain = ExplosionConfig.TERRAIN_DAMAGE.get();
        ServerPlayer caster = damageTestPlayer(level, "PvpCaster", "ada00000-0000-0000-0000-000000000004");
        ServerPlayer first = damageTestPlayer(level, "PvpTargetOn", "ada00000-0000-0000-0000-000000000005");
        ServerPlayer second = damageTestPlayer(level, "PvpTargetOff", "ada00000-0000-0000-0000-000000000006");
        try {
            ExplosionConfig.TERRAIN_DAMAGE.set(false);
            caster.m_6034_(0,64,0); caster.m_146922_(0); caster.m_146926_(0);
            boolean addedCaster = level.m_7967_(caster);
            level.m_7731_(new BlockPos(0,65,12), Blocks.f_50069_.m_49966_(), 3);
            GrandExplosionSpell spell = (GrandExplosionSpell)ModSpells.GRAND_EXPLOSION.get();
            MagicData data = MagicData.getPlayerMagicData(caster);
            first.m_6034_(0,64,10);
            first.m_21051_(Attributes.f_22276_).m_22100_(500); first.m_21153_(500);
            clearSpawnProtection(first); // New ServerPlayers otherwise ignore PvP during spawn protection.
            boolean addedOn = level.m_7967_(first);
            float selfHealth = caster.m_21223_();
            boolean canHarm = caster.m_7099_(first);
            boolean friendly = io.redspace.ironsspellbooks.damage.DamageSources.isFriendlyFireBetween(caster, first);
            boolean directDamage = first.m_6469_(level.m_269111_().m_287172_(), 1f);
            float directHealth = first.m_21223_();
            first.m_21153_(500);
            first.f_19802_ = 0;
            boolean vanillaPvpDamage = first.m_6469_(level.m_269111_().m_269075_(caster), 1f);
            float vanillaPvpHealth = first.m_21223_();
            first.m_21153_(500);
            first.f_19802_ = 0;
            boolean directSpellDamage = io.redspace.ironsspellbooks.damage.DamageSources.applyDamage(
                    first, 20f, spell.getDamageSource(caster));
            float directSpellHealth = first.m_21223_();
            first.m_21153_(500);
            first.f_19802_ = 0;
            net.minecraft.world.phys.Vec3 aim = io.redspace.ironsspellbooks.api.util.RaycastBuilder.begin(level, caster)
                    .range(48).checkForBlocks(true).build().m_82450_();
            int targetCount = level.m_6249_(caster, new net.minecraft.world.phys.AABB(
                    aim.f_82479_-24, aim.f_82480_-24, aim.f_82481_-24,
                    aim.f_82479_+24, aim.f_82480_+24, aim.f_82481_+24), e -> e instanceof net.minecraft.world.entity.LivingEntity).size();
            ExplosionConfig.PVP_DAMAGE.set(true);
            spell.onCast(level, 5, caster, CastSource.COMMAND, data);
            boolean otherDamaged = first.m_21223_() < 500;
            boolean selfSafe = caster.m_21223_() == selfHealth;
            first.m_146870_();
            second.m_6034_(0,64,10);
            second.m_21051_(Attributes.f_22276_).m_22100_(500); second.m_21153_(500);
            clearSpawnProtection(second);
            boolean addedOff = level.m_7967_(second);
            ExplosionConfig.PVP_DAMAGE.set(false);
            spell.onCast(level, 5, caster, CastSource.COMMAND, data);
            boolean otherSafeWhenOff = second.m_21223_() == 500;
            System.out.println("GRAND_PVP_RESULT addedCaster=" + addedCaster + " addedOn=" + addedOn + " addedOff=" + addedOff
                    + " canHarm=" + canHarm + " targetCreative=" + first.m_7500_()
                    + " friendly=" + friendly + " directDamage=" + directDamage + " directHealth=" + directHealth
                    + " vanillaPvpDamage=" + vanillaPvpDamage + " vanillaPvpHealth=" + vanillaPvpHealth
                    + " directSpellDamage=" + directSpellDamage + " directSpellHealth=" + directSpellHealth
                    + " aim=" + aim + " targetCount=" + targetCount
                    + " otherDamaged=" + otherDamaged + " selfSafe=" + selfSafe
                    + " otherSafeWhenOff=" + otherSafeWhenOff);
        } finally {
            ExplosionConfig.PVP_DAMAGE.set(oldPvp);
            ExplosionConfig.TERRAIN_DAMAGE.set(oldTerrain);
            first.m_146870_(); second.m_146870_();
            caster.m_146870_();
        }
    }
}
