package com.example.mealsync.util;

import com.example.mealsync.data.models.InvitationCode;

public final class InvitationCodeValidator {

    private InvitationCodeValidator() {
    }

    public static boolean isValidFormat(String code) {
        return code != null && code.trim().matches("\\d{6}");
    }

    public static boolean isValidAndActive(InvitationCode invitationCode, long currentTimeMillis) {
        if (invitationCode == null) {
            return false;
        }
        if (!isValidFormat(invitationCode.getCode())) {
            return false;
        }
        if (!invitationCode.isActive()) {
            return false;
        }
        return invitationCode.getExpiresAt() > currentTimeMillis;
    }
}
