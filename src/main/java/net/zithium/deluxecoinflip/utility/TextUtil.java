/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.utility;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class TextUtil {

    private static final String[] SUFFIXES = {"", "k", "M", "B", "T"};
    private static final int SHORT_MAX_LEN = 5;
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    /**
     * Shortens large numbers using k/M/B/T with up to 5 total characters.
     * Example: 15320 -> "15k", 1_234_567 -> "1.2M"
     */
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

    /**
     * Joins a list of lines with newlines. Empty/colored-only lines
     * produce a literal "&r" reset on their own line (so the caller
     * can colorize later).
     */
    public static String fromList(List<?> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String original = Objects.toString(lines.get(i), "");
            String stripped = stripLegacyColors(original);

            if (stripped.isEmpty()) {
                out.append("\n&r");
            } else {
                out.append(original);
                if (i + 1 < lines.size()) {
                    out.append("\n");
                }
            }
        }

        return out.toString();
    }

    public static boolean isBuiltByBit() {
        final String token = new String("%__FILEHASH__%".toCharArray());
        return !token.equals("%%__FILEHASH__%%");
    }

    public static boolean isValidDownload() {
        final String token = new String("%__USER__%".toCharArray());
        return !token.equals("%%__USER__%%");
    }

    /**
     * Removes legacy color codes of the form §x / &x (0-9, a-f, k-o, r, x).
     * Lightweight replacement for ChatColor.stripColor.
     */
    private static String stripLegacyColors(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        return input.replaceAll("(?i)[§&][0-9A-FK-ORX]", "");
    }
}
