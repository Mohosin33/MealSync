package com.example.mealsync.util;

import com.example.mealsync.data.models.InvitationCode;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvitationCodeValidatorTest {

    @Test
    public void testValidFormat() {
        assertTrue(InvitationCodeValidator.isValidFormat("123456"));
        assertTrue(InvitationCodeValidator.isValidFormat("000000"));
        assertTrue(InvitationCodeValidator.isValidFormat("999999"));

        assertFalse(InvitationCodeValidator.isValidFormat(null));
        assertFalse(InvitationCodeValidator.isValidFormat(""));
        assertFalse(InvitationCodeValidator.isValidFormat("12345"));
        assertFalse(InvitationCodeValidator.isValidFormat("1234567"));
        assertFalse(InvitationCodeValidator.isValidFormat("abcdef"));
    }

    @Test
    public void testValidAndActiveCode() {
        long now = System.currentTimeMillis();
        long future = now + 86400000L;
        InvitationCode code = new InvitationCode("123456", "mess_1", "user_1", now, future, true);

        assertTrue(InvitationCodeValidator.isValidAndActive(code, now));
    }

    @Test
    public void testInactiveCode() {
        long now = System.currentTimeMillis();
        long future = now + 86400000L;
        InvitationCode code = new InvitationCode("123456", "mess_1", "user_1", now, future, false);

        assertFalse(InvitationCodeValidator.isValidAndActive(code, now));
    }

    @Test
    public void testExpiredCode() {
        long now = System.currentTimeMillis();
        long past = now - 1000L;
        InvitationCode code = new InvitationCode("123456", "mess_1", "user_1", now - 86400000L, past, true);

        assertFalse(InvitationCodeValidator.isValidAndActive(code, now));
    }
}
