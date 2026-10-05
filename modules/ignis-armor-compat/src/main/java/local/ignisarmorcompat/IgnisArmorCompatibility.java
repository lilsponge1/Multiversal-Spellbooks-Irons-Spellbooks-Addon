package local.ignisarmorcompat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(IgnisArmorCompatibility.MOD_ID)
public final class IgnisArmorCompatibility {
    public static final String MOD_ID = "ignis_armor_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IgnisArmorCompatibility() {
        if (Boolean.getBoolean("ignis_armor_compat.diagnostics"))
            MinecraftForge.EVENT_BUS.register(new ArmorDiagnostics());
    }
}
