package de.lpchat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrideFlagTest {
    @Test void transUsesAllFiveStripesAndMatchesSeparator() {
        var name = NameStyle.flag(PrideFlag.TRANS).nameAndSeparator("ABCDE");
        assertEquals("ABCDE >>  ", ChatFormat.plain(name));
        String[] expected = {"#5bcefa", "#f5a9b8", "#ffffff", "#f5a9b8", "#5bcefa", "#5bcefa"};
        for (int i = 0; i < expected.length; i++) assertEquals(NameStyle.color(expected[i]).value(), name.children().get(i).color().value());
    }

    @Test void gayMenAndLesbianUseTheirSevenDistinctStripes() {
        for (PrideFlag flag : new PrideFlag[]{PrideFlag.GAY_MEN, PrideFlag.LESBIAN}) {
            var name = NameStyle.flag(flag).nameAndSeparator("ABCDEFG");
            assertEquals(7, flag.colors().size());
            for (int i = 0; i < 7; i++) assertEquals(flag.colors().get(i), name.children().get(i).color());
            assertEquals(0xffffff, name.children().get(3).color().value());
        }
    }

    @Test void everyFlagPersistsAndKeepsShortNamesValid() {
        for (PrideFlag flag : PrideFlag.values()) {
            NameStyle style = NameStyle.flag(flag);
            assertEquals(style, NameStyle.fromStored(style.stored()));
            assertFalse(style.gradient());
            assertEquals("X >>  ", ChatFormat.plain(style.nameAndSeparator("X")));
            assertEquals(flag.colors().getFirst(), style.nameAndSeparator("X").children().getLast().color());
            assertEquals("A\uD83D\uDE00B >>  ", ChatFormat.plain(style.nameAndSeparator("A\uD83D\uDE00B")));
        }
        assertEquals(NameStyle.DEFAULT, NameStyle.fromStored("flag:invalid"));
        assertEquals(PrideFlag.GAY_MEN, PrideFlag.find("gay men"));
        assertEquals(PrideFlag.NONBINARY, PrideFlag.find("non-binary"));
    }
}
