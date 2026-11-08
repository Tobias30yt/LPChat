package de.lpchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChatFormatTest {
    @Test void teamRankNameAndExactlyTwoSpaces() {
        Component header = ChatFormat.header(ChatFormat.legacy("&aTeam"), ChatFormat.prefix("&4Supporter ", false), "Tobias30");
        assertEquals("(Team) Supporter Tobias30 >>  Yo was geht", ChatFormat.plain(ChatFormat.render(header,
                Component.text("Yo was geht"), NamedTextColor.WHITE)));
    }

    @Test void missingTeamAndRankLeaveNoExtraSeparators() {
        assertEquals("Tobias30 >>  Text", ChatFormat.plain(ChatFormat.render(ChatFormat.header(Component.empty(),
                Component.empty(), "Tobias30"), Component.text("Text"), NamedTextColor.WHITE)));
    }

    @Test void prefixFormatsSupportColors() {
        assertEquals(NamedTextColor.DARK_RED, ChatFormat.prefix("&4Supporter", false).color());
        assertEquals(NamedTextColor.DARK_RED, ChatFormat.prefix("\u00a74Supporter", false).color());
        assertEquals(NamedTextColor.DARK_RED, ChatFormat.prefix("<dark_red>Supporter</dark_red>", true).color());
        assertEquals("Supporter", ChatFormat.plain(ChatFormat.prefix("&#123456Supporter", false)));
    }

    @Test void playerMarkupRemainsLiteralAndColorsDoNotBleed() {
        Component header = ChatFormat.header(Component.empty(), ChatFormat.prefix("&4&lSupporter", false), "Tobias30");
        Component message = Component.text("<red>Hallo &4");
        Component rendered = ChatFormat.render(header, message, NamedTextColor.AQUA);
        assertEquals("Supporter Tobias30 >>  <red>Hallo &4", ChatFormat.plain(rendered));
        Component player = rendered.children().get(rendered.children().size() - 2);
        Component body = rendered.children().getLast();
        assertEquals(NamedTextColor.GRAY, player.color());
        assertEquals(NamedTextColor.AQUA, body.color());
        assertEquals(message, body.children().getFirst());
    }
}
