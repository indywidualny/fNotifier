package org.indywidualni.fnotifier;

public enum LedColor {

    WHITE(0), RED(1), GREEN(2), BLUE(3), CYAN(4), MAGENTA(5);

    private final int value;

    LedColor(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LedColor toEnum(String string) {
        try {
            return valueOf(string.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // color not found
            return WHITE;
        }
    }

}
