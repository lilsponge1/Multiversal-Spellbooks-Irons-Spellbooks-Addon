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
    static void start(AbstractClientPlayer player) {
        if(LAYERS.containsKey(player.m_20148_())) return;
        var data=PlayerAnimationRegistry.getAnimation(ID); if(data==null) return;
        ModifierLayer<KeyframeAnimationPlayer> layer=new ModifierLayer<>();
        layer.setAnimation(new KeyframeAnimationPlayer(data));
        PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(2500,layer);
        LAYERS.put(player.m_20148_(),layer); PLAYERS.put(player.m_20148_(),player);
    }
    static void stop(UUID id) {
        var layer=LAYERS.remove(id); var player=PLAYERS.remove(id);
        if(layer!=null) { layer.setAnimation(null); if(player!=null) PlayerAnimationAccess.getPlayerAnimLayer(player).removeLayer(layer); }
    }
    static void clear() { for(UUID id:new ArrayList<>(LAYERS.keySet())) stop(id); }
    private OmegaAnimations() {}
}
