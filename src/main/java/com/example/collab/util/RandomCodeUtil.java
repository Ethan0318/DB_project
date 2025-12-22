package com.example.collab.util;

import java.security.SecureRandom;

public final class RandomCodeUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomCodeUtil() {
    }

    public static String numericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
