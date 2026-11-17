package local.omegarush;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class OmegaFormSpells {
    // Iron's treats selections prefixed with "spellbook" as mana-paying book casts.
    static final String RUSH_SLOT = "spellbook_irons_omega_rush_form";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addFormRush(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        var selection = event.getManager();
        if (OmegaFormManager.active(player)) {
            var rush = ModSpells.OMEGA_RUSH.get();
            if (selection.getAllSpells().stream().noneMatch(option -> option.spellData.getSpell() == rush)) {
                event.addSelectionOption(new SpellData(rush, 1), RUSH_SLOT, 0);
                return;
            }
        }

        var current = selection.getCurrentSelection();
        if (player instanceof ServerPlayer && RUSH_SLOT.equals(current.equipmentSlot) && current.index >= 0) {
            // Prevent Iron's from casting another spell while the old selection is in
            // flight (Form ended, or an equipped Rush replaced the temporary entry).
            event.addSelectionOption(SpellData.EMPTY, RUSH_SLOT, current.index);
        }
    }

    private OmegaFormSpells() {}
}
