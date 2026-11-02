package com.example.mealsync.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InvitationCodeGeneratorTest {

    @Test
    public void testGenerateCodeFormat() {
        String code = InvitationCodeGenerator.generateCode();
        assertNotNull(code);
        assertTrue("Code should be exactly 6 digits, got: " + code, code.matches("\\d{6}"));
    }

    @Test
    public void testMultipleGeneratedCodes() {
        for (int i = 0; i < 100; i++) {
            String code = InvitationCodeGenerator.generateCode();
            assertNotNull(code);
            assertTrue("Code should match 6 digits: " + code, code.matches("\\d{6}"));
        }
    }
}
