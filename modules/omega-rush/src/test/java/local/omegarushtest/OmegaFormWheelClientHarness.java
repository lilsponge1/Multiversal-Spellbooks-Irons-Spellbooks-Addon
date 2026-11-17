package local.omegarushtest;

import io.netty.channel.*;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import io.redspace.ironsspellbooks.network.casting.CastPacket;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import io.redspace.ironsspellbooks.network.gui.SelectSpellPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import local.omegarush.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/** Opt-in one-shot smoke for a disposable client connected to a loopback test server. */
@Mod.EventBusSubscriber(modid = "omega_rush_test", value = Dist.CLIENT)
public final class OmegaFormWheelClientHarness {
    private static final String FIXTURE_SLOT = "spellbook_omega_test_previous";
    private static final String PIPE_NAME = "omega_form_wheel_client_smoke";
    private static final boolean ENABLED = Boolean.getBoolean("omega.formClientSmoke");
    private static final long SESSION = Long.MAX_VALUE - 20;
    private static final Queue<String> OUTBOUND = new ConcurrentLinkedQueue<>();
    private static LocalPlayer testPlayer;
    private static Channel channel;
    private static SpellSelection originalSelection;
    private static Long originalEndedSession;
    private static Class<?> formClient;
    private static boolean ran;
    private static int readyTicks;
    private static int verifyTicks;
    private static String rushSlot;

    @SubscribeEvent public static void priorSpell(SpellSelectionManager.SpellSelectionEvent event) {
        if (ENABLED && testPlayer != null && event.getEntity() == testPlayer) {
            // Provide a previous wheel entry without modifying any inventory or Curio.
            event.addSelectionOption(new SpellData(ModSpells.OMEGA_FORM.get(), 1), FIXTURE_SLOT, 0);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || ran) return;
        var player = Minecraft.m_91087_().f_91074_;
        var connection = Minecraft.m_91087_().m_91403_();
        if (player == null || connection == null || !(connection.m_104910_().m_129523_() instanceof InetSocketAddress address)
                || address.getAddress() == null || !address.getAddress().isLoopbackAddress()) return;
        try {
            if (testPlayer == null) {
                if (++readyTicks < 60) return;
                begin(player, connection.m_104910_().channel());
                return;
            }
            if (++verifyTicks < 10) return;
            check(OUTBOUND.stream().noneMatch("cast"::equals), "expiry sent an Iron's cast packet");
            check(List.copyOf(OUTBOUND).equals(List.of(rushSlot, FIXTURE_SLOT, rushSlot, FIXTURE_SLOT)),
                  "unexpected Iron's selection packets " + OUTBOUND);
            System.out.println("OMEGA_FORM_CLIENT_WHEEL_PASS charge_grant_close_timeout_previous_selection_no_cast");
            finish();
        } catch (Throwable error) {
            System.err.println("OMEGA_FORM_CLIENT_WHEEL_FAIL " + error);
            error.printStackTrace();
            try { finish(); } catch (ReflectiveOperationException cleanup) { cleanup.printStackTrace(); }
        }
    }

    private static void begin(LocalPlayer player, Channel socket) throws ReflectiveOperationException {
        check(!OmegaFormManager.active(player), "test must start outside Form");
        formClient = Class.forName("local.omegarush.OmegaFormClient");
        testPlayer = player;
        originalEndedSession = ended().get(player.m_20148_());
        var data = ClientMagicData.getSyncedSpellData(player);
        originalSelection = data.getSpellSelection();
        channel = socket;
        observeIronPackets();
        data.setSpellSelection(new SpellSelection(FIXTURE_SLOT, 0));
        ClientMagicData.updateSpellSelectionManager();
        check(rushCount() == 0 && previousSelected(), "unexpected initial wheel");

        receive(SESSION, 1, (byte) 0);
        check(rushCount() == 0, "Rush appeared before ritual completion");
        receive(SESSION, 2, (byte) 1);
        check(previousSelected(), "grant changed the player's selected spell");
        selectRush();
        receive(SESSION, 3, (byte) 2);
        check(rushCount() == 0 && previousSelected(), "closed Form failed to restore prior selection");

        receive(SESSION + 1, 4, (byte) 1);
        selectRush();
        var formsField = formClient.getDeclaredField("FORMS");
        formsField.setAccessible(true);
        Object form = ((Map<?, ?>) formsField.get(null)).get(player.m_20148_());
        var received = form.getClass().getDeclaredField("received");
        received.setAccessible(true);
        received.setLong(form, System.nanoTime() - 3_000_000_000L);
        var tick = formClient.getDeclaredMethod("tick");
        tick.setAccessible(true);
        tick.invoke(null);
        check(rushCount() == 0 && previousSelected(), "stale Form failed to restore prior selection");
    }

    private static void receive(long session, long tick, byte phase) throws ReflectiveOperationException {
        var player = testPlayer;
        var position = player.m_20182_();
        var dimension = player.m_9236_().m_46472_().m_135782_().toString();
        var packet = new OmegaFormPacket(player.m_20148_(), player.m_19879_(), dimension, session, tick, phase,
                false, false, 0, 128, -1, position, Vec3.f_82478_, 0.6, 0.4, player.m_20068_(), true, phase == 2, position, 0);
        var state = formClient.getDeclaredMethod("state", OmegaFormPacket.class);
        state.setAccessible(true);
        state.invoke(null, packet);
    }

    private static void selectRush() {
        var wheel = ClientMagicData.getSpellSelectionManager();
        check(rushCount() == 1, "expected exactly one temporary Rush entry");
        var option = wheel.getAllSpells().stream().filter(OmegaFormWheelClientHarness::isRush).findFirst().orElseThrow();
        check(option.spellData.getLevel() == 1 && option.getCastSource() == CastSource.SPELLBOOK, "unexpected rank or cast source");
        rushSlot = option.slot;
        wheel.makeSelection(option.globalIndex);
        check(wheel.getSelectedSpellData().getSpell() == ModSpells.OMEGA_RUSH.get(), "Rush could not be selected");
    }

    private static boolean isRush(SpellSelectionManager.SelectionOption option) {
        return option.spellData.getSpell() == ModSpells.OMEGA_RUSH.get();
    }

    private static long rushCount() {
        return ClientMagicData.getSpellSelectionManager().getAllSpells().stream().filter(OmegaFormWheelClientHarness::isRush).count();
    }

    private static boolean previousSelected() {
        var wheel = ClientMagicData.getSpellSelectionManager();
        var display = wheel.getSelection();
        var selection = ClientMagicData.getSyncedSpellData(testPlayer).getSpellSelection();
        return display != null && FIXTURE_SLOT.equals(display.slot) && FIXTURE_SLOT.equals(selection.equipmentSlot) && selection.index == 0;
    }

    private static void observeIronPackets() throws ReflectiveOperationException {
        var field = PacketDistributor.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        var iron = (SimpleChannel) field.get(null);
        var sample = (ServerboundCustomPayloadPacket) iron.toVanillaPacket(new SelectSpellPacket(new SpellSelection()), NetworkDirection.PLAY_TO_SERVER);
        var name = sample.getName();
        int selectId = new FriendlyByteBuf(sample.getInternalData().duplicate()).m_130242_();
        int castId = ironId(iron, new CastPacket());
        int quickCastId = ironId(iron, new QuickCastPacket(0));
        channel.pipeline().addLast(PIPE_NAME, new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof ServerboundCustomPayloadPacket payload && payload.getName().equals(name)) {
                    var buffer = new FriendlyByteBuf(payload.getInternalData().duplicate());
                    int id = buffer.m_130242_();
                    if (id == castId || id == quickCastId) OUTBOUND.add("cast");
                    if (id == selectId) {
                        var selection = new SpellSelection();
                        selection.readFromBuffer(buffer);
                        if (selection.equipmentSlot.equals(FIXTURE_SLOT) || selection.equipmentSlot.startsWith("spellbook_irons_omega_rush"))
                            OUTBOUND.add(selection.equipmentSlot);
                    }
                }
                super.write(ctx, msg, promise);
            }
        });
    }

    private static int ironId(SimpleChannel iron, Object packet) {
        var payload = (ServerboundCustomPayloadPacket) iron.toVanillaPacket(packet, NetworkDirection.PLAY_TO_SERVER);
        return new FriendlyByteBuf(payload.getInternalData().duplicate()).m_130242_();
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Long> ended() throws ReflectiveOperationException {
        var field = formClient.getDeclaredField("ENDED");
        field.setAccessible(true);
        return (Map<UUID, Long>) field.get(null);
    }

    private static void finish() throws ReflectiveOperationException {
        ran = true;
        if (channel != null && channel.pipeline().get(PIPE_NAME) != null) channel.pipeline().remove(PIPE_NAME);
        if (testPlayer == null) return;
        var player = testPlayer;
        if (OmegaFormManager.active(player)) receive(SESSION + 1, 5, (byte) 2);
        if (originalEndedSession == null) ended().remove(player.m_20148_());
        else ended().put(player.m_20148_(), originalEndedSession);
        testPlayer = null;
        if (originalSelection == null || Minecraft.m_91087_().f_91074_ != player) return;
        ClientMagicData.getSyncedSpellData(player).setSpellSelection(originalSelection);
        ClientMagicData.updateSpellSelectionManager();
        PacketDistributor.sendToServer(new SelectSpellPacket(originalSelection));
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
