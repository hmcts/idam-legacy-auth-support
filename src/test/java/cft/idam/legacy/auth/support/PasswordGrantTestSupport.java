package cft.idam.legacy.auth.support;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(classes = PasswordGrantTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
            "idam.legacy.password-grant.registration-reference=selected-client",
            "idam.legacy.password-grant.service-account.email-address=service+test@example.test",
            "idam.legacy.password-grant.service-account.password=p&ss=word+%",
            "idam.legacy.password-grant.endpoint-regex=/protected/.*"
        })
class PasswordGrantTestSupport {

    protected static final String TOKEN_URI = "https://idam.example.test/token";
    protected static final String REGISTRATION = "selected-client";
    protected static final String CLIENT_ID = "client+id";

    @Autowired
    @Qualifier("defaultPasswordGrantInterceptor")
    protected RequestInterceptor interceptor;

    @Autowired
    protected MockRestServiceServer server;

    @Autowired
    protected OAuth2AuthorizedClientService clients;

    @Autowired
    protected ClientRegistrationRepository registrations;

    @BeforeEach
    void resetState() {
        server.reset();
        clients.removeAuthorizedClient(REGISTRATION, CLIENT_ID);
    }

    @AfterEach
    void verifyTokenRequests() {
        server.verify();
    }

    protected void expectPasswordGrant(String token) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.setAll(Map.of("grant_type", "password", "username", "service+test@example.test",
                "password", "p&ss=word+%", "client_id", CLIENT_ID, "client_secret", "secret&value",
                "scope", "openid profile"));
        server.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(form)).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(tokenResponse(token), MediaType.APPLICATION_JSON));
    }

    protected ResponseActions expectRefresh() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.setAll(Map.of("grant_type", "refresh_token", "refresh_token", "refresh-token",
                "client_id", CLIENT_ID, "client_secret", "secret&value"));
        return server.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(form));
    }

    protected String tokenResponse(String token) {
        return "{\"access_token\":\"" + token + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
    }

    protected RequestTemplate apply(String url) {
        RequestTemplate request = new RequestTemplate().uri(url);
        interceptor.apply(request);
        return request;
    }

    protected void saveToken(Instant expiresAt, OAuth2RefreshToken refreshToken) {
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "old-token",
                Instant.now().minusSeconds(3600), expiresAt, Set.of("openid", "profile"));
        clients.saveAuthorizedClient(new OAuth2AuthorizedClient(registrations.findByRegistrationId(REGISTRATION),
                CLIENT_ID, token, refreshToken), new ClientPrincipal(CLIENT_ID));
    }

}
