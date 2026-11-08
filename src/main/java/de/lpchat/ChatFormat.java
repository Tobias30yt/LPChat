package de.lpchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class ChatFormat {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private ChatFormat() {}

    static Component legacy(String text) {
        return LEGACY.deserialize(text.replace('\u00a7', '&'));
    }

    static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    static Component prefix(String raw, boolean miniMessage) {
        if (raw == null || raw.isBlank()) return Component.empty();
        return miniMessage ? MiniMessage.miniMessage().deserialize(raw.strip()) : legacy(raw.strip());
    }

    static Component header(Component team, Component prefix, String playerName) {
        return header(team, prefix, playerName, NameStyle.DEFAULT);
    }

    static Component header(Component team, Component prefix, String playerName, NameStyle nameStyle) {
        Component header = Component.empty();
        if (!plain(team).isBlank()) {
            header = header.append(Component.text("(", NamedTextColor.GRAY))
                    .append(team).append(Component.text(") ", NamedTextColor.GRAY));
        }
        if (!plain(prefix).isBlank()) {
            header = header.append(prefix);
            if (!Character.isWhitespace(plain(prefix).charAt(plain(prefix).length() - 1))) {
                header = header.append(Component.space());
            }
        }
        return header.append(nameStyle.nameAndSeparator(playerName));
    }

    static Component render(Component header, Component message, NamedTextColor color) {
        // Keep the actual message component; never interpret player text as markup.
        return header.append(Component.empty().color(color).append(message));
    }
}
