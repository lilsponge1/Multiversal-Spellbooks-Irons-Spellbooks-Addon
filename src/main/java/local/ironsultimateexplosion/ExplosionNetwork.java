package local.ironsultimateexplosion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ExplosionNetwork {
    private static final String VERSION = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GrandExplosionMod.ID, "effects"), () -> VERSION, VERSION::equals, VERSION::equals);

    static void register() {
        CHANNEL.registerMessage(0, ExplosionPacket.class, ExplosionPacket::encode, ExplosionPacket::decode, ExplosionPacket::handle);
        CHANNEL.messageBuilder(ThundercrashInputPacket.class, 1, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(ThundercrashInputPacket::encode).decoder(ThundercrashInputPacket::decode).consumerNetworkThread(ThundercrashInputPacket::handle).add();
        CHANNEL.messageBuilder(ThundercrashStatePacket.class, 2, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ThundercrashStatePacket::encode).decoder(ThundercrashStatePacket::decode).consumerNetworkThread(ThundercrashStatePacket::handle).add();
        CHANNEL.messageBuilder(ThundercrashImpactPacket.class, 3, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ThundercrashImpactPacket::encode).decoder(ThundercrashImpactPacket::decode).consumerNetworkThread(ThundercrashImpactPacket::handle).add();
    }

    static void input(ThundercrashInputPacket packet) { CHANNEL.sendToServer(packet); }
    static void state(net.minecraft.server.level.ServerPlayer player, ThundercrashStatePacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
        if (player.f_8906_ != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    static void stateTo(net.minecraft.server.level.ServerPlayer viewer, ThundercrashStatePacket packet) {
        if (viewer.f_8906_ != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
    }
    static void impact(ServerLevel level, ThundercrashImpactPacket packet) {
        Vec3 p = packet.position();
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(p.f_82479_, p.f_82480_, p.f_82481_, 96, level.m_46472_())), packet);
    }

    static void charge(ServerLevel level, Vec3 point, int visualRadius, long seed, int duration) {
        send(level, point, visualRadius, seed, ExplosionPacket.CHARGE, duration);
    }

    static void detonate(ServerLevel level, Vec3 point, int visualRadius, long seed) {
        send(level, point, visualRadius, seed, ExplosionPacket.DETONATE, 1500);
    }

    static void cancel(ServerLevel level, Vec3 point, int visualRadius, long seed) {
        send(level, point, visualRadius, seed, ExplosionPacket.CANCEL, 0);
    }

    private static void send(ServerLevel level, Vec3 point, int visualRadius, long seed, byte phase, int duration) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                point.f_82479_, point.f_82480_, point.f_82481_, visualRadius * 2.0, level.m_46472_())),
                new ExplosionPacket(level.m_46472_().m_135782_().toString(), point.f_82479_, point.f_82480_, point.f_82481_,
                        visualRadius, seed, phase, duration));
    }
    private ExplosionNetwork() {}
}
