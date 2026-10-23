/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordsTest {

    @Test
    void replacesOnlyWholeWords() {
        assertEquals("worldLocation&&true", Words.replace("worldLocation&&location", "location", "true"));
    }

    @Test
    void ignoresCase() {
        assertEquals("!true||false", Words.replace("!LOCATION||false", "location", "true"));
    }

    @Test
    void leavesPlaceholderNamesAlone() {
        assertEquals(
                "%player_time%>5&&true",
                Words.pattern(List.of("time")).matcher("%player_time%>5&&time").replaceAll("true"));
    }

    @Test
    void patternPrefersLongerWords() {
        assertEquals(
                "x&&x",
                Words.pattern(List.of("location", "worldLocation"))
                        .matcher("location&&worldLocation")
                        .replaceAll("x"));
    }
}
