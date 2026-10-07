package local.omegarush;
import dev.kosmx.playerAnim.api.layered.*;
import dev.kosmx.playerAnim.minecraftApi.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Render-only forward flight posture; never changes pose, hitbox, or camera mode. */
public final class OmegaAnimations {
    private static final ResourceLocation ID=new ResourceLocation(OmegaMod.ID,"omega_rush_flight");
    private static final Map<UUID,ModifierLayer<KeyframeAnimationPlayer>> LAYERS=new HashMap<>();
    private static final Map<UUID,AbstractClientPlayer> PLAYERS=new HashMap<>();
    private static final Map<UUID,ModifierLayer<KeyframeAnimationPlayer>> CHARGES=new HashMap<>();
    private static final Map<UUID,AbstractClientPlayer> CHARGE_PLAYERS=new HashMap<>();
    static void charge(AbstractClientPlayer p,int duration){
        if(CHARGES.containsKey(p.m_20148_()))return;var layer=new ModifierLayer<KeyframeAnimationPlayer>();
        layer.setAnimation(new KeyframeAnimationPlayer(OmegaChargeAnimation.create(duration)));
        PlayerAnimationAccess.getPlayerAnimLayer(p).addAnimLayer(2600,layer);CHARGES.put(p.m_20148_(),layer);CHARGE_PLAYERS.put(p.m_20148_(),p);
    }
    static void stopCharge(UUID id){var layer=CHARGES.remove(id);var p=CHARGE_PLAYERS.remove(id);if(layer!=null){layer.setAnimation(null);if(p!=null)PlayerAnimationAccess.getPlayerAnimLayer(p).removeLayer(layer);}}
    static void start(AbstractClientPlayer player) {
        if(LAYERS.containsKey(player.m_20148_())) return;
        var data=PlayerAnimationRegistry.getAnimation(ID); if(data==null) return;
        ModifierLayer<KeyframeAnimationPlayer> layer=new ModifierLayer<>();
        layer.setAnimation(new KeyframeAnimationPlayer(data));
        PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(2500,layer);
        LAYERS.put(player.m_20148_(),layer); PLAYERS.put(player.m_20148_(),player);
    }
    static void stop(UUID id) {
        var player=PLAYERS.get(id);
        if(player!=null&&(OmegaClient.owns(player)||OmegaFormClient.hovering(player)))return;
        removeLayer(id);
    }
    private static void removeLayer(UUID id) {
        var layer=LAYERS.remove(id); var player=PLAYERS.remove(id);
        if(layer!=null) { layer.setAnimation(null); if(player!=null) PlayerAnimationAccess.getPlayerAnimLayer(player).removeLayer(layer); }
    }
    static void clear() { for(UUID id:new ArrayList<>(LAYERS.keySet())) removeLayer(id);for(UUID id:new ArrayList<>(CHARGES.keySet()))stopCharge(id); }
    private OmegaAnimations() {}
}
