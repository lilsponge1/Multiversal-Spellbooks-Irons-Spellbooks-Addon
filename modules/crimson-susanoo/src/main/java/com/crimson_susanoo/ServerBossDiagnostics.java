package com.crimson_susanoo;

import com.crimson_susanoo.entity.CrimsonEntity;
import com.crimson_susanoo.spell.CrimsonSpell;
import com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.Ignis_Entity;
import com.github.L_Ender.cataclysm.init.ModEntities;
import com.github.L_Ender.cataclysm.config.CMCommonConfig;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Explicitly armed disposable-server encounter; never registered during normal play. */
public final class ServerBossDiagnostics {
    private ServerLevel level;
    private FakePlayer owner;
    private CrimsonEntity guardian;
    private Ignis_Entity boss;
    private Map<UUID, ServerPlayer> ownerLookup;
    private final Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
    private final Set<Long> forcedHere = new HashSet<>();
    private final Set<UUID> originalEntities = new HashSet<>();
    private final Set<Integer> guardianActions = new HashSet<>();
    private final Set<Integer> bossAnimations = new HashSet<>();
    private AABB arena;
    private BlockPos origin;
    private boolean oldGriefing;
    private boolean ruleChanged;
    private boolean oldIgnisGriefing;
    private boolean running;
    private boolean fixturesCreated;
    private int elapsed;
    private int terminalTick = -1;
    private final Set<Entity> fixtureEntities = new HashSet<>();
    private int outgoingHits;
    private int incomingHits;
    private int guardHits;
    private boolean attributionCorrect = true;
    private float minimumBossHealth;
    private float minimumGuardianHealth;
    private float guardBeforeAmount;
    private float guardBeforeMana;
    private LivingDamageEvent pendingGuard;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void start(ServerStartedEvent event) {
        if (!event.getServer().getPlayerList().getPlayers().isEmpty()) {
            CrimsonSusanoo.LOGGER.error("Boss encounter refused: real players are online");
            return;
        }
        level = event.getServer().overworld();
        try {
            io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                    new net.minecraftforge.event.OnDatapackSyncEvent(event.getServer().getPlayerList(), null));
            // Select the exact UUID -> ServerPlayer map by its generic type, independent of field remapping.
            for (var field : PlayerList.class.getDeclaredFields()) {
                String type = field.getGenericType().getTypeName();
                if (type.equals("java.util.Map<java.util.UUID, net.minecraft.server.level.ServerPlayer>")) {
                    field.setAccessible(true);
                    ownerLookup = (Map<UUID, ServerPlayer>) field.get(event.getServer().getPlayerList());
                    break;
                }
            }
            if (ownerLookup == null) throw new IllegalStateException("Player UUID lookup not found");
            origin = new BlockPos(level.getSharedSpawnPos().getX() + 2048, 239,
                    level.getSharedSpawnPos().getZ() + 2048);
            arena = new AABB(origin.offset(-24, 0, -24), origin.offset(25, 17, 25));
            for (int cx = (origin.getX() - 24) >> 4; cx <= (origin.getX() + 24) >> 4; cx++) {
                for (int cz = (origin.getZ() - 24) >> 4; cz <= (origin.getZ() + 24) >> 4; cz++) {
                    long key = ChunkPos.asLong(cx, cz);
                    if (!level.getForcedChunks().contains(key)) {
                        level.setChunkForced(cx, cz, true);
                        forcedHere.add(key);
                    }
                }
            }
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-24, 0, -24), origin.offset(24, 16, 24))) {
                BlockState state = level.getBlockState(pos);
                if (level.getBlockEntity(pos) != null || (pos.getY() > origin.getY() && !state.isAir())) {
                    throw new IllegalStateException("Encounter arena must have clear air and no block entities");
                }
                savedBlocks.put(pos.immutable(), state);
            }
            for (Entity entity : level.getEntities((Entity) null, arena.inflate(8))) originalEntities.add(entity.getUUID());
            if (!originalEntities.isEmpty()) throw new IllegalStateException("Encounter arena is occupied");
            oldGriefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, event.getServer());
            oldIgnisGriefing = CMCommonConfig.Ignis.ignoreMobGriefing;
            CMCommonConfig.Ignis.ignoreMobGriefing = false;
            ruleChanged = true;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-24, 0, -24), origin.offset(24, 0, 24))) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
            fixturesCreated = true;
            owner = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CrimsonBossTest")) {
                // Forge's default fake player cannot be hurt; allow real boss damage events and Guard.
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
            };
            // Owner health/mana are fixture reserves. Boss and guardian retain production balance and AI.
            owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            owner.setHealth(1000);
            owner.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(5000);
            owner.getAttribute(AttributeRegistry.MANA_REGEN.get()).setBaseValue(0);
            MagicData.getPlayerMagicData(owner).setMana(5000);
            owner.moveTo(origin.getX() + .5, origin.getY() + 1, origin.getZ() + .5, 0, 0);
            ownerLookup.put(owner.getUUID(), owner);
            level.addNewPlayer(owner);
            guardian = ((CrimsonSpell) CrimsonSusanoo.SPELL.get()).summonForVisualTest(owner, level);
            if (guardian == null) throw new IllegalStateException("Natural guardian placement failed");
            guardian.setHunting(true);
            boss = ModEntities.IGNIS.get().create(level);
            if (boss == null) throw new IllegalStateException("Ignis creation failed");
            boss.moveTo(origin.getX() + .5, origin.getY() + 1, origin.getZ() + 8.5, 180, 0);
            boss.setTarget(owner);
            if (!level.addFreshEntity(boss)) throw new IllegalStateException("Ignis registration failed");
            minimumBossHealth = boss.getHealth();
            minimumGuardianHealth = guardian.getHealth();
            running = true;
            CrimsonSusanoo.LOGGER.info("Boss encounter START guardian={} boss={} bossHealth={} guardianHealth={} origin={} ownerHealth=1000 ownerMana=5000",
                    guardian.getId(), boss.getId(), boss.getHealth(), guardian.getHealth(), origin);
        } catch (Exception error) {
            CrimsonSusanoo.LOGGER.error("Boss encounter SETUP FAILED", error);
            cleanup();
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (!running || event.phase != TickEvent.Phase.END) return;
        if (!event.getServer().getPlayerList().getPlayers().isEmpty()) {
            CrimsonSusanoo.LOGGER.error("Boss encounter ABORT: a real player joined");
            cleanup();
            return;
        }
        elapsed++;
        // Forge FakePlayer.tick() is empty; advance its clock for normal pack mana/target timestamps.
        owner.tickCount++;
        if (elapsed == 400 && boss.isAlive()) {
            boss.setTarget(owner);
            CrimsonSusanoo.LOGGER.info("Boss encounter owner-aggro phase at tick={}", elapsed);
        }
        // A slow six-block orbit exercises pursuit. Actual entities advance through ordinary level ticks.
        double angle = elapsed * .012;
        owner.setPos(origin.getX() + .5 + 6 * Math.sin(angle), origin.getY() + 1,
                origin.getZ() + .5 + 6 * Math.cos(angle));
        guardianActions.add(guardian.getAction());
        bossAnimations.add(java.util.Arrays.asList(boss.getAnimations()).indexOf(boss.getAnimation()));
        minimumBossHealth = Math.min(minimumBossHealth, boss.getHealth());
        minimumGuardianHealth = Math.min(minimumGuardianHealth, guardian.getHealth());
        if (elapsed % 20 == 0) {
            CrimsonSusanoo.LOGGER.info("Boss encounter SAMPLE tick={} guardianTick={} bossTick={} guardianHealth={} bossHealth={} ownerHealth={} mana={} action={} bossAnimation={} guardianPos={} bossPos={} ownerDistance={}",
                    elapsed, guardian.tickCount, boss.tickCount, guardian.getHealth(), boss.getHealth(), owner.getHealth(),
                    MagicData.getPlayerMagicData(owner).getMana(), guardian.getAction(),
                    java.util.Arrays.asList(boss.getAnimations()).indexOf(boss.getAnimation()), guardian.position(), boss.position(), guardian.distanceTo(owner));
        }
        if (terminalTick < 0 && (!guardian.isAlive() || !boss.isAlive() || !owner.isAlive()
                || guardian.isRemoved() || boss.isRemoved() || guardian.isDismissing())) terminalTick = elapsed;
        // Observe the natural end and the guardian's complete 32-tick death/fade window before cleanup.
        if (elapsed >= 1900 || (terminalTick >= 0 && elapsed - terminalTick >= 40)) {
            int changedBlocks = 0;
            Map<String, Integer> blockChanges = new HashMap<>();
            for (var entry : savedBlocks.entrySet()) {
                BlockState expected = entry.getKey().getY() == origin.getY() ? Blocks.STONE.defaultBlockState() : entry.getValue();
                if (!level.getBlockState(entry.getKey()).equals(expected)) {
                    changedBlocks++;
                    String change = expected + " -> " + level.getBlockState(entry.getKey());
                    blockChanges.merge(change, 1, Integer::sum);
                }
            }
            CrimsonSusanoo.LOGGER.info("Boss encounter END ticks={} guardianTick={} bossTick={} guardianActions={} bossAnimations={} outgoingHits={} incomingHits={} guardHits={} attributionCorrect={} minimumBossHealth={} minimumGuardianHealth={} terrainChanges={} guardianAlive={} bossAlive={}",
                    elapsed, guardian.tickCount, boss.tickCount, guardianActions, bossAnimations, outgoingHits, incomingHits,
                    guardHits, attributionCorrect, minimumBossHealth, minimumGuardianHealth, changedBlocks, guardian.isAlive(), boss.isAlive());
            CrimsonSusanoo.LOGGER.info("Boss encounter LIFECYCLE guardianDeathTime={} guardianRemoved={} cooldown={} ownerMarker={} blockChanges={}",
                    guardian.deathTime, guardian.isRemoved(), MagicData.getPlayerMagicData(owner).getPlayerCooldowns().isOnCooldown(CrimsonSusanoo.SPELL.get()),
                    owner.getPersistentData().hasUUID(CrimsonSpell.ACTIVE_ID), blockChanges);
            cleanup();
        }
    }

    @SubscribeEvent
    public void captureSpawn(EntityJoinLevelEvent event) {
        if (fixturesCreated && level != null && event.getLevel() == level
                && !(event.getEntity() instanceof ServerPlayer) && arena.inflate(8).intersects(event.getEntity().getBoundingBox())
                && !originalEntities.contains(event.getEntity().getUUID())) fixtureEntities.add(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeGuard(LivingDamageEvent event) {
        if (running && event.getEntity() == owner) {
            pendingGuard = event;
            guardBeforeAmount = event.getAmount();
            guardBeforeMana = MagicData.getPlayerMagicData(owner).getMana();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void damage(LivingDamageEvent event) {
        if (!running) return;
        if (event.getEntity() == boss && event.getSource() instanceof SpellDamageSource source
                && source.spell() == CrimsonSusanoo.SPELL.get()) {
            outgoingHits++;
            attributionCorrect &= source.getEntity() == owner;
            CrimsonSusanoo.LOGGER.info("Boss encounter HIT outgoing amount={} ownerCause={} direct={}",
                    event.getAmount(), source.getEntity() == owner, source.getDirectEntity());
        }
        if (event.getEntity() == guardian && event.getSource().getEntity() == boss) incomingHits++;
        if (event == pendingGuard) {
            float spent = guardBeforeMana - MagicData.getPlayerMagicData(owner).getMana();
            float redirected = guardBeforeAmount - event.getAmount();
            if (redirected > 0 && spent > 0) guardHits++;
            CrimsonSusanoo.LOGGER.info("Boss encounter GUARD before={} after={} manaSpent={} redirected={} distance={}",
                    guardBeforeAmount, event.getAmount(), spent, redirected, guardian.distanceTo(owner));
            pendingGuard = null;
        }
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) { cleanup(); }

    private void cleanup() {
        running = false;
        if (level == null) return;
        if (guardian != null) guardian.discard();
        if (boss != null) boss.discard();
        if (owner != null) {
            CrimsonSpell.clearOwnerMarker(owner);
            level.removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
            if (ownerLookup != null) ownerLookup.remove(owner.getUUID(), owner);
        }
        for (Entity entity : fixtureEntities) entity.discard();
        for (var entry : savedBlocks.entrySet()) {
            if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        if (ruleChanged) {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(oldGriefing, level.getServer());
            CMCommonConfig.Ignis.ignoreMobGriefing = oldIgnisGriefing;
        }
        for (long key : forcedHere) level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        int unrestoredBlocks = 0;
        for (var entry : savedBlocks.entrySet()) if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) unrestoredBlocks++;
        long unrestoredChunks = forcedHere.stream().filter(key -> level.getForcedChunks().contains(key)).count();
        boolean ownerRemoved = owner == null || ownerLookup == null || ownerLookup.get(owner.getUUID()) != owner;
        boolean rulesRestored = !ruleChanged || (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) == oldGriefing
                && CMCommonConfig.Ignis.ignoreMobGriefing == oldIgnisGriefing);
        long remainingEntities = fixtureEntities.stream().filter(entity -> !entity.isRemoved()).count();
        CrimsonSusanoo.LOGGER.info("Boss encounter CLEANUP verified blocks={} chunks={} ownerRemoved={} rulesRestored={} remainingEntities={}",
                unrestoredBlocks, unrestoredChunks, ownerRemoved, rulesRestored, remainingEntities);
        fixtureEntities.clear();
        savedBlocks.clear();
        forcedHere.clear();
        ruleChanged = false;
        level = null;
        CrimsonSusanoo.LOGGER.info("Boss encounter CLEANUP complete: fixture entities, terrain, owner lookup, griefing and forced chunks restored");
    }
}