package com.crimson_susanoo;

import com.crimson_susanoo.entity.CrimsonEntity;
import com.crimson_susanoo.spell.CrimsonSpell;
import com.mojang.brigadier.Command;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CommonEvents {
    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("crimson_susanoo")
                .then(Commands.literal("hunt").executes(context -> setHunting(context.getSource(), true)))
                .then(Commands.literal("guard").executes(context -> setHunting(context.getSource(), false)))
                .then(Commands.literal("book").requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ItemStack book = new ItemStack(ItemRegistry.LEGENDARY_SPELL_BOOK.get());
                            ISpellContainerMutable container = ISpellContainer.getOrCreate(book).mutableCopy();
                            if (!container.addSpell(CrimsonSusanoo.SPELL.get(), 1, false)) return 0;
                            ISpellContainer.set(book, container.toImmutable());
                            if (!player.addItem(book)) player.drop(book, false);
                            context.getSource().sendSuccess(() -> Component.literal("Granted Crimson Susanoo test spellbook"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("scroll").requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ItemStack scroll = new ItemStack(ItemRegistry.SCROLL.get());
                            ISpellContainer.set(scroll, ISpellContainer.createScrollContainer(CrimsonSusanoo.SPELL.get(), 1, scroll));
                            if (!player.addItem(scroll)) player.drop(scroll, false);
                            context.getSource().sendSuccess(() -> Component.literal("Granted Crimson Susanoo test scroll"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("visual_test").requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (!(player.level() instanceof ServerLevel level)
                                    || ((CrimsonSpell) CrimsonSusanoo.SPELL.get()).summonForVisualTest(player, level) == null) {
                                context.getSource().sendFailure(Component.literal("No clear placement or guardian already active"));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> Component.literal("Summoned Crimson Susanoo for visual test"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("dismiss").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (player.level() instanceof ServerLevel level) {
                        CrimsonEntity guardian = CrimsonSpell.findActive(player, level);
                        if (guardian != null) guardian.dismiss("voluntary");
                    }
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private int setHunting(net.minecraft.commands.CommandSourceStack source, boolean hunting)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CrimsonEntity guardian = CrimsonSpell.findActive(player, player.serverLevel());
        if (guardian == null || guardian.isManifesting()) {
            source.sendFailure(Component.literal("You have no active Crimson Susanoo"));
            return 0;
        }
        guardian.setHunting(hunting);
        source.sendSuccess(() -> Component.literal(hunting
                ? "Crimson Susanoo hunts enemies within 32 blocks of you."
                : "Crimson Susanoo guards and patrols near you."), false);
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public void onPlayerDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || !(owner.level() instanceof ServerLevel level)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || event.getAmount() <= 0) return;
        CrimsonEntity guardian = CrimsonSpell.findActive(owner, level);
        if (guardian == null || guardian.isDismissing() || guardian.distanceToSqr(owner) > Math.pow(ServerConfig.GUARD_RADIUS.get(), 2)) return;
        float redirected = (float) (event.getAmount() * ServerConfig.GUARD_PERCENT.get());
        float cost = (float) (redirected * ServerConfig.GUARD_MANA_PER_DAMAGE.get());
        if (redirected <= 0 || !CrimsonEntity.chargeMana(owner, cost)) return;
        // The entity hit is a guardian, so this handler cannot redirect the second damage event.
        if (!guardian.hurt(event.getSource(), redirected)) {
            MagicData magic = MagicData.getPlayerMagicData(owner);
            magic.setMana(magic.getMana() + cost);
            PacketDistributor.sendToPlayer(owner, new SyncManaPacket(magic));
            return;
        }
        event.setAmount(Math.max(0, event.getAmount() - redirected));
        CrimsonSusanoo.LOGGER.debug("Guard absorbed {} damage for {} at {} mana", redirected, owner.getUUID(), cost);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner && owner.level() instanceof ServerLevel level) {
            dismissForOwnerTransition(owner, level, "owner_death");
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner && owner.level() instanceof ServerLevel level) {
            // Unsaved guardians cannot survive a restart; resolve any saved owner marker now.
            CrimsonSpell.findActive(owner, level);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner && owner.level() instanceof ServerLevel level) {
            dismissForOwnerTransition(owner, level, "owner_logout");
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            ServerLevel previous = owner.getServer().getLevel(event.getFrom());
            dismissForOwnerTransition(owner, previous != null ? previous : (ServerLevel) owner.level(), "owner_dimension");
        }
    }

    private void dismissForOwnerTransition(ServerPlayer owner, ServerLevel guardianLevel, String reason) {
        boolean fullySummoned = owner.getPersistentData().getBoolean(CrimsonSpell.ACTIVE_READY);
        CrimsonEntity guardian = CrimsonSpell.findActive(owner, guardianLevel);
        if (guardian != null) guardian.dismiss(reason);
        CrimsonSpell.clearOwnerMarker(owner);
        // The event still has the owner even if the guardian's player-list lookup no longer does.
        if (fullySummoned) {
            MagicData magic = MagicData.getPlayerMagicData(owner);
            if (!magic.getPlayerCooldowns().isOnCooldown(CrimsonSusanoo.SPELL.get())) {
                MagicHelper.MAGIC_MANAGER.addCooldown(owner, CrimsonSusanoo.SPELL.get(),
                        io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK);
                magic.getPlayerCooldowns().syncToPlayer(owner);
            }
        }
    }
}
