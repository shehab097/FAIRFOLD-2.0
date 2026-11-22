package com.fairfold.screening;

public enum Verdict {
    met, partial, not_met;

    public static Verdict parse(String value) {
        if (value == null) {
            return not_met;
        }
        try {
            return valueOf(value.strip().toLowerCase());
        } catch (IllegalArgumentException e) {
            return not_met;
        }
    }
}
