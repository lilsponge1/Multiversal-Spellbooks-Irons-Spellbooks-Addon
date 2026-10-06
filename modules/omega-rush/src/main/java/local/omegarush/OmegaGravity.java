package local.omegarush;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
/** Shared ownership: Rush must not capture a temporary hover value as normal gravity. */
public final class OmegaGravity {
    private static final String KEY="irons_omega_rush.sharedGravity";
    private static final Map<UUID,Lease> LEASES=new HashMap<>();
    private static final class Lease { final boolean original; final Set<String> owners=new HashSet<>(); Lease(boolean o){original=o;} }
    public static boolean original(ServerPlayer p){Lease l=LEASES.get(p.m_20148_());return l==null?p.m_20068_():l.original;}
    public static void acquire(ServerPlayer p,String owner){
        Lease l=LEASES.computeIfAbsent(p.m_20148_(),k->new Lease(p.m_20068_()));l.owners.add(owner);
        p.getPersistentData().m_128379_(KEY,l.original);p.m_20242_(true);p.m_183634_();
    }
    public static void release(ServerPlayer p,String owner){
        Lease l=LEASES.get(p.m_20148_());if(l==null)return;l.owners.remove(owner);
        if(l.owners.isEmpty()){LEASES.remove(p.m_20148_());p.m_20242_(l.original);p.getPersistentData().m_128473_(KEY);}else p.m_20242_(true);
    }
    public static void recover(ServerPlayer p){
        LEASES.remove(p.m_20148_());if(p.getPersistentData().m_128441_(KEY)){p.m_20242_(p.getPersistentData().m_128471_(KEY));p.getPersistentData().m_128473_(KEY);}
    }
    private OmegaGravity(){}
}
