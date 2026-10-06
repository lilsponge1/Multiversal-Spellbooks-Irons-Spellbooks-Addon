package local.omegarush;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
public record OmegaStatePacket(UUID caster,int entity,String dimension,long session,long tick,byte phase,
        int remaining,int age,int acceptedInput,Vec3 position,Vec3 previous,Vec3 velocity,double speed,double steering,boolean originalGravity,boolean clearTrail,boolean closed) {
    static void vector(FriendlyByteBuf b,Vec3 v) { b.writeDouble(v.f_82479_).writeDouble(v.f_82480_).writeDouble(v.f_82481_); }
    static Vec3 vector(FriendlyByteBuf b) { return new Vec3(b.readDouble(),b.readDouble(),b.readDouble()); }
    static void encode(OmegaStatePacket p,FriendlyByteBuf b) {
        b.m_130077_(p.caster); b.writeInt(p.entity); b.m_130070_(p.dimension); b.writeLong(p.session).writeLong(p.tick);
        b.writeByte(p.phase).writeInt(p.remaining).writeInt(p.age).writeInt(p.acceptedInput);
        vector(b,p.position); vector(b,p.previous); vector(b,p.velocity);
        b.writeDouble(p.speed).writeDouble(p.steering).writeBoolean(p.originalGravity).writeBoolean(p.clearTrail).writeBoolean(p.closed);
    }
    static OmegaStatePacket decode(FriendlyByteBuf b) {
        return new OmegaStatePacket(b.m_130259_(),b.readInt(),b.m_130136_(128),b.readLong(),b.readLong(),b.readByte(),
            b.readInt(),b.readInt(),b.readInt(),vector(b),vector(b),vector(b),b.readDouble(),b.readDouble(),b.readBoolean(),b.readBoolean(),b.readBoolean());
    }
    static void handle(OmegaStatePacket p,Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get(); c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->OmegaClient.state(p))); c.setPacketHandled(true);
    }
}
