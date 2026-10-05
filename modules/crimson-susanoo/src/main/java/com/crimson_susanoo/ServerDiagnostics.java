package com.crimson_susanoo;

import com.crimson_susanoo.entity.CrimsonEntity;
import com.crimson_susanoo.entity.CrimsonWave;
import com.crimson_susanoo.entity.SafePlacement;
import com.crimson_susanoo.spell.CrimsonSpell;
import com.github.L_Ender.cataclysm.init.ModEffect;
import com.github.L_Ender.cataclysm.init.ModEntities;
import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Opt-in isolated-server checks against the installed Iron's and Cataclysm APIs. */
public final class ServerDiagnostics {
    private void verifyFriendlyProtection(ServerLevel level, FakePlayer owner, CrimsonEntity guardian) throws Exception {
        FakePlayer visitor = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CrimsonGuest"));
        visitor.moveTo(guardian.getX()+3, guardian.getY(), guardian.getZ());
        Zombie hostile = EntityType.ZOMBIE.create(level);
        Wolf pet = EntityType.WOLF.create(level);
        var arrow = EntityType.ARROW.create(level);
        CrimsonEntity companion = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (hostile == null || pet == null || arrow == null || companion == null)
            throw new IllegalStateException("Could not create friendly-fire fixtures");
        hostile.moveTo(guardian.getX()+1, guardian.getY(), guardian.getZ());
        pet.setTame(true);
        pet.setOwnerUUID(visitor.getUUID());
        companion.setOwner(visitor);
        arrow.setOwner(visitor);
        boolean previousPolicy = ServerConfig.FRIENDLY_FIRE.get();
        float previousHealth = guardian.getHealth();
        LivingEntity previousAttacker = guardian.getLastHurtByMob();
        LivingEntity previousOwnerAttacker = owner.getLastHurtByMob();
        LivingEntity previousOwnerVictim = owner.getLastHurtMob();
        var target = CrimsonEntity.class.getDeclaredField("attackTarget");
        target.setAccessible(true);
        Object previousTarget = target.get(guardian);
        var choose = CrimsonEntity.class.getDeclaredMethod("selectTarget", net.minecraft.server.level.ServerPlayer.class);
        choose.setAccessible(true);
        try {
            ServerConfig.FRIENDLY_FIRE.set(false);
            check(visitor.getTeam() == null, "friendly guest requires no scoreboard team");
            check(!guardian.canHarm(owner, visitor) && !guardian.canStrike(owner, visitor),
                    "default guardian damage excludes an unteamed player");
            guardian.setLastHurtByMob(hostile);
            int retaliationTimestamp = guardian.getLastHurtByMobTimestamp();
            check(!guardian.hurt(level.damageSources().playerAttack(visitor), 20), "friendly melee hit is rejected");
            check(!guardian.hurt(level.damageSources().arrow(arrow, visitor), 20), "friendly arrow is rejected");
            check(!guardian.hurt(level.damageSources().arrow(arrow, null), 20),
                    "friendly projectile owner is resolved when damage source omits its attacker");
            check(!guardian.hurt(level.damageSources().indirectMagic(arrow, visitor), 20), "friendly attributed spell hit is rejected");
            check(guardian.getHealth() == previousHealth && guardian.getLastHurtByMob() == hostile
                            && guardian.getLastHurtByMobTimestamp() == retaliationTimestamp,
                    "friendly hits preserve health and existing hostile retaliation history");
            check(!guardian.canHarm(owner, pet) && !guardian.hurt(level.damageSources().mobAttack(pet), 20),
                    "another player's tamed pet is protected in both directions");
            check(!guardian.canHarm(owner, companion) && !guardian.hurt(level.damageSources().mobAttack(companion), 20),
                    "another player's Crimson summon is protected in both directions");
            for (int source=0; source<3; source++) {
                owner.setLastHurtByMob(source == 0 ? visitor : null);
                owner.setLastHurtMob(source == 1 ? visitor : null);
                guardian.setLastHurtByMob(source == 2 ? visitor : null);
                target.set(guardian, visitor);
                choose.invoke(guardian, owner);
                check(target.get(guardian) != visitor, "player cannot enter retaliation targeting through history source " + source);
            }
            check(guardian.canHarm(owner, hostile), "co-op protection still permits hostile mob combat");
            guardian.invulnerableTime = 0;
            check(guardian.hurt(level.damageSources().mobAttack(hostile), 20) && guardian.getHealth() < previousHealth,
                    "hostile mob damage still reaches the guardian");
            ServerConfig.FRIENDLY_FIRE.set(true);
            check(guardian.canHarm(owner, visitor) == owner.canHarmPlayer(visitor),
                    "explicit player-combat opt-in follows server/team PvP permission");
            check(!guardian.hurt(level.damageSources().playerAttack(owner), 20),
                    "owner's own hit stays harmless even with friendly combat enabled");
        } finally {
            ServerConfig.FRIENDLY_FIRE.set(previousPolicy);
            guardian.setHealth(previousHealth);
            guardian.invulnerableTime = 0;
            guardian.hurtTime = 0;
            guardian.setLastHurtByMob(previousAttacker);
            owner.setLastHurtByMob(previousOwnerAttacker);
            owner.setLastHurtMob(previousOwnerVictim);
            target.set(guardian, previousTarget);
            hostile.discard(); pet.discard(); arrow.discard(); companion.discard();
        }
    }

    private void verifyScrollForge(ServerLevel level, FakePlayer player) {
        var spell = CrimsonSusanoo.SPELL.get();
        var tile = new io.redspace.ironsspellbooks.block.scroll_forge.ScrollForgeTile(
                player.blockPosition(), io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get().defaultBlockState());
        tile.setLevel(level);
        // Use the real tile/menu and result-slot handler without placing a block.
        var menu = (io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu)
                tile.createMenu(91, player.getInventory(), player);
        try {
            check(spell.allowCrafting() && spell.canBeCraftedBy(player), "Crimson scroll is available for Scroll Forge crafting");
            check(spell.getMinLevelForRarity(io.redspace.ironsspellbooks.api.spells.SpellRarity.LEGENDARY) == 1
                            && spell.getMinLevelForRarity(io.redspace.ironsspellbooks.api.spells.SpellRarity.EPIC) == 0,
                    "Scroll Forge rarity selection requires Legendary ink for level one");
            menu.getBlankScrollSlot().set(new ItemStack(net.minecraft.world.item.Items.PAPER, 2));
            menu.getInkSlot().set(new ItemStack(ItemRegistry.INK_LEGENDARY.get(), 2));
            menu.getFocusSlot().set(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD, 2));
            menu.setRecipeSpell(spell);
            ItemStack result = menu.getResultSlot().getItem();
            var stored = ISpellContainer.get(result).getSpellAtIndex(0);
            check(result.is(ItemRegistry.SCROLL.get()) && result.getCount() == 1
                            && stored.getSpell() == spell && stored.getLevel() == 1,
                    "Legendary ink + paper + blaze rod produces a level-one Crimson scroll");
            ItemStack taken = menu.getResultSlot().remove(1);
            menu.getResultSlot().onTake(player, taken);
            check(menu.getBlankScrollSlot().getItem().getCount() == 1
                            && menu.getInkSlot().getItem().getCount() == 1 && menu.getFocusSlot().getItem().getCount() == 1,
                    "taking the forged scroll consumes exactly one of each ingredient");
            menu.getFocusSlot().set(new ItemStack(net.minecraft.world.item.Items.DIRT));
            menu.setRecipeSpell(spell);
            check(menu.getResultSlot().getItem().isEmpty(), "wrong school focus cannot forge a Crimson scroll");
            menu.getFocusSlot().set(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD));
            menu.getBlankScrollSlot().set(ItemStack.EMPTY);
            menu.setRecipeSpell(spell);
            check(menu.getResultSlot().getItem().isEmpty(), "missing paper cannot forge a Crimson scroll");
            check(!spell.getUniqueInfo(1, null).isEmpty(), "scroll description resolves without a player for recipe previews");
        } finally {
            tile.clearContent();
            tile.invalidateCaps();
        }
    }

    private int passed;
    private int failed;
    private UUID discountedCastOwner;
    private FakePlayer diagnosticCombatOwner;
    private boolean correctDamageAttribution;
    private int attributedHits;

    @SubscribeEvent
    public void onDiagnosticDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (diagnosticCombatOwner == null
                || !(event.getSource() instanceof io.redspace.ironsspellbooks.damage.SpellDamageSource source)
                || source.spell() != CrimsonSusanoo.SPELL.get()
                || (source.getEntity() != diagnosticCombatOwner && source.getDirectEntity() != diagnosticCombatOwner)) return;
        attributedHits++;
        correctDamageAttribution &= source.getEntity() == diagnosticCombatOwner
                && (source.getDirectEntity() instanceof CrimsonEntity || source.getDirectEntity() instanceof CrimsonWave);
    }

    @SubscribeEvent
    public void onDiagnosticSpellCost(SpellOnCastEvent event) {
        if (discountedCastOwner != null && discountedCastOwner.equals(event.getEntity().getUUID())
                && CrimsonSusanoo.SPELL.get().getSpellId().equals(event.getSpellId())) {
            event.setManaCost(40);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Iron 3.16 builds its effective spell config at datapack sync/player
        // login. A headless server has no login: perform that normal sync before
        // testing, otherwise AbstractSpell reads generic Common/level-one defaults.
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                new net.minecraftforge.event.OnDatapackSyncEvent(event.getServer().getPlayerList(), null));
        ServerLevel level = event.getServer().overworld();
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CrimsonTest"));
        diagnosticCombatOwner = player;
        correctDamageAttribution = true;
        attributedHits = 0;
        AbstractSpell spell = CrimsonSusanoo.SPELL.get();
        try {
            // Isolate spell-power accounting from the pack's random critical strikes.
            // Only this disposable fake player is changed; normal combat keeps its bonuses.
            diagnosticAttribute(player, "attributeslib:crit_chance", 0);
            diagnosticAttribute(player, "attributeslib:crit_damage", 1);
            verifySynchronizedPackets(level, player);
            verifyAnimationHandoff(level);
            verifyCrescentWalls(level, player);
            verifyNavigationPriority(level, player);
            verifyPursuitCorridors(level);
            verifyUnevenTerrainNavigation(level);
            verifyMovingInterception(level, player);
            verifyScrollForge(level, player);
            MagicData magic = MagicData.getPlayerMagicData(player);
            var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA.get());
            if (maxMana == null) throw new IllegalStateException("Fake player lacks Iron's Max Mana attribute");
            maxMana.setBaseValue(300);
            magic.setMana(200);
            CrimsonSusanoo.LOGGER.info("Diagnostic mana after setting 200: {} (max-mana attribute {})",
                    magic.getMana(), maxMana.getValue());
            check(spell.checkPreCastConditions(level, 1, player, magic), "cast permitted with 200 mana");
            magic.setMana(149);
            check(!spell.checkPreCastConditions(level, 1, player, magic), "cast rejected with 149 mana");

            magic.setMana(200);
            check(CrimsonEntity.chargeMana(player, 12), "upkeep payment accepted");
            check(Math.abs(magic.getMana() - 188) < 0.001, "upkeep charged exactly 12 mana");
            magic.setMana(11);
            check(!CrimsonEntity.chargeMana(player, 12), "upkeep rejected below 12 mana");
            check(Math.abs(magic.getMana() - 11) < 0.001, "failed upkeep leaves mana unchanged");

            verifyManaRegeneration(level, player);

            var firePower = player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER.get());
            if (firePower == null) throw new IllegalStateException("Fake player lacks Iron's Fire Spell Power attribute");
            double basePower = spell.getSpellPower(1, player);
            verifyDamageBalance(level, player);
            double oldFirePower = firePower.getBaseValue();
            firePower.setBaseValue(oldFirePower + 1);
            double effectiveFirePower = firePower.getValue();
            double boostedPower = spell.getSpellPower(1, player);
            CrimsonSusanoo.LOGGER.info("Diagnostic Fire Spell Power: base={} boosted={} attribute {} -> {} effective={} school={} expectedFire={}",
                    basePower, boostedPower, oldFirePower, oldFirePower + 1, effectiveFirePower,
                    spell.getSchoolType().getId(), SchoolRegistry.FIRE.get().getId());
            firePower.setBaseValue(oldFirePower);
            check(boostedPower > basePower, "Iron's Fire Spell Power increases guardian damage multiplier");

            var cooldownReduction = player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get());
            if (cooldownReduction == null) throw new IllegalStateException("Fake player lacks Iron's Cooldown Reduction attribute");
            int baseCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
            double oldReduction = cooldownReduction.getBaseValue();
            cooldownReduction.setBaseValue(oldReduction + 0.5);
            int reducedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);
            cooldownReduction.setBaseValue(oldReduction);
            check(baseCooldown > 0 && reducedCooldown < baseCooldown,
                    "Iron's cooldown reduction shortens the post-dismissal timer");

            BlockPos spawn = level.getSharedSpawnPos();
            CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
            if (guardian == null) throw new IllegalStateException("Could not create diagnostic guardian");
            Method findPosition = CrimsonSpell.class.getDeclaredMethod("findPosition", ServerLevel.class,
                    net.minecraft.server.level.ServerPlayer.class, CrimsonEntity.class);
            findPosition.setAccessible(true);
            Vec3 safePosition = null;
            int[] offsets = {0, 16, -16, 32, -32, 48, -48};
            for (int dx : offsets) {
                for (int dz : offsets) {
                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            spawn.offset(dx, 0, dz));
                    player.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5, 0, 0);
                    safePosition = (Vec3) findPosition.invoke(spell, level, player, guardian);
                    if (safePosition != null) break;
                }
                if (safePosition != null) break;
            }
            CrimsonSusanoo.LOGGER.info("Diagnostic placement origin={} result={}", player.blockPosition(), safePosition);
            check(safePosition != null && level.noCollision(guardian,
                    guardian.getDimensions(guardian.getPose()).makeBoundingBox(safePosition)),
                    "guardian placement has no block collision");
            if (safePosition != null) {
                BlockPos waterProbe = null;
                for (int height = 7; height >= 2; height--) {
                    BlockPos candidate = BlockPos.containing(safePosition.x, safePosition.y + height, safePosition.z);
                    if (level.getBlockState(candidate).isAir()) {
                        waterProbe = candidate;
                        break;
                    }
                }
                if (waterProbe == null) {
                    check(false, "guardian placement rejects liquid above its feet");
                } else {
                    var original = level.getBlockState(waterProbe);
                    try {
                        if (!level.setBlock(waterProbe, Blocks.WATER.defaultBlockState(), 3)) {
                            check(false, "guardian placement rejects liquid above its feet");
                        } else {
                            check(level.noCollision(guardian,
                                            guardian.getDimensions(guardian.getPose()).makeBoundingBox(safePosition))
                                            && !SafePlacement.isClear(level, guardian, safePosition),
                                    "guardian placement rejects liquid above its feet");
                        }
                    } finally {
                        level.setBlock(waterProbe, original, 3);
                    }
                }
                guardian.setOwner(player);
                guardian.moveTo(safePosition.x, safePosition.y, safePosition.z);
                check(level.addFreshEntity(guardian), "guardian registers in the server level");
                verifyFriendlyProtection(level, player, guardian);
                player.getPersistentData().putUUID(CrimsonSpell.ACTIVE_ID, guardian.getUUID());
                magic.setMana(200);
                check(CrimsonSpell.findActive(player, level) == guardian, "owner finds its active guardian");
                check(!spell.checkPreCastConditions(level, 1, player, magic), "second summon is rejected");

                FakePlayer secondOwner = FakePlayerFactory.get(level,
                        new GameProfile(UUID.randomUUID(), "CrimsonTwo"));
                secondOwner.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(300);
                MagicData secondMagic = MagicData.getPlayerMagicData(secondOwner);
                secondMagic.setMana(200);
                check(spell.checkPreCastConditions(level, 1, secondOwner, secondMagic),
                        "another owner may cast while the first guardian is active");
                CrimsonEntity secondGuardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (secondGuardian == null) throw new IllegalStateException("Could not create second diagnostic guardian");
                Vec3 secondPosition = null;
                for (int dx : offsets) {
                    for (int dz : offsets) {
                        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                spawn.offset(dx, 0, dz));
                        secondOwner.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5, 0, 0);
                        if (secondOwner.distanceToSqr(player) < 24 * 24) continue;
                        secondPosition = (Vec3) findPosition.invoke(spell, level, secondOwner, secondGuardian);
                        if (secondPosition != null) break;
                    }
                    if (secondPosition != null) break;
                }
                check(secondPosition != null, "second owner has a collision-free guardian position");
                if (secondPosition != null) {
                    secondGuardian.setOwner(secondOwner);
                    secondGuardian.moveTo(secondPosition.x, secondPosition.y, secondPosition.z);
                    check(level.addFreshEntity(secondGuardian), "second owner's guardian registers concurrently");
                    secondOwner.getPersistentData().putUUID(CrimsonSpell.ACTIVE_ID, secondGuardian.getUUID());
                    check(CrimsonSpell.findActive(secondOwner, level) == secondGuardian
                                    && CrimsonSpell.findActive(player, level) == guardian,
                            "two owners retain distinct active guardians");
                    CrimsonSpell.clearOwnerMarker(secondOwner);
                    secondGuardian.discard();
                }

                Zombie cleaveTarget = EntityType.ZOMBIE.create(level);
                if (cleaveTarget == null) throw new IllegalStateException("Could not create Cleave target");
                cleaveTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                if (cleaveTarget.getAttribute(Attributes.ARMOR) != null) {
                    cleaveTarget.getAttribute(Attributes.ARMOR).setBaseValue(0);
                }
                cleaveTarget.setHealth(200);
                Vec3 inFront = guardian.position().add(guardian.getLookAngle().normalize().scale(3));
                cleaveTarget.moveTo(inFront.x, inFront.y, inFront.z);
                check(level.addFreshEntity(cleaveTarget), "Cleave target registers in the server level");
                Zombie cleaveBehind = EntityType.ZOMBIE.create(level);
                Zombie cleaveSide = EntityType.ZOMBIE.create(level);
                if (cleaveBehind == null || cleaveSide == null) throw new IllegalStateException("Could not create Cleave arc targets");
                for (Zombie outside : new Zombie[] {cleaveBehind, cleaveSide}) {
                    outside.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
                    outside.setHealth(200);
                }
                Vec3 arcSide = new Vec3(-guardian.getLookAngle().z, 0, guardian.getLookAngle().x).normalize();
                Vec3 behind = guardian.position().subtract(guardian.getLookAngle().normalize().scale(3));
                Vec3 beside = guardian.position().add(arcSide.scale(3));
                cleaveBehind.moveTo(behind.x, behind.y, behind.z);
                cleaveSide.moveTo(beside.x, beside.y, beside.z);
                if (!level.addFreshEntity(cleaveBehind) || !level.addFreshEntity(cleaveSide)) {
                    throw new IllegalStateException("Could not register Cleave arc targets");
                }
                var attackTargetField = CrimsonEntity.class.getDeclaredField("attackTarget");
                attackTargetField.setAccessible(true);
                attackTargetField.set(guardian, cleaveTarget);
                Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocksBefore = new HashMap<>();
                BlockPos center = BlockPos.containing(guardian.position());
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, 5, 6))) {
                    blocksBefore.put(pos.immutable(), level.getBlockState(pos));
                }
                Method cleave = CrimsonEntity.class.getDeclaredMethod("cleave", net.minecraft.server.level.ServerPlayer.class);
                cleave.setAccessible(true);
                cleave.invoke(guardian, player);
                check(cleaveTarget.getHealth() < cleaveTarget.getMaxHealth(), "Cleave damages a hostile target in front");
                float baseCleaveDamage = 200 - cleaveTarget.getHealth();
                check(Math.abs(cleaveBehind.getHealth() - 200) < 0.001
                                && Math.abs(cleaveSide.getHealth() - 200) < 0.001,
                        "Cleave excludes targets behind and outside its 120-degree arc");
                check(Math.abs(baseCleaveDamage - ServerConfig.CLEAVE_DAMAGE.get() * basePower * ServerConfig.DAMAGE_MULTIPLIER.get()) < 0.2,
                        "Cleave damages its front target once");
                cleaveTarget.setHealth(200);
                cleaveTarget.invulnerableTime = 0;
                cleaveTarget.moveTo(inFront.x, inFront.y, inFront.z);
                try {
                    firePower.setBaseValue(oldFirePower + 1);
                    cleave.invoke(guardian, player);
                } finally {
                    firePower.setBaseValue(oldFirePower);
                }
                float boostedCleaveDamage = 200 - cleaveTarget.getHealth();
                CrimsonSusanoo.LOGGER.info("Diagnostic Cleave damage: {} -> {}, power {} -> {}",
                        baseCleaveDamage, boostedCleaveDamage, basePower, boostedPower);
                check(baseCleaveDamage > 0 && Math.abs(boostedCleaveDamage
                                - baseCleaveDamage * boostedPower / basePower) < 0.2,
                        "Cleave applies Fire Spell Power once to actual damage");
                check(blocksBefore.entrySet().stream().allMatch(entry -> level.getBlockState(entry.getKey()).equals(entry.getValue())),
                        "Cleave leaves nearby terrain unchanged");
                Method turnAttack = CrimsonEntity.class.getDeclaredMethod("beginAttack", int.class, int.class);
                turnAttack.setAccessible(true);
                float originalHeading = guardian.getYRot();
                cleaveTarget.setHealth(200);
                cleaveTarget.invulnerableTime = 0;
                attackTargetField.set(guardian, cleaveBehind);
                turnAttack.invoke(guardian, 1, 11);
                check(Math.abs(net.minecraft.util.Mth.wrapDegrees(guardian.getYRot() - originalHeading)) <= 24.001,
                        "attack entry bounds body turning instead of snapping to a rear target");
                Method trackAttack = CrimsonEntity.class.getDeclaredMethod("advanceAttackStep", net.minecraft.server.level.ServerPlayer.class, ServerLevel.class);
                trackAttack.setAccessible(true);
                var trackingTicks = CrimsonEntity.class.getDeclaredField("actionTicks");
                trackingTicks.setAccessible(true);
                for (int remaining = 11; remaining > 4; remaining--) {
                    float beforeTurn = guardian.getYRot();
                    trackingTicks.setInt(guardian, remaining);
                    trackAttack.invoke(guardian, player, level);
                    check(Math.abs(net.minecraft.util.Mth.wrapDegrees(guardian.getYRot() - beforeTurn)) <= 24.001,
                            "anticipation bounds body turn at remaining tick " + remaining);
                }
                cleave.invoke(guardian, player);
                check(cleaveBehind.getHealth() < 200 && Math.abs(cleaveTarget.getHealth() - 200) < 0.001,
                        "Cleave windup turns toward a rear target and excludes the old forward target");
                check(Math.abs(guardian.yBodyRot - guardian.getYRot()) < 0.001,
                        "attack windup aligns rendered body with authoritative Cleave facing");
                float committedHeading = guardian.getYRot();
                attackTargetField.set(guardian, cleaveTarget);
                trackingTicks.setInt(guardian, 4);
                trackAttack.invoke(guardian, player, level);
                check(guardian.getYRot() == committedHeading,
                        "final strike direction stays committed when target changes sides");
                guardian.setYRot(originalHeading);
                guardian.setYBodyRot(originalHeading);
                guardian.setYHeadRot(originalHeading);
                cleaveTarget.discard();
                cleaveBehind.discard();
                cleaveSide.discard();

                LivingEntity cataclysmBoss = ModEntities.ENDER_GOLEM.get().create(level);
                if (cataclysmBoss == null) throw new IllegalStateException("Could not create Cataclysm Ender Golem");
                cataclysmBoss.moveTo(inFront.x, inFront.y, inFront.z);
                check(level.addFreshEntity(cataclysmBoss), "Cataclysm boss registers in the server level");
                attackTargetField.set(guardian, cataclysmBoss);
                float bossHealth = cataclysmBoss.getHealth();
                cleave.invoke(guardian, player);
                check(cataclysmBoss.getHealth() < bossHealth, "Cleave damages a Cataclysm boss");
                float guardianHealth = guardian.getHealth();
                guardian.hurt(level.damageSources().mobAttack(cataclysmBoss), 40);
                check(guardian.getHealth() < guardianHealth, "Cataclysm boss damage injures guardian");
                cataclysmBoss.discard();
                guardian.invulnerableTime = 0; // Keep the following Guard scenario independent of this hit.

                magic.setMana(200);
                LivingDamageEvent guardHit = new LivingDamageEvent(player, level.damageSources().generic(), 20);
                new CommonEvents().onPlayerDamage(guardHit);
                check(Math.abs(guardHit.getAmount() - 15) < 0.001, "Guard redirects 25 percent of owner damage");
                check(Math.abs(magic.getMana() - 197.5) < 0.001, "Guard charges half a mana per redirected damage");
                float healthAfterGuard = guardian.getHealth();
                magic.setMana(2);
                LivingDamageEvent unaffordableHit = new LivingDamageEvent(player, level.damageSources().generic(), 20);
                new CommonEvents().onPlayerDamage(unaffordableHit);
                check(Math.abs(unaffordableHit.getAmount() - 20) < 0.001
                                && Math.abs(magic.getMana() - 2) < 0.001
                                && guardian.getHealth() == healthAfterGuard,
                        "Guard leaves damage and mana unchanged when its cost cannot be paid");
                Vec3 ownerPosition = player.position();
                player.moveTo(guardian.getX() + 20, guardian.getY(), guardian.getZ());
                magic.setMana(200);
                LivingDamageEvent distantHit = new LivingDamageEvent(player, level.damageSources().generic(), 20);
                new CommonEvents().onPlayerDamage(distantHit);
                check(Math.abs(distantHit.getAmount() - 20) < 0.001
                                && Math.abs(magic.getMana() - 200) < 0.001
                                && guardian.getHealth() == healthAfterGuard,
                        "Guard does not redirect damage outside its radius");
                player.moveTo(ownerPosition.x, ownerPosition.y, ownerPosition.z);

                Method beginAttack = CrimsonEntity.class.getDeclaredMethod("beginAttack", int.class, int.class);
                Method executeAttack = CrimsonEntity.class.getDeclaredMethod("executeAttack", net.minecraft.server.level.ServerPlayer.class);
                beginAttack.setAccessible(true);
                executeAttack.setAccessible(true);
                Zombie slashPrimary = EntityType.ZOMBIE.create(level);
                Zombie slashNearby = EntityType.ZOMBIE.create(level);
                if (slashPrimary == null || slashNearby == null) throw new IllegalStateException("Could not create Slash targets");
                slashPrimary.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                slashNearby.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                slashPrimary.getAttribute(Attributes.ARMOR).setBaseValue(0);
                slashNearby.getAttribute(Attributes.ARMOR).setBaseValue(0);
                slashPrimary.setHealth(100);
                slashNearby.setHealth(100);
                slashPrimary.moveTo(inFront.x, inFront.y, inFront.z);
                slashNearby.moveTo(inFront.x + 2, inFront.y, inFront.z);
                level.addFreshEntity(slashPrimary);
                level.addFreshEntity(slashNearby);
                attackTargetField.set(guardian, slashPrimary);
                magic.setMana(200);
                beginAttack.invoke(guardian, 3, 20);
                executeAttack.invoke(guardian, player);
                float primaryLoss = 100 - slashPrimary.getHealth();
                float nearbyLoss = 100 - slashNearby.getHealth();
                check(Math.abs(magic.getMana() - 170) < 0.001, "Slash execution charges 30 mana");
                check(primaryLoss > nearbyLoss && nearbyLoss > 0,
                        "Slash direct target takes more damage than nearby hostile");
                Vec3 primaryAway = slashPrimary.position().subtract(guardian.position()).multiply(1, 0, 1).normalize();
                check(slashPrimary.getDeltaMovement().dot(primaryAway) > 1.5,
                        "Slash direct hit supplies strong horizontal knockback away from guardian");
                check(slashNearby.getDeltaMovement().x > 1.5,
                        "Slash area hit retains radial knockback away from impact");
                slashPrimary.setHealth(100);
                slashNearby.setHealth(100);
                slashPrimary.invulnerableTime = 0;
                slashNearby.invulnerableTime = 0;
                magic.setMana(200);
                try {
                    firePower.setBaseValue(oldFirePower + 1);
                    beginAttack.invoke(guardian, 3, 20);
                    executeAttack.invoke(guardian, player);
                } finally {
                    firePower.setBaseValue(oldFirePower);
                }
                float boostedPrimaryLoss = 100 - slashPrimary.getHealth();
                float boostedNearbyLoss = 100 - slashNearby.getHealth();
                CrimsonSusanoo.LOGGER.info("Diagnostic Slash damage: primary {} -> {}, nearby {} -> {}, power {} -> {}",
                        primaryLoss, boostedPrimaryLoss, nearbyLoss, boostedNearbyLoss, basePower, boostedPower);
                check(primaryLoss > 0 && nearbyLoss > 0
                                && Math.abs(boostedPrimaryLoss - primaryLoss * boostedPower / basePower) < 0.2
                                && Math.abs(boostedNearbyLoss - nearbyLoss * boostedPower / basePower) < 0.2,
                        "Slash applies Fire Spell Power once to direct and area damage");
                check(blocksBefore.entrySet().stream().allMatch(entry -> level.getBlockState(entry.getKey()).equals(entry.getValue())),
                        "Slash leaves nearby terrain unchanged");
                slashPrimary.discard();
                slashNearby.discard();

                Zombie crescentTarget = EntityType.ZOMBIE.create(level);
                if (crescentTarget == null) throw new IllegalStateException("Could not create Crescent target");
                crescentTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                crescentTarget.getAttribute(Attributes.ARMOR).setBaseValue(0);
                crescentTarget.setHealth(100);
                Vec3 farAhead = null;
                for (double range : new double[] {10.0, 8.0, 7.0}) {
                    for (int heading = 0; heading < 8; heading++) {
                        double angle = heading * Math.PI / 4;
                        Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                        Vec3 candidate = guardian.position().add(direction.scale(range)).add(0, 1.0, 0);
                        crescentTarget.moveTo(candidate.x, candidate.y, candidate.z);
                        Vec3 waveStart = guardian.position().add(direction.scale(2)).add(0, 1.8, 0);
                        Vec3 waveEnd = guardian.position().add(direction.scale(range + 1)).add(0, 1.8, 0);
                        if (!level.noCollision(crescentTarget, crescentTarget.getBoundingBox())) continue;
                        if (level.clip(new ClipContext(waveStart, waveEnd, ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE, guardian)).getType() != HitResult.Type.MISS) continue;
                        if (level.clip(new ClipContext(waveStart, crescentTarget.getBoundingBox().getCenter(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, guardian)).getType()
                                != HitResult.Type.MISS) continue;
                        farAhead = candidate;
                        break;
                    }
                    if (farAhead != null) break;
                }
                if (farAhead == null) throw new IllegalStateException("No open lane for Crescent diagnostic");
                crescentTarget.moveTo(farAhead.x, farAhead.y, farAhead.z);
                level.addFreshEntity(crescentTarget);
                attackTargetField.set(guardian, crescentTarget);
                magic.setMana(200);
                beginAttack.invoke(guardian, 2, 14);
                executeAttack.invoke(guardian, player);
                check(Math.abs(magic.getMana() - 180) < 0.001, "Crescent execution charges 20 mana");
                var waves = level.getEntitiesOfClass(CrimsonWave.class, guardian.getBoundingBox().inflate(12));
                check(waves.size() == 1, "Crescent spawns one traveling wave");
                if (waves.size() == 1) {
                    CrimsonWave launched = waves.get(0);
                    Vec3 expectedTravel = new Vec3(crescentTarget.getX() - guardian.getX(), 0,
                            crescentTarget.getZ() - guardian.getZ()).normalize();
                    check(launched.getVisualDirection().distanceToSqr(expectedTravel) < 0.001,
                            "Crescent exposes its travel direction through synchronized entity data");
                    check(launched.getAddEntityPacket() != null,
                            "Crescent provides a Forge client spawn packet");
                    Method advance = CrimsonWave.class.getDeclaredMethod("advance", ServerLevel.class,
                            net.minecraft.server.level.ServerPlayer.class, CrimsonEntity.class);
                    advance.setAccessible(true);
                    for (int step = 0; step < 12 && !launched.isRemoved(); step++) {
                        advance.invoke(launched, level, player, guardian);
                    }
                    CrimsonSusanoo.LOGGER.info("Diagnostic Crescent target={} wave={} health={} brand={}",
                            crescentTarget.position(), launched.position(), crescentTarget.getHealth(),
                            crescentTarget.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()));
                    check(crescentTarget.getHealth() < 100, "traveling Crescent damages a hostile target");
                    check(crescentTarget.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()) != null,
                            "traveling Crescent applies Cataclysm Blazing Brand on impact");
                    float baseCrescentLoss = 100 - crescentTarget.getHealth();
                    launched.discard();
                    crescentTarget.setHealth(100);
                    crescentTarget.invulnerableTime = 0;
                    crescentTarget.removeEffect(ModEffect.EFFECTBLAZING_BRAND.get());
                    magic.setMana(200);
                    try {
                        firePower.setBaseValue(oldFirePower + 1);
                        beginAttack.invoke(guardian, 2, 14);
                        executeAttack.invoke(guardian, player);
                    } finally {
                        firePower.setBaseValue(oldFirePower);
                    }
                    var boostedWaves = level.getEntitiesOfClass(CrimsonWave.class,
                            guardian.getBoundingBox().inflate(12));
                    if (boostedWaves.size() != 1) throw new IllegalStateException("Crescent boost wave did not spawn once");
                    CrimsonWave boostedWave = boostedWaves.get(0);
                    for (int step = 0; step < 12 && !boostedWave.isRemoved(); step++) {
                        advance.invoke(boostedWave, level, player, guardian);
                    }
                    float boostedCrescentLoss = 100 - crescentTarget.getHealth();
                    check(baseCrescentLoss > 0
                                    && Math.abs(boostedCrescentLoss - baseCrescentLoss * boostedPower / basePower) < 0.2,
                            "traveling Crescent applies Fire Spell Power once to actual damage");
                    boostedWave.discard();
                }
                check(blocksBefore.entrySet().stream().allMatch(entry -> level.getBlockState(entry.getKey()).equals(entry.getValue())),
                        "Crescent leaves nearby terrain unchanged during flight");
                waves.forEach(CrimsonWave::discard);
                magic.setMana(200);
                attackTargetField.set(guardian, null);
                beginAttack.invoke(guardian, 2, 14);
                executeAttack.invoke(guardian, player);
                check(Math.abs(magic.getMana() - 200) < 0.001,
                        "Crescent does not charge mana if its target vanishes during windup");
                attackTargetField.set(guardian, crescentTarget);
                beginAttack.invoke(guardian, 3, 20);
                executeAttack.invoke(guardian, player);
                check(Math.abs(magic.getMana() - 200) < 0.001,
                        "Slash does not charge mana if its target moves out of range");
                crescentTarget.discard();

                FakePlayer teammate = FakePlayerFactory.get(level,
                        new GameProfile(UUID.randomUUID(), "CrimsonAlly"));
                var scoreboard = level.getScoreboard();
                PlayerTeam team = scoreboard.addPlayerTeam("cr" + UUID.randomUUID().toString().substring(0, 8));
                team.setAllowFriendlyFire(false);
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                scoreboard.addPlayerToTeam(teammate.getScoreboardName(), team);
                try {
                    check(!guardian.canHarm(player, teammate), "guardian refuses to harm a teammate");
                } finally {
                    scoreboard.removePlayerTeam(team);
                }
                Wolf pet = EntityType.WOLF.create(level);
                if (pet == null) throw new IllegalStateException("Could not create diagnostic tame animal");
                pet.setOwnerUUID(player.getUUID());
                pet.setTame(true);
                check(!guardian.canHarm(player, pet), "guardian refuses to harm owner's tame animal");

                guardian.moveTo(player.getX() + 30, player.getY(), player.getZ());
                Method teleportNear = CrimsonEntity.class.getDeclaredMethod("teleportNear",
                        net.minecraft.server.level.ServerPlayer.class, ServerLevel.class);
                teleportNear.setAccessible(true);
                boolean movedSafely = (boolean) teleportNear.invoke(guardian, player, level);
                CrimsonSusanoo.LOGGER.info("Diagnostic teleport moved={} position={} owner={} distance={} collisionFree={}",
                        movedSafely, guardian.position(), player.position(), guardian.distanceTo(player),
                        level.noCollision(guardian, guardian.getBoundingBox()));
                check(movedSafely && guardian.distanceTo(player) < 24
                                && level.noCollision(guardian, guardian.getBoundingBox()),
                        "guardian teleport finds collision-free ground near owner");
                CrimsonSpell.clearOwnerMarker(player);
                guardian.discard();
            }

            magic.setMana(200);
            magic.setSyncedData(new SyncedSpellData(player));
            magic.initiateCast(spell, 1, 80, CastSource.SPELLBOOK, "");
            for (int tick = 0; tick < 20; tick++) magic.handleCastDuration();
            spell.onServerCastTick(level, 1, player, magic);
            CrimsonEntity preview = CrimsonSpell.findActive(player, level);
            check(preview != null && preview.isManifesting(),
                    "Iron's cast tick creates a manifesting guardian preview");
            var ageField = CrimsonEntity.class.getDeclaredField("ageTicks");
            ageField.setAccessible(true);
            if (preview != null) ageField.setInt(preview, 20);
            spell.castSpell(level, 1, player, CastSource.SPELLBOOK, true);
            check(preview != null && CrimsonSpell.findActive(player, level) == preview
                            && !preview.isManifesting(),
                    "completed Iron's cast activates the same guardian");
            check(preview != null && ageField.getInt(preview) == 0,
                    "guardian lifetime starts when manifestation completes");
            check(Math.abs(magic.getMana() - 50) < 0.001,
                    "completed Iron's spellbook cast charges initial mana once");
            check(player.getPersistentData().getBoolean(CrimsonSpell.ACTIVE_READY)
                            && !magic.getPlayerCooldowns().isOnCooldown(spell),
                    "completed cast marks guardian active without starting cooldown");
            spell.onServerCastComplete(level, 1, player, magic, false);
            CrimsonSpell.clearOwnerMarker(player);
            if (preview != null) preview.discard();

            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.set(scroll, ISpellContainer.createScrollContainer(spell, 1, scroll));
            magic.setMana(149);
            check(!spell.attemptInitiateCast(scroll, 1, level, player, CastSource.SCROLL, false, ""),
                    "Iron's scroll initiation rejects less than 150 mana");
            magic.setMana(200);
            check(spell.attemptInitiateCast(scroll, 1, level, player, CastSource.SCROLL, false, "")
                            && magic.isCasting(),
                    "Iron's scroll item initiates the guardian cast");
            spell.castSpell(level, 1, player, CastSource.SCROLL, true);
            CrimsonEntity scrollGuardian = CrimsonSpell.findActive(player, level);
            check(scrollGuardian != null && !scrollGuardian.isManifesting(),
                    "completed Iron's scroll cast summons an active guardian");
            check(Math.abs(magic.getMana() - 50) < 0.001,
                    "completed Iron's scroll cast charges initial mana once");
            check(!magic.getPlayerCooldowns().isOnCooldown(spell),
                    "completed scroll cast leaves cooldown clear while active");
            spell.onServerCastComplete(level, 1, player, magic, false);
            magic.setMana(1);
            check(magic.getPlayerRecasts().hasRecastForSpell(spell)
                            && spell.getEffectiveCastTime(1, player) == 0,
                    "active guardian exposes an instant Iron's recall");
            check(spell.attemptInitiateCast(scroll, 1, level, player, CastSource.SPELLBOOK, false, ""),
                    "Iron's spellbook initiation accepts recall below the summon mana cost");
            spell.castSpell(level, 1, player, CastSource.SPELLBOOK, true);
            check(scrollGuardian != null && scrollGuardian.isDismissing()
                            && CrimsonSpell.findActive(player, level) == null,
                    "recasting dismisses the same guardian rather than spawning another");
            check(Math.abs(magic.getMana() - 1) < 0.001,
                    "recall costs no additional mana");
            check(!magic.getPlayerRecasts().hasRecastForSpell(spell)
                            && magic.getPlayerCooldowns().isOnCooldown(spell),
                    "recall clears the recast overlay and starts the existing cooldown");
            spell.onServerCastComplete(level, 1, player, magic, false);
            magic.setMana(200);
            spell.castSpell(level, 1, player, CastSource.SPELLBOOK, true);
            check(CrimsonSpell.findActive(player, level) == null && Math.abs(magic.getMana() - 200) < 0.001,
                    "late recall completion cannot resummon or spend mana during cooldown");
            CrimsonSpell.clearOwnerMarker(player);
            if (scrollGuardian != null) scrollGuardian.discard();

            player.getPersistentData().putUUID(CrimsonSpell.ACTIVE_ID, UUID.randomUUID());
            player.getPersistentData().putBoolean(CrimsonSpell.ACTIVE_READY, true);
            check(CrimsonSpell.findActive(player, level) == null
                            && !player.getPersistentData().hasUUID(CrimsonSpell.ACTIVE_ID)
                            && !player.getPersistentData().getBoolean(CrimsonSpell.ACTIVE_READY),
                    "stale saved summon marker clears when no guardian exists");
            check(magic.getPlayerCooldowns().isOnCooldown(spell),
                    "fully summoned stale marker starts Iron's cooldown on recovery");
            magic.setMana(200);
            check(!spell.attemptInitiateCast(scroll, 1, level, player, CastSource.SCROLL, false, ""),
                    "Iron's scroll initiation respects the post-dismissal cooldown");

            FakePlayer refundOwner = FakePlayerFactory.get(level,
                    new GameProfile(UUID.randomUUID(), "CrimsonRefund"));
            refundOwner.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(300);
            MagicData refundMagic = MagicData.getPlayerMagicData(refundOwner);
            refundOwner.moveTo(player.getX(), level.getMaxBuildHeight() + 32, player.getZ());
            discountedCastOwner = refundOwner.getUUID();
            try {
                refundMagic.setMana(200);
                spell.castSpell(level, 1, refundOwner, CastSource.SPELLBOOK, true);
                check(Math.abs(refundMagic.getMana() - 200) < 0.001,
                        "failed spellbook placement refunds Iron's discounted cast exactly");
                check(CrimsonSpell.findActive(refundOwner, level) == null,
                        "failed spellbook placement leaves no guardian marker");
                spell.castSpell(level, 1, refundOwner, CastSource.SCROLL, true);
                check(Math.abs(refundMagic.getMana() - 200) < 0.001,
                        "failed scroll placement restores the exact pre-cast mana");
                refundOwner.moveTo(player.getX(), player.getY(), player.getZ());
                spell.castSpell(level, 1, refundOwner, CastSource.SPELLBOOK, true);
                CrimsonEntity discountedGuardian = CrimsonSpell.findActive(refundOwner, level);
                check(discountedGuardian != null && !discountedGuardian.isManifesting(),
                        "discounted Iron's spellbook cast still summons the guardian");
                check(Math.abs(refundMagic.getMana() - 160) < 0.001,
                        "successful discounted cast charges Iron's adjusted 40 mana once");
                CrimsonSpell.clearOwnerMarker(refundOwner);
                if (discountedGuardian != null) discountedGuardian.discard();
            } finally {
                discountedCastOwner = null;
            }

            Zombie target = EntityType.ZOMBIE.create(level);
            CrimsonWave wave = CrimsonSusanoo.WAVE.get().create(level);
            if (target == null || wave == null) throw new IllegalStateException("Could not create diagnostic entities");
            Method applyBrand = CrimsonWave.class.getDeclaredMethod("applyBrand", net.minecraft.world.entity.LivingEntity.class);
            applyBrand.setAccessible(true);
            for (int expected = 0; expected <= 2; expected++) {
                applyBrand.invoke(wave, target);
                var effect = target.getEffect(ModEffect.EFFECTBLAZING_BRAND.get());
                check(effect != null && effect.getAmplifier() == expected, "Cataclysm Blazing Brand level " + (expected + 1));
            }
            applyBrand.invoke(wave, target);
            check(target.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()).getAmplifier() == 2,
                    "Blazing Brand stays capped at III");

            if (safePosition != null) {
                CommonEvents events = new CommonEvents();
                for (String transition : new String[] {"logout", "death", "dimension"}) {
                    FakePlayer transitionOwner = FakePlayerFactory.get(level,
                            new GameProfile(UUID.randomUUID(), "CrimsonExit"));
                    CrimsonEntity departingGuardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                    if (departingGuardian == null) throw new IllegalStateException("Could not create lifecycle guardian");
                    departingGuardian.setOwner(transitionOwner);
                    departingGuardian.moveTo(safePosition.x, safePosition.y, safePosition.z);
                    check(level.addFreshEntity(departingGuardian), transition + " guardian registers");
                    transitionOwner.getPersistentData().putUUID(CrimsonSpell.ACTIVE_ID, departingGuardian.getUUID());
                    transitionOwner.getPersistentData().putBoolean(CrimsonSpell.ACTIVE_READY, true);
                    switch (transition) {
                        case "logout" -> events.onLogout(new PlayerEvent.PlayerLoggedOutEvent(transitionOwner));
                        case "death" -> events.onPlayerDeath(new LivingDeathEvent(transitionOwner,
                                level.damageSources().generic()));
                        case "dimension" -> events.onDimensionChange(new PlayerEvent.PlayerChangedDimensionEvent(
                                transitionOwner, net.minecraft.world.level.Level.OVERWORLD,
                                net.minecraft.world.level.Level.NETHER));
                        default -> throw new IllegalStateException(transition);
                    }
                    check(departingGuardian.isDismissing(), transition + " dismisses the active guardian");
                    check(!transitionOwner.getPersistentData().hasUUID(CrimsonSpell.ACTIVE_ID)
                                    && !transitionOwner.getPersistentData().getBoolean(CrimsonSpell.ACTIVE_READY),
                            transition + " clears the saved owner marker");
                    check(MagicData.getPlayerMagicData(transitionOwner).getPlayerCooldowns().isOnCooldown(spell),
                            transition + " starts Iron's cooldown when the guardian ends");
                    if (transition.equals("logout")) {
                        for (int tick = 0; tick < 29; tick++) departingGuardian.tick();
                        check(!departingGuardian.isRemoved() && departingGuardian.getFade() > 0,
                                "normal dismissal remains visible during its 30-tick fade");
                        departingGuardian.tick();
                        check(departingGuardian.isRemoved(),
                                "normal dismissal leaves no guardian entity after the fade");
                    } else {
                        departingGuardian.discard();
                    }
                }

                FakePlayer previewOwner = FakePlayerFactory.get(level,
                        new GameProfile(UUID.randomUUID(), "CrimsonPreviewExit"));
                CrimsonEntity interruptedPreview = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (interruptedPreview == null) throw new IllegalStateException("Could not create logout preview");
                interruptedPreview.setOwner(previewOwner);
                interruptedPreview.moveTo(safePosition.x, safePosition.y, safePosition.z);
                interruptedPreview.startManifesting();
                check(level.addFreshEntity(interruptedPreview), "logout preview registers");
                previewOwner.getPersistentData().putUUID(CrimsonSpell.ACTIVE_ID, interruptedPreview.getUUID());
                events.onLogout(new PlayerEvent.PlayerLoggedOutEvent(previewOwner));
                check(interruptedPreview.isRemoved(), "interrupted preview disappears on logout");
                check(!previewOwner.getPersistentData().hasUUID(CrimsonSpell.ACTIVE_ID),
                        "preview logout clears the saved owner marker immediately");
                check(!MagicData.getPlayerMagicData(previewOwner).getPlayerCooldowns().isOnCooldown(spell),
                        "unfinished preview logout does not start cooldown");

                FakePlayer lifetimeOwner = FakePlayerFactory.get(level,
                        new GameProfile(UUID.randomUUID(), "CrimsonLifetime"));
                lifetimeOwner.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(300);
                MagicData lifetimeMagic = MagicData.getPlayerMagicData(lifetimeOwner);
                Method advanceLifetime = CrimsonEntity.class.getDeclaredMethod("advanceLifetime",
                        net.minecraft.server.level.ServerPlayer.class);
                advanceLifetime.setAccessible(true);
                CrimsonEntity upkeepGuardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (upkeepGuardian == null) throw new IllegalStateException("Could not create upkeep guardian");
                upkeepGuardian.setOwner(lifetimeOwner);
                upkeepGuardian.moveTo(safePosition.x, safePosition.y, safePosition.z);
                check(level.addFreshEntity(upkeepGuardian), "upkeep guardian registers");
                lifetimeMagic.setMana(200);
                ageField.setInt(upkeepGuardian, 18);
                check((boolean) advanceLifetime.invoke(upkeepGuardian, lifetimeOwner)
                                && Math.abs(lifetimeMagic.getMana() - 200) < 0.001,
                        "upkeep does not charge before the twentieth active tick");
                check((boolean) advanceLifetime.invoke(upkeepGuardian, lifetimeOwner)
                                && Math.abs(lifetimeMagic.getMana() - 188) < 0.001,
                        "twentieth active tick charges exactly 12 mana");
                ageField.setInt(upkeepGuardian, 39);
                lifetimeMagic.setMana(11);
                check(!(boolean) advanceLifetime.invoke(upkeepGuardian, lifetimeOwner)
                                && upkeepGuardian.isDismissing() && upkeepGuardian.isManaCollapsing()
                                && Math.abs(lifetimeMagic.getMana() - 11) < 0.001,
                        "unaffordable upkeep dismisses immediately without negative mana");
                upkeepGuardian.discard();

                CrimsonEntity durationGuardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (durationGuardian == null) throw new IllegalStateException("Could not create duration guardian");
                durationGuardian.setOwner(lifetimeOwner);
                durationGuardian.moveTo(safePosition.x, safePosition.y, safePosition.z);
                check(level.addFreshEntity(durationGuardian), "duration guardian registers");
                lifetimeMagic.setMana(200);
                ageField.setInt(durationGuardian, ServerConfig.DURATION.get() * 20 - 2);
                check((boolean) advanceLifetime.invoke(durationGuardian, lifetimeOwner)
                                && !durationGuardian.isDismissing(),
                        "guardian remains active before the 90-second limit");
                check(!(boolean) advanceLifetime.invoke(durationGuardian, lifetimeOwner)
                                && durationGuardian.isDismissing() && !durationGuardian.isManaCollapsing()
                                && Math.abs(lifetimeMagic.getMana() - 200) < 0.001,
                        "guardian expires at 90 seconds before another upkeep charge");
                durationGuardian.discard();

                FakePlayer fallenOwner = FakePlayerFactory.get(level,
                        new GameProfile(UUID.randomUUID(), "CrimsonFallen"));
                CrimsonEntity fallenGuardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (fallenGuardian == null) throw new IllegalStateException("Could not create death-timing guardian");
                fallenGuardian.setOwner(fallenOwner);
                fallenGuardian.moveTo(safePosition.x, safePosition.y, safePosition.z);
                check(level.addFreshEntity(fallenGuardian), "death-timing guardian registers");
                fallenGuardian.setHealth(1);
                fallenGuardian.hurt(level.damageSources().generic(), 1000);
                check(fallenGuardian.isDeadOrDying(), "guardian enters death animation after lethal damage");
                for (int tick = 0; tick < 20; tick++) fallenGuardian.tick();
                check(!fallenGuardian.isRemoved(), "guardian remains for the full death animation after vanilla's 20 ticks");
                for (int tick = 20; tick < 32; tick++) fallenGuardian.tick();
                check(fallenGuardian.isRemoved(), "guardian removes itself after the 1.5-second death animation");
            }
        } catch (Throwable error) {
            failed++;
            CrimsonSusanoo.LOGGER.error("Crimson Susanoo diagnostics aborted", error);
        }
        check(attributedHits > 0 && correctDamageAttribution,
                "actual spell damage events attribute owner as cause and guardian or wave as direct attacker");
        diagnosticCombatOwner = null;
        CrimsonSusanoo.LOGGER.info("Crimson Susanoo diagnostics: {} passed, {} failed", passed, failed);
    }

    private void verifyDamageBalance(ServerLevel level, FakePlayer owner) throws Exception {
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        Method damage = CrimsonEntity.class.getDeclaredMethod("scaledDamage", net.minecraft.server.level.ServerPlayer.class, double.class);
        Method tooltip = CrimsonSpell.class.getDeclaredMethod("formatDamage", double.class, float.class);
        damage.setAccessible(true);
        tooltip.setAccessible(true);
        double previous = ServerConfig.DAMAGE_MULTIPLIER.get();
        float power = CrimsonSusanoo.SPELL.get().getSpellPower(1, owner);
        try {
            for (double base : new double[]{ServerConfig.CLEAVE_DAMAGE.get(), ServerConfig.CRESCENT_DAMAGE.get(),
                    ServerConfig.SLASH_DAMAGE.get(), ServerConfig.SLASH_DAMAGE.get() * .5}) {
                ServerConfig.DAMAGE_MULTIPLIER.set(1.0);
                float original = (float) damage.invoke(guardian, owner, base);
                ServerConfig.DAMAGE_MULTIPLIER.set(.5);
                float reduced = (float) damage.invoke(guardian, owner, base);
                check(Math.abs(reduced - original * .5) < .00001,
                        "damage multiplier halves configured attack base " + base + " after spell power");
                String expected = java.math.BigDecimal.valueOf(reduced).setScale(1, java.math.RoundingMode.HALF_UP)
                        .stripTrailingZeros().toPlainString();
                check(expected.equals(tooltip.invoke(null, base, power)),
                        "scroll damage matches gameplay for attack base " + base);
            }
        } finally {
            ServerConfig.DAMAGE_MULTIPLIER.set(previous);
        }
    }

    private void verifyManaRegeneration(ServerLevel level, FakePlayer owner) throws Exception {
        // Exercise Iron's installed regeneration function and the real upkeep
        // helper at their normal cadence. Both update orders must retain regen.
        // This headless fixture does not replace a connected-player tick test.
        MagicData magic = MagicData.getPlayerMagicData(owner);
        var regeneration = owner.getAttribute(AttributeRegistry.MANA_REGEN.get());
        if (regeneration == null) throw new IllegalStateException("Missing mana regeneration attribute");
        double originalRegeneration = regeneration.getValue();
        double originalMultiplier = io.redspace.ironsspellbooks.config.ServerConfigs.MANA_REGEN_MULTIPLIER.get();
        float originalMana = magic.getMana();
        Method advanceLifetime = CrimsonEntity.class.getDeclaredMethod("advanceLifetime",
                net.minecraft.server.level.ServerPlayer.class);
        advanceLifetime.setAccessible(true);
        try {
            magic.setMana(200);
            boolean regenerated = io.redspace.ironsspellbooks.IronsSpellbooks.MAGIC_MANAGER
                    .regenPlayerMana(owner, magic);
            float perPulse = magic.getMana() - 200;
            check(regenerated && perPulse > 0,
                    "Iron's normal mana regeneration increases mana before upkeep");
            for (boolean regenFirst : new boolean[]{true, false}) {
                CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (guardian == null) throw new IllegalStateException("Missing regeneration guardian");
                guardian.setOwner(owner);
                magic.setMana(200);
                boolean remainedActive = true;
                try {
                    for (int tick = 1; tick <= 40; tick++) {
                        boolean regenTick = tick % MagicManager.MANA_REGEN_TICKS == 0;
                        if (regenFirst && regenTick) {
                            io.redspace.ironsspellbooks.IronsSpellbooks.MAGIC_MANAGER.regenPlayerMana(owner, magic);
                        }
                        remainedActive &= (boolean) advanceLifetime.invoke(guardian, owner);
                        if (!regenFirst && regenTick) {
                            io.redspace.ironsspellbooks.IronsSpellbooks.MAGIC_MANAGER.regenPlayerMana(owner, magic);
                        }
                    }
                    float expected = 200 + (40 / MagicManager.MANA_REGEN_TICKS) * perPulse
                            - 2 * ServerConfig.UPKEEP.get().floatValue();
                    check(remainedActive && !guardian.isDismissing()
                                    && Math.abs(magic.getMana() - expected) < 0.001,
                            "natural regeneration and two upkeep payments coexist, regenFirst=" + regenFirst);
                    CrimsonSusanoo.LOGGER.info("Mana regeneration diagnostic regenFirst={} pulse={} expected={} actual={}",
                            regenFirst, perPulse, expected, magic.getMana());
                } finally {
                    guardian.discard();
                }
            }
            check(regeneration.getValue() == originalRegeneration
                            && io.redspace.ironsspellbooks.config.ServerConfigs.MANA_REGEN_MULTIPLIER.get() == originalMultiplier,
                    "guardian upkeep preserves Iron's regeneration attribute and multiplier");
        } finally {
            magic.setMana(originalMana);
        }
    }
    private void verifyNavigationPriority(ServerLevel level, FakePlayer owner) throws Exception {
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (guardian == null || target == null) throw new IllegalStateException("Missing navigation fixtures");
        Vec3 originalOwnerPosition = owner.position();
        float originalNavigationMana = MagicData.getPlayerMagicData(owner).getMana();
        Vec3 origin = Vec3.atBottomCenterOf(level.getSharedSpawnPos()).add(0, 100, 0);
        guardian.setOwner(owner);
        guardian.setPos(origin);
        target.setPos(origin.add(10, 0, 0));
        var targetField = CrimsonEntity.class.getDeclaredField("attackTarget");
        targetField.setAccessible(true);
        var windupField = CrimsonEntity.class.getDeclaredField("actionTicks");
        windupField.setAccessible(true);
        var recoveryField = CrimsonEntity.class.getDeclaredField("recoveryTicks");
        recoveryField.setAccessible(true);
        Method reposition = CrimsonEntity.class.getDeclaredMethod("reposition", net.minecraft.server.level.ServerPlayer.class, ServerLevel.class);
        reposition.setAccessible(true);
        Method begin = CrimsonEntity.class.getDeclaredMethod("beginAttack", int.class, int.class);
        begin.setAccessible(true);
        BlockPos destination = BlockPos.containing(target.position());
        try {
            owner.setPos(origin.add(4, 0, 0));
            check(!guardian.isPushable() && guardian.canBeCollidedWith(),
                    "active guardian blocks entity collision without being pushable");
            guardian.setDeltaMovement(Vec3.ZERO);
            guardian.push(.5, 0, .5);
            guardian.push(target);
            check(guardian.getDeltaMovement().equals(Vec3.ZERO),
                    "entity and velocity pushes cannot displace guardian");
            Method inRange = CrimsonEntity.class.getDeclaredMethod("inPursuitRange",
                    net.minecraft.server.level.ServerPlayer.class, LivingEntity.class);
            inRange.setAccessible(true);
            target.setPos(origin.add(30, 0, 0));
            check(!(boolean) inRange.invoke(guardian, owner, target), "guard pursuit stays near owner");
            targetField.set(guardian, target);
            check((boolean) inRange.invoke(guardian, owner, target),
                    "guard retains acquired target across its acquisition boundary");
            targetField.set(guardian, null);
            guardian.setHunting(true);
            check(guardian.isHunting() && (boolean) inRange.invoke(guardian, owner, target),
                    "hunt mode extends pursuit to a hostile 26 blocks from owner");
            target.setPos(origin.add(40, 0, 0));
            check(!(boolean) inRange.invoke(guardian, owner, target), "hunt mode retains a bounded leash");
            guardian.setHunting(false);
            target.setPos(origin.add(10, 0, 0));
            targetField.set(guardian, target);
            var pursuit = diagnosticPath(destination);
            if (!guardian.getNavigation().moveTo(pursuit, 1)) throw new IllegalStateException("Navigation rejected fixture path");
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().getPath() == pursuit && !guardian.getNavigation().isDone(),
                    "nearby owner reposition preserves an active combat pursuit path");
            owner.setPos(origin.add(17, 0, 0));
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().getPath() == pursuit,
                    "guard mode pursuit is not overwritten by owner follow outside follow radius");
            owner.setPos(origin.add(30, 0, 0));
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().getPath() == pursuit && guardian.position().equals(origin),
                    "active pursuit survives the idle teleport boundary without owner-path arbitration");
            owner.setPos(origin.add(4, 0, 0));
            guardian.setYRot(0);
            guardian.getMoveControl().setWantedPosition(origin.x, origin.y, origin.z - 10, 1.45);
            guardian.getMoveControl().tick();
            check(Math.abs(net.minecraft.util.Mth.wrapDegrees(guardian.getYRot())) <= 18.001
                            && guardian.getSpeed() > 0,
                    "walking control bounds a rear turn while retaining forward movement");
            Zombie distraction = EntityType.ZOMBIE.create(level);
            if (distraction == null) throw new IllegalStateException("Missing distraction fixture");
            distraction.setPos(origin.add(3, 0, 0));
            target.setTarget(owner);
            owner.setLastHurtMob(distraction);
            check(level.addFreshEntity(target) && level.addFreshEntity(distraction),
                    "defense priority fixtures register");
            try {
                Method select = CrimsonEntity.class.getDeclaredMethod("selectTarget", net.minecraft.server.level.ServerPlayer.class);
                select.setAccessible(true);
                select.invoke(guardian, owner);
                check(targetField.get(guardian) == target,
                        "enemy targeting owner outranks owner's older offensive target");
                Method attack = CrimsonEntity.class.getDeclaredMethod("selectAttack", net.minecraft.server.level.ServerPlayer.class);
                attack.setAccessible(true);
                MagicData.getPlayerMagicData(owner).setMana(0);
                attack.invoke(guardian, owner);
                check(guardian.getAction() == 0,
                        "owner-bound enemy triggers pursuit when Crescent mana is unavailable");
                MagicData.getPlayerMagicData(owner).setMana(200);
                attack.invoke(guardian, owner);
                check(guardian.getAction() == 2,
                        "clear owner-bound threat receives an available ranged response before a long chase");
                target.setPos(origin.add(5.5, 0, 0));
                MagicData.getPlayerMagicData(owner).setMana(0);
                guardian.getNavigation().stop();
                attack.invoke(guardian, owner);
                check(guardian.getAction() == 1,
                        "owner defense starts ready Cleave at 5.5 blocks even with finished navigation and no ranged mana");
                target.setPos(origin.add(10, 0, 0));
                MagicData.getPlayerMagicData(owner).setMana(200);
                BlockPos laneBlock = BlockPos.containing(origin.add(5,1.8,0));
                var oldLaneBlock = level.getBlockState(laneBlock);
                try {
                    for (int scenario=0; scenario<3; scenario++) {
                        CrimsonEntity candidate = CrimsonSusanoo.GUARDIAN.get().create(level);
                        if (candidate == null) throw new IllegalStateException("Missing lane-selection fixture");
                        candidate.setOwner(owner);
                        candidate.setPos(origin);
                        targetField.set(candidate,target);
                        target.setTarget(scenario==0 ? owner : null);
                        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scenario==1 ? 200 : 20);
                        level.setBlockAndUpdate(laneBlock,Blocks.STONE.defaultBlockState());
                        attack.invoke(candidate,owner);
                        check(candidate.getAction()==0,"blocked Crescent lane preserves pursuit: scenario="+scenario);
                        level.setBlockAndUpdate(laneBlock,Blocks.AIR.defaultBlockState());
                        attack.invoke(candidate,owner);
                        check(candidate.getAction()==2,"open Crescent lane allows ranged response: scenario="+scenario);
                    }
                } finally {
                    level.setBlockAndUpdate(laneBlock,oldLaneBlock);
                    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
                    target.setTarget(owner);
                }
                Method step = CrimsonEntity.class.getDeclaredMethod("advanceAttackStep", net.minecraft.server.level.ServerPlayer.class, ServerLevel.class);
                step.setAccessible(true);
                guardian.setYRot(-90);
                guardian.setYBodyRot(-90);
                guardian.setYHeadRot(-90);
                begin.invoke(guardian, 1, 11);
                windupField.setInt(guardian, 7);
                step.invoke(guardian, owner, level);
                check(guardian.position().equals(origin), "attack step refuses unsupported air above ground");
                BlockPos floor = BlockPos.containing(origin).below();
                BlockPos obstruction = floor.above(4);
                var oldFloor = level.getBlockState(floor);
                var oldObstruction = level.getBlockState(obstruction);
                try {
                    level.setBlockAndUpdate(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    step.invoke(guardian, owner, level);
                    check(guardian.getX() > origin.x + .1 && guardian.getX() < origin.x + .13,
                            "melee windup advances one bounded step on supported clear ground");
                    double[] strides = new double[6];
                    for (int i = 0; i < strides.length; i++) {
                        guardian.setPos(origin);
                        windupField.setInt(guardian, 8 - i);
                        step.invoke(guardian, owner, level);
                        strides[i] = guardian.getX() - origin.x;
                    }
                    check(strides[0] < strides[1] && strides[1] < strides[2]
                                    && strides[3] > strides[4] && strides[4] > strides[5]
                                    && Math.abs(java.util.Arrays.stream(strides).sum() - .72) < .00001,
                            "attack stride accelerates and brakes while retaining its total travel budget");
                    guardian.setPos(origin);
                    windupField.setInt(guardian, 7);
                    level.setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    step.invoke(guardian, owner, level);
                    check(guardian.position().equals(origin), "attack step refuses upper-body wall collision");
                    level.setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                    step.invoke(guardian, owner, level);
                    check(guardian.position().equals(origin), "attack step refuses liquid above otherwise safe feet");
                } finally {
                    level.setBlockAndUpdate(obstruction, oldObstruction);
                    level.setBlockAndUpdate(floor, oldFloor);
                    guardian.setPos(origin);
                }
                windupField.setInt(guardian, 0);
                // Reset the synchronized action before the independent navigation assertions.
                guardian.setHunting(false);
            } finally {
                owner.setLastHurtMob(null);
                target.setTarget(null);
                distraction.discard();
            }
            targetField.set(guardian, null);
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().isDone(), "idle guardian stops navigation within owner follow radius");
            targetField.set(guardian, target);
            owner.setPos(origin.add(30, 0, 0));
            begin.invoke(guardian, 3, 20);
            float committedYaw = guardian.getYRot();
            guardian.getMoveControl().setWantedPosition(origin.x - 10, origin.y, origin.z, 1.45);
            guardian.getMoveControl().tick();
            check(guardian.getYRot() == committedYaw && guardian.getSpeed() == 0
                            && !guardian.getMoveControl().hasWanted(),
                    "stale walking waypoint cannot steer a planted attack");
            guardian.getNavigation().moveTo(diagnosticPath(destination), 1);
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().isDone() && guardian.position().equals(origin),
                    "owner reposition neither walks nor teleports guardian during attack windup");
            windupField.setInt(guardian, 0);
            recoveryField.setInt(guardian, 6);
            guardian.getNavigation().moveTo(diagnosticPath(destination), 1);
            reposition.invoke(guardian, owner, level);
            check(guardian.getNavigation().isDone() && guardian.position().equals(origin),
                    "owner reposition keeps guardian planted during attack recovery");
        } finally {
            owner.setPos(originalOwnerPosition);
            MagicData.getPlayerMagicData(owner).setMana(originalNavigationMana);
            guardian.discard();
            target.discard();
        }
    }

    private void verifyPursuitCorridors(ServerLevel level) {
        BlockPos base = level.getSharedSpawnPos().above(140);
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> saved = new HashMap<>();
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (guardian == null) throw new IllegalStateException("Missing corridor fixture");
        try {
            for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
                BlockPos p = base.offset(x, -1, z);
                saved.put(p, level.getBlockState(p));
                level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
            }
            guardian.setPos(Vec3.atBottomCenterOf(base));
            guardian.setOnGround(true);
            var navigation = (com.crimson_susanoo.entity.CrimsonNavigation) guardian.getNavigation();
            Vec3 destination = guardian.position().add(5, 0, 0);
            check(navigation.canTraverseCorridor(destination), "wide guardian can shortcut clear supported corridor");
            for (int obstacle = 0; obstacle < 3; obstacle++) {
                // Upper-body obstruction/liquid catches checks that only inspect feet.
                BlockPos p = base.offset(3, obstacle == 2 ? -1 : 5, 0);
                saved.putIfAbsent(p, level.getBlockState(p));
                var before = level.getBlockState(p);
                level.setBlock(p, (obstacle == 0 ? Blocks.STONE : obstacle == 1 ? Blocks.WATER : Blocks.AIR).defaultBlockState(), 3);
                check(!navigation.canTraverseCorridor(destination),
                        "shortcut rejects " + (obstacle == 0 ? "upper-body wall" : obstacle == 1 ? "upper-body liquid" : "floor gap"));
                level.setBlock(p, before, 3);
            }
            for (int direction = 0; direction < 4; direction++) {
                navigation.stop();
                guardian.setDeltaMovement(Vec3.ZERO);
                guardian.setPos(base.getX() + .13, base.getY(), base.getZ() + .79);
                guardian.setOnGround(true);
                Vec3 start = guardian.position();
                double angle = direction * Math.PI / 2 + Math.PI / 4;
                Vec3 forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                guardian.setYRot((float) Math.toDegrees(Math.atan2(-forward.x, forward.z)));
                double turning = 0;
                for (int tick = 0; tick < 80; tick++) {
                    if (tick % 5 == 0) {
                        Vec3 target = start.add(forward.scale(18)).add(Math.sin(tick * .1), 0, 0);
                        navigation.moveTo(target.x, target.y, target.z, 1.3);
                    }
                    float yaw = guardian.getYRot();
                    guardian.tickCount++;
                    guardian.aiStep();
                    turning += Math.abs(net.minecraft.util.Mth.wrapDegrees(guardian.getYRot() - yaw));
                }
                double progress = guardian.position().subtract(start).dot(forward);
                CrimsonSusanoo.LOGGER.info("Repeated-repath direction={} progress={} totalTurning={}", direction, progress, turning);
                check(progress > 8, "repeated repath makes targetward progress in direction " + direction);
                check(turning < 270, "repeated repath avoids full circling in direction " + direction);
            }
            // The recorded retreat exposed finished paths at 6-7 blocks, outside
            // melee reach. Exercise actual pathfinding in every approach direction.
            for (int direction = 0; direction < 8; direction++) {
                navigation.stop();
                guardian.setDeltaMovement(Vec3.ZERO);
                guardian.setPos(base.getX() + .13, base.getY(), base.getZ() + .79);
                guardian.setOnGround(true);
                double angle = direction * Math.PI / 4;
                Vec3 forward = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                Vec3 target = guardian.position().add(forward.scale(12));
                guardian.setYRot((float) Math.toDegrees(Math.atan2(-forward.x, forward.z)));
                int reached = -1;
                for (int tick = 0; tick < 100; tick++) {
                    if (tick % 5 == 0) navigation.moveTo(target.x, target.y, target.z, 1.3);
                    guardian.tickCount++;
                    guardian.aiStep();
                    if (guardian.position().distanceTo(target) <= 6) { reached = tick; break; }
                }
                CrimsonSusanoo.LOGGER.info("Melee-approach direction={} reachedTick={} remaining={}",
                        direction, reached, guardian.position().distanceTo(target));
                check(reached >= 0 && reached < 70,
                        "clear approach reaches six-block melee range without an endpoint stall in direction " + direction);
            }
        } finally {
            guardian.discard();
            saved.forEach((p, state) -> level.setBlock(p, state, 3));
        }
    }

    /** Real vanilla pathfinding, jump control and collision physics; no client or teleport assistance. */
    private void verifyUnevenTerrainNavigation(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = new BlockPos(spawn.getX(), Math.min(spawn.getY() + 150,
                level.getMaxBuildHeight() - 14), spawn.getZ());
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> saved = new HashMap<>();
        CrimsonEntity guardian = null;
        String[] labels = {"stairs uphill", "stairs downhill", "half-slab ridge", "tall wall detour",
                "pit detour", "two-block ledge detour", "full-block uphill"};
        try {
            // Read everything before editing; never replace an existing block entity.
            for (int x = -8; x <= 36; x++) for (int z = -16; z <= 16; z++) for (int y = -1; y <= 11; y++) {
                BlockPos p = base.offset(x, y, z);
                if (level.getBlockEntity(p) != null) throw new IllegalStateException("Terrain fixture overlaps a block entity: " + p);
                saved.put(p, level.getBlockState(p));
            }
            for (int scenario = 0; scenario < labels.length; scenario++) {
                for (var p : saved.keySet()) level.setBlock(p,
                        (p.getY() == base.getY() - 1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
                if (scenario <= 1) {
                    for (int x = 8; x <= 36; x++) for (int z = -16; z <= 16; z++) {
                        int height = x >= 16 ? 2 : x >= 9 ? 1 : 0;
                        for (int y = 0; y < height; y++) level.setBlock(base.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                        if (x == 8 || x == 15) level.setBlock(base.offset(x, x == 8 ? 0 : 1, z),
                                Blocks.STONE_STAIRS.defaultBlockState().setValue(
                                        net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                                        net.minecraft.core.Direction.EAST), 2);
                    }
                } else if (scenario == 2) {
                    for (int x = 8; x <= 19; x++) for (int z = -16; z <= 16; z++)
                        level.setBlock(base.offset(x, 0, z), Blocks.STONE_SLAB.defaultBlockState(), 2);
                } else if (scenario == 6) {
                    for (int x = 8; x <= 36; x++) for (int z = -16; z <= 16; z++)
                        level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
                } else if (scenario == 4) {
                    for (int x = 10; x <= 15; x++) for (int z = -6; z <= 6; z++)
                        level.setBlock(base.offset(x, -1, z), Blocks.AIR.defaultBlockState(), 2);
                } else {
                    int height = scenario == 3 ? 9 : 2;
                    int width = scenario == 3 ? 1 : 4;
                    for (int x = 12; x < 12 + width; x++) for (int z = -5; z <= 5; z++) for (int y = 0; y < height; y++)
                        level.setBlock(base.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
                guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                if (guardian == null) throw new IllegalStateException("Missing terrain navigation fixture");
                Vec3 start = Vec3.atBottomCenterOf(base).add(scenario == 1 ? 26 : 0, scenario == 1 ? 2 : 0, 0);
                Vec3 target = Vec3.atBottomCenterOf(base).add(scenario == 1 ? 0 : 26, scenario == 0 ? 2 : scenario == 6 ? 1 : 0, 0);
                guardian.setPos(start);
                guardian.setOnGround(true);
                guardian.setYRot(scenario == 1 ? 90 : -90);
                var navigation = (com.crimson_susanoo.entity.CrimsonNavigation) guardian.getNavigation();
                int arrived = -1, collisions = 0, airborne = 0, unsupported = 0, risingAirborne = 0;
                double minY = start.y, maxY = start.y, lateral = 0;
                for (int tick = 0; tick < 260; tick++) {
                    if (tick % 10 == 0) navigation.moveTo(target.x, target.y, target.z, 1.3);
                    guardian.tickCount++;
                    guardian.aiStep();
                    minY = Math.min(minY, guardian.getY());
                    maxY = Math.max(maxY, guardian.getY());
                    lateral = Math.max(lateral, Math.abs(guardian.getZ() - start.z));
                    if (!guardian.onGround()) {
                        airborne++;
                        if (guardian.getDeltaMovement().y > .05) risingAirborne++;
                    }
                    if (guardian.onGround()) {
                        BlockPos floor = BlockPos.containing(guardian.getX(), guardian.getY() - .05, guardian.getZ());
                        if (level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()) unsupported++;
                    }
                    if (level.getBlockCollisions(guardian, guardian.getBoundingBox().deflate(1.0E-4)).iterator().hasNext()) collisions++;
                    if (guardian.getY() < base.getY() - 2) break;
                    if (guardian.position().distanceTo(target) <= 6 && Math.abs(guardian.getY() - target.y) < .05
                            && guardian.onGround()) { arrived = tick; break; }
                }
                CrimsonSusanoo.LOGGER.info("Terrain-navigation scenario={} reachedTick={} pos={} minY={} maxY={} lateral={} airborneTicks={} risingAirborneTicks={} collisionSamples={} unsupportedCenterSamples={}",
                        labels[scenario], arrived, guardian.position(), minY - base.getY(), maxY - base.getY(), lateral, airborne, risingAirborne, collisions, unsupported);
                check(arrived >= 0, labels[scenario] + " reaches melee range using actual navigation and movement");
                check(collisions == 0 && minY >= base.getY() - .001,
                        labels[scenario] + " never intersects solid blocks or falls through the floor");
                if (scenario == 4) check(unsupported == 0, "pit route keeps the grounded center over actual floor support");
                if (scenario == 2) {
                    check(maxY >= base.getY() + .49, "slab ridge is crossed over its raised collision surface");
                    check(maxY <= base.getY() + .50001,
                            "half slabs are stepped onto without jumping above their collision surface");
                }
                if (scenario <= 2) check(risingAirborne == 0,
                        labels[scenario] + " steps over small risers without an upward jump impulse");
                if (scenario >= 3 && scenario <= 5) check(lateral >= 7, labels[scenario] + " uses the available route around the obstacle");
                if (scenario == 6) check(risingAirborne > 0,
                        "a full-block rise retains the ordinary jump instead of stalling");
                guardian.discard();
                guardian = null;
            }
        } finally {
            if (guardian != null) guardian.discard();
            saved.forEach((p, state) -> level.setBlock(p, state, 2));
            check(saved.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()).equals(e.getValue())),
                    "uneven-terrain navigation restores every fixture block");
        }
    }

    private void verifyMovingInterception(ServerLevel level, FakePlayer owner) throws Exception {
        // Run real AI/movement steps in a restored temporary arena, without a client.
        // The production combat routine receives the fake owner explicitly because
        // FakePlayer is deliberately absent from the connected-player lookup.
        Vec3 original = owner.position();
        BlockPos base = level.getSharedSpawnPos().above(105);
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> saved = new HashMap<>();
        Method combat = CrimsonEntity.class.getDeclaredMethod("tickCombat", net.minecraft.server.level.ServerPlayer.class, ServerLevel.class);
        combat.setAccessible(true);
        Method advanceWave = CrimsonWave.class.getDeclaredMethod("advance", ServerLevel.class,
                net.minecraft.server.level.ServerPlayer.class, CrimsonEntity.class);
        advanceWave.setAccessible(true);
        float originalInterceptionMana = MagicData.getPlayerMagicData(owner).getMana();
        var age = CrimsonEntity.class.getDeclaredField("ageTicks");
        age.setAccessible(true);
        var selected = CrimsonEntity.class.getDeclaredField("attackTarget");
        selected.setAccessible(true);
        boolean invulnerable = owner.getAbilities().invulnerable;
        float ownerHealth = owner.getHealth();
        try {
            // An invulnerable/creative owner makes vanilla melee goals abandon approach.
            owner.getAbilities().invulnerable = false;
            for (int x = -5; x <= 35; x++) for (int z = -9; z <= 9; z++) {
                BlockPos p = base.offset(x, -1, z);
                saved.put(p, level.getBlockState(p));
                level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
            }
            for (int scenario = 0; scenario < 3; scenario++) {
                boolean wall = scenario == 2;
                boolean turningOwner = scenario == 1;
                if (wall) for (int z = -9; z <= 2; z++) for (int y = 0; y < 9; y++) {
                    BlockPos p = base.offset(8, y, z);
                    saved.putIfAbsent(p, level.getBlockState(p));
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), 3);
                }
                CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
                var husk = EntityType.HUSK.create(level);
                if (guardian == null || husk == null) throw new IllegalStateException("Missing moving interception fixtures");
                try {
                    MagicData.getPlayerMagicData(owner).setMana(200);
                    owner.setPos(Vec3.atBottomCenterOf(base.offset(22, 0, 0)));
                    guardian.setOwner(owner);
                    guardian.setPos(Vec3.atBottomCenterOf(base));
                    husk.setPos(Vec3.atBottomCenterOf(base.offset(15, 0, 0)));
                    husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                    husk.setHealth(1000);
                    husk.setTarget(owner);
                    if (!level.addFreshEntity(guardian) || !level.addFreshEntity(husk))
                        throw new IllegalStateException("Could not register interception fixtures");
                    double initialX = guardian.getX(), initialHuskX = husk.getX();
                    double maxSide = 0;
                    int acquired = -1, hit = -1, reachedOwner = -1, firstWindup = -1;
                    double cumulativeTurn = 0;
                    for (int tick = 1; tick <= (turningOwner ? 240 : 180); tick++) {
                        if (turningOwner) {
                            // Continuous oval route with two reversals; no real player's input is involved.
                            double phase = tick <= 80 ? tick * .035 : tick <= 160 ? (160 - tick) * .035 : (tick - 160) * .035;
                            owner.setPos(base.getX() + 18 + 10 * Math.cos(phase), base.getY(), base.getZ() + 6 * Math.sin(phase));
                            husk.setHealth(1000);
                            owner.setHealth(ownerHealth);
                        }
                        float previousYaw = guardian.getYRot();
                        guardian.tickCount++;
                        husk.tickCount++;
                        age.setInt(guardian, tick);
                        husk.setTarget(owner);
                        combat.invoke(guardian, owner, level);
                        guardian.aiStep();
                        cumulativeTurn += Math.abs(net.minecraft.util.Mth.wrapDegrees(guardian.getYRot() - previousYaw));
                        if (tick % 5 == 0) husk.getNavigation().moveTo(owner, 1.0);
                        husk.aiStep();
                        for (CrimsonWave wave : level.getEntitiesOfClass(CrimsonWave.class,
                                guardian.getBoundingBox().inflate(40))) advanceWave.invoke(wave, level, owner, guardian);
                        if (selected.get(guardian) == husk && acquired < 0) acquired = tick;
                        if (firstWindup < 0 && guardian.getAction() != 0) firstWindup = tick;
                        if (reachedOwner < 0 && husk.distanceToSqr(owner) <= 2.5 * 2.5) reachedOwner = tick;
                        if (tick % 10 == 0) CrimsonSusanoo.LOGGER.info(
                                "Interception trace wall={} tick={} guardian={} target={} distance={} ownerDistance={} action={} yaw={} pitch={}",
                                wall, tick, guardian.position(), husk.position(), guardian.distanceTo(husk),
                                husk.distanceTo(owner), guardian.getAction(), guardian.getYRot(), guardian.getXRot());
                        maxSide = Math.max(maxSide, Math.abs(guardian.getZ() - (base.getZ() + .5)));
                        if (husk.getHealth() < 1000) {
                            if (hit < 0) hit = tick;
                            if (!turningOwner) break;
                        }
                    }
                    String label = turningOwner ? "turning owner" : wall ? "wall detour" : "open floor";
                    if (turningOwner) {
                        CrimsonSusanoo.LOGGER.info("Turning-owner diagnostic cumulativeYaw={} finalDistance={}", cumulativeTurn, guardian.distanceTo(husk));
                        check(selected.get(guardian) == husk && hit > 0,
                                "sustained reversing-owner fixture retains and damages its pursuing target");
                        continue;
                    }
                    CrimsonSusanoo.LOGGER.info("Interception {}: acquired={} hit={} guardianTravel={} huskTravel={} side={} firstWindup={} reachedOwner={}",
                            label, acquired, hit, guardian.getX() - initialX, husk.getX() - initialHuskX, maxSide, firstWindup, reachedOwner);
                    check(acquired > 0 && acquired <= 5, label + " acquires owner-bound moving husk within five ticks");
                    check(husk.getX() - initialHuskX > 1,
                            label + " runs actual approaching husk movement");
                    // The wall route is diagonal; measure horizontal displacement,
                    // not only X, before the earlier ranged hit ends the fixture.
                    if (wall) check(Math.hypot(guardian.getX() - initialX,
                                    guardian.getZ() - (base.getZ() + .5)) > 5,
                            "blocked ranged lane falls back to actual guardian pursuit");
                    check(hit > 0, label + " intercepts and damages moving husk within nine simulated seconds");
                    if (!wall) check(hit > 0 && reachedOwner < 0,
                            "clear-lane defense damages approaching husk before it reaches owner melee distance");
                    if (wall) check(maxSide > 3, "guardian navigates around wall rather than through it");
                } finally {
                    level.getEntitiesOfClass(CrimsonWave.class, guardian.getBoundingBox().inflate(40)).forEach(CrimsonWave::discard);
                    guardian.discard();
                    husk.discard();
                }
            }
        } finally {
            saved.forEach((p, state) -> level.setBlock(p, state, 3));
            owner.setPos(original);
            owner.getAbilities().invulnerable = invulnerable;
            owner.setHealth(ownerHealth);
            MagicData.getPlayerMagicData(owner).setMana(originalInterceptionMana);
        }
    }

    private net.minecraft.world.level.pathfinder.Path diagnosticPath(BlockPos destination) {
        // Real navigation consumes this small synthetic path; this checks arbitration, not pathfinding.
        return new net.minecraft.world.level.pathfinder.Path(new java.util.ArrayList<>(java.util.List.of(
                new net.minecraft.world.level.pathfinder.Node(destination.getX(), destination.getY(), destination.getZ()))), destination, true);
    }

    private void verifyCrescentWalls(ServerLevel level, FakePlayer owner) throws Exception {
        // An air-only fixture above the disposable world's spawn avoids natural terrain ambiguity.
        BlockPos origin = new BlockPos(level.getSharedSpawnPos().getX(), level.getMaxBuildHeight() - 16,
                level.getSharedSpawnPos().getZ());
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -4), origin.offset(5, 5, 4))) {
            if (!level.getBlockState(pos).isAir()) throw new IllegalStateException("Crescent wall fixture is not empty");
        }
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        Zombie covered = EntityType.ZOMBIE.create(level);
        Zombie exposed = EntityType.ZOMBIE.create(level);
        if (guardian == null || covered == null || exposed == null) throw new IllegalStateException("Missing wall fixtures");
        guardian.setOwner(owner);
        guardian.setPos(Vec3.atBottomCenterOf(origin));
        covered.moveTo(origin.getX() + 1.5, origin.getY(), origin.getZ() + 2.5);
        exposed.moveTo(origin.getX() + 1.5, origin.getY(), origin.getZ() - 1.5);
        for (Zombie target : new Zombie[] {covered, exposed}) {
            target.setNoAi(true);
            target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setHealth(100);
        }
        var originals = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var spawnedWaves = new java.util.ArrayList<CrimsonWave>();
        Method advance = CrimsonWave.class.getDeclaredMethod("advance", ServerLevel.class,
                net.minecraft.server.level.ServerPlayer.class, CrimsonEntity.class);
        advance.setAccessible(true);
        try {
            if (!level.addFreshEntity(covered) || !level.addFreshEntity(exposed)) {
                throw new IllegalStateException("Wall targets failed to register");
            }
            BlockPos edge = origin.offset(1, 0, 1);
            originals.put(edge, level.getBlockState(edge));
            originals.put(edge.above(), level.getBlockState(edge.above()));
            level.setBlock(edge, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(edge.above(), Blocks.STONE.defaultBlockState(), 3);
            for (int scenario = 0; scenario < 3; scenario++) {
                if (scenario == 1) {
                    level.setBlock(edge, originals.get(edge), 3);
                    level.setBlock(edge.above(), originals.get(edge.above()), 3);
                }
                if (scenario == 2) {
                    BlockPos centerWall = origin.offset(1, 0, 0);
                    originals.put(centerWall, level.getBlockState(centerWall));
                    level.setBlock(centerWall, Blocks.STONE.defaultBlockState(), 3);
                    covered.setHealth(100);
                    covered.invulnerableTime = 0;
                    covered.removeEffect(ModEffect.EFFECTBLAZING_BRAND.get());
                }
                CrimsonWave wave = CrimsonSusanoo.WAVE.get().create(level);
                if (wave == null) throw new IllegalStateException("Missing wall-test wave");
                spawnedWaves.add(wave);
                wave.setup(owner, guardian, new Vec3(1, 0, 0), 20);
                wave.setPos(origin.getX() + 0.5, origin.getY() + 0.8, origin.getZ() + 0.5);
                advance.invoke(wave, level, owner, guardian);
                if (scenario == 0) {
                    check(!wave.isRemoved() && exposed.getHealth() < 100,
                            "Crescent passes wall edge and hits exposed control target");
                    check(covered.getHealth() == 100 && covered.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()) == null,
                            "Crescent wide hit area does not damage or Brand target behind wall edge");
                    check(level.getBlockState(edge).is(Blocks.STONE) && level.getBlockState(edge.above()).is(Blocks.STONE),
                            "Crescent wall-edge attack preserves obstacle blocks");
                } else if (scenario == 1) {
                    check(covered.getHealth() < 100 && covered.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()) != null,
                            "removing cover allows the same Crescent path to damage and Brand target");
                } else {
                    check(wave.isRemoved() && covered.getHealth() == 100
                                    && covered.getEffect(ModEffect.EFFECTBLAZING_BRAND.get()) == null
                                    && level.getBlockState(origin.offset(1, 0, 0)).is(Blocks.STONE),
                            "Crescent stops at centerline wall without damaging target or terrain");
                }
            }
        } finally {
            covered.discard();
            exposed.discard();
            spawnedWaves.forEach(CrimsonWave::discard);
            originals.forEach((pos, state) -> level.setBlock(pos, state, 3));
        }
    }

    private void verifyAnimationHandoff(ServerLevel level) throws Exception {
        // Run the production controller through GeckoLib's real processor without a GPU.
        // Empty clips isolate queue handoff; this does not test bone poses or shaders.
        CrimsonEntity guardian = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (guardian == null) throw new IllegalStateException("Missing animation fixture");
        var manager = new software.bernie.geckolib.core.animation.AnimatableManager<CrimsonEntity>(guardian);
        var model = new DiagnosticAnimationModel();
        var controller = manager.getAnimationControllers().get("main");
        var state = new software.bernie.geckolib.core.animation.AnimationState<>(guardian, 0, 0, 0.5F, false);
        for (int frame = 0; frame < 10; frame++)
            model.processor.tickAnimation(guardian, model, manager, frame + 0.5, state, false);
        check(controller.getCurrentAnimation() != null
                        && controller.getCurrentAnimation().animation().name().equals("animation.guardian.idle"),
                "real GeckoLib processor loads guardian idle clip");
        var movingField = CrimsonEntity.class.getDeclaredField("MOVING");
        movingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var moving = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) movingField.get(null);
        guardian.setDeltaMovement(Vec3.ZERO);
        guardian.getEntityData().set(moving, true);
        for (int frame = 10; frame < 18; frame++)
            model.processor.tickAnimation(guardian, model, manager, frame + .5, state, false);
        check(controller.getCurrentAnimation().animation().name().equals("animation.guardian.walk"),
                "server movement flag selects walking even when GeckoLib reports no movement");
        guardian.getEntityData().set(moving, false);
        Method begin = CrimsonEntity.class.getDeclaredMethod("beginAttack", int.class, int.class);
        begin.setAccessible(true);
        var visualField = CrimsonEntity.class.getDeclaredField("VISUAL_START");
        visualField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var visualStart = (net.minecraft.network.syncher.EntityDataAccessor<Long>) visualField.get(null);
        String[] clips = {"cleave", "crescent", "slash", "dismiss", "death"};
        for (int stage = 0; stage < clips.length; stage++) {
            if (stage < 3) begin.invoke(guardian, stage + 1, 14);
            else if (stage == 3) guardian.dismiss("diagnostic");
            else guardian.setHealth(0);
            guardian.getEntityData().set(visualStart, level.getGameTime() - 8);
            for (int frame = 0; frame < 3; frame++)
                model.processor.tickAnimation(guardian, model, manager, 20 + stage * 10 + frame + 0.5, state, false);
            check(controller.getCurrentAnimation() != null
                            && controller.getCurrentAnimation().animation().name().equals("animation.guardian." + clips[stage]),
                    "real GeckoLib processor switches to " + clips[stage] + " at nonzero server phase");
            if (stage < 3) {
                guardian.hurtTime = 8;
                for (int frame = 3; frame < 6; frame++)
                    model.processor.tickAnimation(guardian, model, manager, 20 + stage * 10 + frame + .5, state, false);
                check(controller.getCurrentAnimation().animation().name().equals("animation.guardian." + clips[stage]),
                        "damage reaction cannot interrupt committed " + clips[stage] + " animation");
                guardian.hurtTime = 0;
            }
        }
    }

    private static final class DiagnosticAnimationModel implements
            software.bernie.geckolib.core.animatable.model.CoreGeoModel<CrimsonEntity> {
        private final software.bernie.geckolib.core.animation.AnimationProcessor<CrimsonEntity> processor =
                new software.bernie.geckolib.core.animation.AnimationProcessor<>(this);
        @Override public software.bernie.geckolib.core.animatable.model.CoreBakedGeoModel getBakedGeoModel(String name) {
            throw new UnsupportedOperationException("Diagnostic uses no rendered bones");
        }
        @Override public software.bernie.geckolib.core.animation.AnimationProcessor<CrimsonEntity> getAnimationProcessor() {
            return processor;
        }
        @Override public software.bernie.geckolib.core.animation.Animation getAnimation(CrimsonEntity entity, String name) {
            return new software.bernie.geckolib.core.animation.Animation(name, 60,
                    software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE,
                    new software.bernie.geckolib.core.keyframe.BoneAnimation[0],
                    new software.bernie.geckolib.core.animation.Animation.Keyframes(
                            new software.bernie.geckolib.core.keyframe.event.data.SoundKeyframeData[0],
                            new software.bernie.geckolib.core.keyframe.event.data.ParticleKeyframeData[0],
                            new software.bernie.geckolib.core.keyframe.event.data.CustomInstructionKeyframeData[0]));
        }
        @Override public void handleAnimations(CrimsonEntity entity, long id,
                software.bernie.geckolib.core.animation.AnimationState<CrimsonEntity> state) {}
    }

    private void verifySynchronizedPackets(ServerLevel level, FakePlayer owner) throws Exception {
        // Unregistered entities exercise the real packet codec without spawning fake clients.
        // This proves wire-state delivery, not client rendering or transport latency.
        CrimsonEntity source = CrimsonSusanoo.GUARDIAN.get().create(level);
        CrimsonEntity first = CrimsonSusanoo.GUARDIAN.get().create(level);
        CrimsonEntity second = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (source == null || first == null || second == null) throw new IllegalStateException("Missing sync fixtures");
        source.startManifesting(43);
        var groundedField = CrimsonEntity.class.getDeclaredField("GROUNDED");
        var movingField = CrimsonEntity.class.getDeclaredField("MOVING");
        groundedField.setAccessible(true);
        movingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var grounded = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) groundedField.get(null);
        @SuppressWarnings("unchecked")
        var moving = (net.minecraft.network.syncher.EntityDataAccessor<Boolean>) movingField.get(null);
        source.getEntityData().set(grounded, true);
        source.getEntityData().set(moving, true);
        var startField = CrimsonEntity.class.getDeclaredField("MANIFEST_START");
        startField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var start = (net.minecraft.network.syncher.EntityDataAccessor<Long>) startField.get(null);
        source.getEntityData().set(start, level.getGameTime() - 21);
        copyWireState(source, true, first, second);
        first.setOnGround(false);
        second.setOnGround(false);
        first.setDeltaMovement(Vec3.ZERO);
        second.setDeltaMovement(Vec3.ZERO);
        check(first.isVisuallyGrounded() && second.isVisuallyGrounded()
                        && first.isVisuallyMoving() && second.isVisuallyMoving(),
                "two receivers retain grounded walking state independently of local physics flags");
        source.getEntityData().set(grounded, false);
        source.getEntityData().set(moving, false);
        copyWireState(source, false, first, second);
        check(!first.isVisuallyGrounded() && !second.isVisuallyGrounded()
                        && !first.isVisuallyMoving() && !second.isVisuallyMoving(),
                "airborne and stopped movement delta clears both receivers");
        first.tickCount = 21;
        second.tickCount = 0;
        double expectedPhase = 21.5 * 60.0 / 43;
        check(Math.abs(first.getManifestAnimationTick(0.5) - expectedPhase) < 0.000001
                        && Math.abs(second.getManifestAnimationTick(0.5) - expectedPhase) < 0.000001,
                "late manifest receiver shares current animation phase despite different local entity age");
        source.getEntityData().set(start, level.getGameTime() - 100);
        copyWireState(source, false, second);
        check(second.getManifestAnimationTick(0.5) == 59.999,
                "delayed activation holds final manifest pose instead of replaying it");
        var durationField = CrimsonEntity.class.getDeclaredField("MANIFEST_TICKS");
        durationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var duration = (net.minecraft.network.syncher.EntityDataAccessor<Integer>) durationField.get(null);
        check(first.isManifesting() && second.isManifesting()
                        && first.getEntityData().get(duration) == 43 && second.getEntityData().get(duration) == 43,
                "manifest state and modified duration round-trip to two independent receivers");
        source.getEntityData().packDirty();
        source.activate();
        copyWireState(source, false, first, second);
        check(!first.isManifesting() && !second.isManifesting(),
                "activation delta clears manifestation on both receivers");
        Method begin = CrimsonEntity.class.getDeclaredMethod("beginAttack", int.class, int.class);
        begin.setAccessible(true);
        var visualField = CrimsonEntity.class.getDeclaredField("VISUAL_START");
        visualField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var visualStart = (net.minecraft.network.syncher.EntityDataAccessor<Long>) visualField.get(null);
        for (int action = 1; action <= 3; action++) {
            begin.invoke(source, action, 14);
            check(source.getEntityData().get(visualStart) == level.getGameTime(),
                    "attack " + action + " records server animation start");
            source.getEntityData().set(visualStart, level.getGameTime() - 8);
            copyWireState(source, false, first, second);
            check(first.getAction() == action && second.getAction() == action,
                    "attack " + action + " delta round-trips to both receivers");
            check(first.getActionAnimationTick(0.5) == 8.5 && second.getActionAnimationTick(0.5) == 8.5,
                    "attack " + action + " receivers share phase despite different local ages");
            source.getEntityData().set(visualStart, level.getGameTime() - 100);
            copyWireState(source, true, second);
            double lastTick = (action == 1 ? 23 : action == 2 ? 26 : 32) - 0.001;
            check(second.getActionAnimationTick(0.5) == lastTick,
                    "late attack " + action + " receiver holds final pose until state clears");
        }
        source.dismiss("mana_collapse");
        check(source.getEntityData().get(visualStart) == level.getGameTime(),
                "dismissal resets the server animation start after combat");
        source.getEntityData().set(visualStart, level.getGameTime() - 12);
        copyWireState(source, false, first, second);
        check(first.getFade() == 1 && second.getFade() == 1
                        && first.isManaCollapsing() && second.isManaCollapsing(),
                "mana-collapse fade delta round-trips to both receivers");
        CrimsonEntity late = CrimsonSusanoo.GUARDIAN.get().create(level);
        if (late == null) throw new IllegalStateException("Missing late receiver");
        copyWireState(source, true, late);
        check(late.getFade() == 1 && late.isManaCollapsing() && late.getAction() == 3,
                "new tracker receives current non-default visual state");
        check(first.getEndingAnimationTick(0.5) == 12.5 && late.getEndingAnimationTick(0.5) == 12.5,
                "late dismissal tracker shares the existing fade phase");
        source.setHealth(0);
        source.die(level.damageSources().genericKill());
        check(source.getEntityData().get(visualStart) == level.getGameTime(),
                "death records a fresh server animation start");
        source.getEntityData().set(visualStart, level.getGameTime() - 18);
        copyWireState(source, true, late);
        check(late.isDeadOrDying() && late.getEndingAnimationTick(0.5) == 18.5,
                "late death tracker receives health and current death phase");
        CrimsonWave wave = CrimsonSusanoo.WAVE.get().create(level);
        CrimsonWave firstWave = CrimsonSusanoo.WAVE.get().create(level);
        CrimsonWave secondWave = CrimsonSusanoo.WAVE.get().create(level);
        if (wave == null || firstWave == null || secondWave == null) throw new IllegalStateException("Missing wave fixtures");
        Vec3 heading = new Vec3(3, 0, -4).normalize();
        wave.setup(owner, source, heading, 1);
        copyWireState(wave, true, firstWave, secondWave);
        check(firstWave.getVisualDirection().distanceToSqr(heading) < 0.000001
                        && secondWave.getVisualDirection().distanceToSqr(heading) < 0.000001,
                "Crescent direction round-trips to two independent receivers");
    }

    private void copyWireState(Entity source, boolean initial, Entity... receivers) {
        var values = initial ? source.getEntityData().getNonDefaultValues() : source.getEntityData().packDirty();
        if (values == null || values.isEmpty()) throw new IllegalStateException("Expected synchronized state packet");
        for (Entity receiver : receivers) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new ClientboundSetEntityDataPacket(source.getId(), values).write(buffer);
                var decoded = new ClientboundSetEntityDataPacket(buffer);
                if (decoded.id() != source.getId() || buffer.readableBytes() != 0) {
                    throw new IllegalStateException("Entity data packet failed exact round-trip");
                }
                receiver.getEntityData().assignValues(decoded.packedItems());
            } finally {
                buffer.release();
            }
        }
    }

    private void diagnosticAttribute(FakePlayer player, String id, double value) {
        var attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
        if (attribute == null) return;
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        double previous = instance.getValue();
        instance.setBaseValue(value);
        CrimsonSusanoo.LOGGER.info("Diagnostic-only {}: {} -> {}", id, previous, instance.getValue());
    }

    private void check(boolean condition, String label) {
        if (condition) {
            passed++;
            CrimsonSusanoo.LOGGER.info("Crimson diagnostic PASS: {}", label);
        } else {
            failed++;
            CrimsonSusanoo.LOGGER.error("Crimson diagnostic FAIL: {}", label);
        }
    }
}

