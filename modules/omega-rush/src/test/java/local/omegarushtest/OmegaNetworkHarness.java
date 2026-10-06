package local.omegarushtest;

import java.util.*;
import java.util.concurrent.TimeUnit;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.channel.*;
import local.omegarush.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

/** Observability and artificial latency for disposable local clients only. Never in release. */
public final class OmegaNetworkHarness {
    private final Map<UUID,Probe> probes=new HashMap<>();
    private record Probe(ServerPlayer player,Object state,Vec3 start,long started) {}
    public OmegaNetworkHarness() {
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->MinecraftForge.EVENT_BUS.addListener(ClientProbe::tick));
    }
    private static Object field(Object object,String name) {
        try { var f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static Map<?,?> states() {
        try { var f=OmegaManager.class.getDeclaredField("STATES"); f.setAccessible(true); return (Map<?,?>)f.get(null); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END) return;
        for(Object s:states().values()) {
            ServerPlayer p=(ServerPlayer)field(s,"player");
            if(!p.m_6302_().startsWith("Omega")||!OmegaManager.owns(p)||probes.containsKey(p.m_20148_())) continue;
            Vec3 start=(Vec3)field(s,"previous"); probes.put(p.m_20148_(),new Probe(p,s,start,System.nanoTime()));
            System.out.println("OMEGA_LIVE_START name="+p.m_6302_()+" session="+field(s,"session")+" start="+start+" speed="+field(s,"speed"));
        }
        probes.entrySet().removeIf(entry->{
            Probe probe=entry.getValue(); if(OmegaManager.owns(probe.player)) return false;
            Vec3 end=(Vec3)field(probe.state,"position");
            System.out.println("OMEGA_LIVE_END name="+probe.player.m_6302_()+" session="+field(probe.state,"session")+" age="+field(probe.state,"age")+" displacement="+end.m_82554_(probe.start)+" seconds="+(System.nanoTime()-probe.started)/1e9+" gravityRestored="+!probe.player.m_20068_());
            return true;
        });
    }
    private void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("omega_latency").requires(s->s.m_6761_(4))
            .then(net.minecraft.commands.Commands.m_82129_("player",StringArgumentType.word())
            .then(net.minecraft.commands.Commands.m_82129_("oneWayMs",IntegerArgumentType.integer(0,125)).executes(c->{
                ServerPlayer p=c.getSource().m_81377_().m_6846_().m_11255_(StringArgumentType.getString(c,"player"));
                if(p==null||p.f_8906_==null) return 0;
                Channel channel=p.f_8906_.f_9742_.channel();
                if(!(channel.remoteAddress() instanceof java.net.InetSocketAddress address)||!address.getAddress().isLoopbackAddress()) return 0;
                int delay=IntegerArgumentType.getInteger(c,"oneWayMs");
                channel.eventLoop().execute(()->{
                    if(channel.pipeline().get("omega_test_latency")!=null) channel.pipeline().remove("omega_test_latency");
                    if(delay>0) channel.pipeline().addBefore("packet_handler","omega_test_latency",new ChannelDuplexHandler() {
                        @Override public void channelRead(ChannelHandlerContext ctx,Object msg) {
                            if(msg instanceof Packet<?>) ctx.executor().schedule(()->ctx.fireChannelRead(msg),delay,TimeUnit.MILLISECONDS); else ctx.fireChannelRead(msg);
                        }
                        @Override public void write(ChannelHandlerContext ctx,Object msg,ChannelPromise promise) {
                            if(msg instanceof Packet<?>) ctx.executor().schedule(()->ctx.writeAndFlush(msg,promise),delay,TimeUnit.MILLISECONDS); else ctx.write(msg,promise);
                        }
                    });
                    System.out.println("OMEGA_LIVE_LATENCY name="+p.m_6302_()+" oneWayMs="+delay+" roundTripMs="+(delay*2));
                });
                return 1;
            }))));
    }
    public static final class ClientProbe {
        private static final Map<String,Object> sessions=new HashMap<>();
        private static final Map<String,Integer> endedAt=new HashMap<>();
        private static int tick;
        @SuppressWarnings("unchecked") private static Map<UUID,?> flights() {
            try { var f=OmegaClient.class.getDeclaredField("FLIGHTS"); f.setAccessible(true); return (Map<UUID,?>)f.get(null); }
            catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        public static void tick(TickEvent.ClientTickEvent e) {
            if(e.phase!=TickEvent.Phase.END) return; tick++;
            var mc=net.minecraft.client.Minecraft.m_91087_(); if(mc.f_91073_==null||mc.f_91074_==null) { sessions.clear(); endedAt.clear(); return; }
            Set<String> current=new HashSet<>();
            for(Object f:flights().values()) {
                var p=(OmegaStatePacket)field(f,"packet"); if(p.phase()!=1) continue;
                String key=p.caster()+":"+p.session(); current.add(key);
                if(!sessions.containsKey(key)) {
                    sessions.put(key,f);
                    System.out.println("OMEGA_CLIENT_SEEN viewer="+mc.f_91074_.m_6302_()+" caster="+p.caster()+" session="+p.session()+" local="+p.caster().equals(mc.f_91074_.m_20148_())+" quality="+OmegaConfig.QUALITY.get()+" flash="+OmegaConfig.FLASH.get()+" overlay="+OmegaConfig.OVERLAY.get());
                }
            }
            for(String key:new ArrayList<>(sessions.keySet())) {
                if(current.contains(key)) { endedAt.remove(key); continue; }
                int end=endedAt.computeIfAbsent(key,k->tick); if(tick-end<20) continue;
                Object f=sessions.remove(key); endedAt.remove(key);
                var p=(OmegaStatePacket)field(f,"packet");
                System.out.println("OMEGA_CLIENT_CLEAN viewer="+mc.f_91074_.m_6302_()+" session="+p.session()+" soundStopped="+(field(f,"sound")==null)+" remainingFlight="+flights().containsKey(p.caster())+" localGravity="+mc.f_91074_.m_20068_());
            }
        }
    }
}
