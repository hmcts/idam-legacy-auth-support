package cft.idam.legacy.auth.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;


class ClientPrincipalTest {

    @Test
    void testGetName() {
        ClientPrincipal underTest = new ClientPrincipal("test-client-id");
        assertEquals("test-client-id", underTest.getName(), "The principal name should be the client ID");
    }

    @Test
    void testOthers() {
        ClientPrincipal underTest = new ClientPrincipal("test-client-id");
        assertNull(underTest.getPrincipal(), "A client principal should not expose principal");
        assertNull(underTest.getCredentials(), "A client principal should not expose credentials");
        assertNull(underTest.getDetails(), "A client principal should not expose details");
        assertEquals(0, underTest.getAuthorities().size(), "A client principal should have no authorities");
        assertFalse(underTest.isAuthenticated(), "A client principal should not be authenticated");
    }
}
