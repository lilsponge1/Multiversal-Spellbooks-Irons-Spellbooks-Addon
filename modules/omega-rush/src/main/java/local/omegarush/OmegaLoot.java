package local.omegarush;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.loot.*;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
public final class OmegaLoot {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,OmegaMod.ID);
    public static final RegistryObject<Codec<Chest>> CHESTS=SERIALIZERS.register("flowery_scarf_chests",()->Chest.CODEC);
    private static final Map<LivingEntity,Boolean> BOSSES=new WeakHashMap<>();
    public static boolean eligibleBoss(ResourceLocation id){return id!=null&&OmegaConfig.SCARF_BOSSES.get().contains(id.toString());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void drop(LivingDropsEvent e){
        if(e.isCanceled()||!e.getEntity().m_9236_().m_46469_().m_46207_(net.minecraft.world.level.GameRules.f_46135_)||!eligibleBoss(ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().m_6095_()))||BOSSES.containsKey(e.getEntity()))return;
        if(!(e.getSource().m_7639_() instanceof Player)&&!e.isRecentlyHit())return;
        BOSSES.put(e.getEntity(),true);
        if(e.getEntity().m_217043_().m_188500_()<OmegaConfig.BOSS_CHANCE.get()){
            ItemStack s=new ItemStack(OmegaItems.SCARF.get());FloweryScarf.prepare(s);
            e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(e.getEntity().m_9236_(),e.getEntity().m_20185_(),e.getEntity().m_20186_(),e.getEntity().m_20189_(),s));
        }
    }
    public static final class Chest extends LootModifier {
        static final Codec<Chest> CODEC=RecordCodecBuilder.create(instance->codecStart(instance).apply(instance,Chest::new));
        private final Map<LootContext,Boolean> rolled=new WeakHashMap<>();
        public Chest(LootItemCondition[] c){super(c);}
        @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
            ResourceLocation id=context.getQueriedLootTableId();if(id==null)return loot;
            var table=context.m_278643_().m_278676_(id);
            if(table==null||table.m_79122_()!=LootContextParamSets.m_81431_(new ResourceLocation("minecraft","chest")))return loot;
            if(rolled.put(context,true)!=null)return loot;
            if(context.m_230907_().m_188500_()<OmegaConfig.CHEST_CHANCE.get()){ItemStack s=new ItemStack(OmegaItems.SCARF.get());FloweryScarf.prepare(s);loot.add(s);}return loot;
        }
        @Override public Codec<? extends IGlobalLootModifier> codec(){return CODEC;}
    }
    private OmegaLoot(){}
}
