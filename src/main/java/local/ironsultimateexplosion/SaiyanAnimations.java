package local.ironsultimateexplosion;

import dev.kosmx.playerAnim.api.layered.*;
import dev.kosmx.playerAnim.minecraftApi.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

final class SaiyanAnimations {
    private record Layer(AbstractClientPlayer player, ModifierLayer<KeyframeAnimationPlayer> animation) {}
    private static final Map<UUID, Layer> LAYERS = new HashMap<>();
    static void start(AbstractClientPlayer player) {
        if (LAYERS.containsKey(player.m_20148_())) return;
        var data = PlayerAnimationRegistry.getAnimation(new ResourceLocation(GrandExplosionMod.ID, "saiyan_charge"));
        if (data == null) return;
        ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>(); layer.setAnimation(new KeyframeAnimationPlayer(data));
        PlayerAnimationAccess.getPlayerAnimLayer(player).addAnimLayer(2400, layer); LAYERS.put(player.m_20148_(), new Layer(player, layer));
    }
    static void stop(UUID id) { var layer = LAYERS.remove(id); if (layer != null) { layer.animation.setAnimation(null); PlayerAnimationAccess.getPlayerAnimLayer(layer.player).removeLayer(layer.animation); } }
    private SaiyanAnimations() {}
}
