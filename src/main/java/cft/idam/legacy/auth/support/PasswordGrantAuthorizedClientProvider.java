package cft.idam.legacy.auth.support;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestHeadersConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

/**
 * Provides the legacy password grant removed from Spring Security 7.
 * Valid tokens remain cached by Spring's authorized-client manager; expired tokens
 * with a refresh token are delegated to Spring's refresh-token provider.
 */
public final class PasswordGrantAuthorizedClientProvider implements OAuth2AuthorizedClientProvider {

    /** Username attribute passed from the interceptor to the authorization context. */
    public static final String USERNAME = "username";

    /** Password attribute passed from the interceptor to the authorization context. */
    public static final String PASSWORD = "password";

    private static final AuthorizationGrantType PASSWORD_GRANT = new AuthorizationGrantType("password");

    private final RestClient restClient;

    /**
     * Creates a provider using an HTTP client configured for OAuth token responses.
     * @param restClient client with form and OAuth response converters and OAuth error handling
     */
    public PasswordGrantAuthorizedClientProvider(RestClient restClient) {
        Assert.notNull(restClient, "restClient must not be null");
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter") // Spring exposes authorization state through this context object graph.
    public OAuth2AuthorizedClient authorize(OAuth2AuthorizationContext context) {
        ClientRegistration registration = context.getClientRegistration();
        if (!PASSWORD_GRANT.equals(registration.getAuthorizationGrantType())) {
            return null;
        }
        OAuth2AuthorizedClient existing = context.getAuthorizedClient();
        if (existing != null && (existing.getRefreshToken() != null
                || !Instant.now().isAfter(existing.getAccessToken().getExpiresAt().minusSeconds(60)))) {
            return null;
        }
        String username = context.getAttribute(USERNAME);
        String password = context.getAttribute(PASSWORD);
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return null;
        }
        validateAuthenticationMethod(registration);
        OAuth2AccessTokenResponse response = requestToken(registration, username, password);
        return new OAuth2AuthorizedClient(registration, context.getPrincipal().getName(),
                response.getAccessToken(), response.getRefreshToken());
    }

    @SuppressWarnings("PMD.LawOfDemeter") // Inspect the token response returned by Spring's HTTP converter.
    private OAuth2AccessTokenResponse requestToken(ClientRegistration registration, String username, String password) {
        PasswordGrantRequest request = new PasswordGrantRequest(registration);
        HttpHeaders headers = new DefaultOAuth2TokenRequestHeadersConverter<PasswordGrantRequest>().convert(request);
        MultiValueMap<String, String> parameters = passwordParameters(registration, username, password);
        OAuth2AccessTokenResponse response;
        try {
            response = restClient.post()
                    .uri(registration.getProviderDetails().getTokenUri())
                    .headers(target -> target.addAll(headers))
                    .body(parameters)
                    .retrieve()
                    .body(OAuth2AccessTokenResponse.class);
        } catch (OAuth2AuthorizationException exception) {
            throw new ClientAuthorizationException(exception.getError(), registration.getRegistrationId(), exception);
        } catch (RestClientException exception) {
            throw new ClientAuthorizationException(new OAuth2Error("invalid_token_response",
                    "Unable to retrieve the password grant token response", null),
                    registration.getRegistrationId(), exception);
        }
        if (response == null) {
            throw new ClientAuthorizationException(new OAuth2Error("invalid_token_response"),
                    registration.getRegistrationId());
        }
        // OAuth permits the server to omit scope when it matches the requested scope.
        if (response.getAccessToken().getScopes().isEmpty() && !CollectionUtils.isEmpty(registration.getScopes())) {
            return OAuth2AccessTokenResponse.withResponse(response).scopes(registration.getScopes()).build();
        }
        return response;
    }

    private MultiValueMap<String, String> passwordParameters(
            ClientRegistration registration, String username, String password) {
        PasswordGrantRequest request = new PasswordGrantRequest(registration);
        MultiValueMap<String, String> parameters =
                new DefaultOAuth2TokenRequestParametersConverter<PasswordGrantRequest>().convert(request);
        parameters.set(USERNAME, username);
        parameters.set(PASSWORD, password);
        if (!CollectionUtils.isEmpty(registration.getScopes())) {
            parameters.set(OAuth2ParameterNames.SCOPE, String.join(" ", registration.getScopes()));
        }
        return parameters;
    }

    private void validateAuthenticationMethod(ClientRegistration registration) {
        ClientAuthenticationMethod method = registration.getClientAuthenticationMethod();
        Assert.isTrue(ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(method)
                        || ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(method)
                        || ClientAuthenticationMethod.NONE.equals(method),
                "Password grants support client_secret_basic, client_secret_post and none authentication");
    }

    private static final class PasswordGrantRequest extends AbstractOAuth2AuthorizationGrantRequest {

        private PasswordGrantRequest(ClientRegistration registration) {
            super(PASSWORD_GRANT, registration);
        }
    }
}
