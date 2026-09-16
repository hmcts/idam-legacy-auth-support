package cft.idam.legacy.auth.support;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.ClientAuthorizationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PasswordGrantFailureTest extends PasswordGrantTestSupport {

    @Test
    void rejectsInvalidCredentials() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withBadRequest()
                .body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> apply("/protected/cases")).as("Rejected credentials should fail authorization")
                .isInstanceOf(ClientAuthorizationException.class).hasMessageContaining("invalid_grant")
                .hasMessageNotContaining("p&ss=word+%");
    }

    @Test
    void failedGrantDoesNotAddAuthorizationHeader() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withBadRequest()
                .body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));
        RequestTemplate request = new RequestTemplate().uri("/protected/cases");
        try {
            interceptor.apply(request);
        } catch (ClientAuthorizationException expected) {
            // The separate rejection test verifies the exception; this verifies the request remains unauthenticated.
        }
        assertThat(request.headers()).as("Failed authentication must not attach a bearer token")
                .doesNotContainKey("Authorization");
    }

    @Test
    void rejectsMalformedTokenResponse() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> apply("/protected/cases")).as("Malformed responses should fail authorization")
                .isInstanceOf(ClientAuthorizationException.class).hasMessageContaining("invalid_token_response");
    }

    @Test
    void rejectsEmptyTokenResponse() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> apply("/protected/cases")).as("An empty response cannot authorize a request")
                .isInstanceOf(ClientAuthorizationException.class).hasMessageContaining("invalid_token_response");
    }

    @Test
    void rejectsResponseWithoutAccessToken() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess("{\"token_type\":\"Bearer\"}",
                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> apply("/protected/cases")).as("A success response still needs an access token")
                .isInstanceOf(ClientAuthorizationException.class).hasMessageContaining("invalid_token_response");
    }

}
