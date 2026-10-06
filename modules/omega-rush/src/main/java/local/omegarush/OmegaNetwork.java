package local.omegarush;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class OmegaNetwork {
    private static final String VERSION="3";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(OmegaMod.ID,"flight"),()->VERSION,VERSION::equals,VERSION::equals);
    static void register() {
        CHANNEL.messageBuilder(OmegaInputPacket.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(OmegaInputPacket::encode).decoder(OmegaInputPacket::decode).consumerNetworkThread(OmegaInputPacket::handle).add();
        CHANNEL.messageBuilder(OmegaStatePacket.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(OmegaStatePacket::encode).decoder(OmegaStatePacket::decode).consumerNetworkThread(OmegaStatePacket::handle).add();
        CHANNEL.messageBuilder(OmegaBurstPacket.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(OmegaBurstPacket::encode).decoder(OmegaBurstPacket::decode).consumerNetworkThread(OmegaBurstPacket::handle).add();
        CHANNEL.messageBuilder(OmegaFormInputPacket.class,3,NetworkDirection.PLAY_TO_SERVER).encoder(OmegaFormInputPacket::encode).decoder(OmegaFormInputPacket::decode).consumerNetworkThread(OmegaFormInputPacket::handle).add();
        CHANNEL.messageBuilder(OmegaFormPacket.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder(OmegaFormPacket::encode).decoder(OmegaFormPacket::decode).consumerNetworkThread(OmegaFormPacket::handle).add();
    }
    static void input(OmegaInputPacket p) { CHANNEL.sendToServer(p); }
    static void formInput(OmegaFormInputPacket p){CHANNEL.sendToServer(p);}
    static void form(ServerPlayer p,OmegaFormPacket packet){CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->p),packet);formTo(p,packet);}
    static void formTo(ServerPlayer p,OmegaFormPacket packet){if(p.f_8906_!=null)CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet);}
    static void state(ServerPlayer p,OmegaStatePacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->p),packet); stateTo(p,packet);
    }
    static void stateTo(ServerPlayer p,OmegaStatePacket packet) { if(p.f_8906_!=null) CHANNEL.send(PacketDistributor.PLAYER.with(()->p),packet); }
    static void burst(ServerLevel level,OmegaBurstPacket packet) {
        Vec3 p=packet.position();
        CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(p.f_82479_,p.f_82480_,p.f_82481_,96,level.m_46472_())),packet);
    }
    private OmegaNetwork() {}
}
