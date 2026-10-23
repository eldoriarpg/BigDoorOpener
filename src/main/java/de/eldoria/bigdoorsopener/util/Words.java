/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) EldoriaRPG Team and Contributor
 */
package de.eldoria.bigdoorsopener.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Replaces whole words in evaluators, ignoring case. A word only matches where it is not part of a longer word, so
 * {@code location} does not match inside {@code worldLocation}.
 */
public final class Words {
    private Words() {
    }

    /**
     * Builds a pattern matching any of the words as a whole word.
     *
     * @param words words to match
     * @return case insensitive pattern
     */
    public static Pattern pattern(Collection<String> words) {
        String alternatives = words.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return Pattern.compile("\\b(?:" + alternatives + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    /**
     * Replaces a whole word.
     *
     * @param text        text to replace in
     * @param word        word to replace
     * @param replacement replacement of the word
     * @return text with the word replaced
     */
    public static String replace(String text, String word, String replacement) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }
}
