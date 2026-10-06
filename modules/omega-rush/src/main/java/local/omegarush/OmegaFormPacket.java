package local.omegarush;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
public record OmegaFormPacket(UUID caster,int entity,String dimension,long session,long tick,byte phase,boolean hover,int age,int chargeTicks,int accepted,Vec3 position,Vec3 velocity,double speed,double rise,boolean originalGravity,boolean reset,boolean closed){
    static void encode(OmegaFormPacket p,FriendlyByteBuf b){b.m_130077_(p.caster);b.writeInt(p.entity);b.m_130070_(p.dimension);b.writeLong(p.session).writeLong(p.tick).writeByte(p.phase).writeBoolean(p.hover).writeInt(p.age).writeInt(p.chargeTicks).writeInt(p.accepted);OmegaStatePacket.vector(b,p.position);OmegaStatePacket.vector(b,p.velocity);b.writeDouble(p.speed).writeDouble(p.rise).writeBoolean(p.originalGravity).writeBoolean(p.reset).writeBoolean(p.closed);}
    static OmegaFormPacket decode(FriendlyByteBuf b){return new OmegaFormPacket(b.m_130259_(),b.readInt(),b.m_130136_(128),b.readLong(),b.readLong(),b.readByte(),b.readBoolean(),b.readInt(),b.readInt(),b.readInt(),OmegaStatePacket.vector(b),OmegaStatePacket.vector(b),b.readDouble(),b.readDouble(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
    static void handle(OmegaFormPacket p,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->OmegaFormClient.state(p)));c.setPacketHandled(true);}
}
