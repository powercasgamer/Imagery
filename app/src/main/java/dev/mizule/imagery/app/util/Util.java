package dev.mizule.imagery.app.util;

/**
 * Libby's utility class.
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException("Util class.");
    }

    /**
     * Replaces the "{}" inside the provided string with a dot.
     *
     * @param str The string to replace
     * @return The string with "{}" replaced
     */
    public static String replaceWithDots(String str) {
        return str.replace("{}", ".");
    }

    /**
     * Convert a String of hex character to a byte array
     *
     * @param string The string to convert
     * @return The byte array
     */
    public static byte[] hexStringToByteArray(String string) {
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                                  + Character.digit(string.charAt(i+1), 16));
        }
        return data;
    }
}