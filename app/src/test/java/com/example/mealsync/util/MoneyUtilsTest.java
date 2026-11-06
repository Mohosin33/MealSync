package com.example.mealsync.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MoneyUtilsTest {

    @Test
    public void testParseTakaToPaisaValid() {
        assertEquals(1200L, MoneyUtils.parseTakaToPaisa("12"));
        assertEquals(1250L, MoneyUtils.parseTakaToPaisa("12.5"));
        assertEquals(1250L, MoneyUtils.parseTakaToPaisa("12.50"));
        assertEquals(100L, MoneyUtils.parseTakaToPaisa("1.00"));
        assertEquals(50L, MoneyUtils.parseTakaToPaisa("0.50"));
        assertEquals(100000000L, MoneyUtils.parseTakaToPaisa("1000000"));
        assertEquals(100000000L, MoneyUtils.parseTakaToPaisa("1000000.00"));
    }

    @Test
    public void testParseTakaToPaisaRejectsNegatives() {
        try {
            MoneyUtils.parseTakaToPaisa("-12");
            fail("Should reject negative amount");
        } catch (IllegalArgumentException expected) {
            assertEquals("Amount cannot be negative", expected.getMessage());
        }
    }

    @Test
    public void testParseTakaToPaisaRejectsZero() {
        try {
            MoneyUtils.parseTakaToPaisa("0");
            fail("Should reject zero");
        } catch (IllegalArgumentException expected) {
            assertEquals("Amount must be greater than zero", expected.getMessage());
        }
        try {
            MoneyUtils.parseTakaToPaisa("0.00");
            fail("Should reject zero decimal");
        } catch (IllegalArgumentException expected) {
            assertEquals("Amount must be greater than zero", expected.getMessage());
        }
    }

    @Test
    public void testParseTakaToPaisaPromptCases() {
        String[] invalidInputs = new String[]{"0", "-5", "abc", "12.345", "", "1000001"};
        for (String input : invalidInputs) {
            try {
                MoneyUtils.parseTakaToPaisa(input);
                fail("Should reject invalid input: " + input);
            } catch (IllegalArgumentException expected) {
            }
        }

        assertEquals(1200L, MoneyUtils.parseTakaToPaisa("12"));
        assertEquals(1250L, MoneyUtils.parseTakaToPaisa("12.5"));
        assertEquals(1250L, MoneyUtils.parseTakaToPaisa("12.50"));

        assertEquals("৳1,234.50", MoneyUtils.formatPaisa(123450L));
        assertEquals("-৳1,234.50", MoneyUtils.formatPaisa(-123450L));
    }

    @Test
    public void testParseTakaToPaisaRejectsMoreThanTwoDecimals() {
        try {
            MoneyUtils.parseTakaToPaisa("12.505");
            fail("Should reject 3 decimals");
        } catch (IllegalArgumentException expected) {
            assertEquals("More than 2 decimal places not allowed", expected.getMessage());
        }
    }

    @Test
    public void testParseTakaToPaisaRejectsAboveOneMillion() {
        try {
            MoneyUtils.parseTakaToPaisa("1000000.01");
            fail("Should reject above 1,000,000 taka");
        } catch (IllegalArgumentException expected) {
            assertEquals("Amount exceeds maximum allowed (1,000,000 Taka)", expected.getMessage());
        }
    }

    @Test
    public void testFormatPaisa() {
        assertEquals("৳1,234.50", MoneyUtils.formatPaisa(123450L));
        assertEquals("-৳1,234.50", MoneyUtils.formatPaisa(-123450L));
        assertEquals("৳0.00", MoneyUtils.formatPaisa(0L));
        assertEquals("৳0.50", MoneyUtils.formatPaisa(50L));
        assertEquals("-৳0.50", MoneyUtils.formatPaisa(-50L));
        assertEquals("৳100.00", MoneyUtils.formatPaisa(10000L));
    }
}
