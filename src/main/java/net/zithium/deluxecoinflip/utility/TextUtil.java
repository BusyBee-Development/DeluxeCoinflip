/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import net.zithium.library.utils.ColorUtil;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final String[] SUFFIXES = {"", "k", "M", "B", "T"};
    private static final int SHORT_MAX_LEN = 5;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private static final Pattern COLOR_SIMPLE      = Pattern.compile("(?i)(?<!&r|§r)([&§][0-9a-f])");
    private static final Pattern COLOR_HEX_HASH    = Pattern.compile("(?i)(?<!&r|§r)&#([0-9a-f]{6})");
    private static final Pattern COLOR_HEX_SECTION = Pattern.compile("(?i)(?<!&r|§r)([&§]x(?:[&§][0-9a-f]){6})");

    private static final char NBSP = '\u00A0';

    public static String format(double number) {
        String repr = new DecimalFormat("##0E0").format(number);
        int expDigit = Character.getNumericValue(repr.charAt(repr.length() - 1));
        int suffixIndex = Math.max(0, Math.min(expDigit / 3, SUFFIXES.length - 1));
        repr = repr.replaceAll("E\\d", SUFFIXES[suffixIndex]);

        while (repr.length() > SHORT_MAX_LEN || repr.matches("\\d+\\.[a-zA-Z]")) {
            repr = repr.substring(0, repr.length() - 2) + repr.substring(repr.length() - 1);
        }

        return repr;
    }

    public static String numberFormat(long amount) {
        return NUMBER_FORMAT.format(amount);
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalized = enforceColorResets(preserveLeadingIndent(input));
        return ColorUtil.color(normalized);
    }

    public static List<String> color(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return lines;
        }

        for (int i = 0; i < lines.size(); i++) {
            String original = Objects.toString(lines.get(i), "");
            String stripped = stripLegacyColors(original);

            if (stripped.isEmpty()) {
                lines.set(i, ColorUtil.color("&r" + NBSP));
            } else {
                String normalized = enforceColorResets(preserveLeadingIndent(original));
                lines.set(i, ColorUtil.color(normalized));
            }
        }

        return lines;
    }

    /**
     * Join a list of lines for chat: preserves "middle"/top/bottom blank lines
     * (exactly one line each), keeps leading spaces/tabs, and prevents
     * style bleed by injecting resets before color changes.
     */
    public static String fromList(List<?> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        List<String> rendered = new ArrayList<>(lines.size());
        for (Object o : lines) {
            String original = Objects.toString(o, "");
            String stripped = stripLegacyColors(original);

            if (stripped.isEmpty()) {
                rendered.add("&r" + NBSP);
            } else {
                rendered.add(enforceColorResets(preserveLeadingIndent(original)));
            }
        }

        return String.join("\n", rendered);
    }

    public static boolean isBuiltByBit() {
        final String token = new String("%__FILEHASH__%".toCharArray());
        return !token.equals("%%__FILEHASH__%%");
    }

    public static boolean isValidDownload() {
        final String token = new String("%__USER__%".toCharArray());
        return !token.equals("%%__USER__%%");
    }

    public static String enforceColorResets(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String out = COLOR_SIMPLE.matcher(input).replaceAll("&r$1");
        out = COLOR_HEX_HASH.matcher(out).replaceAll("&r&#$1");
        out = COLOR_HEX_SECTION.matcher(out).replaceAll("&r$1");
        return out.replaceAll("(?i)(?:[&§]r){2,}", "&r");
    }

    private static String stripLegacyColors(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String out = input.replaceAll("(?i)[§&][0-9A-FK-OR]", "");
        out = out.replaceAll("(?i)&#[0-9A-F]{6}", "");
        out = out.replaceAll("(?i)[§&]x(?:[§&][0-9A-F]){6}", "");
        return out;
    }

    private static String preserveLeadingIndent(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        int i = 0;
        StringBuilder lead = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ') {
                lead.append(NBSP);
            } else if (c == '\t') {
                lead.append(NBSP).append(NBSP).append(NBSP).append(NBSP);
            } else {
                break;
            }

            i++;
        }

        if (lead.isEmpty()) {
            return s;
        }

        return lead.append(s.substring(i)).toString();
    }
}
