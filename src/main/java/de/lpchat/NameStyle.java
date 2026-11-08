package de.lpchat;

import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

record NameStyle(TextColor start, TextColor end, PrideFlag flag) {
    static final NameStyle DEFAULT = new NameStyle(NamedTextColor.GRAY, NamedTextColor.GRAY);

    NameStyle {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
    }

    NameStyle(TextColor start, TextColor end) { this(start, end, null); }

    static NameStyle flag(PrideFlag flag) {
        return new NameStyle(flag.colors().getFirst(), flag.colors().getLast(), flag);
    }

    static TextColor color(String value) {
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.matches("#[0-9a-f]{6}") ? TextColor.fromHexString(normalized)
                : NamedTextColor.NAMES.value(normalized);
    }

    static NameStyle fromStored(String stored) {
        if (stored == null) return DEFAULT;
        if (stored.startsWith("flag:")) {
            PrideFlag flag = PrideFlag.find(stored.substring(5));
            return flag == null ? DEFAULT : flag(flag);
        }
        String[] colors = stored.split(":", -1);
        if (colors.length != 2) return DEFAULT;
        TextColor start = color(colors[0]);
        TextColor end = color(colors[1]);
        return start == null || end == null ? DEFAULT : new NameStyle(start, end);
    }

    String stored() { return flag != null ? "flag:" + flag.id() : start.asHexString() + ":" + end.asHexString(); }

    boolean gradient() { return flag == null && start.value() != end.value(); }

    Component nameAndSeparator(String name) {
        Component result = Component.empty().color(start);
        int[] characters = name.codePoints().toArray();
        for (int index = 0; index < characters.length; index++) {
            float position = characters.length < 2 ? 0 : (float) index / (characters.length - 1);
            TextColor color = flag == null ? TextColor.lerp(position, start, end) : flag.colorAt(index, characters.length);
            result = result.append(Component.text(new String(Character.toChars(characters[index])), color));
        }
        TextColor separator = flag != null && characters.length > 0 ? flag.colorAt(characters.length - 1, characters.length) : end;
        return result.append(Component.text(" >>  ", separator));
    }
}
