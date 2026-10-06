package local.omegarush;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class OmegaState {
    static final byte CHARGE=0, FLIGHT=1, END=2;
    final ServerPlayer player;
    final String dimension;
    final long session;
    final boolean originalGravity;
    final double speed,steering,radius,spacing;
    final float damage;
    final int duration,chargeTicks;
    final OmegaPath path=new OmegaPath();
    Vec3 position,previous,velocity;
    float yaw,pitch;
    int age,inputSequence=-1,inputTick=-1,packetsThisTick;
    byte phase=CHARGE;
    boolean terminal;
    OmegaState(ServerPlayer p,long session,float damage,int chargeTicks) {
        player=p; this.session=session; this.damage=Math.max(0,Math.min(10000,damage));
        this.chargeTicks=Math.max(1,chargeTicks); duration=OmegaConfig.DURATION.get();
        dimension=OmegaManager.dimension(p); originalGravity=OmegaGravity.original(p);
        position=previous=p.m_20182_(); yaw=p.m_146908_(); pitch=p.m_146909_();
        speed=OmegaConfig.SPEED.get(); steering=OmegaConfig.STEERING.get();
        radius=OmegaConfig.RADIUS.get(); spacing=OmegaConfig.SPACING.get();
        velocity=OmegaMovement.look(yaw,pitch).m_82490_(speed);
    }
    OmegaStatePacket packet(long tick,boolean clearTrail) {
        return new OmegaStatePacket(player.m_20148_(),player.m_19879_(),dimension,session,tick,phase,
            phase==CHARGE?chargeTicks:Math.max(0,duration-age),age,inputSequence,position,previous,velocity,speed,steering,originalGravity,clearTrail,terminal);
    }
}
