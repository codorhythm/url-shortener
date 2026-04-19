package com.shortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = CHARACTERS.length(); // 62

    /**
     * Encodes a numeric ID to a Base62 string.
     * Example: 125 -> "cb", 1000000 -> "4c92"
     *
     * Why Base62? URL-safe (no +, /, = like Base64).
     * 7 characters gives us 62^7 = ~3.5 trillion unique URLs.
     */
    public String encode(long id) {
        if (id == 0) return String.valueOf(CHARACTERS.charAt(0));

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(CHARACTERS.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    public long decode(String shortCode) {
        long result = 0;
        for (char c : shortCode.toCharArray()) {
            result = result * BASE + CHARACTERS.indexOf(c);
        }
        return result;
    }
}