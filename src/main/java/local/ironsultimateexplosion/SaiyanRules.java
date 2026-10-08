package local.ironsultimateexplosion;

/** Pure decisions shared by the controller and its regression checks. All durations use server ticks. */
public final class SaiyanRules {
    public enum Form { BASE, SUPER_SAIYAN_1, SUPER_SAIYAN_2, SUPER_SAIYAN_3 }
    public enum Release { CANCEL, ASCEND, POWER_DOWN }
    public record Drain(Form form, double amount) {}
    public static Release release(Form form, int held, int shortLimit, int downLimit) {
        if (form == Form.BASE) return Release.CANCEL;
        if (held >= downLimit) return Release.POWER_DOWN;
        return (form == Form.SUPER_SAIYAN_1 || form == Form.SUPER_SAIYAN_2) && held < Math.min(shortLimit, downLimit)
                ? Release.ASCEND : Release.CANCEL;
    }
    public static Drain drain(Form form, double mana, double first, double second) {
        if (!Double.isFinite(mana) || mana < 0) return new Drain(Form.BASE, 0);
        if (form == Form.SUPER_SAIYAN_3) return new Drain(Form.BASE, 0);
        if (form == Form.SUPER_SAIYAN_2 && mana >= second) return new Drain(form, second);
        if (form != Form.BASE && mana >= first) return new Drain(Form.SUPER_SAIYAN_1, first);
        return new Drain(Form.BASE, 0);
    }
    public static Drain drainThird(double mana, double perTick) {
        return Double.isFinite(mana) && mana > 0 && Double.isFinite(perTick) && perTick >= 0 && mana >= perTick
                ? new Drain(Form.SUPER_SAIYAN_3, perTick) : new Drain(Form.BASE, 0);
    }
    private SaiyanRules() {}
}
