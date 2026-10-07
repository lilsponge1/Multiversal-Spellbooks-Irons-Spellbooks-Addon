package local.omegarush;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Indices 0..5 announce flowers; 6 announces successful absorption. */
public record OmegaRitualEventPacket(UUID caster,long session,String dimension,int index,Vec3 position){
    static void encode(OmegaRitualEventPacket p,FriendlyByteBuf b){b.m_130077_(p.caster);b.writeLong(p.session);b.m_130070_(p.dimension);b.writeByte(p.index);OmegaStatePacket.vector(b,p.position);}
    static OmegaRitualEventPacket decode(FriendlyByteBuf b){return new OmegaRitualEventPacket(b.m_130259_(),b.readLong(),b.m_130136_(128),b.readUnsignedByte(),OmegaStatePacket.vector(b));}
    static void handle(OmegaRitualEventPacket p,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->OmegaRitualEffects.event(p)));c.setPacketHandled(true);}
}
