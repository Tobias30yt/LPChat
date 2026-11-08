package de.lpchat;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.format.TextColor;

enum PrideFlag {
    TRANS("trans", "Trans", "5BCEFA", "F5A9B8", "FFFFFF", "F5A9B8", "5BCEFA"),
    GAY_MEN("gay_men", "Gay Men", "078D70", "26CEAA", "98E8C1", "FFFFFF", "7BADE2", "5049CC", "3D1A78"),
    LESBIAN("lesbian", "Lesbian", "D52D00", "EF7627", "FF9A56", "FFFFFF", "D162A4", "B55690", "A30262"),
    RAINBOW("rainbow", "Rainbow Pride", "E40303", "FF8C00", "FFED00", "008026", "004DFF", "750787"),
    BISEXUAL("bisexual", "Bisexual", "D60270", "D60270", "9B4F96", "0038A8", "0038A8"),
    PANSEXUAL("pansexual", "Pansexual", "FF218C", "FFD800", "21B1FF"),
    NONBINARY("nonbinary", "Nonbinary", "FFF430", "FFFFFF", "9C59D1", "2D2D2D"),
    ASEXUAL("asexual", "Asexual", "000000", "A3A3A3", "FFFFFF", "800080"),
    AROMANTIC("aromantic", "Aromantic", "3DA542", "A7D379", "FFFFFF", "A9A9A9", "000000"),
    AGENDER("agender", "Agender", "000000", "BCC4C7", "FFFFFF", "B7F684", "FFFFFF", "BCC4C7", "000000"),
    GENDERFLUID("genderfluid", "Genderfluid", "FF75A2", "FFFFFF", "BE18D6", "000000", "333EBD"),
    GENDERQUEER("genderqueer", "Genderqueer", "B57EDC", "FFFFFF", "4A8123"),
    INTERSEX("intersex", "Intersex (Farbpalette)", "FFD800", "7902AA", "FFD800");

    private final String id;
    private final String label;
    private final List<TextColor> colors;

    PrideFlag(String id, String label, String... hex) {
        this.id = id;
        this.label = label;
        colors = Arrays.stream(hex).map(value -> TextColor.fromHexString("#" + value)).toList();
    }

    String id() { return id; }
    String label() { return label; }
    List<TextColor> colors() { return colors; }

    TextColor colorAt(int index, int length) {
        int stripe = length < 2 ? 0 : Math.round((float) index * (colors.size() - 1) / (length - 1));
        return colors.get(stripe);
    }

    static PrideFlag find(String input) {
        String id = input.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        id = switch (id) {
            case "transgender" -> "trans";
            case "gay", "gaymen", "mlm" -> "gay_men";
            case "pride", "regenbogen" -> "rainbow";
            case "lesbisch" -> "lesbian";
            case "bi" -> "bisexual";
            case "pan" -> "pansexual";
            case "non_binary", "nb" -> "nonbinary";
            case "ace" -> "asexual";
            case "aro" -> "aromantic";
            default -> id;
        };
        for (PrideFlag flag : values()) if (flag.id.equals(id)) return flag;
        return null;
    }
}
