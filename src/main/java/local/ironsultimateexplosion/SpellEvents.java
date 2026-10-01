package local.ironsultimateexplosion;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SpellEvents {
    private static final Map<UUID, Float> PRE_CAST_MANA = new HashMap<>();

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!GrandExplosionSpell.ID.toString().equals(event.getSpellId())) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getCastSource() != CastSource.SPELLBOOK &&
                (event.getCastSource() != CastSource.SWORD ||
                 !player.m_21205_().m_150930_(ModItems.CINDERSTAR_STAFF.get()) ||
                 !io.redspace.ironsspellbooks.config.ServerConfigs.SWORDS_CONSUME_MANA.get())) return;
        float current = MagicData.getPlayerMagicData(player).getMana();
        PRE_CAST_MANA.put(player.m_20148_(), current);
        event.setManaCost((int)Math.ceil(Math.max(0, Math.min(current, Integer.MAX_VALUE))));
    }

    static float takePreCastMana(ServerPlayer player) {
        Float value = PRE_CAST_MANA.remove(player.m_20148_());
        return value == null ? 0 : value;
    }

    private SpellEvents() {}
}
