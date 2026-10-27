package com.avabuilds.packetrunner.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test fun normalEmailsPass() {
        assertNull(AuthValidation.email("ava@example.com"))
        assertNull(AuthValidation.email("  ava.b+game@cau.ac.kr "))
    }

    @Test fun brokenEmailsAreRefused() {
        listOf("", "   ", "ava", "ava@", "@example.com", "ava@example", "a b@example.com").forEach {
            assertNotNull("\"$it\"", AuthValidation.email(it))
        }
    }

    @Test fun passwordsNeedSixCharacters() {
        assertNotNull(AuthValidation.password("12345"))
        assertNull(AuthValidation.password("123456"))
    }

    @Test fun nicknamesAreTwoToSixteenLettersAfterTrimming() {
        assertNotNull(AuthValidation.nickname("A"))
        assertNotNull(AuthValidation.nickname("  A  "))
        assertNull(AuthValidation.nickname("Ava"))
        assertNull(AuthValidation.nickname("a".repeat(16)))
        assertNotNull(AuthValidation.nickname("a".repeat(17)))
    }

    @Test fun everyKnownFirebaseCodeHasItsOwnFriendlyMessage() {
        val codes = listOf(
            "ERROR_INVALID_EMAIL", "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_WEAK_PASSWORD", "ERROR_WRONG_PASSWORD",
            "ERROR_USER_DISABLED", "ERROR_TOO_MANY_REQUESTS", "ERROR_REQUIRES_RECENT_LOGIN", "NETWORK",
        )
        val generic = friendlyAuthError("SOMETHING_NEW")
        codes.forEach { assertNotEquals(it, generic, friendlyAuthError(it)) }
    }

    @Test fun wrongPasswordAndUnknownUserLookTheSameSoNobodyCanProbeForAccounts() {
        assertEquals(friendlyAuthError("ERROR_WRONG_PASSWORD"), friendlyAuthError("ERROR_USER_NOT_FOUND"))
        assertEquals(friendlyAuthError("ERROR_WRONG_PASSWORD"), friendlyAuthError("ERROR_INVALID_CREDENTIAL"))
    }

    @Test fun unknownCodesGetAGenericMessage() {
        assertEquals("Something went wrong. Please try again.", friendlyAuthError(null))
    }
}
