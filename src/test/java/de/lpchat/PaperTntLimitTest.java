package de.lpchat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PaperTntLimitTest {
    public static class Settings { public int maxTntTicksPerTick = 100; public int untouched = 20; }
    public static class Invalid { public final int maxTntTicksPerTick = 100; }
    public static class WrongType { public long maxTntTicksPerTick = 100; }
    @Test void overridesExistingLimitAndIsIdempotent() throws Exception {
        Settings settings = new Settings();
        PaperTntLimit.setUnlimited(settings); PaperTntLimit.setUnlimited(settings);
        assertEquals(-1, settings.maxTntTicksPerTick); assertEquals(20, settings.untouched);
    }
    @Test void rejectsIncompatiblePaperSettings() {
        assertThrows(NoSuchFieldException.class, () -> PaperTntLimit.setUnlimited(new Invalid()));
        assertThrows(NoSuchFieldException.class, () -> PaperTntLimit.setUnlimited(new WrongType()));
        assertThrows(NoSuchFieldException.class, () -> PaperTntLimit.setUnlimited(new Object()));
    }
}
