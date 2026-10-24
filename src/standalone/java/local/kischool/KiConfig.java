package local.kischool;

import net.minecraftforge.common.ForgeConfigSpec;

/** Uses ki_school-server/client.toml, without changing another addon's configs. */
public final class KiConfig {
    public static final ForgeConfigSpec SERVER, CLIENT;
    public static final ForgeConfigSpec.IntValue PARTICLE_QUALITY;
    static {
        var server = new ForgeConfigSpec.Builder();
        SaiyanConfig.server(server);
        SERVER = server.build();
        var client = new ForgeConfigSpec.Builder();
        PARTICLE_QUALITY = client.defineInRange("particleQuality", 2, 0, 2);
        SaiyanConfig.client(client);
        CLIENT = client.build();
    }
    private KiConfig() {}
}
