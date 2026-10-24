package local.kischool;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Independent channel; the base addon's protocol remains untouched. */
public final class KiNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KiSchoolMod.ID, "effects"), () -> VERSION, VERSION::equals, VERSION::equals);
    static void register() {
        CHANNEL.messageBuilder(SaiyanInputPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaiyanInputPacket::encode).decoder(SaiyanInputPacket::decode).consumerNetworkThread(SaiyanInputPacket::handle).add();
        CHANNEL.messageBuilder(SaiyanStatePacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SaiyanStatePacket::encode).decoder(SaiyanStatePacket::decode).consumerNetworkThread(SaiyanStatePacket::handle).add();
        CHANNEL.messageBuilder(SaiyanCombatPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SaiyanCombatPacket::encode).decoder(SaiyanCombatPacket::decode).consumerNetworkThread(SaiyanCombatPacket::handle).add();
    }
    public static void saiyanInput(SaiyanInputPacket packet) { CHANNEL.sendToServer(packet); }
    static void saiyanState(ServerPlayer player, SaiyanStatePacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
        saiyanTo(player, packet);
    }
    static void saiyanTo(ServerPlayer viewer, SaiyanStatePacket packet) {
        if (viewer.f_8906_ != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
    }
    static void saiyanCombat(ServerPlayer player, SaiyanCombatPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
        if (player.f_8906_ != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    private KiNetwork() {}
}
