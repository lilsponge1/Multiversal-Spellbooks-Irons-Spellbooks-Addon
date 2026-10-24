package local.kischool;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;

/** Preserve Thundercrash movement ownership when that optional addon is installed. */
final class KiCompatibility {
    private static boolean resolved;
    private static Method owns;
    static boolean owns(Entity entity) {
        if (!resolved) {
            resolved = true;
            try { owns = Class.forName("local.ironsultimateexplosion.ThundercrashManager").getMethod("owns", Entity.class); }
            catch (ClassNotFoundException | NoSuchMethodException ignored) { /* Optional addon absent. */ }
        }
        if (owns == null) return false;
        try { return Boolean.TRUE.equals(owns.invoke(null, entity)); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot query Thundercrash movement ownership", error); }
    }
    private KiCompatibility() {}
}
