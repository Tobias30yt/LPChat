package de.lpchat;

import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.format.NamedTextColor;

final class ColorPolicy {
    private ColorPolicy() {}

    static int unlocked(Predicate<String> permission, int size) {
        if (!permission.test("chatcolor.use")) return 0;
        if (permission.test("chatcolor.*")) return size;
        for (int count = 16; count >= 1; count--) {
            if (permission.test("chatcolor." + count)) return Math.min(count, size);
        }
        return 0;
    }

    static boolean allowed(String color, List<String> order, int unlocked) {
        int index = order.indexOf(color);
        return index >= 0 && index < unlocked;
    }

    static NamedTextColor effective(String selected, List<String> order, int unlocked,
                                    NamedTextColor fallback) {
        return selected != null && allowed(selected, order, unlocked)
                ? NamedTextColor.NAMES.value(selected) : fallback;
    }
}
