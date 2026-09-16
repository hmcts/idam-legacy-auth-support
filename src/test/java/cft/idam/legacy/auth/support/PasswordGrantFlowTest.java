package cft.idam.legacy.auth.support;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordGrantFlowTest extends PasswordGrantTestSupport {

    @Test
    void exchangesConfiguredCredentialsForBearerToken() {
        expectPasswordGrant("access-token");
        assertThat(apply("/protected/cases").headers()).as("The configured password grant should authorize Feign")
                .containsEntry("Authorization", List.of("Bearer access-token"));
    }

    @Test
    void preservesRequestedScopesWhenResponseOmitsThem() {
        expectPasswordGrant("access-token");
        apply("/protected/cases");
        OAuth2AuthorizedClient saved = clients.loadAuthorizedClient(REGISTRATION, CLIENT_ID);
        assertThat(saved.getAccessToken().getScopes()).as("An omitted response scope means the requested scopes")
                .containsExactlyInAnyOrder("openid", "profile");
    }

    @Test
    void reusesTokenAcrossRequests() {
        expectPasswordGrant("cached-token");
        apply("/protected/first");
        assertThat(apply("/protected/second").headers()).as("A second call should reuse the saved token")
                .containsEntry("Authorization", List.of("Bearer cached-token"));
    }

    @Test
    void renewsExpiredTokenWithoutRefreshToken() {
        saveToken(Instant.now().minusSeconds(10), null);
        expectPasswordGrant("renewed-token");
        assertThat(apply("/protected/cases").headers()).as("An expired token needs a new password grant")
                .containsEntry("Authorization", List.of("Bearer renewed-token"));
    }

    @Test
    void renewsTokenWithinClockSkew() {
        saveToken(Instant.now().plusSeconds(30), null);
        expectPasswordGrant("renewed-token");
        assertThat(apply("/protected/cases").headers()).as("Tokens within the 60-second expiry margin need renewal")
                .containsEntry("Authorization", List.of("Bearer renewed-token"));
    }

    @Test
    void ignoresUnmatchedRequests() {
        assertThat(apply("/public/cases").headers()).as("Unmatched requests must not fetch or attach credentials")
                .doesNotContainKey("Authorization");
    }

}
