package com.example.mealsync.util;

import java.security.SecureRandom;
import java.util.Locale;

public final class InvitationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private InvitationCodeGenerator() {
    }

    public static String generateCode() {
        int number = RANDOM.nextInt(1000000);
        return String.format(Locale.US, "%06d", number);
    }
}
