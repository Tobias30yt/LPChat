package de.lpchat;

import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorPolicyTest {
    private final List<String> colors = List.of("white", "gray", "green", "aqua", "yellow", "gold");

    @Test void highestPermissionWinsWithoutAddingCounts() {
        var permissions = Set.of("chatcolor.use", "chatcolor.2", "chatcolor.5");
        int count = ColorPolicy.unlocked(permissions::contains, colors.size());
        assertEquals(5, count);
        assertTrue(ColorPolicy.allowed("yellow", colors, count));
        assertFalse(ColorPolicy.allowed("gold", colors, count));
    }

    @Test void wildcardAndLargeTierAreCappedByPalette() {
        assertEquals(6, ColorPolicy.unlocked(Set.of("chatcolor.use", "chatcolor.*")::contains, 6));
        assertEquals(6, ColorPolicy.unlocked(Set.of("chatcolor.use", "chatcolor.16")::contains, 6));
    }

    @Test void missingOrRevokedPermissionFallsBack() {
        assertEquals(0, ColorPolicy.unlocked(Set.of("chatcolor.5")::contains, 6));
        assertEquals(0, ColorPolicy.unlocked(Set.of("chatcolor.use")::contains, 6));
        assertEquals(NamedTextColor.WHITE, ColorPolicy.effective("yellow", colors, 2, NamedTextColor.WHITE));
        assertEquals(NamedTextColor.WHITE, ColorPolicy.effective(null, colors, 5, NamedTextColor.WHITE));
        assertEquals(NamedTextColor.WHITE, ColorPolicy.effective("invalid", colors, 5, NamedTextColor.WHITE));
        assertEquals(NamedTextColor.YELLOW, ColorPolicy.effective("yellow", colors, 5, NamedTextColor.WHITE));
    }
}
