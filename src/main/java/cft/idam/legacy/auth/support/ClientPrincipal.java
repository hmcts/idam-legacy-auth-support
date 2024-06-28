package cft.idam.legacy.auth.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

/**
 * An authentication principal that holds client id. Used for spring security auth calls.
 */
public class ClientPrincipal implements Authentication {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Service client id.
     */
    private final String clientId;

    /**
     * Constructor.
     * @param clientId service client id.
     */
    public ClientPrincipal(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptySet();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {

    }

    @Override
    public String getName() {
        return clientId;
    }
}
