package local.ironsultimateexplosion;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Small, rate-limited cosmetic bursts. No damage or motion is encoded. */
public record SaiyanCombatPacket(UUID player,String dimension,Vec3 position,byte kind) {
    public static final byte HIT=0,SPELL=1,JUMP=2,LAND=3;
    static void encode(SaiyanCombatPacket p,FriendlyByteBuf b) { b.m_130077_(p.player); b.m_130070_(p.dimension); b.writeDouble(p.position.f_82479_).writeDouble(p.position.f_82480_).writeDouble(p.position.f_82481_).writeByte(p.kind); }
    static SaiyanCombatPacket decode(FriendlyByteBuf b) { return new SaiyanCombatPacket(b.m_130259_(),b.m_130136_(128),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readByte()); }
    static void handle(SaiyanCombatPacket p,Supplier<NetworkEvent.Context> context) { var c=context.get(); c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->SaiyanVisuals.combat(p))); c.setPacketHandled(true); }
}
