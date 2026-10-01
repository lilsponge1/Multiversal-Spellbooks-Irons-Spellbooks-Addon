package local.ironsultimateexplosion;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ThundercrashStatePacket(UUID caster, int entity, String dimension, long session, long tick,
        byte phase, int remaining, int age, int acceptedInput, Vec3 position, Vec3 previous, Vec3 velocity,
        Vec3 launchLook, double speed, double steering, double lift, int launchTicks, boolean guided, boolean originalGravity) {
    static void vector(FriendlyByteBuf b, Vec3 v) { b.writeDouble(v.f_82479_).writeDouble(v.f_82480_).writeDouble(v.f_82481_); }
    static Vec3 vector(FriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    static void encode(ThundercrashStatePacket p, FriendlyByteBuf b) {
        b.m_130077_(p.caster); b.writeInt(p.entity); b.m_130070_(p.dimension); b.writeLong(p.session).writeLong(p.tick);
        b.writeByte(p.phase).writeInt(p.remaining).writeInt(p.age).writeInt(p.acceptedInput);
        vector(b,p.position); vector(b,p.previous); vector(b,p.velocity); vector(b,p.launchLook);
        b.writeDouble(p.speed).writeDouble(p.steering).writeDouble(p.lift).writeInt(p.launchTicks);
        b.writeBoolean(p.guided).writeBoolean(p.originalGravity);
    }
    static ThundercrashStatePacket decode(FriendlyByteBuf b) {
        return new ThundercrashStatePacket(b.m_130259_(), b.readInt(), b.m_130136_(128), b.readLong(), b.readLong(),
                b.readByte(), b.readInt(), b.readInt(), b.readInt(), vector(b), vector(b), vector(b), vector(b),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(), b.readBoolean(), b.readBoolean());
    }
    static void handle(ThundercrashStatePacket p, Supplier<NetworkEvent.Context> supplier) {
        var c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ThundercrashClient.state(p)));
        c.setPacketHandled(true);
    }
}
