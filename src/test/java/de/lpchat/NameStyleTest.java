package de.lpchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameStyleTest {
    @Test void gradientHasExactEndpointsAndMatchingSeparator() {
        NameStyle style = new NameStyle(NamedTextColor.RED, NamedTextColor.BLUE);
        Component name = style.nameAndSeparator("Tobias30");
        assertEquals("Tobias30 >>  ", ChatFormat.plain(name));
        assertEquals(NamedTextColor.RED.value(), name.children().getFirst().color().value());
        assertEquals(NamedTextColor.BLUE.value(), name.children().get(7).color().value());
        assertEquals(NamedTextColor.BLUE.value(), name.children().getLast().color().value());
        assertNotEquals(NamedTextColor.RED.value(), name.children().get(3).color().value());
        assertNotEquals(NamedTextColor.BLUE.value(), name.children().get(3).color().value());
    }

    @Test void solidColorAlsoColorsSeparator() {
        Component name = new NameStyle(NamedTextColor.AQUA, NamedTextColor.AQUA).nameAndSeparator("Tobias30");
        assertTrue(name.children().stream().allMatch(c -> c.color().value() == NamedTextColor.AQUA.value()));
    }

    @Test void persistentEncodingRoundTripsAndInvalidDataFallsBack() {
        NameStyle original = new NameStyle(NameStyle.color("#123456"), NameStyle.color("#abcdef"));
        assertEquals(original, NameStyle.fromStored(original.stored()));
        for (String invalid : new String[]{"", "red", "red:bogus", "red:blue:green", "#f:red"}) {
            assertEquals(NameStyle.DEFAULT, NameStyle.fromStored(invalid));
        }
        assertEquals(NameStyle.DEFAULT, NameStyle.fromStored(null));
        assertEquals(NamedTextColor.RED, NameStyle.color("RED"));
        assertNull(NameStyle.color("<red>"));
    }

    @Test void gradientDoesNotLeakIntoMessageAndSupportsUnicodeNames() {
        NameStyle style = new NameStyle(NamedTextColor.RED, NamedTextColor.BLUE);
        Component header = ChatFormat.header(Component.empty(), Component.empty(), "A\uD83D\uDE00B", style);
        Component rendered = ChatFormat.render(header, Component.text("Test"), NamedTextColor.YELLOW);
        assertEquals("A\uD83D\uDE00B >>  Test", ChatFormat.plain(rendered));
        assertEquals(NamedTextColor.YELLOW, rendered.children().getLast().color());
        assertEquals(4, style.nameAndSeparator("A\uD83D\uDE00B").children().size());
        assertEquals("X >>  ", ChatFormat.plain(style.nameAndSeparator("X")));
    }
}
