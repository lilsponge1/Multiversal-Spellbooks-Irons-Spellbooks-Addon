package local.grandexplosiontest;

import com.gametechbc.traveloptics.init.TravelopticsItems;
import com.gametechbc.traveloptics.item.TravelopticsArmorMaterials;
import com.gametechbc.traveloptics.config.ArmorConfig;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.*;
import local.ironsultimateexplosion.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Opt-in fixture only; never packaged into the production addon. */
public final class ThunderstarHarness {
    private int passed, failed;
    private final Map<UUID,Integer> hurts = new HashMap<>();
    public ThunderstarHarness() {
        if (Boolean.getBoolean("thunderstar.diagnostics")) {
            MinecraftForge.EVENT_BUS.addListener(this::started);
            MinecraftForge.EVENT_BUS.addListener(this::hurt);
        }
    }
    private void hurt(LivingHurtEvent event) {
        hurts.computeIfPresent(event.getEntity().m_20148_(), (id,count) -> count+1);
    }
    private void check(String name, boolean condition) {
        if (condition) { passed++; System.out.println("THUNDERSTAR_PASS " + name); }
        else { failed++; System.out.println("THUNDERSTAR_FAIL " + name); }
    }
    private void started(ServerStartedEvent event) {
        try { run(event.getServer().m_129783_()); }
        catch (Throwable error) { failed++; error.printStackTrace(System.out); }
        System.out.println("Thunderstar diagnostics: " + passed + " passed, " + failed + " failed");
    }
    private void run(ServerLevel level) throws Exception {
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(new net.minecraftforge.event.OnDatapackSyncEvent(level.m_7654_().m_6846_(),null));
        Item baseline = TravelopticsItems.RIPTIDE_SOVEREIGN_ARMOR_FINCLOAK.get();
        ThunderstarArmorItem armor = (ThunderstarArmorItem)ModItems.THUNDERSTAR_CUIRASS.get();
        ItemStack baseStack=new ItemStack(baseline), newStack=new ItemStack(armor);
        check("durability_matches",baseStack.m_41776_()==newStack.m_41776_());
        check("enchantability_matches",baseline.m_6473_()==armor.m_6473_());
        check("enchanted_table_predicate_matches",baseline.m_8120_(baseStack)==armor.m_8120_(newStack));
        check("rarity_matches",baseStack.m_41791_()==newStack.m_41791_());
        var protection=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft","protection"));
        check("protection_supported",armor.canApplyAtEnchantingTable(newStack,protection));
        boolean oldBreakable=ArmorConfig.armorsShouldBeBreakable.get();
        try {
            for(boolean enabled:new boolean[]{false,true}) {
                ArmorConfig.armorsShouldBeBreakable.set(enabled);
                check("durability_policy_"+enabled,armor.isDamageable(newStack)==baseline.isDamageable(baseStack));
            }
        } finally { ArmorConfig.armorsShouldBeBreakable.set(oldBreakable); }
        armor.initializeSpellContainer(newStack);
        var container=io.redspace.ironsspellbooks.api.spells.ISpellContainer.getOrCreate(newStack);
        check("imbueable_slot_count",container.getMaxSpellCount()==1);
        var oldAttributes=baseline.m_7167_(EquipmentSlot.CHEST);
        var newAttributes=armor.m_7167_(EquipmentSlot.CHEST);
        check("same_attribute_count",oldAttributes.size()==newAttributes.size());
        for(var entry:oldAttributes.asMap().entrySet()) {
            Attribute attribute=entry.getKey();
            var originals=entry.getValue(); var replacements=newAttributes.get(attribute);
            check("one_modifier_"+ForgeRegistries.ATTRIBUTES.getKey(attribute), originals.size()==1 && replacements.size()==1);
            AttributeModifier original=originals.iterator().next(), replacement=replacements.iterator().next();
            double expected=attribute==AttributeRegistry.LIGHTNING_SPELL_POWER.get()?0.10:
                    attribute==com.gametechbc.traveloptics.api.init.TravelopticsAttributes.AQUA_SPELL_POWER.get()?0.05:original.m_22218_();
            check("amount_"+ForgeRegistries.ATTRIBUTES.getKey(attribute),Math.abs(replacement.m_22218_()-expected)<1e-9);
            check("operation_"+ForgeRegistries.ATTRIBUTES.getKey(attribute),original.m_22217_()==replacement.m_22217_());
            check("slot_uuid_"+ForgeRegistries.ATTRIBUTES.getKey(attribute),original.m_22209_().equals(replacement.m_22209_()));
            System.out.println("THUNDERSTAR_STAT "+ForgeRegistries.ATTRIBUTES.getKey(attribute)+"="+replacement.m_22218_()+" "+replacement.m_22217_());
        }
        for(EquipmentSlot slot:EquipmentSlot.values()) if(slot!=EquipmentSlot.CHEST)
            check("no_offslot_attributes_"+slot,armor.m_7167_(slot).isEmpty());
        System.out.println("THUNDERSTAR_STAT durability="+newStack.m_41776_()+" enchantability="+armor.m_6473_());
        check("repair_ingredient_delegate",ThunderstarArmorMaterial.INSTANCE.m_6230_()==TravelopticsArmorMaterials.RIPTIDE_SOVEREIGN.m_6230_());
        double normal=impact(level,"none",0), boosted=impact(level,"chest",0);
        check("one_twenty_percent_center_bonus",normal>0 && Math.abs(boosted/normal-1.20)<0.001);
        check("held_item_not_a_perk",Math.abs(impact(level,"held",0)-normal)<0.02);
        check("removed_midflight_not_a_perk",Math.abs(impact(level,"removed",0)-normal)<0.02);
        check("equipped_midflight_perk",Math.abs(impact(level,"late",0)-boosted)<0.02);
        double normalEdge=impact(level,"none",3), boostedEdge=impact(level,"chest",3);
        check("same_bonus_after_falloff",normalEdge>0 && Math.abs(boostedEdge/normalEdge-1.20)<0.001);
        check("other_custom_spell_no_unique_bonus",Math.abs(otherSpell(level,baseline)-otherSpell(level,armor))<0.02);
    }
    private ServerPlayer caster(ServerLevel level) throws Exception {
        ServerPlayer player=new ServerPlayer(level.m_7654_(),level,new GameProfile(UUID.randomUUID(),"ThunderstarTest"));
        player.f_8906_=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"ThunderstarConn")).f_8906_;
        var spawn=ServerPlayer.class.getDeclaredField("f_8921_");spawn.setAccessible(true);spawn.setInt(player,0);
        player.m_6034_(0,80,0); player.m_21051_(Attributes.f_22276_).m_22100_(1000);player.m_21153_(1000);
        level.m_7967_(player); return player;
    }
    private LivingEntity target(ServerLevel level,double distance) {
        LivingEntity target=EntityType.f_20503_.m_20615_(level);
        target.m_6034_(distance,80,4);target.m_21051_(Attributes.f_22276_).m_22100_(1000);target.m_21153_(1000);
        target.m_21051_(Attributes.f_22284_).m_22100_(0);
        level.m_7967_(target);hurts.put(target.m_20148_(),0);return target;
    }
    private double impact(ServerLevel level,String mode,double distance) throws Exception {
        ServerPlayer player=caster(level); LivingEntity target=target(level,distance);
        ItemStack stack=new ItemStack(ModItems.THUNDERSTAR_CUIRASS.get());
        if(mode.equals("chest")||mode.equals("removed"))player.m_8061_(EquipmentSlot.CHEST,stack);
        if(mode.equals("held"))player.m_8061_(EquipmentSlot.MAINHAND,stack);
        try {
            check("activation_"+mode+"_"+distance,ThundercrashManager.start(player,100));
            var states=ThundercrashManager.class.getDeclaredField("STATES");states.setAccessible(true);
            Object active=((Map<?,?>)states.get(null)).get(player.m_20148_());
            if(mode.equals("removed"))player.m_8061_(EquipmentSlot.CHEST,ItemStack.f_41583_);
            if(mode.equals("late"))player.m_8061_(EquipmentSlot.CHEST,stack);
            var finish=ThundercrashManager.class.getDeclaredMethod("finish",active.getClass(),ThundercrashCollision.Hit.class,boolean.class);
            finish.setAccessible(true);
            finish.invoke(null,active,new ThundercrashCollision.Hit(0,new Vec3(0,80,4),target),true);
            float remaining=target.m_21223_();
            finish.invoke(null,active,new ThundercrashCollision.Hit(0,new Vec3(0,80,4),target),true);
            check("single_damage_event_"+mode+"_"+distance,hurts.get(target.m_20148_())==1 && target.m_21223_()==remaining);
            return 1000-remaining;
        } finally { ThundercrashManager.abort(player);target.m_146870_();player.m_146870_(); }
    }
    private double otherSpell(ServerLevel level,Item chest) throws Exception {
        ServerPlayer player=caster(level); LivingEntity target=target(level,0);
        try {
            player.m_8061_(EquipmentSlot.CHEST,new ItemStack(chest));
            DamageSources.applyDamage(target,100,ModSpells.GRAND_EXPLOSION.get().getDamageSource(player));
            return 1000-target.m_21223_();
        } finally { target.m_146870_();player.m_146870_(); }
    }
}
