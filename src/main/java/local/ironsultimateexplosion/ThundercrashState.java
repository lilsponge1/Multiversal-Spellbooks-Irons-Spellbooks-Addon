package local.ironsultimateexplosion;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class ThundercrashState {
    static final byte CHARGE = 0, LAUNCH = 1, FLIGHT = 2, END = 3;
    final ServerPlayer player;
    final String dimension;
    final long session;
    final boolean originalGravity, guided;
    final double speed, steering, lift, radius, damage, edgeDamage, knockback, range;
    final int spellLevel, launchTicks, duration;
    Vec3 position, previous, velocity = Vec3.f_82478_, launchLook;
    float yaw, pitch;
    int age, inputSequence = -1, inputTick = -1, packetsThisTick;
    double travelled;
    byte phase = CHARGE;
    boolean terminal;
    ThundercrashState(ServerPlayer player, long session, int spellLevel, float power) {
        this.player = player; this.session = session;
        this.spellLevel = spellLevel;
        dimension = player.m_9236_().m_46472_().m_135782_().toString();
        originalGravity = player.m_20068_();
        position = previous = player.m_20182_();
        yaw = player.m_146908_(); pitch = player.m_146909_();
        launchLook = ThundercrashMovement.look(yaw, pitch);
        speed = ThundercrashConfig.SPEED.get(); steering = ThundercrashConfig.STEERING.get();
        lift = ThundercrashConfig.LIFT.get(); radius = ThundercrashConfig.RADIUS.get();
        damage = Math.max(0, Math.min(10000, power * ThundercrashConfig.DAMAGE.get()));
        edgeDamage = ThundercrashConfig.EDGE_DAMAGE.get();
        knockback = ThundercrashConfig.KNOCKBACK.get(); range = ThundercrashConfig.RANGE.get();
        guided = ThundercrashConfig.GUIDED.get();
        launchTicks = ThundercrashConfig.LAUNCH.get(); duration = ThundercrashConfig.DURATION.get();
    }
    ThundercrashStatePacket packet(long tick) {
        return new ThundercrashStatePacket(player.m_20148_(), player.m_19879_(), dimension, session, tick, phase,
                Math.max(0, duration + launchTicks - age), age, inputSequence, position, previous, velocity,
                launchLook, speed, steering, lift, launchTicks, guided, originalGravity);
    }
}
