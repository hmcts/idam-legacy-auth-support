package cft.idam.legacy.auth.support;

import cft.idam.legacy.auth.support.config.DefaultPasswordGrantAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PasswordGrantAuthorizedClientProviderTest {

    private final RestClient.Builder builder =
            new DefaultPasswordGrantAutoConfiguration().legacyPasswordGrantRestClient().mutate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PasswordGrantAuthorizedClientProvider provider =
            new PasswordGrantAuthorizedClientProvider(builder.build());

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void supportsEncodedBasicAuthentication() {
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                "client%2Bid:secret%26value".getBytes(StandardCharsets.UTF_8));
        server.expect(requestTo("https://idam.example.test/token"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic))
                .andExpect(content().formData(passwordForm()))
                .andRespond(withSuccess(tokenResponse(), MediaType.APPLICATION_JSON));
        assertThat(provider.authorize(context(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)))
                .as("Basic authentication should exchange credentials without adding client secrets to the form")
                .isNotNull();
    }

    @Test
    void supportsPublicClients() {
        MultiValueMap<String, String> form = passwordForm();
        form.set("client_id", "client+id");
        server.expect(requestTo("https://idam.example.test/token"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(content().formData(form))
                .andRespond(withSuccess(tokenResponse(), MediaType.APPLICATION_JSON));
        assertThat(provider.authorize(context(ClientAuthenticationMethod.NONE)))
                .as("Public clients should send their client ID without a client secret")
                .isNotNull();
    }

    @Test
    void rejectsUnsupportedClientAuthenticationBeforeSendingCredentials() {
        assertThatThrownBy(() -> provider.authorize(context(ClientAuthenticationMethod.PRIVATE_KEY_JWT)))
                .as("Unsupported client authentication must fail before making an HTTP request")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client_secret_basic, client_secret_post and none");
    }

    @Test
    void ignoresOtherGrantTypes() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("other")
                .clientId("client").authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("https://idam.example.test/token").build();
        OAuth2AuthorizationContext context = OAuth2AuthorizationContext.withClientRegistration(registration)
                .principal(new ClientPrincipal("client")).build();
        assertThat(provider.authorize(context)).as("The legacy provider must not handle other grant types").isNull();
    }

    @Test
    void requiresServiceAccountCredentials() {
        OAuth2AuthorizationContext context = OAuth2AuthorizationContext
                .withClientRegistration(context(ClientAuthenticationMethod.NONE).getClientRegistration())
                .principal(new ClientPrincipal("client+id")).build();
        assertThat(provider.authorize(context)).as("Missing credentials should not produce a token request").isNull();
    }

    private OAuth2AuthorizationContext context(ClientAuthenticationMethod method) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("test")
                .clientId("client+id").clientSecret("secret&value")
                .clientAuthenticationMethod(method).authorizationGrantType(new AuthorizationGrantType("password"))
                .tokenUri("https://idam.example.test/token").build();
        return OAuth2AuthorizationContext.withClientRegistration(registration)
                .principal(new ClientPrincipal("client+id"))
                .attributes(attributes -> attributes.putAll(Map.of(
                        PasswordGrantAuthorizedClientProvider.USERNAME, "service@example.test",
                        PasswordGrantAuthorizedClientProvider.PASSWORD, "password")))
                .build();
    }

    private MultiValueMap<String, String> passwordForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.setAll(Map.of("grant_type", "password", "username", "service@example.test", "password", "password"));
        return form;
    }

    private String tokenResponse() {
        return "{\"access_token\":\"token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
    }
}
