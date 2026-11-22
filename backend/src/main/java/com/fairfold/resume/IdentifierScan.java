package com.fairfold.resume;

import java.util.Locale;

/**
 * Final gate before any candidate text leaves the server for an AI provider.
 * Blocks the call when a known identifier still appears.
 */
public final class IdentifierScan {

    private IdentifierScan() {
    }

    public static boolean leaks(String text, ResumeStripper.Identifiers identifiers) {
        if (text == null || text.isBlank() || identifiers == null) {
            return false;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        if (contains(haystack, identifiers.fullName())) {
            return true;
        }
        for (String token : new String[]{identifiers.firstName(), identifiers.lastName()}) {
            if (token != null) {
                for (String word : token.trim().split("\\s+")) {
                    if (word.length() >= 3 && contains(haystack, word)) {
                        return true;
                    }
                }
            }
        }
        if (contains(haystack, identifiers.email())) {
            return true;
        }
        if (identifiers.phone() != null) {
            String digits = identifiers.phone().replaceAll("\\D", "");
            if (digits.length() >= 7) {
                String tail = digits.substring(digits.length() - 7);
                if (haystack.replaceAll("\\D", "").contains(tail)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(String haystack, String value) {
        return value != null && value.trim().length() >= 3
                && haystack.contains(value.trim().toLowerCase(Locale.ROOT));
    }
}
