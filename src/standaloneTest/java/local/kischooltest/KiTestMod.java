package local.kischooltest;

import local.kischool.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Separate disposable diagnostics, excluded from the downloadable mod. */
@Mod("ki_school_test")
public final class KiTestMod {
    public KiTestMod() {
        new SaiyanHarness();
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::login);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Boolean.getBoolean("ki.review")) new KiClientCheck();
        });
    }
    private void started(ServerStartedEvent event) {
        int passed = 0, failed = 0;
        for (String id : new String[]{"ki_school", "irons_ultimate_explosion", "irons_omega_rush", "crimson_susanoo", "ignis_armor_compat"}) {
            boolean good = ModList.get().isLoaded(id);
            if (good) passed++; else failed++;
            System.out.println((good ? "KI_COEXIST_PASS " : "KI_COEXIST_FAIL ") + id);
        }
        for (String id : new String[]{"ki_school:saiyan_wig", "irons_omega_rush:flowery_scarf"}) {
            boolean good = ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id));
            if (good) passed++; else failed++;
            System.out.println((good ? "KI_COEXIST_PASS " : "KI_COEXIST_FAIL ") + id);
        }
        boolean recipe = event.getServer().m_129783_().m_7465_().m_44043_(new ResourceLocation("ki_school", "saiyan_wig")).isPresent();
        if (recipe) passed++; else failed++;
        boolean namespace = ModSpells.SAIYAN_ASCENSION.getId().toString().equals("ki_school:saiyan_ascension");
        if (namespace) passed++; else failed++;
        System.out.println("KI_COEXIST_DIAGNOSTICS " + passed + " passed, " + failed + " failed");
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("ki.review") || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var level = player.m_284548_();
        for (int x=-8; x<=8; x++) for (int z=-8; z<=8; z++) level.m_46597_(new BlockPos(x,79,z), Blocks.f_50069_.m_49966_());
        player.f_8906_.m_9774_(0,80,0,0,0);
        ItemStack wig = new ItemStack(ModItems.SAIYAN_WIG.get());
        ((SaiyanWig)ModItems.SAIYAN_WIG.get()).initializeSpellContainer(wig);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow().setEquippedCurio("head",0,wig);
        ItemStack book = new ItemStack(ModItems.SAIYAN_BOOK.get());
        ((SaiyanBook)ModItems.SAIYAN_BOOK.get()).initializeSpellContainer(book);
        player.m_8061_(EquipmentSlot.MAINHAND,book);
        player.m_21051_(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()).m_22100_(4000);
        var data = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
        data.setMana(3500);
        io.redspace.ironsspellbooks.setup.PacketDistributor.sendToPlayer(player,new io.redspace.ironsspellbooks.network.SyncManaPacket(data));
        System.out.println("KI_CLIENT_KIT_READY");
    }
}
