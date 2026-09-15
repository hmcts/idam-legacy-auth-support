package cft.idam.legacy.auth.support;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PasswordGrantRefreshTest extends PasswordGrantTestSupport {

    @Test
    void reusesValidTokenWithoutRefreshing() {
        saveToken(Instant.now().plusSeconds(3600), new OAuth2RefreshToken("refresh-token", Instant.now()));
        assertThat(apply("/protected/cases").headers()).as("A valid token should not trigger a refresh")
                .containsEntry("Authorization", List.of("Bearer old-token"));
    }

    @Test
    void obtainsNewPasswordGrantOnNextRequestAfterRejectedRefresh() {
        saveToken(Instant.now().minusSeconds(10), new OAuth2RefreshToken("refresh-token", Instant.now()));
        expectRefresh().andRespond(withBadRequest().body("{\"error\":\"invalid_grant\"}")
                .contentType(MediaType.APPLICATION_JSON));
        expectPasswordGrant("new-grant");
        assertThatThrownBy(() -> apply("/protected/first")).as("The rejected refresh should fail its request")
                .isInstanceOf(ClientAuthorizationException.class);
        assertThat(apply("/protected/next").headers()).as("The next request should acquire a fresh password grant")
                .containsEntry("Authorization", List.of("Bearer new-grant"));
    }

    @Test
    void refreshesExpiredToken() {
        saveToken(Instant.now().minusSeconds(10), new OAuth2RefreshToken("refresh-token", Instant.now()));
        expectRefresh().andRespond(withSuccess(tokenResponse("refreshed-token"), MediaType.APPLICATION_JSON));
        assertThat(apply("/protected/cases").headers()).as("An expired token with a refresh token should refresh")
                .containsEntry("Authorization", List.of("Bearer refreshed-token"));
    }

    @Test
    void reusesRefreshedToken() {
        saveToken(Instant.now().minusSeconds(10), new OAuth2RefreshToken("refresh-token", Instant.now()));
        expectRefresh().andRespond(withSuccess(tokenResponse("refreshed-token"), MediaType.APPLICATION_JSON));
        apply("/protected/first");
        assertThat(apply("/protected/second").headers()).as("The manager should persist the refreshed token")
                .containsEntry("Authorization", List.of("Bearer refreshed-token"));
    }

    @Test
    void retainsRefreshTokenWhenServerDoesNotRotateIt() {
        saveToken(Instant.now().minusSeconds(10), new OAuth2RefreshToken("refresh-token", Instant.now()));
        expectRefresh().andRespond(withSuccess(tokenResponse("refreshed-token"), MediaType.APPLICATION_JSON));
        apply("/protected/cases");
        OAuth2AuthorizedClient saved = clients.loadAuthorizedClient(REGISTRATION, CLIENT_ID);
        assertThat(saved.getRefreshToken().getTokenValue()).as("Refresh responses may omit an unchanged refresh token")
                .isEqualTo("refresh-token");
    }

    @Test
    void removesClientAfterRejectedRefresh() {
        saveToken(Instant.now().minusSeconds(10), new OAuth2RefreshToken("refresh-token", Instant.now()));
        expectRefresh().andRespond(withBadRequest().body("{\"error\":\"invalid_grant\"}")
                .contentType(MediaType.APPLICATION_JSON));
        try {
            apply("/protected/cases");
        } catch (ClientAuthorizationException expected) {
            // A rejected refresh must evict the cached client so a later request can obtain a fresh grant.
        }
        assertThat(clients.<OAuth2AuthorizedClient>loadAuthorizedClient(REGISTRATION, CLIENT_ID))
                .as("A rejected refresh should remove the stale client").isNull();
    }

}
