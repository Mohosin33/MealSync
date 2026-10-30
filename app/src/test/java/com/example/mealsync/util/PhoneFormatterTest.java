package com.example.mealsync.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PhoneFormatterTest {

    @Test
    public void testBangladeshLocalNumber() {
        assertEquals("+8801712345678", PhoneFormatter.toE164("01712345678"));
        assertEquals("+8801812345678", PhoneFormatter.toE164("018 1234-5678"));
    }

    @Test
    public void testInternationalNumber() {
        assertEquals("+16505553434", PhoneFormatter.toE164("+16505553434"));
        assertEquals("+8801712345678", PhoneFormatter.toE164("+8801712345678"));
        assertEquals("+442071234567", PhoneFormatter.toE164("00442071234567"));
    }

    @Test
    public void testInvalidNumber() {
        assertNull(PhoneFormatter.toE164("123"));
        assertNull(PhoneFormatter.toE164("abc"));
        assertNull(PhoneFormatter.toE164(""));
        assertNull(PhoneFormatter.toE164(null));
    }
}
