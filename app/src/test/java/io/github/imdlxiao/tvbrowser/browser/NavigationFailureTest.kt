/* Author: imdlxiao */
package io.github.imdlxiao.tvbrowser.browser
import org.junit.Assert.*
import org.junit.Test

class NavigationFailureTest {
    @Test fun preservesRealErrorAndExplainsLocalResolutionWithoutClaimingItsCause() {
        val error = NavigationFailure.web(-2, "net::ERR_NAME_NOT_RESOLVED", "http://Abyss.local:8765/login.html?token=secret")
        assertTrue(error.display().contains("ERROR_HOST_LOOKUP (-2)"))
        assertTrue(error.display().contains("net::ERR_NAME_NOT_RESOLVED"))
        assertTrue(error.advice.contains("可能"))
        assertFalse(error.display().contains("secret"))
    }
    @Test fun distinguishesTransportAndCleartextErrors() {
        assertTrue(NavigationFailure.web(-6,"net::ERR_CONNECTION_REFUSED","http://192.168.1.2:8765").code.contains("ERROR_CONNECT"))
        assertTrue(NavigationFailure.web(-1,"net::ERR_CLEARTEXT_NOT_PERMITTED","http://192.168.1.2:8765").code.contains("CLEARTEXT_BLOCKED"))
        assertEquals("http://Abyss.local:8765/", AddressResolver.resolve("Abyss.local:8765/"))
        assertEquals("http://192.168.5.19:8765/", AddressResolver.resolve("192.168.5.19:8765/"))
    }
}
