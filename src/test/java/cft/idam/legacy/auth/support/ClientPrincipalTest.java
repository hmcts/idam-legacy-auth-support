package cft.idam.legacy.auth.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;


class ClientPrincipalTest {

    @Test
    void testGetName() {
        ClientPrincipal underTest = new ClientPrincipal("test-client-id");
        assertEquals("test-client-id", underTest.getName());
    }

    @Test
    void testOthers() {
        ClientPrincipal underTest = new ClientPrincipal("test-client-id");
        assertNull(underTest.getPrincipal());
        assertNull(underTest.getCredentials());
        assertNull(underTest.getDetails());
        assertEquals(0, underTest.getAuthorities().size());
        assertFalse(underTest.isAuthenticated());
    }
}